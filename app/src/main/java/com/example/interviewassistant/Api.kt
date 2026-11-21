package com.example.interviewassistant

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiException(message: String) : Exception(message)

data class Career(val name: String, val questions: Int)

data class Question(
    val number: Int,
    val total: Int,
    val text: String,
    val difficulty: String,
    val category: String
)

data class AnswerResult(
    val score: Int,
    val similarity: Double,
    val keywordCoverage: Double,
    val missingKeywords: List<String>,
    val expectedAnswer: String,
    val feedback: String
)

data class KnnCandidate(val question: String, val difficulty: String, val similarity: Double)

data class AnswerResponse(
    val result: AnswerResult,
    val answered: Int,
    val averageScore: Double,
    val currentDifficulty: String,
    val finished: Boolean,
    val nextQuestion: Question?,
    val knnCandidates: List<KnnCandidate>
)

data class QuestionFeedback(
    val question: String,
    val difficulty: String,
    val score: Double,
    val verdict: String,
    val yourAnswer: String,
    val modelAnswer: String,
    val feedback: String,
    val missingTerms: List<String>,
    val studyNext: List<String>
)

data class Report(
    val career: String,
    val averageScore: Double,
    val level: String,
    val summary: String,
    val strongTopics: List<String>,
    val studyPlan: List<String>,
    val questions: List<QuestionFeedback>
)

private fun JSONArray.strings(): List<String> = List(length()) { i -> getString(i) }

private fun JSONObject.toQuestion(): Question = Question(
    number = getInt("number"),
    total = getInt("total"),
    text = getString("question"),
    difficulty = getString("difficulty"),
    category = getString("category")
)

/** Talks to the Flask API (app.py). Uses only classes that ship with Android, so no extra libraries. */
object Api {
    // 10.0.2.2 = your PC, seen from the Android emulator. For a real phone use your PC's Wi-Fi address.
    var baseUrl: String = "http://10.0.2.2:5000"

    private fun request(method: String, path: String, body: JSONObject? = null): JSONObject {
        val conn = URL(baseUrl.trim().trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 8000
            conn.readTimeout = 20000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (code !in 200..299) {
                throw ApiException(json.optString("error", "Server error ($code)"))
            }
            return json
        } finally {
            conn.disconnect()
        }
    }

    fun careers(): List<Career> {
        val arr = request("GET", "/careers").getJSONArray("careers")
        return List(arr.length()) { i ->
            val c = arr.getJSONObject(i)
            Career(c.getString("name"), c.getInt("questions"))
        }
    }

    fun start(career: String, maxQuestions: Int = 10): Pair<String, Question> {
        val json = request(
            "POST", "/start",
            JSONObject().put("career", career).put("max_questions", maxQuestions)
        )
        return json.getString("session_id") to json.getJSONObject("question").toQuestion()
    }

    fun answer(sessionId: String, answer: String): AnswerResponse {
        val json = request(
            "POST", "/answer",
            JSONObject().put("session_id", sessionId).put("answer", answer)
        )
        val r = json.getJSONObject("result")
        val p = json.getJSONObject("progress")
        val candidates = json.optJSONObject("knn")?.getJSONArray("candidates")?.let { arr ->
            List(arr.length()) { i ->
                val c = arr.getJSONObject(i)
                KnnCandidate(c.getString("question"), c.getString("difficulty"), c.getDouble("similarity"))
            }
        } ?: emptyList()

        return AnswerResponse(
            result = AnswerResult(
                score = r.getInt("score"),
                similarity = r.getDouble("similarity"),
                keywordCoverage = r.getDouble("keyword_coverage"),
                missingKeywords = r.getJSONArray("missing_keywords").strings(),
                expectedAnswer = r.getString("expected_answer"),
                feedback = r.getString("feedback")
            ),
            answered = p.getInt("answered"),
            averageScore = p.getDouble("average_score"),
            currentDifficulty = p.getString("current_difficulty"),
            finished = json.getBoolean("finished"),
            nextQuestion = json.optJSONObject("next_question")?.toQuestion(),
            knnCandidates = candidates
        )
    }

    fun end(sessionId: String) {
        request("POST", "/end", JSONObject().put("session_id", sessionId))
    }

    fun feedback(sessionId: String): Report {
        val j = request("GET", "/feedback/$sessionId")
        val qs = j.getJSONArray("questions")
        return Report(
            career = j.getString("career"),
            averageScore = j.getDouble("average_score"),
            level = j.getString("performance_level"),
            summary = j.getString("summary"),
            strongTopics = j.getJSONArray("strong_topics").strings(),
            studyPlan = j.getJSONArray("study_plan").strings(),
            questions = List(qs.length()) { i ->
                val q = qs.getJSONObject(i)
                val study = q.getJSONArray("study_next")
                QuestionFeedback(
                    question = q.getString("question"),
                    difficulty = q.optString("difficulty"),
                    score = q.getDouble("score"),
                    verdict = q.getString("verdict"),
                    yourAnswer = q.optString("your_answer"),
                    modelAnswer = q.optString("model_answer"),
                    feedback = q.optString("feedback"),
                    missingTerms = q.getJSONArray("missing_terms").strings(),
                    studyNext = List(study.length()) { k -> study.getJSONObject(k).getString("question") }
                )
            }
        )
    }
}
