package edu.skku.cs.modedang

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.dsl.languages
import io.github.rosemoe.sora.langs.textmate.registry.model.DefaultGrammarDefinition
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import okhttp3.*
import org.eclipse.tm4e.core.registry.IThemeSource
import org.json.JSONObject
import java.io.IOException

class ActivitySolveProblem : AppCompatActivity() {

    private lateinit var problemDetail: TextView
    private lateinit var inputDescription: TextView
    private lateinit var outputDescription: TextView
    private lateinit var editor: CodeEditor
    private lateinit var problemTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solve_problem)

        problemDetail = findViewById(R.id.problemDetail)
        inputDescription = findViewById(R.id.inputDescription)
        outputDescription = findViewById(R.id.outputDescription)
        editor = findViewById(R.id.codeEditor)

        val problemId = intent.getIntExtra("PROBLEM_ID", -1)
        if (problemId != -1) {
            fetchProblemDetail(problemId)
        }

        // Configure the code editor
        editor.setText("Hello, world!") // Set text
        editor.typefaceText = Typeface.MONOSPACE // Use Monospace Typeface
        editor.nonPrintablePaintingFlags =
            CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or
                    CodeEditor.FLAG_DRAW_LINE_SEPARATOR or
                    CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION // Show Non-Printable Characters
        editor.requestFocus()

        val fabKeyboard: FloatingActionButton = findViewById(R.id.fab_keyboard)
        fabKeyboard.setOnClickListener {
            showKeyboard()
        }

        val fabSubmit: FloatingActionButton = findViewById(R.id.fab_submit)
        fabSubmit.setOnClickListener {
            val problemId = intent.getIntExtra("PROBLEM_ID", -1)
            val code = editor.text.toString()

            val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val sharedPref = sharedPreferences.edit()
            sharedPref.putInt("PROBLEM_ID", problemId)
            sharedPref.putString("PROBLEM_TITLE", problemTitle)
            sharedPref.putString("CODE", code)
            sharedPref.apply()

            // Activity3으로 이동
            val intent = Intent(this, ActivitySubmissionDetail::class.java)
            startActivity(intent)
        }

        val rootView = findViewById<View>(android.R.id.content)
        val screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                fabKeyboard.visibility = View.GONE
            } else {
                fabKeyboard.visibility = View.VISIBLE
            }
        }
    }

    private fun fetchProblemDetail(problemId: Int) {
        val baseUrl = getString(R.string.base_url)
        val url = "$baseUrl/problem/$problemId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    problemDetail.text = "Failed to load problem details"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    val json = JSONObject(it)
                    runOnUiThread {
                        displayProblemDetail(json)
                    }
                }
            }
        })
    }

    private fun displayProblemDetail(json: JSONObject) {
        problemTitle = json.getString("title")
        val description = json.getString("description")
        val inputDesc = json.getString("inputDescription")
        val outputDesc = json.getString("outputDescription")
        val hint = json.optString("hint", "")

        problemDetail.text = HtmlCompat.fromHtml("<h2>$problemTitle</h2><br>${description}", HtmlCompat.FROM_HTML_MODE_LEGACY)
        inputDescription.text = HtmlCompat.fromHtml("<h3>Input Description</h3><br>${inputDesc}", HtmlCompat.FROM_HTML_MODE_LEGACY)
        outputDescription.text = HtmlCompat.fromHtml("<h3>Output Description</h3><br>${outputDesc}", HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    private fun showKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = this.currentFocus ?: View(this)
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onDestroy() {
        super.onDestroy()
        editor.release()
    }
}
