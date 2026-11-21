"use strict";

/********* External Imports ********/

const { stringToBuffer, bufferToString, encodeBuffer, decodeBuffer, getRandomBytes } = require("./lib");
const { subtle } = require("crypto").webcrypto;

/********* Constants ********/

const PBKDF2_ITERATIONS = 100000; // number of iterations for PBKDF2 algorithm
const MAX_PASSWORD_LENGTH = 64;   // we can assume no password is longer than this many bytes

/********* Implementation ********/
class Keychain {
  /**
   * Initializes the keychain using the provided information. Note that external
   * users should likely never invoke the constructor directly and instead use
   * either Keychain.init or Keychain.load.
   *
   * @param {object} kvs        key-value store: HMAC(domain) -> {iv, ct}
   * @param {string} salt       Base64 PBKDF2 salt (stored in the clear)
   * @param {object} check      encrypted known value used to verify the password
   * @param {CryptoKey} hmacKey sub-key used to MAC domain names
   * @param {CryptoKey} aesKey  sub-key used for AES-GCM encryption of passwords
   */
  constructor(kvs, salt, check, hmacKey, aesKey) {
    this.data = { kvs, salt, check };
    this.secrets = { hmacKey, aesKey };
  }

  /**
   * Derives the two sub-keys from the password and salt.
   * PBKDF2 is called exactly once -> master key k.
   * HMAC(k, label) is then used as a PRF to get the two sub-keys.
   */
  static async deriveKeys(password, saltBuf) {
    const rawKey = await subtle.importKey(
      "raw", stringToBuffer(password), "PBKDF2", false, ["deriveKey"]);

    const masterKey = await subtle.deriveKey(
      { name: "PBKDF2", salt: saltBuf, iterations: PBKDF2_ITERATIONS, hash: "SHA-256" },
      rawKey,
      { name: "HMAC", hash: "SHA-256", length: 256 },
      false,
      ["sign"]
    );

    const hmacMaterial = await subtle.sign("HMAC", masterKey, stringToBuffer("domain-mac-key"));
    const aesMaterial = await subtle.sign("HMAC", masterKey, stringToBuffer("password-encryption-key"));

    const hmacKey = await subtle.importKey(
      "raw", hmacMaterial, { name: "HMAC", hash: "SHA-256" }, false, ["sign", "verify"]);
    const aesKey = await subtle.importKey(
      "raw", aesMaterial, "AES-GCM", false, ["encrypt", "decrypt"]);

    return { hmacKey, aesKey };
  }

  /** AES-GCM encrypt `plainBuf`, binding it to `aad`. Returns {iv, ct} (Base64). */
  static async encrypt(aesKey, plainBuf, aad) {
    const iv = getRandomBytes(12);
    const ct = await subtle.encrypt(
      { name: "AES-GCM", iv, additionalData: aad }, aesKey, plainBuf);
    return { iv: encodeBuffer(iv), ct: encodeBuffer(ct) };
  }

  /** AES-GCM decrypt a {iv, ct} record. Throws if anything was tampered with. */
  static async decrypt(aesKey, record, aad) {
    try {
      return await subtle.decrypt(
        { name: "AES-GCM", iv: decodeBuffer(record.iv), additionalData: aad },
        aesKey, decodeBuffer(record.ct));
    } catch (e) {
      throw "Tampering detected: decryption failed";
    }
  }

  /**
   * Creates an empty keychain with the given master password.
   *
   * @param {string} password
   * @returns {Keychain}
   */
  static async init(password) {
    const saltBuf = getRandomBytes(16); // 128-bit salt
    const { hmacKey, aesKey } = await Keychain.deriveKeys(password, saltBuf);

    // Encrypted known value: lets load() detect a wrong master password.
    const check = await Keychain.encrypt(
      aesKey, stringToBuffer("keychain-password-check"), stringToBuffer("check"));

    return new Keychain({}, encodeBuffer(saltBuf), check, hmacKey, aesKey);
  }

  /**
   * Loads the keychain state from the provided representation (repr). The
   * trustedDataCheck is an *optional* SHA-256 checksum that, if provided, is
   * used to confirm the integrity (rollback protection) of the serialized data.
   *
   * @param {string} password
   * @param {string} repr
   * @param {string} trustedDataCheck
   * @returns {Keychain}
   */
  static async load(password, repr, trustedDataCheck) {
    // Rollback defense: compare SHA-256 of the serialization to the trusted hash.
    if (trustedDataCheck !== undefined) {
      const hash = encodeBuffer(await subtle.digest("SHA-256", stringToBuffer(repr)));
      if (hash !== trustedDataCheck) {
        throw "Tampering detected: checksum mismatch";
      }
    }

    const parsed = JSON.parse(repr);
    const { hmacKey, aesKey } = await Keychain.deriveKeys(password, decodeBuffer(parsed.salt));

    // Verify the master password.
    let plain;
    try {
      plain = await subtle.decrypt(
        { name: "AES-GCM", iv: decodeBuffer(parsed.check.iv), additionalData: stringToBuffer("check") },
        aesKey, decodeBuffer(parsed.check.ct));
    } catch (e) {
      throw "Invalid password (or data was tampered with)";
    }
    if (bufferToString(plain) !== "keychain-password-check") {
      throw "Invalid password";
    }

    return new Keychain(parsed.kvs, parsed.salt, parsed.check, hmacKey, aesKey);
  }

  /**
   * Returns a JSON serialization of the keychain and a SHA-256 hash of it.
   * The hash must be stored in trusted storage.
   *
   * @returns {Array} [json string, Base64 SHA-256 hash]
   */
  async dump() {
    const json = JSON.stringify(this.data);
    const hash = encodeBuffer(await subtle.digest("SHA-256", stringToBuffer(json)));
    return [json, hash];
  }

  /** Base64(HMAC(domain)) -- the lookup key in the KVS. */
  async _key(name) {
    return encodeBuffer(await subtle.sign("HMAC", this.secrets.hmacKey, stringToBuffer(name)));
  }

  /**
   * Fetches the password for `name`, or null if not present.
   * Throws if the record was tampered with / swapped.
   */
  async get(name) {
    const key = await this._key(name);
    const record = this.data.kvs[key];
    if (record === undefined) return null;

    // The key (HMAC of the domain) is used as associated data -> swap defense.
    const padded = new Uint8Array(
      await Keychain.decrypt(this.secrets.aesKey, record, stringToBuffer(key)));

    const len = padded[0];
    return bufferToString(padded.slice(1, 1 + len));
  }

  /**
   * Inserts or updates the password for `name`.
   */
  async set(name, value) {
    const valueBuf = new Uint8Array(stringToBuffer(value));
    if (valueBuf.length > MAX_PASSWORD_LENGTH) {
      throw "Password too long";
    }

    // Fixed-size plaintext: 1 length byte + 64 bytes (zero padded) -> hides length.
    const padded = new Uint8Array(1 + MAX_PASSWORD_LENGTH);
    padded[0] = valueBuf.length;
    padded.set(valueBuf, 1);

    const key = await this._key(name);
    this.data.kvs[key] = await Keychain.encrypt(
      this.secrets.aesKey, padded, stringToBuffer(key));
  }

  /**
   * Removes `name`. Returns true if it existed, false otherwise.
   */
  async remove(name) {
    const key = await this._key(name);
    if (this.data.kvs[key] === undefined) return false;
    delete this.data.kvs[key];
    return true;
  }
}

module.exports = { Keychain };
