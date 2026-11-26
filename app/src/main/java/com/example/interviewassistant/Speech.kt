package com.example.interviewassistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import android.media.AudioAttributes
import android.util.Log

/** Text-to-speech: the AI interviewer reads each question aloud. If speech is unavailable, it just continues silently. */
class Speaker(context: Context) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingDone: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            val engine = tts
            if (status == TextToSpeech.SUCCESS && engine != null) {
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                val r = engine.setLanguage(Locale.US)
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("Speaker", "US English voice missing: install it in Text-to-speech settings")
                }
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        finish()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        finish()
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        finish()
                    }
                })
                ready = true
            } else {
                Log.e("Speaker", "TTS init failed, status=$status")
            }
        }
    }
    private fun finish() {
        main.postDelayed({
            val callback = pendingDone
            pendingDone = null
            callback?.invoke()
        }, 400)
    }

    /** Speaks [text]; calls [onDone] on the main thread when finished (or at once if speech is unavailable). */
    fun speak(text: String, onDone: () -> Unit) {
        val engine = tts
        // Settings > "Read questions aloud" off, or speech unavailable: carry on silently
        if (!ready || engine == null || !AppPrefs.readAloud(app)) {
            onDone()
            return
        }
        engine.setSpeechRate(AppPrefs.rate(app))
        pendingDone = onDone
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "question")
    }

    fun stop() {
        pendingDone = null
        tts?.stop()
    }

    fun shutdown() {
        pendingDone = null
        tts?.stop()
        tts?.shutdown()
    }
}