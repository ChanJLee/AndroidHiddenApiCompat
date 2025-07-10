package me.chan.lib.hiddenapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import me.chan.lib.hiddenapi.HiddenApiCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val text = findViewById<TextView>(R.id.text)
        text.text = """
            Demo usage:
            1. Without using HiddenApi, calling methods in HiddenApi will throw exceptions.
            2. When using HiddenApi, calling methods in HiddenApi will work normally.
        """.trimIndent()

        findViewById<View>(R.id.compat).setOnClickListener {
            HiddenApiCompat.compat(this)
            Toast.makeText(this, "Compatibility applied successfully", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.invoke).setOnClickListener {
            try {
                val clazz = Class.forName("dalvik.system.VMRuntime")
                val method =
                    clazz.getDeclaredMethod("setHiddenApiExemptions", Array<String>::class.java)
                Toast.makeText(this, "Invoke succeeded: $method", Toast.LENGTH_SHORT).show()
            } catch (e: Throwable) {
                Toast.makeText(this, "Invoke failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}