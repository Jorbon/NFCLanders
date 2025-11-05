package edu.jorbonism.nfclanders.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.jorbonism.nfclanders.AppState
import edu.jorbonism.nfclanders.formatByte
import edu.jorbonism.nfclanders.formatByteArray

@Composable
fun DataPage(appState: AppState) {
    val tagDump by appState.tagDump.collectAsState()
    val readDumpError by appState.readDumpError.collectAsState()

    if (tagDump == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(vertical = 50.dp)
                .fillMaxSize(),
        ) {
            Text(
                "Scan a tag to view its data.",
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Light,
                    fontStyle = FontStyle.Italic,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.sp,
                ),
                color = Color(
                    MaterialTheme.colorScheme.onBackground.red,
                    MaterialTheme.colorScheme.onBackground.green,
                    MaterialTheme.colorScheme.onBackground.blue,
                    0.5f,
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }

    if (tagDump != null || readDumpError != null) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
        ) {
            readDumpError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    softWrap = true,
                )
            }

            tagDump?.let {
                for (blockIndex in 0 until 64) {
                    Text(
                        "Block ${formatByte(blockIndex.toByte())}:  ${formatByteArray(it.copyOfRange(blockIndex * 0x10, (blockIndex + 1) * 0x10))}",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            letterSpacing = 0.sp,
                        ),
                        modifier = Modifier
                            .padding(bottom = if (blockIndex % 4 == 3) 10.dp else 0.dp)
                    )
                }
            }
        }
    }
}
