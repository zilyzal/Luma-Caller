package app.luma.caller

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import app.luma.caller.ui.LumaApp

/** What an incoming intent asked the app to show. [id] makes repeated identical requests distinct. */
data class Launch(
    val dial: String? = null,
    val sms: String? = null,
    val tab: String? = null,
    val id: Long = System.nanoTime()
)

class MainActivity : ComponentActivity() {
    private val launch = mutableStateOf<Launch?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        if (savedInstanceState == null) launch.value = parse(intent)
        setContent {
            LumaApp(
                launch = launch.value,
                openCall = {
                    startActivity(Intent(this, InCallActivity::class.java))
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launch.value = parse(intent)
    }

    private fun parse(intent: Intent?): Launch? {
        if (intent == null) return null
        val data = intent.data
        val tab = intent.getStringExtra(EXTRA_TAB)
        val number = data?.schemeSpecificPart?.substringBefore('?')?.takeIf { it.isNotBlank() }
        return when {
            data?.scheme == "tel" && number != null -> Launch(dial = number, tab = tab)
            data != null && data.scheme in SMS_SCHEMES && number != null -> Launch(sms = number, tab = tab)
            tab != null -> Launch(tab = tab)
            else -> null
        }
    }

    companion object {
        const val EXTRA_TAB = "luma_tab"
        private val SMS_SCHEMES = setOf("sms", "smsto", "mms", "mmsto")
    }
}
