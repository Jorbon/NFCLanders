package edu.jorbonism.nfclanders

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import edu.jorbonism.nfclanders.enums.ToyType
import edu.jorbonism.nfclanders.tag.TagContents
import edu.jorbonism.nfclanders.tag.TagData
import edu.jorbonism.nfclanders.tag.TagHeader
import edu.jorbonism.nfclanders.ui.NFCLandersApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class MainActivity : ComponentActivity() {

    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(this)
    }
    private var pendingIntent: PendingIntent? = null
    private var connection = TagConnectionNFC()
    private val appState: AppState by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NFCLandersApp(appState)
        }

        if (nfcAdapter == null) {
            Toast.makeText(this, "Could not get NFC adapter!", Toast.LENGTH_SHORT).show()
        } else if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Turn on NFC!", Toast.LENGTH_SHORT).show()
        }

        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        }

        val header = TagHeader()
        header.toyType = ToyType.table[8]
        header.tradingCardID = 25u
        appState.tagContents.update { TagContents(header, TagData()) }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            arrayOf(
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            ),
            arrayOf(
                arrayOf(
                    NfcA::class.java.name,
                    NdefFormatable::class.java.name,
                    MifareClassic::class.java.name,
                ),
            ),
        )
    }

    override fun onPause() {
        if (this.isFinishing) {
            nfcAdapter?.disableForegroundDispatch(this)
        }
        super.onPause()
    }

    override fun onDestroy() {
        connection.reset()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent?.action) {
            connection.open(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            } ?: return)

            when (appState.nfcState.value) {
                NfcState.ReadingContents -> {
                    val result = TagContents.readFromConnection(connection)
                    appState.tagContents.update { result.first }
                    appState.readDataError.update { result.second }
                }
                NfcState.ReadingDump -> {
                    val result = connection.readDump()
                    appState.tagDump.update { result.first }
                    appState.readDumpError.update { result.second }
                }
                NfcState.WriteWaitingForTag -> {
                    if (appState.formatBlankTag.value) {
                        val error = connection.formatBlankTag()
                        appState.writeError.update { error?.let { WriteError(WriteError.Stage.Formatting, it) } }
                        if (error != null) {
                            appState.nfcState.update { NfcState.WriteFailure }
                            return
                        }
                    }
                    if (appState.writeData.value) {
                        val error = appState.tagContents.value?.writeToConnection(connection, appState.writeHeader.value)
                        appState.writeError.update { error }
                        if (error != null) {
                            appState.nfcState.update { NfcState.WriteFailure }
                            return
                        }
                    }
                    appState.nfcState.update { NfcState.WriteSuccess }
                }
                else -> return
            }
        }
    }
}

