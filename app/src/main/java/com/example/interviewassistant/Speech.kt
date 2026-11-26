package com.example.interviewassistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

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
                engine.language = Locale.US
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
            }
        }
    }

    private fun finish() {
        main.post {
            val callback = pendingDone
            pendingDone = null
            callback?.invoke()
        }
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