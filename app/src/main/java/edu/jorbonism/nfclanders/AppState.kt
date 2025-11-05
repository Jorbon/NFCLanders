package edu.jorbonism.nfclanders

import androidx.lifecycle.ViewModel
import edu.jorbonism.nfclanders.tag.TagContents
import kotlinx.coroutines.flow.MutableStateFlow

enum class NfcState {
    None,
    ReadingContents,
    ReadingDump,
    WriteWaitingForTag,
    WriteSuccess,
    WriteFailure,
}

class WriteError(val stage: Stage, val message: String) {
    enum class Stage {
        Formatting,
        WritingHeader,
        WritingData,
    }
}

class AppState() : ViewModel() {
    val tagContents: MutableStateFlow<TagContents?> = MutableStateFlow(null)
    val tagDump: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    val writeData: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val writeHeader: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val formatBlankTag: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val nfcState: MutableStateFlow<NfcState> = MutableStateFlow(NfcState.ReadingContents)

    val readDataError: MutableStateFlow<String?> = MutableStateFlow(null)
    val readDumpError: MutableStateFlow<String?> = MutableStateFlow(null)
    val writeError: MutableStateFlow<WriteError?> = MutableStateFlow(null)
}
