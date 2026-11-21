package com.example.interviewassistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

enum class Screen { SIGN_IN, SIGN_UP, WELCOME, CAREER, CV, READY, LIVE, REVIEW }

/** The design shows "5 questions" per career and "Q1/5" in the live interview. */
const val QUESTIONS_PER_INTERVIEW = 5

/** Everything the screens show. Network calls run on a background thread; state is updated on the main thread. */
class InterviewState {
    var screen by mutableStateOf(Screen.SIGN_IN)
    var baseUrl by mutableStateOf(Api.baseUrl)
    var careers by mutableStateOf<List<Career>>(emptyList())
    var selectedCareer by mutableStateOf<String?>(null)
    var cvName by mutableStateOf<String?>(null)

    var sessionId by mutableStateOf("")
    var career by mutableStateOf("")
    var question by mutableStateOf<Question?>(null)
    var answerText by mutableStateOf("")
    var lastNote by mutableStateOf<String?>(null)
    var report by mutableStateOf<Report?>(null)

    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private suspend fun <T> guarded(block: () -> T): T? {
        loading = true
        error = null
        return try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            error = e.message
            null
        } catch (e: IOException) {
            error = "Cannot reach the server at ${Api.baseUrl}. Is app.py running and the address correct?"
            null
        } catch (e: Exception) {
            error = e.message ?: "Something went wrong."
            null
        } finally {
            loading = false
        }
    }

    // ---- navigation ------------------------------------------------------------------
    fun go(target: Screen) {
        error = null
        screen = target
    }

    val canGoBack: Boolean
        get() = screen != Screen.SIGN_IN && screen != Screen.WELCOME

    fun back() {
        when (screen) {
            Screen.SIGN_UP -> go(Screen.SIGN_IN)
            Screen.CAREER -> go(Screen.WELCOME)
            Screen.CV -> go(Screen.CAREER)
            Screen.READY -> go(Screen.CV)
            Screen.LIVE -> go(Screen.READY)
            Screen.REVIEW -> go(Screen.WELCOME)
            else -> Unit
        }
    }

    fun setServer(url: String) {
        baseUrl = url
        Api.baseUrl = url.trim()
        careers = emptyList()
    }

    // ---- API actions -----------------------------------------------------------------
    suspend fun loadCareers() {
        guarded { Api.careers() }?.let { careers = it }
    }

    suspend fun start(name: String) {
        guarded { Api.start(name, QUESTIONS_PER_INTERVIEW) }?.let { (id, q) ->
            sessionId = id
            career = name
            question = q
            answerText = ""
            lastNote = null
            report = null
            screen = Screen.LIVE
        }
    }

    suspend fun submit(text: String) {
        val id = sessionId
        val r = guarded { Api.answer(id, text) } ?: return
        answerText = ""
        lastNote = "LAST ANSWER ${r.result.score}/100 · NEXT LEVEL ${r.currentDifficulty.uppercase()}"
        if (r.finished || r.nextQuestion == null) {
            loadReport()
        } else {
            question = r.nextQuestion
        }
    }

    suspend fun endEarly() {
        val id = sessionId
        guarded { Api.end(id) }
        if (error == null) loadReport()
    }

    suspend fun loadReport() {
        val id = sessionId
        guarded { Api.feedback(id) }?.let {
            report = it
            screen = Screen.REVIEW
        }
    }
}
