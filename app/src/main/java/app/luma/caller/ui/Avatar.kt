package app.luma.caller.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

object PhotoCache {
    private val cache = LruCache<String, ImageBitmap>(48)

    suspend fun load(ctx: Context, uri: String, maxDim: Int): ImageBitmap? {
        val key = "$uri@$maxDim"
        cache.get(key)?.let { return it }
        val bmp = withContext(Dispatchers.IO) { decode(ctx, uri, maxDim) } ?: return null
        cache.put(key, bmp)
        return bmp
    }

    private fun decode(ctx: Context, uri: String, maxDim: Int): ImageBitmap? = try {
        val resolver = ctx.contentResolver
        val parsed = Uri.parse(uri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

private val avatarPalette = listOf(
    listOf(Color(0xFF6C7BFF), Color(0xFFB06BFF)),
    listOf(Color(0xFFFF7AD9), Color(0xFFFFB36B)),
    listOf(Color(0xFF35E0C4), Color(0xFF3AA0FF)),
    listOf(Color(0xFFFFB36B), Color(0xFFFF7A8A))
)

fun avatarBrush(seed: String): Brush {
    val idx = (seed.hashCode() and 0x7fffffff) % avatarPalette.size
    return Brush.linearGradient(avatarPalette[idx])
}

fun initialOf(name: String): String {
    val c = name.trim().firstOrNull { it.isLetter() } ?: return "#"
    return c.uppercaseChar().toString()
}

@Composable
fun Avatar(name: String, photoUri: String?, size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val photo by produceState<ImageBitmap?>(null, photoUri, px) {
        value = if (photoUri == null) null else PhotoCache.load(ctx, photoUri, px)
    }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarBrush(name))
            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val p = photo
        if (p != null) {
            Image(
                bitmap = p,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        } else {
            LText(initialOf(name), (size.value * 0.38f).toInt(), FontWeight.SemiBold)
        }
    }
}
