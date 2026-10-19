package app.luma.caller.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Tab(val label: String, val icon: String?) {
    Keypad("Keypad", null),
    Recents("Recents", LIcon.Clock),
    Messages("Messages", LIcon.Message),
    Contacts("Contacts", LIcon.Person)
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(48.dp)
            .glass(RoundedCornerShape(24.dp), fill = 0.14f, base = 0.35f)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LumaIcon(LIcon.Search, size = 19.dp, tint = Color.White.copy(alpha = 0.8f), strokeWidth = 2f)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) LText(hint, 16, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp, textDirection = TextDirection.Content),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Box(Modifier.size(28.dp).pressable { onChange("") }, contentAlignment = Alignment.Center) {
                LumaIcon(LIcon.Close, size = 16.dp, strokeWidth = 2f)
            }
        }
    }
}

@Composable
fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val count = Tab.entries.size
    val index = selected.ordinal
    BoxWithConstraints(
        modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(72.dp)
            .glass(RoundedCornerShape(36.dp), fill = 0.14f, base = 0.6f)
    ) {
        val inner = maxWidth - 16.dp
        val cell = inner / count
        // The two edges of the pill chase the target with different stiffness, so it stretches like a liquid drop.
        val left by animateDpAsState(
            8.dp + cell * index,
            spring(dampingRatio = 0.7f, stiffness = 380f), label = "pillL"
        )
        val right by animateDpAsState(
            8.dp + cell * (index + 1),
            spring(dampingRatio = 0.7f, stiffness = 170f), label = "pillR"
        )
        val pillW = (right - left).coerceAtLeast(cell * 0.8f)
        Box(
            Modifier
                .offset(x = left, y = 8.dp)
                .width(pillW)
                .height(56.dp)
                .glass(RoundedCornerShape(28.dp), fill = 0.26f, depth = 1.2f)
        )
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            for (tab in Tab.entries) {
                val isSelected = tab == selected
                val tint by animateColorAsState(
                    if (isSelected) Color.White else Color.White.copy(alpha = 0.7f), label = "tint"
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pressable(shape = RoundedCornerShape(28.dp)) { onSelect(tab) },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (tab.icon == null) DotsIcon(tint = tint) else LumaIcon(tab.icon, tint = tint)
                    Spacer(Modifier.height(3.dp))
                    LText(tab.label, 11, if (isSelected) FontWeight.SemiBold else FontWeight.Medium, tint)
                }
            }
        }
    }
}
