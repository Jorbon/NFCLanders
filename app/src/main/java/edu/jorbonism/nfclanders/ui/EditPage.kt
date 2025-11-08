package edu.jorbonism.nfclanders.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import edu.jorbonism.nfclanders.NfcState
import edu.jorbonism.nfclanders.WriteError
import edu.jorbonism.nfclanders.formatByte
import edu.jorbonism.nfclanders.formatByteArray
import edu.jorbonism.nfclanders.tag.TagContents
import edu.jorbonism.nfclanders.tag.TagHeader
import edu.jorbonism.nfclanders.enums.Character
import kotlinx.coroutines.flow.update
import java.nio.charset.Charset
import kotlin.math.pow

enum class Popup {
    None,
    ToyType,
    Hat,
    Trinket,
}


@Composable
fun <T> EditField(
    label: String,
    value: T,
    update: (T) -> Unit,
    toString: (T) -> String,
    fromString: (String) -> T?,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        var valueText by rememberSaveable { mutableStateOf(toString(value)) }

        // Detect if something external changed the value and update the text
        var oldValue by rememberSaveable { mutableStateOf(value) }
        if (oldValue != value) {
            oldValue = value
            valueText = toString(value)
        }

        Text(
            label,
            style = Typography.bodyLarge,
            modifier = Modifier
                .padding(10.dp)
        )
        TextField(
            valueText,
            textStyle = Typography.bodySmall,
            isError = toString(value) != valueText,
            onValueChange = {
                valueText = it
                fromString(valueText)?.let { newValue ->
                    if (it == toString(newValue) && newValue != value) {
                        oldValue = newValue
                        update(newValue)
                    }
                }
            },
            enabled = enabled,
            modifier = Modifier
                .padding(10.dp)
                .weight(1.0f)
        )
    }
}


@Composable
fun SearchSelect(exit: () -> Unit) {
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
                .fillMaxWidth(1.0f)
                .fillMaxHeight(0.8f)
                .background(MaterialTheme.colorScheme.background)
        ) {

        }
    }
}


