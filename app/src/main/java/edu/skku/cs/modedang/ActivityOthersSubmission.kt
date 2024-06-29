package edu.skku.cs.modedang

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
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
import org.json.JSONObject
import java.io.IOException

class ActivityOthersSubmission : AppCompatActivity() {

    private lateinit var problemTitle: TextView
    private lateinit var submissionIdText: TextView
    private lateinit var codeEditor: CodeEditor
    private lateinit var resultTextView: TextView
    private lateinit var seeOthersButton: Button
    private lateinit var tryAgainButton: Button
    private lateinit var solveOtherProblemsButton: Button

    private var submissionId: Int = -1

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

        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val problemId = sharedPreferences.getInt("PROBLEM_ID", -1)

        submissionId = intent.getIntExtra("SUBMISSION_ID", -1)

        if (problemId != -1 && submissionId != -1) {
            fetchSubmissionDetail(problemId, submissionId)
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Invalid Problem ID or Submission ID", Snackbar.LENGTH_LONG).show()
        }

        solveOtherProblemsButton.setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.remove("PROBLEM_ID")
            editor.remove("PROBLEM_TITLE")
            editor.apply()

            val intent = Intent(this, ActivityProblemList::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun fetchSubmissionDetail(problemId: Int, submissionId: Int) {
        val baseUrl = getString(R.string.base_url)
        val url = "$baseUrl/submission/$submissionId?problemId=$problemId"

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

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Snackbar.make(findViewById(android.R.id.content), "Failed to fetch submission detail", Snackbar.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val result = jsonResponse.getString("result")
                    val code = jsonResponse.getString("code")
                    runOnUiThread {
                        displaySubmissionDetail(problemId, submissionId, result, code)
                    }
                } else {
                    runOnUiThread {
                        Snackbar.make(findViewById(android.R.id.content), "Failed to fetch submission detail", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    private fun displaySubmissionDetail(problemId: Int, submissionId: Int, result: String, code: String) {
        problemTitle.text = "Problem ID: $problemId"
        submissionIdText.text = "Submission ID: $submissionId"
        codeEditor.setText(code)
        codeEditor.isEditable = false
        codeEditor.typefaceText = Typeface.MONOSPACE

        resultTextView.text = "Result: $result"
        resultTextView.setTextColor(
            when (result) {
                "Accepted" -> ContextCompat.getColor(this, R.color.skyBlue)
                "CompileError", "WrongAnswer" -> ContextCompat.getColor(this, R.color.red)
                else -> ContextCompat.getColor(this, R.color.black)
            }
        )

        seeOthersButton.visibility = View.GONE
        tryAgainButton.visibility = View.GONE
    }
}
