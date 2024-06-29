package edu.skku.cs.modedang

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import io.github.rosemoe.sora.widget.CodeEditor
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ActivitySubmissionDetail: AppCompatActivity() {

    private lateinit var problemTitle: TextView
    private lateinit var submissionIdText: TextView
    private lateinit var codeEditor: CodeEditor
    private lateinit var resultTextView: TextView
    private lateinit var seeOthersButton: Button
    private lateinit var tryAgainButton: Button
    private lateinit var solveOtherProblemsButton: Button

    private var submissionId: Int = -1
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var fetchResultRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submission_detail)

        problemTitle = findViewById(R.id.problemTitle)
        submissionIdText = findViewById(R.id.submissionId)
        codeEditor = findViewById(R.id.codeEditor)
        resultTextView = findViewById(R.id.resultTextView)
        seeOthersButton = findViewById(R.id.seeOthersButton)
        tryAgainButton = findViewById(R.id.tryAgainButton)
        solveOtherProblemsButton = findViewById(R.id.solveOtherProblemsButton)

        seeOthersButton.visibility = View.GONE
        tryAgainButton.visibility = View.GONE
        solveOtherProblemsButton.visibility = View.GONE


        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val problemId = sharedPreferences.getInt("PROBLEM_ID", -1)
        val code = sharedPreferences.getString("CODE", "") ?: ""

        problemTitle.text = "Problem ID: $problemId"
        codeEditor.setText(code)
        codeEditor.isEditable = false
        codeEditor.typefaceText = Typeface.MONOSPACE

        if (problemId != -1 && code.isNotEmpty()) {
            submitCode(problemId, code)
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Invalid Problem ID or Code", Snackbar.LENGTH_LONG).show()
        }

        // Runnable 정의
        fetchResultRunnable = object : Runnable {
            override fun run() {
                fetchSubmissionResult(problemId)
                handler.postDelayed(this, 1000)
            }
        }

        tryAgainButton.setOnClickListener {
            // 이전 액티비티인 ActivitySolveProblem으로 이동
            val intent = Intent(this@ActivitySubmissionDetail, ActivitySolveProblem::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun submitCode(problemId: Int, code: String) {
        val baseUrl = getString(R.string.base_url)
        val url = "$baseUrl/submission?problemId=$problemId"
        val client = OkHttpClient()

        val jsonBody = JSONObject()
        jsonBody.put("language", "C")

        val codeArray = JSONArray()
        val codeObject = JSONObject()
        codeObject.put("id", 1)
        codeObject.put("text", code)
        codeObject.put("locked", false)
        codeArray.put(codeObject)

        jsonBody.put("code", codeArray)

        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val authToken = sharedPreferences.getString("AUTH_TOKEN", null)

        if (authToken == null) {
            Toast.makeText(this, "Auth token is missing. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", authToken)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("call", call.toString())
                runOnUiThread {
                    Snackbar.make(findViewById(android.R.id.content), "Failed to submit code", Snackbar.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    submissionId = jsonResponse.getInt("id")
                    runOnUiThread {
                        submissionIdText.text = "Submission ID: $submissionId"
                        handler.post(fetchResultRunnable) // Runnable 실행 시작
                    }
                } else {
                    runOnUiThread {
                        Snackbar.make(findViewById(android.R.id.content), "Failed to submit code", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun fetchSubmissionResult(problemId: Int) {
        if (submissionId == -1) return

        val baseUrl = getString(R.string.base_url)
        val url = "$baseUrl/submission/$submissionId?problemId=$problemId"

        val client = OkHttpClient()

        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val authToken = sharedPreferences.getString("AUTH_TOKEN", null)

        if (authToken == null) {
            Toast.makeText(this, "Auth token is missing. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", authToken)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Snackbar.make(findViewById(android.R.id.content), "Failed to fetch result", Snackbar.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val result = jsonResponse.getString("result")
                    runOnUiThread {
                        resultTextView.text = "Result: $result"
                        if (result == "Accepted") {
                            resultTextView.setTextColor(ContextCompat.getColor(this@ActivitySubmissionDetail, R.color.skyBlue))
                            seeOthersButton.visibility = View.VISIBLE
                            tryAgainButton.visibility = View.GONE
                            seeOthersButton.setOnClickListener {
                                val intent = Intent(this@ActivitySubmissionDetail, ActivitySubmissionList::class.java)
                                startActivity(intent)
                            }
                        } else {
                            resultTextView.setTextColor(ContextCompat.getColor(this@ActivitySubmissionDetail, R.color.red))
                            seeOthersButton.visibility = View.GONE
                            tryAgainButton.visibility = View.VISIBLE
                        }
                    }
                } else {
                    runOnUiThread {
                        Snackbar.make(findViewById(android.R.id.content), "Failed to fetch result", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
