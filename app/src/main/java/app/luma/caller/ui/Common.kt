package app.luma.caller.ui

import androidx.compose.foundation.background
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
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
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
fun BottomBar(selected: Tab, unread: Int, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(72.dp)
            .glass(RoundedCornerShape(36.dp), fill = 0.14f, base = 0.6f)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (tab in Tab.entries) {
            val isSelected = tab == selected
            val tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.72f)
            val base = Modifier.width(72.dp).height(56.dp)
            Column(
                (if (isSelected) base.glass(RoundedCornerShape(28.dp), fill = 0.22f) else base)
                    .pressable { onSelect(tab) },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    if (tab.icon == null) DotsIcon(tint = tint) else LumaIcon(tab.icon, tint = tint)
                    if (tab == Tab.Messages && unread > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-4).dp)
                                .size(16.dp)
                                .background(Brush.linearGradient(Accent), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            LText(if (unread > 9) "9+" else unread.toString(), 9, FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                LText(tab.label, 11, if (isSelected) FontWeight.SemiBold else FontWeight.Medium, tint)
            }
        }
    }
}
