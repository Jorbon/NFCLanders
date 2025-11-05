package edu.jorbonism.nfclanders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.jorbonism.nfclanders.AppState
import edu.jorbonism.nfclanders.NfcState
import edu.jorbonism.nfclanders.WriteError
import edu.jorbonism.nfclanders.formatByte
import edu.jorbonism.nfclanders.formatByteArray
import kotlinx.coroutines.flow.update

enum class AppPages(
    val label: String,
    val icon: ImageVector,
) {
    Edit ("Edit", Icons.Default.Build),
    Write("Write", Icons.Default.Edit),
    Data ("Raw Data", Icons.Default.Search),
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun NFCLandersApp(appState: AppState = AppState()) {
    val nfcState by appState.nfcState.collectAsState()
    val currentPage = when (nfcState) {
        NfcState.ReadingContents -> AppPages.Edit
        NfcState.ReadingDump -> AppPages.Data
        else -> AppPages.Write
    }

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
                        onClick = { when (it) {
                            AppPages.Edit -> appState.nfcState.update { NfcState.ReadingContents }
                            AppPages.Data -> appState.nfcState.update { NfcState.ReadingDump }
                            AppPages.Write -> if (currentPage != AppPages.Write) { appState.nfcState.update{ NfcState.None } }
                        } },
                    )
                }
            }
        ) {
            Scaffold(
                topBar = { CenterAlignedTopAppBar(
                    title = { Text(
                        when (currentPage) {
                            AppPages.Edit  -> "Current Data"
                            AppPages.Write -> "Write to Tag"
                            AppPages.Data  -> "View Raw Data"
                        },
                    ) },
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
                    modifier = Modifier
                        .padding(innerPadding)
                ) {
                    when (currentPage) {
                        AppPages.Edit  -> EditPage(appState)
                        AppPages.Write -> WritePage(appState)
                        AppPages.Data  -> DataPage(appState)
                    }
                }
            }
        }
    }
}
