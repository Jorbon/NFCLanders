package edu.jorbonism.nfclanders.ui

import android.util.Log
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.jorbonism.nfclanders.AppState
import edu.jorbonism.nfclanders.formatByte
import edu.jorbonism.nfclanders.formatByteArray
import edu.jorbonism.nfclanders.tag.TagContents
import edu.jorbonism.nfclanders.tag.TagHeader
import edu.jorbonism.nfclanders.enums.Character
import kotlinx.coroutines.flow.update
import kotlin.math.pow

@Composable
fun EditContents(appState: AppState) {
    val tagContentsOption by appState.tagContents.collectAsState()
    val tagContents = tagContentsOption?: return
    val header = tagContents.header?: return

    Text(
        "Identity",
        style = TextStyle(
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
    )

    Button(
        onClick = {},
        colors = ButtonColors(
            containerColor = MaterialTheme.colorScheme.onSecondary,
            contentColor = MaterialTheme.colorScheme.secondary,
            disabledContainerColor = MaterialTheme.colorScheme.onSecondary,
            disabledContentColor = MaterialTheme.colorScheme.secondary,
        ),
        modifier = Modifier
            .padding(10.dp)
    ) {
        Column(

        ) {
            Text(
                header.toyType.name,
                style = Typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 7.dp)
                    .fillMaxWidth()
            )
            Text(
                Character.entries.getOrNull(header.toyType.characterID.toInt())?.type?.toString()?: "Unknown",
                style = Typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(3.dp)
                    .fillMaxWidth()
            )
            Text(
                header.toyType.game.toString(),
                style = TextStyle(
                    fontWeight = FontWeight.Light,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(0.dp)
                    .fillMaxWidth()
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var tradingCardIDOld by rememberSaveable { mutableStateOf(header.tradingCardID.toLong()) }
        var tradingCardIDText by rememberSaveable { mutableStateOf(header.tradingCardID.toString()) }
        if (tradingCardIDOld.toULong() != header.tradingCardID) {
            // When something besides this component changes the value
            tradingCardIDOld = header.tradingCardID.toLong()
            tradingCardIDText = header.tradingCardID.toString()
        }

        Text(
            "Trading card ID: ",
            style = Typography.bodyLarge,
            modifier = Modifier
                .padding(10.dp)
        )
        TextField(
            tradingCardIDText,
            textStyle = Typography.bodyLarge,
            isError = header.tradingCardID.toString() != tradingCardIDText,
            onValueChange = {
                tradingCardIDText = it
                val n = try { it.toULong() } catch (e: Exception) { null }
                n?.let { n ->
                    if (it == n.toString() && n != header.tradingCardID && n < 29.0.pow(10.0).toULong()) {
                        header.tradingCardID = n
                        tradingCardIDOld = n.toLong()
                        appState.tagContents.update { TagContents(header, tagContents.data) }
                    }
                }
            },
            modifier = Modifier
                .padding(10.dp)
        )
    }

    Text(
        if (tagContents.data == null) "No Content Data found on tag" else "Content Data",
        style = TextStyle(
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 30.dp)
            .fillMaxWidth()
    )

    val data = tagContents.data?: return
}

@Composable
fun EditPage(appState: AppState) {
    val tagContents by appState.tagContents.collectAsState()
    val readDataError by appState.readDataError.collectAsState()

    if (tagContents == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(vertical = 50.dp)
                .fillMaxSize()
        ) {
            Text(
                "Scan a tag to edit its contents.",
                modifier = Modifier.fillMaxWidth(),
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
            )
        }
    }

    if (tagContents != null || readDataError != null) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            readDataError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    softWrap = true,
                )
            }

            tagContents?.let { tagContents ->
                EditContents(appState)
            }
        }
    }
}
