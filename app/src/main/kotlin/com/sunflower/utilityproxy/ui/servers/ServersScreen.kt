package com.sunflower.utilityproxy.ui.servers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunflower.utilityproxy.R
import com.sunflower.utilityproxy.data.local.ServerEntity
import com.sunflower.utilityproxy.tunnel.SunflowerVpnService
import com.sunflower.utilityproxy.tunnel.TunnelState
import com.sunflower.utilityproxy.ui.components.GlassCard
import com.sunflower.utilityproxy.ui.theme.ShapePill
import com.sunflower.utilityproxy.ui.theme.SunflowerGold

private const val TAG = "ServersScreen"

/**
 * Первый экран, откуда реально можно нажать "Подключиться": проверяет
 * VpnService.prepare() (системный диалог согласия), при необходимости
 * запрашивает его через ActivityResult, затем стартует SunflowerVpnService.
 *
 * Реального туннелирования трафика всё ещё не будет (LibXrayBridge пока
 * бросает исключение, пока не подключён настоящий .aar — см.
 * ARCHITECTURE.md), но весь путь ДО этой точки теперь реален и нажимаем,
 * а не просто написан в файлах.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    onOpenSubscriptions: () -> Unit,
    viewModel: ServersViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val tunnelState by viewModel.tunnelState.collectAsStateWithLifecycle()
    var pendingServer by remember { mutableStateOf<ServerEntity?>(null) }

    val vpnConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val server = pendingServer
        pendingServer = null
        if (result.resultCode == Activity.RESULT_OK && server != null) {
            startTunnel(context, server, viewModel)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_servers)) },
                actions = { TextButton(onClick = onOpenSubscriptions) { Text(stringResource(R.string.nav_subscriptions)) } },
            )
        },
    ) { padding ->
        if (servers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.servers_empty))
                    TextButton(onClick = onOpenSubscriptions) { Text(stringResource(R.string.action_add_subscription)) }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(servers, key = { it.id }) { server ->
                    val isThisConnected = (tunnelState as? TunnelState.Connected)?.serverId == server.id
                    val isBusy = tunnelState is TunnelState.Connecting
                    ServerRow(
                        server = server,
                        isConnected = isThisConnected,
                        isBusy = isBusy,
                        onToggleFavorite = { viewModel.toggleFavorite(server) },
                        onConnect = {
                            val consentIntent = VpnService.prepare(context)
                            if (consentIntent != null) {
                                pendingServer = server
                                vpnConsentLauncher.launch(consentIntent)
                            } else {
                                startTunnel(context, server, viewModel)
                            }
                        },
                        onDisconnect = { context.stopService(Intent(context, SunflowerVpnService::class.java)) },
                    )
                }
            }
        }
    }
}

private fun startTunnel(context: Context, server: ServerEntity, viewModel: ServersViewModel) {
    val configJson = try {
        viewModel.buildConfigFor(server)
    } catch (e: IllegalArgumentException) {
        Log.e(TAG, "Не удалось собрать конфиг для ${server.name}: ${e.message}")
        return
    }
    val intent = Intent(context, SunflowerVpnService::class.java).apply {
        putExtra(SunflowerVpnService.EXTRA_SERVER_ID, server.id)
        putExtra(SunflowerVpnService.EXTRA_XRAY_CONFIG_JSON, configJson)
    }
    context.startService(intent)
}

@Composable
private fun ServerRow(
    server: ServerEntity,
    isConnected: Boolean,
    isBusy: Boolean,
    onToggleFavorite: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (server.isFavorite) Icons.Filled.Star else Icons.Filled.Star,
                        contentDescription = stringResource(
                            if (server.isFavorite) R.string.favorite_remove else R.string.favorite_add,
                        ),
                        tint = if (server.isFavorite) SunflowerGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column {
                    Text(server.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.server_summary, server.protocol, server.host, server.port),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isConnected) {
                TextButton(onClick = onDisconnect) { Text(stringResource(R.string.action_disconnect)) }
            } else {
                Button(onClick = onConnect, enabled = !isBusy, shape = ShapePill) {
                    Text(stringResource(if (isBusy) R.string.action_connecting else R.string.action_connect))
                }
            }
        }
    }
}
