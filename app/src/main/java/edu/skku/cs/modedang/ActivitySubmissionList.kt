package edu.skku.cs.modedang

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ActivitySubmissionList : AppCompatActivity() {

    private lateinit var problemTitle: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SubmissionAdapter
    private lateinit var loadMoreButton: Button
    private var submissions = mutableListOf<Submission>()
    private var offset = 0
    private val take = 100
    private lateinit var solveOtherProblemsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submission_list)

        problemTitle = findViewById(R.id.problemTitle)
        recyclerView = findViewById(R.id.recyclerView)
        loadMoreButton = findViewById(R.id.loadMoreButton)
        recyclerView.layoutManager = LinearLayoutManager(this)

        solveOtherProblemsButton = findViewById(R.id.solveOtherProblemsButton)

        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val problemId = sharedPreferences.getInt("PROBLEM_ID", -1)
        val problemTitleStr = sharedPreferences.getString("PROBLEM_TITLE", "Problem Submissions")

        problemTitle.text = problemTitleStr

        loadMoreButton.setOnClickListener {
            fetchSubmissionList(problemId, offset)
        }

        if (problemId != -1) {
            fetchSubmissionList(problemId, offset)
        } else {
            Log.e("ActivitySubmissionList", "Invalid Problem ID")
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

    private fun fetchSubmissionList(problemId: Int, offset: Int) {
        val baseUrl = getString(R.string.base_url)
        val url = "$baseUrl/submission?problemId=$problemId&take=$take&offset=$offset"

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
                Log.e("ActivitySubmissionList", "Failed to fetch submission list", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let {
                        val jsonResponse = JSONObject(it)
                        val submissionArray = jsonResponse.getJSONArray("data")
                        runOnUiThread {
                            handleNewSubmissions(submissionArray)
                        }
                    }
                } else {
                    Log.e("ActivitySubmissionList", "Failed to fetch submission list: ${response.code}")
                }
            }
        })
    }

    private fun handleNewSubmissions(submissionArray: JSONArray) {
        for (i in 0 until submissionArray.length()) {
            val item = submissionArray.getJSONObject(i)
            val submission = Submission(
                number = submissions.size + 1,
                id = item.getInt("id"),
                username = item.getJSONObject("user").getString("username"),
                createTime = item.getString("createTime"),
                language = item.getString("language"),
                result = item.getString("result"),
                codeSize = item.getInt("codeSize")
            )
            submissions.add(submission)
        }
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        } else {
            adapter = SubmissionAdapter(submissions)
            recyclerView.adapter = adapter
        }

        offset += submissionArray.length()
        loadMoreButton.visibility = if (submissionArray.length() < take) View.GONE else View.VISIBLE
    }
}

data class Submission(
    val number: Int,
    val id: Int,
    val username: String,
    val createTime: String,
    val language: String,
    val result: String,
    val codeSize: Int
)

class SubmissionAdapter(private val submissions: List<Submission>) :
    RecyclerView.Adapter<SubmissionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.number)
        val username: TextView = view.findViewById(R.id.username)
        val createTime: TextView = view.findViewById(R.id.createTime)
        val language: TextView = view.findViewById(R.id.language)
        val result: TextView = view.findViewById(R.id.result)
        val codeSize: TextView = view.findViewById(R.id.codeSize)

        init {
            view.setOnClickListener {
                val context = view.context
                val intent = Intent(context, ActivityOthersSubmission::class.java)
                intent.putExtra("SUBMISSION_ID", it.tag as Int)
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_submission, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val submission = submissions[position]
        holder.number.text = submission.number.toString()
        holder.username.text = submission.username
        holder.createTime.text = submission.createTime
        holder.language.text = submission.language
        holder.result.text = submission.result
        holder.result.setTextColor(
            when (submission.result) {
                "Accepted" -> holder.itemView.context.getColor(R.color.skyBlue)
                "CompileError", "WrongAnswer" -> holder.itemView.context.getColor(R.color.red)
                else -> holder.itemView.context.getColor(R.color.black)
            }
        )
        holder.codeSize.text = "${submission.codeSize} B"
        holder.itemView.tag = submission.id
    }

    override fun getItemCount() = submissions.size
}
