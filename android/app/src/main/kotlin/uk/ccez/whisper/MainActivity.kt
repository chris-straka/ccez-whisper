package uk.ccez.whisper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** The model's state, and the button to get it. Nothing else to set. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = Model(this)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { Screen(model) }
            }
        }
    }
}

@Composable
private fun Screen(model: Model) {
    var ready by remember { mutableStateOf(model.ready) }
    var progress by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                progress = model.progress()
                ready = model.ready
            }
            delay(1000)
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Whisper large-v3-turbo", style = MaterialTheme.typography.titleLarge)
        val p = progress
        when {
            ready -> Text("Ready", color = MaterialTheme.colorScheme.primary)
            p != null -> {
                LinearProgressIndicator(progress = { p.first.toFloat() / p.second }, modifier = Modifier.fillMaxWidth())
                Text("${p.first / 1_000_000} of ${p.second / 1_000_000} MB", style = MaterialTheme.typography.bodySmall)
            }
            else -> Button(onClick = { model.download() }) { Text("Download · 874 MB") }
        }
    }
}
