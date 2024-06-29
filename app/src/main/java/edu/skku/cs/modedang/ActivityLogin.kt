package edu.skku.cs.modedang

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ActivityLogin : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)

        // 이메일 필드에 포커스 설정
        editTextEmail.requestFocus()

        buttonLogin.setOnClickListener {
            login()
        }

        editTextPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login()
                true
            } else {
                false
            }
        }
    }

    private fun login() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            performLogin(email, password)
        } else {
            Toast.makeText(this, "Please enter user ID and password", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogin(email: String, password: String) {
        val baseUrl = getString(R.string.base_url)
        val url = "$baseUrl/auth/login"
        val json = JSONObject()
        json.put("username", email)
        json.put("password", password)

        val client = OkHttpClient()
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        Log.d("request", request.toString())

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("response", e.toString())
                runOnUiThread {
                    Toast.makeText(this@ActivityLogin, "Login failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("response", response.toString())
                if (response.isSuccessful) {
                    val authToken = response.header("authorization")
                    val setCookie = response.header("set-cookie")

                    // Store tokens and username globally (SharedPreferences used here as an example)
                    val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putString("AUTH_TOKEN", authToken)
                        putString("COOKIE", setCookie)
                        putString("USERNAME", email) // 저장한 username
                        apply()
                    }

                    runOnUiThread {
                        Toast.makeText(this@ActivityLogin, "Login successful", Toast.LENGTH_SHORT).show()
                        // Navigate to ActivityMain
                        val intent = Intent(this@ActivityLogin, ActivityMain::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ActivityLogin, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
