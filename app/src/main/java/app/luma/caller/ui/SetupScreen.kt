package app.luma.caller.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.luma.caller.data.Perms

@Composable
fun SetupScreen(onChanged: () -> Unit, onSkip: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val permsMissing = remember(tick) { Perms.missing(ctx).isNotEmpty() }
    val dialer = remember(tick) { Perms.dialerHeld(ctx) }
    val sms = remember(tick) { Perms.smsHeld(ctx) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        tick++
        onChanged()
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        tick++
        onChanged()
    }

    LumaBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))
            Box(
                Modifier.size(84.dp).gradientFill(Accent, CircleShape),
                contentAlignment = Alignment.Center
            ) { LumaIcon(LIcon.Call, size = 38.dp, strokeWidth = 1.9f) }
            Spacer(Modifier.height(20.dp))
            LText("Luma", 44, FontWeight.Bold, spacing = -1f)
            Spacer(Modifier.height(8.dp))
            LText(
                "Calls and messages in one glass app.\nThree quick steps to get started.",
                16, color = Color.White.copy(alpha = 0.75f), align = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))

            SetupRow("Allow access", "Contacts, call log, calls and messages.", !permsMissing, "Allow") {
                permLauncher.launch(Perms.core)
            }
            Spacer(Modifier.height(14.dp))
            SetupRow("Default phone app", "Lets Luma show incoming and active calls.", dialer, "Set") {
                roleLauncher.launch(Perms.dialerIntent(ctx))
            }
            Spacer(Modifier.height(14.dp))
            SetupRow("Default messages app", "Lets Luma receive and store text messages.", sms, "Set") {
                roleLauncher.launch(Perms.smsIntent(ctx))
            }

            Spacer(Modifier.height(28.dp))
            LText("Skip for now", 15, FontWeight.Medium, SoftBlue, modifier = Modifier.pressable(onClick = onSkip))
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SetupRow(title: String, description: String, done: Boolean, action: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(26.dp), fill = 0.14f, base = 0.35f)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            LText(title, 17, FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            LText(description, 13, color = Color.White.copy(alpha = 0.7f))
        }
        Spacer(Modifier.width(12.dp))
        if (done) {
            LText("Done", 15, FontWeight.Bold, Color(0xFF46EBA0))
        } else {
            Box(
                Modifier
                    .height(40.dp)
                    .width(76.dp)
                    .pressable(shape = RoundedCornerShape(20.dp), onClick = onClick)
                    .gradientFill(Accent, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) { LText(action, 15, FontWeight.Bold) }
        }
    }
}
