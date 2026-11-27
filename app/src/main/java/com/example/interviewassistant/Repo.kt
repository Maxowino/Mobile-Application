package com.example.interviewassistant

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** One row of the history list. The full report is loaded only when the row is opened. */
data class HistoryItem(
    val id: String,
    val date: Long,
    val career: String,
    val score: Double,
    val level: String
)

/**
 * All Firestore access in one place.
 *
 *   users/{uid}                    name, email
 *   users/{uid}/interviews/{id}    date, career, score, level, reportJson
 */
object Repo {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    val email: String get() = auth.currentUser?.email ?: ""

    private fun userDoc(): DocumentReference? =
        auth.currentUser?.uid?.let { db.collection("users").document(it) }

    fun saveProfile(name: String, done: (Boolean) -> Unit = {}) {
        val doc = userDoc() ?: return done(false)
        doc.set(mapOf("name" to name, "email" to email), SetOptions.merge())
            .addOnCompleteListener { done(it.isSuccessful) }
    }

    fun loadProfile(result: (String) -> Unit) {
        val doc = userDoc() ?: return result("")
        doc.get()
            .addOnSuccessListener { result(it.getString("name") ?: "") }
            .addOnFailureListener { result("") }
    }

    fun saveInterview(report: Report, done: (Boolean) -> Unit = {}) {
        val doc = userDoc() ?: return done(false)
        val data = mapOf(
            "date" to System.currentTimeMillis(),
            "career" to report.career,
            "score" to report.averageScore,
            "level" to report.level,
            "reportJson" to report.toJson().toString()
        )
        doc.collection("interviews").add(data).addOnCompleteListener { done(it.isSuccessful) }
    }

    /** [result] gets the list (newest first) or an error message. */
    fun loadHistory(result: (List<HistoryItem>?, String?) -> Unit) {
        val doc = userDoc() ?: return result(null, "You are not signed in.")
        doc.collection("interviews").orderBy("date", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { snap ->
                result(snap.documents.map { d ->
                    HistoryItem(
                        id = d.id,
                        date = d.getLong("date") ?: 0L,
                        career = d.getString("career") ?: "",
                        score = d.getDouble("score") ?: 0.0,
                        level = d.getString("level") ?: ""
                    )
                }, null)
            }
            .addOnFailureListener { result(null, it.message ?: "Could not load history.") }
    }

    fun loadReport(id: String, result: (Report?) -> Unit) {
        val doc = userDoc() ?: return result(null)
        doc.collection("interviews").document(id).get()
            .addOnSuccessListener { d ->
                result(d.getString("reportJson")?.let { runCatching { reportFromJson(JSONObject(it)) }.getOrNull() })
            }
            .addOnFailureListener { result(null) }
    }
    /** [done] gets null on success, or a message to show the user. */
    fun changePassword(current: String, new: String, done: (String?) -> Unit) {
        val user = auth.currentUser
        val mail = user?.email
        if (user == null || mail == null) return done("You are not signed in.")
        user.reauthenticate(EmailAuthProvider.getCredential(mail, current))
            .addOnSuccessListener {
                user.updatePassword(new)
                    .addOnSuccessListener { done(null) }
                    .addOnFailureListener { done(passwordError(it, afterReauth = true)) }
            }
            .addOnFailureListener { done(passwordError(it, afterReauth = false)) }
    }
}

//  Report <-> JSON (stored as one string, so the Review screen can be reused for history)

private fun List<String>.toJsonArray() = JSONArray().also { a -> forEach { a.put(it) } }
private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }

private fun Report.toJson(): JSONObject = JSONObject()
    .put("career", career).put("averageScore", averageScore).put("level", level)
    .put("summary", summary)
    .put("strongTopics", strongTopics.toJsonArray())
    .put("studyPlan", studyPlan.toJsonArray())
    .put("questions", JSONArray().also { arr ->
        questions.forEach { q ->
            arr.put(
                JSONObject().put("question", q.question).put("difficulty", q.difficulty)
                    .put("score", q.score).put("verdict", q.verdict).put("yourAnswer", q.yourAnswer)
                    .put("modelAnswer", q.modelAnswer).put("feedback", q.feedback)
                    .put("missingTerms", q.missingTerms.toJsonArray())
                    .put("studyNext", q.studyNext.toJsonArray())
            )
        }
    })

private fun reportFromJson(j: JSONObject): Report {
    val qs = j.getJSONArray("questions")
    return Report(
        career = j.getString("career"),
        averageScore = j.getDouble("averageScore"),
        level = j.getString("level"),
        summary = j.getString("summary"),
        strongTopics = j.getJSONArray("strongTopics").strings(),
        studyPlan = j.getJSONArray("studyPlan").strings(),
        questions = List(qs.length()) { i ->
            val q = qs.getJSONObject(i)
            QuestionFeedback(
                question = q.getString("question"),
                difficulty = q.optString("difficulty"),
                score = q.getDouble("score"),
                verdict = q.getString("verdict"),
                yourAnswer = q.optString("yourAnswer"),
                modelAnswer = q.optString("modelAnswer"),
                feedback = q.optString("feedback"),
                missingTerms = q.getJSONArray("missingTerms").strings(),
                studyNext = q.getJSONArray("studyNext").strings()
            )
        }
    )
}
private fun passwordError(e: Exception, afterReauth: Boolean): String = when (e) {
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException ->
        if (afterReauth) "Could not update the password. Sign in again and retry."
        else "Current password is wrong."
    is FirebaseAuthWeakPasswordException -> "That password is too weak. Use a stronger one."
    is FirebaseNetworkException -> "No internet connection. Check your network and try again."
    is FirebaseTooManyRequestsException -> "Too many attempts. Try again later."
    else -> "Could not change the password. Please try again."
}