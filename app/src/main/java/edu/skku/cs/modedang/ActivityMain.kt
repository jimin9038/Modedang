package edu.skku.cs.modedang

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ActivityMain : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var loginButton: Button
    private lateinit var infoText: TextView
    private lateinit var startButton: Button
    private lateinit var switchAccountButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        welcomeText = findViewById(R.id.welcomeText)
        loginButton = findViewById(R.id.loginButton)
        infoText = findViewById(R.id.infoText)
        startButton = findViewById(R.id.startButton)
        switchAccountButton = findViewById(R.id.switchAccountButton)

        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val authToken = sharedPreferences.getString("AUTH_TOKEN", null)
        val username = sharedPreferences.getString("USERNAME", null)
        val cookie = sharedPreferences.getString("COOKIE", null)

        if (authToken == null || cookie == null) {
            // Not logged in
            loginButton.visibility = View.VISIBLE
            infoText.visibility = View.GONE
            startButton.visibility = View.GONE
            switchAccountButton.visibility = View.GONE

            loginButton.setOnClickListener {
                val intent = Intent(this, ActivityLogin::class.java)
                startActivity(intent)
            }
        } else {
            // Logged in, reissue token
            reissueToken(authToken, cookie) { success ->
                runOnUiThread {
                    if (success) {
                        // Token is valid
                        loginButton.visibility = View.GONE
                        infoText.visibility = View.VISIBLE
                        startButton.visibility = View.VISIBLE
                        switchAccountButton.visibility = View.VISIBLE

                        infoText.text = "Logged in as: $username"

                        startButton.setOnClickListener {
                            val intent = Intent(this, ActivityProblemList::class.java)
                            startActivity(intent)
                        }

                        switchAccountButton.setOnClickListener {
                            // Clear auth token and cookie
                            val editor = sharedPreferences.edit()
                            editor.remove("AUTH_TOKEN")
                            editor.remove("COOKIE")
                            editor.apply()

                            // Navigate to login activity
                            val intent = Intent(this, ActivityLogin::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        // Token is not valid, redirect to login
                        Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, ActivityLogin::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

    private fun reissueToken(authToken: String, cookie: String, callback: (Boolean) -> Unit) {
        val baseUrl = getString(R.string.base_url)
        val url = "$baseUrl/auth/reissue"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", authToken)
            .addHeader("COOKIE", cookie)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val newAuthToken = response.headers.get("authorization").toString()
                    val newCookie = response.headers.get("set-cookie").toString()

                    val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("AUTH_TOKEN", newAuthToken)
                    editor.putString("COOKIE", newCookie)
                    editor.apply()

                    callback(true)
                } else {
                    callback(false)
                }
            }
        })
    }
}
