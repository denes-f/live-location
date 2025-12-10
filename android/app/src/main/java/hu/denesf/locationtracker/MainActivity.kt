package hu.denesf.locationtracker

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import hu.denesf.locationtracker.ui.theme.LocationTrackerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setTrackingEnabled(true)
            LocationForegroundService.start(this@MainActivity)
        } else {
            viewModel.setTrackingEnabled(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LocationTrackerTheme {
                val uiState by viewModel.uiState.collectAsState()

                MainScreen(
                    uiState = uiState,
                    onChangeDeviceId = { viewModel.updateDeviceId(it) },
                    onToggleTracking = { enabled ->
                        if (enabled) {
                            if (hasFineLocationPermission()) {
                                viewModel.setTrackingEnabled(true)
                                LocationForegroundService.start(this@MainActivity)
                            } else {
                                // Revert toggle visually until permission is granted
                                viewModel.setTrackingEnabled(false)
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        } else {
                            viewModel.setTrackingEnabled(false)
                            LocationForegroundService.stop(this@MainActivity)
                        }
                    }
                )
            }
        }
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun MainScreen(
    uiState: MainUiState,
    onChangeDeviceId: (String) -> Unit,
    onToggleTracking: (Boolean) -> Unit
) {
    // Only show the dialog automatically when device ID is not set AND DataStore has finished loading it.
    val initialDialogShouldShow = !uiState.deviceIdSet
    var showDeviceIdDialog by remember(uiState.deviceIdSet) {
        mutableStateOf(initialDialogShouldShow)
    }
    var tempDeviceId by remember { mutableStateOf(TextFieldValue(uiState.deviceId)) }

    // First-run / change dialog
    if (showDeviceIdDialog) {
        AlertDialog(
            onDismissRequest = { /* block dismiss on first run */ },
            title = { Text("Set device ID") },
            text = {
                Column {
                    Text("This ID will be used when sending locations to your server.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempDeviceId,
                        onValueChange = { tempDeviceId = it },
                        singleLine = true,
                        label = { Text("Device ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = tempDeviceId.text.trim()
                        if (id.isNotEmpty()) {
                            onChangeDeviceId(id)
                            showDeviceIdDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        )
    }

    // Main content
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Live Location Tracker", style = MaterialTheme.typography.headlineSmall)

            // Device ID row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Device ID", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (uiState.deviceId.isBlank()) "(not set)" else uiState.deviceId,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextButton(
                    onClick = {
                        tempDeviceId = TextFieldValue(uiState.deviceId)
                        showDeviceIdDialog = true
                    }
                ) {
                    Text("Change")
                }
            }

            // Tracking toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tracking", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (uiState.trackingEnabled) "ON (will send locations)"
                        else "OFF",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = uiState.trackingEnabled,
                    onCheckedChange = { onToggleTracking(it) }
                )
            }

            Text(
                text = "Tracking state and device ID are saved, even if you close the app.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}