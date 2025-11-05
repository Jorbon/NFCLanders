package edu.jorbonism.nfclanders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.jorbonism.nfclanders.AppState
import edu.jorbonism.nfclanders.NfcState
import edu.jorbonism.nfclanders.WriteError
import kotlinx.coroutines.flow.update

@Composable
fun ToggleOption(label: String, description: String?, checked: Boolean, enabled: Boolean, onCheckedChange: ((Boolean) -> Unit)) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 10.dp)
    ) {
        Text(
            label,
            style = Typography.bodyLarge,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .weight(1f)
        )
        Switch(
            checked,
            onCheckedChange,
            enabled = enabled,
            modifier = Modifier
                .padding(horizontal = 10.dp)
        )
    }
    description?.let { description ->
        Text(
            description,
            style = Typography.labelSmall,
            modifier = Modifier
                .padding(horizontal = 10.dp)
        )
    }
}

@Composable
fun WritePage(appState: AppState) {
    val tagContents by appState.tagContents.collectAsState()
    val writeData by appState.writeData.collectAsState()
    val writeHeader by appState.writeHeader.collectAsState()
    val formatBlankTag by appState.formatBlankTag.collectAsState()
    val nfcState by appState.nfcState.collectAsState()
    val writeError by appState.writeError.collectAsState()

    Column {
        ToggleOption(
            label = "Write current data",
            description = "Attempt to write the active skylander data to the NFC tag. " +
                    "This will overwrite existing data on the tag.",
            checked = writeData,
            enabled = tagContents?.header != null && nfcState == NfcState.None,
            onCheckedChange = { newState -> appState.writeData.update { newState } },
        )
        ToggleOption(
            label = "Write tag identity",
            description = "Attempt to change the skylander identity info on the tag (Skylander type, variant info, and trading card ID). " +
                    "Does not work for real Skylanders. " +
                    "While off, will only write to tags with matching identity. " +
                    "Useful to avoid writing edits to the wrong tag by mistake.",
            checked = writeHeader,
            enabled = writeData && tagContents?.header != null && nfcState == NfcState.None,
            onCheckedChange = { newState -> appState.writeHeader.update { newState } },
        )
        ToggleOption(
            label = "Format blank tag (permanent!)",
            description = "Try to format a blank NFC tag as a skylander. " +
                    "This requires permamnently locking access codes. " +
                    "Once formatted, the tag can only be used for Skylanders. " +
                    "(Cards with backdoor commands can still be reset with other tools)",
            checked = formatBlankTag,
            enabled = nfcState == NfcState.None,
            onCheckedChange = { newState -> appState.formatBlankTag.update { newState } },
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(vertical = 20.dp)
                .fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (nfcState == NfcState.None) {
                        appState.nfcState.update { NfcState.WriteWaitingForTag }
                    }
                },
                enabled = (formatBlankTag || (tagContents?.header != null && writeData)) && nfcState == NfcState.None,
            ) {
                Text(
                    "Write",
                    style = Typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (nfcState != NfcState.None) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(
                    MaterialTheme.colorScheme.onBackground.red,
                    MaterialTheme.colorScheme.onBackground.green,
                    MaterialTheme.colorScheme.onBackground.blue,
                    0.5f,
                )),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .aspectRatio(1.0f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        when (nfcState) {
                            NfcState.WriteWaitingForTag -> "Tap tag to write"
                            NfcState.WriteSuccess -> "Write succeeded!"
                            NfcState.WriteFailure -> "Write failed!"
                            else -> ""
                        },
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(20.dp)
                    )

                    val formatBlankTagStatus = when (nfcState) {
                        NfcState.WriteSuccess -> "✅"
                        NfcState.WriteFailure -> if (writeError?.stage == WriteError.Stage.Formatting) "❌" else "✅"
                        else -> "•"
                    }

                    val writeHeaderStatus = when (nfcState) {
                        NfcState.WriteSuccess -> "✅"
                        NfcState.WriteFailure -> if (writeError?.stage == WriteError.Stage.Formatting || writeError?.stage == WriteError.Stage.WritingHeader) "❌" else "✅"
                        else -> "•"
                    }

                    val writeDataStatus = when (nfcState) {
                        NfcState.WriteSuccess -> "✅"
                        NfcState.WriteFailure -> if (writeError != null) "❌" else "✅"
                        else -> "•"
                    }

                    Column(

                    ) {
                        if (formatBlankTag) { Text(
                            "$formatBlankTagStatus Format blank tag",
                            style = Typography.labelSmall,
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                        ) }
                        if (writeHeader && writeData && tagContents?.header != null) { Text(
                            "$writeHeaderStatus Write tag identity",
                            style = Typography.labelSmall,
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                        ) }
                        if (writeData && tagContents?.data != null) { Text(
                            "$writeDataStatus Write data",
                            style = Typography.labelSmall,
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                        ) }
                    }

                    if (nfcState == NfcState.WriteFailure) {
                        Text(
                            writeError?.message?: "Unknown error",
                            style = Typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 30.dp, vertical = 20.dp)
                        )
                    }

                    Button(
                        onClick = { appState.nfcState.update { NfcState.None } },
                        enabled = true,
                        modifier = Modifier
                            .padding(20.dp)
                    ) {
                        Text(
                            when (nfcState) {
                                NfcState.WriteWaitingForTag -> "Cancel"
                                else -> "Ok"
                            },
                            style = Typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                }
            }
        }
    }
}
