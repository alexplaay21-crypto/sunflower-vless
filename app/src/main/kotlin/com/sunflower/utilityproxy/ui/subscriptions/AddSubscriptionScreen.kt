package com.sunflower.utilityproxy.ui.subscriptions

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunflower.utilityproxy.R

private const val NAME_MAX_LENGTH = 25

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionScreen(
    onDone: () -> Unit,
    initialUrl: String? = null,
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf(initialUrl ?: "") }
    var allowInsecure by remember { mutableStateOf(false) }
    var sendHwidCookie by remember { mutableStateOf(false) }
    var clipboardMessage by remember { mutableStateOf<String?>(null) }

    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val clipboardEmptyMessage = stringResource(R.string.clipboard_empty)

    LaunchedEffect(importState) {
        if (importState is ImportState.Success) {
            viewModel.resetImportState()
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_add_subscription)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= NAME_MAX_LENGTH) name = it },
                label = { Text(stringResource(R.string.label_name)) },
                supportingText = { Text(stringResource(R.string.name_counter, name.length, NAME_MAX_LENGTH)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                        if (text.isNullOrBlank()) {
                            clipboardMessage = clipboardEmptyMessage
                        } else {
                            url = text
                            clipboardMessage = null
                        }
                    }) { Text(stringResource(R.string.action_paste)) }
                },
            )
            clipboardMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.label_allow_insecure))
                Switch(checked = allowInsecure, onCheckedChange = { allowInsecure = it })
            }
            if (allowInsecure) {
                Text(
                    stringResource(R.string.warning_allow_insecure),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.label_send_hwid_cookie))
                Switch(checked = sendHwidCookie, onCheckedChange = { sendHwidCookie = it })
            }

            if (importState is ImportState.Error) {
                Text((importState as ImportState.Error).message, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.importFromUrl(name, url, allowInsecure, sendHwidCookie) },
                enabled = url.isNotBlank() && importState !is ImportState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (importState is ImportState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
