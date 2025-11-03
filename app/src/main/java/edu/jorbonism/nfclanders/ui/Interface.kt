package edu.jorbonism.nfclanders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import edu.jorbonism.nfclanders.AppState
import edu.jorbonism.nfclanders.WriteState
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun NFCLandersApp(appState: AppState = AppState()) {
    var currentPage by rememberSaveable { mutableStateOf(AppPages.Write) }

    NFCLandersTheme {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppPages.entries.forEach {
                    item(
                        icon = {
                            Icon(
                                it.icon,
                                contentDescription = it.label,
                            )
                        },
                        label = { Text(it.label) },
                        selected = it == currentPage,
                        onClick = { currentPage = it },
                    )
                }
            }
        ) {
            Scaffold(
                topBar = { CenterAlignedTopAppBar(
                    title = { Text(when (currentPage) {
                        AppPages.Edit  -> "Current Data"
                        AppPages.Write -> "Write to Tag"
                        AppPages.Data  -> "View Raw Data"
                    }) },
                    actions = {

                    },
                    colors = TopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) },
            ) { innerPadding ->
                Box(
                    modifier = Modifier.padding(innerPadding),
                    content = { when (currentPage) {
                        AppPages.Edit  -> EditPage(appState)
                        AppPages.Write -> WritePage(appState)
                        AppPages.Data  -> DataPage(appState)
                    } }
                )

            }
        }
    }
}

enum class AppPages(
    val label: String,
    val icon: ImageVector,
) {
    Edit ("Edit", Icons.Default.Build),
    Write("Write", Icons.Default.Edit),
    Data ("Raw Data", Icons.Default.Search),
}

@Composable
fun EditPage(appState: AppState) {
    val tagContents by appState.tagContents.collectAsState()
    Text(
        text = tagContents?.header?.toyType?.name?: "Unknown",
        style = Typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(PaddingValues(10.dp, 10.dp)).fillMaxSize(),
    )
}


@Composable
fun ToggleOption(label: String, description: String?, checked: Boolean, enabled: Boolean, onCheckedChange: ((Boolean) -> Unit)) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = Typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 10.dp).weight(1f),
        )
        Switch(
            checked,
            onCheckedChange,
            modifier = Modifier.padding(horizontal = 10.dp),
            enabled = enabled,
        )
    }
    description?.let { description ->
        Text(
            description,
            style = Typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

@Composable
fun WritePage(appState: AppState) {
    val tagContents by appState.tagContents.collectAsState()
    val writeData by appState.writeData.collectAsState()
    val writeHeader by appState.writeHeader.collectAsState()
    val formatBlankTag by appState.formatBlankTag.collectAsState()
    val writeState by appState.writeState.collectAsState()

    Column {
        ToggleOption(
            label = "Write current data",
            description = "Attempt to write the active skylander data to the NFC tag. " +
                "This will overwrite existing data on the tag.",
            checked = writeData,
            enabled = tagContents != null,
            onCheckedChange = { newState -> appState.writeData.update { newState } },
        )
        ToggleOption(
            label = "Write tag identity",
            description = "Attempt to change the skylander identity info on the tag (Skylander type, variant info, and trading card ID). " +
                "Does not work for real Skylanders. " +
                "While off, will only write to tags with matching identity. " +
                "Useful to avoid writing edits to the wrong tag by mistake.",
            checked = writeHeader,
            enabled = tagContents != null && writeData,
            onCheckedChange = { newState -> appState.writeHeader.update { newState } },
        )
        ToggleOption(
            label = "Format blank tag (permanent!)",
            description = "Try to format a blank NFC tag as a skylander. " +
                "This requires permamnently locking access codes. " +
                "Once formatted, the tag can only be used for Skylanders. " +
                "(Cards with backdoor commands can still be reset with other tools)",
            checked = formatBlankTag,
            enabled = true,
            onCheckedChange = { newState -> appState.formatBlankTag.update { newState } },
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
        ) {
            Button(
                onClick = {
                    if (writeState == WriteState.None) {
                        appState.writeState.update { WriteState.WaitingForTag }
                    }
                },
                enabled = formatBlankTag || (tagContents != null && writeData),
            ) {
                Text(
                    "Write",
                    style = Typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (writeState != WriteState.None) {
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
                modifier = Modifier
                    .fillMaxSize(0.5f)
                    .aspectRatio(0.75f)
                    .background(MaterialTheme.colorScheme.background),
            ) {

            }
        }
    }
}

@Composable
fun DataPage(appState: AppState) {
    Text("Data viewer is TODO :(")
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NFCLandersTheme {
        Text(
            text = "Hello Preview!",
            textAlign = TextAlign.Center,
            modifier = Modifier,
        )
    }
}

