package com.sunflower.utilityproxy.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sunflower.utilityproxy.R
import com.sunflower.utilityproxy.ui.components.GlassCard

/**
 * Пункт 78 промта: "Если URL неизвестен — не добавлять фиктивный URL."
 * Website/Privacy Policy сюда намеренно не включены — у проекта их пока
 * нет, а придумывать placeholder-ссылку — ровно то, что запрещено.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        GlassCard(modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sunflower Utility Proxy", style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.about_version, "0.10"),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}
