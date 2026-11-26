package com.example.interviewassistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** The screens the flow can move to. MainActivity maps each one to a Fragment destination. */
enum class Screen { SIGN_IN, SIGN_UP, WELCOME, CAREER, CV, READY, LIVE, REVIEW }

/** The design shows "5 questions" per career and "Q1/5" in the live interview. */
const val QUESTIONS_PER_INTERVIEW = 5

/**
 * Everything the screens show. It is a ViewModel shared by all Fragments (activityViewModels),
 * so the interview survives screen changes and rotation.
 * Network calls run on a background thread; state is updated on the main thread.
 */
class InterviewState : ViewModel() {
    /** Set by MainActivity: moves to a screen / goes back using the Navigation component. */
    var onNavigate: ((Screen) -> Unit)? = null
    var onBack: (() -> Unit)? = null

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

    // navigation (handled by MainActivity / NavController)
    fun go(target: Screen) {
        error = null
        onNavigate?.invoke(target)
    }

    fun back() {
        onBack?.invoke()
    }

    fun setServer(url: String) {
        baseUrl = url
        Api.baseUrl = url.trim()
        careers = emptyList()
    }

    // Firebase accounts
    fun signIn(email: String, password: String) {
        if (loading) return
        loading = true
        error = null
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                loading = false
                go(Screen.WELCOME)
            }
            .addOnFailureListener {
                loading = false
                error = signInError(it)
            }
    }

    fun signUp(name: String, email: String, password: String) {
        if (loading) return
        loading = true
        error = null
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Repo.saveProfile(name) {
                    loading = false
                    go(Screen.WELCOME)
                }
            }
            .addOnFailureListener {
                loading = false
                error = signUpError(it)
            }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        resetSession()
        go(Screen.SIGN_IN)
    }

    /** Forget everything from the previous user */
    private fun resetSession() {
        selectedCareer = null
        cvName = null
        sessionId = ""
        career = ""
        question = null
        answerText = ""
        lastNote = null
        report = null
        error = null
    }

    // API actions
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
            go(Screen.LIVE)
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
            // keep the finished interview in Firestore so it shows up in "Interview history"
            if (it.questions.isNotEmpty()) Repo.saveInterview(it) { }
            go(Screen.REVIEW)
        }
    }
}
private fun signInError(e: Exception): String = when (e) {
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException -> "Wrong email or password."
    is FirebaseNetworkException -> "No internet connection. Check your network and try again."
    is FirebaseTooManyRequestsException -> "Too many attempts. Try again later."
    else -> "Sign in failed. Please try again."
}

private fun signUpError(e: Exception): String = when (e) {
    is FirebaseAuthUserCollisionException -> "An account with this email already exists."
    is FirebaseAuthWeakPasswordException -> "That password is too weak. Use a stronger one."
    is FirebaseAuthInvalidCredentialsException -> "Enter a valid email address."
    is FirebaseNetworkException -> "No internet connection. Check your network and try again."
    is FirebaseTooManyRequestsException -> "Too many attempts. Try again later."
    else -> "Sign up failed. Please try again."
}