package edu.skku.cs.modedang

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ActivityProblemList : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var problemAdapter: ProblemAdapter
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_problem_list)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        problemAdapter = ProblemAdapter(emptyList()) { problem ->
            val intent = Intent(this, ActivitySolveProblem::class.java).apply {
                putExtra("PROBLEM_ID", problem.id)
            }
            startActivity(intent)
        }
        recyclerView.adapter = problemAdapter

        fetchProblems()
    }

    private fun fetchProblems() {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val authToken = sharedPreferences.getString("AUTH_TOKEN", null)

        if (authToken == null) {
            Toast.makeText(this, "Auth token is missing. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        val baseUrl = getString(R.string.base_url)
        val url = "$baseUrl/problem?take=100"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", authToken)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("Activity1", "Error: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ActivityProblemList, "Failed to load problems", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    val jsonObject = JSONObject(it)
                    val jsonArray = jsonObject.getJSONArray("data")
                    val problems = mutableListOf<Problem>()
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val problem = Problem(
                            jsonObject.getInt("id"),
                            jsonObject.getString("title"),
                            jsonObject.optString("engTitle", null),
                            jsonObject.getString("difficulty"),
                            jsonObject.getInt("submissionCount"),
                            jsonObject.getDouble("acceptedRate"),
                            jsonObject.getJSONArray("tags").let { tagsArray ->
                                List(tagsArray.length()) { index ->
                                    val tagObject = tagsArray.getJSONObject(index)
                                    Tag(tagObject.getInt("id"), tagObject.getString("name"))
                                }
                            }
                        )
                        problems.add(problem)
                    }
                    runOnUiThread {
                        problemAdapter.updateProblems(problems)
                    }
                }
            }
        })
    }
}