@Composable
fun EditContents(appState: AppState, popup: Boolean, openPopup: (Popup) -> Unit) {
    val tagContentsOption by appState.tagContents.collectAsState()
    val tagContents = tagContentsOption?: return
    val header = tagContents.header?: return

    Text(
        "Identity",
        style = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            fontStyle = FontStyle.Italic,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
    )

    Button(
        onClick = { openPopup(Popup.ToyType) },
        enabled = !popup,
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
                style = TextStyle(
                    fontSize = Typography.titleLarge.fontSize,
                    fontWeight = FontWeight.SemiBold,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 7.dp)
                    .fillMaxWidth()
            )
            Text(
                Character.entries.getOrNull(header.toyType.characterID.toInt())?.type?.toString()?: "Unknown",
                style = TextStyle(
                    fontSize = Typography.labelSmall.fontSize,
                    fontWeight = FontWeight.Normal,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(3.dp)
                    .fillMaxWidth()
            )
            Text(
                header.toyType.game.toString(),
                style = TextStyle(
                    fontSize = Typography.labelSmall.fontSize,
                    fontWeight = FontWeight.Light,
                    fontStyle = FontStyle.Italic,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(0.dp)
                    .fillMaxWidth()
            )
        }
    }

    EditField(
        "Trading card ID",
        header.tradingCardID.toLong(),
        {
            header.tradingCardID = it.toULong()
            appState.tagContents.update { TagContents(header, tagContents.data) }
        },
        { it.toULong().toString() },
        { try {
            val n = it.toULong()
            if (n < 29.0.pow(10.0).toULong()) n.toLong() else null
        } catch (e: Exception) { null } },
        enabled = !popup,
    )


    Text(
        if (tagContents.data == null) "No Content Data found on tag" else "Content Data",
        style = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            fontStyle = FontStyle.Italic,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(top = 30.dp)
            .fillMaxWidth()
    )

    val data = tagContents.data?: return

    EditField(
        "Nickname",
        data.nickname,
        {
            data.nickname = it
            appState.tagContents.update { TagContents(header, data) }
        },
        { it },
        { try { if (it.toByteArray(Charsets.ISO_8859_1).size <= 14) it else null } catch (e: Exception) { null } },
        enabled = !popup,
    )

    EditField(
        "Money",
        data.money.toShort(),
        {
            data.money = it.toUShort()
            appState.tagContents.update { TagContents(header, data) }
        },
        { it.toUShort().toString() },
        { try { it.toUShort().toShort() } catch (e: Exception) { null } },
        enabled = !popup,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            "Upgrades",
            style = Typography.bodyLarge,
            modifier = Modifier
                .padding(10.dp)
                .weight(1.0f)
        )

        Text(
            "On path",
            style = Typography.bodySmall,
            modifier = Modifier
        )
        Checkbox(
            data.upgrades.onPath,
            onCheckedChange = {
                data.upgrades.onPath = it
                appState.tagContents.update { TagContents(header, data) }
            },
            enabled = !popup,
            modifier = Modifier
                .padding(end = 10.dp)
        )
        Text(
            "Bottom path",
            style = Typography.bodySmall,
            modifier = Modifier
        )
        Checkbox(
            data.upgrades.bottomPath,
            onCheckedChange = {
                data.upgrades.bottomPath = it
                appState.tagContents.update { TagContents(header, data) }
            },
            enabled = !popup,
            modifier = Modifier
                .padding(end = 20.dp)
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "Main Line",
            style = Typography.bodySmall,
            modifier = Modifier
                .weight(1.0f)
        )
        arrayOf(
            Pair(data.upgrades.main1, { it: Boolean -> data.upgrades.main1 = it }),
            Pair(data.upgrades.main2, { it: Boolean -> data.upgrades.main2 = it }),
            Pair(data.upgrades.main3, { it: Boolean -> data.upgrades.main3 = it }),
            Pair(data.upgrades.main4, { it: Boolean -> data.upgrades.main4 = it }),
        ).forEach {
            Checkbox(
                it.first,
                onCheckedChange = { checked ->
                    it.second(checked)
                    appState.tagContents.update { TagContents(header, data) }
                },
                enabled = !popup,
                modifier = Modifier
                    .padding(start = 10.dp)
            )
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "Path",
            style = Typography.bodySmall,
            modifier = Modifier
                .weight(1.0f)
        )
        arrayOf(
            Pair(data.upgrades.path1, { it: Boolean -> data.upgrades.path1 = it }),
            Pair(data.upgrades.path2, { it: Boolean -> data.upgrades.path2 = it }),
            Pair(data.upgrades.path3, { it: Boolean -> data.upgrades.path3 = it }),
        ).forEach {
            Checkbox(
                it.first,
                onCheckedChange = { checked ->
                    it.second(checked)
                    appState.tagContents.update { TagContents(header, data) }
                },
                enabled = !popup,
                modifier = Modifier
                    .padding(start = 10.dp)
            )
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "Soul Gem",
            style = Typography.bodySmall,
            modifier = Modifier
                .weight(1.0f)
        )
        Checkbox(
            data.upgrades.soulGem,
            onCheckedChange = {
                data.upgrades.soulGem = it
                appState.tagContents.update { TagContents(header, data) }
            },
            enabled = !popup,
            modifier = Modifier
                .padding(start = 10.dp)
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "Wow Pow",
            style = Typography.bodySmall,
            modifier = Modifier
                .weight(1.0f)
        )
        Checkbox(
            data.upgrades.wowPow,
            onCheckedChange = {
                data.upgrades.wowPow = it
                appState.tagContents.update { TagContents(header, data) }
            },
            enabled = !popup,
            modifier = Modifier
                .padding(start = 10.dp)
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "Alt Path",
            style = Typography.bodySmall,
            modifier = Modifier
                .weight(1.0f)
        )
        arrayOf(
            Pair(data.upgrades.altPath1, { it: Boolean -> data.upgrades.altPath1 = it }),
            Pair(data.upgrades.altPath2, { it: Boolean -> data.upgrades.altPath2 = it }),
            Pair(data.upgrades.altPath3, { it: Boolean -> data.upgrades.altPath3 = it }),
        ).forEach {
            Checkbox(
                it.first,
                onCheckedChange = { checked ->
                    it.second(checked)
                    appState.tagContents.update { TagContents(header, data) }
                },
                enabled = !popup,
                modifier = Modifier
                    .padding(start = 10.dp)
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 10.dp)
    ) {
        Text(
            "Hat",
            style = Typography.bodyLarge,
            modifier = Modifier
                .padding(10.dp)
                .weight(1.0f)
        )
        Button(
            onClick = { openPopup(Popup.Hat) },
            enabled = !popup,
            modifier = Modifier
                .padding(start = 20.dp)
        ) {
            Text(
                data.hat.label,
                style = Typography.bodyLarge,
                modifier = Modifier
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 10.dp)
    ) {
        Text(
            "Trinket",
            style = Typography.bodyLarge,
            modifier = Modifier
                .padding(10.dp)
                .weight(1.0f)
        )
        Button(
            onClick = { openPopup(Popup.Trinket) },
            enabled = !popup,
            modifier = Modifier
                .padding(start = 20.dp)
        ) {
            Text(
                data.trinket.label,
                style = Typography.bodyLarge,
                modifier = Modifier
            )
        }
    }

    EditField(
        "XP (Spyro's Adventure)",
        data.xp1.toInt(),
        {
            data.xp1 = it.toUInt()
            appState.tagContents.update { TagContents(header, data) }
        },
        { it.toUInt().toString() },
        { try { it.toUInt().toInt() } catch (e: Exception) { null } },
        enabled = !popup,
    )

    EditField(
        "XP (Giants)",
        data.xp2.toShort(),
        {
            data.xp2 = it.toUShort()
            appState.tagContents.update { TagContents(header, data) }
        },
        { it.toUShort().toString() },
        { try { it.toUShort().toShort() } catch (e: Exception) { null } },
        enabled = !popup,
    )

    EditField(
        "XP (Swap Force +)",
        data.xp3.toInt(),
        {
            data.xp3 = it.toUInt()
            appState.tagContents.update { TagContents(header, data) }
        },
        { it.toUInt().toString() },
        { try { it.toUInt().toInt() } catch (e: Exception) { null } },
        enabled = !popup,
    )
}

@Composable
fun EditPage(appState: AppState) {
    val tagContents by appState.tagContents.collectAsState()
    val readDataError by appState.readDataError.collectAsState()

    var currentPopup by rememberSaveable { mutableStateOf(Popup.None) }

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

            tagContents?.let {
                EditContents(
                    appState,
                    currentPopup != Popup.None,
                    { currentPopup = it },
                )
            }
        }
    }

    when (currentPopup) {
        Popup.None -> {}
        Popup.ToyType -> SearchSelect(
            { currentPopup = Popup.None }
        )
        Popup.Hat -> SearchSelect(
            { currentPopup = Popup.None }
        )
        Popup.Trinket -> SearchSelect(
            { currentPopup = Popup.None }
        )
    }
}
