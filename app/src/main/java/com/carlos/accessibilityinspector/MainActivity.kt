package com.carlos.accessibilityinspector

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private lateinit var preferences: InspectorPreferences
    private lateinit var repository: LogRepository
    private var state by mutableStateOf(InspectorUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = InspectorPreferences(applicationContext)
        repository = LogRepository.get(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InspectorScreen(
                        state = state,
                        onOpenSettings = {
                            try {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (_: ActivityNotFoundException) {
                                toast("Este dispositivo no ofrece la pantalla de ajustes de accesibilidad")
                            }
                        },
                        onModeChange = {
                            preferences.captureMode = it
                            refreshState()
                        },
                        onSelectedPackageChange = {
                            preferences.selectedPackage = it
                            refreshState()
                        },
                        onUseObservedPackage = {
                            preferences.selectedPackage = preferences.lastObservedPackage
                            refreshState()
                        },
                        onStart = ::startCapture,
                        onStop = ::stopCapture,
                        onClear = {
                            repository.clearLogs()
                            refreshState()
                            toast("Registros eliminados")
                        },
                        onShare = ::shareLatestLog,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::preferences.isInitialized) refreshState()
    }

    private fun startCapture() {
        if (!AccessibilityUtils.isInspectorServiceEnabled(this)) {
            toast("Habilita primero el servicio de accesibilidad")
            refreshState()
            return
        }
        if (
            preferences.captureMode == CaptureMode.CABIFY_ONLY &&
            preferences.selectedPackage.isBlank()
        ) {
            toast("Introduce o selecciona primero el paquete de Cabify")
            return
        }
        runCatching {
            repository.startSession(preferences.captureMode, preferences.selectedPackage)
        }.onSuccess {
            preferences.captureEnabled = true
            refreshState()
            toast("Captura iniciada")
        }.onFailure {
            preferences.captureEnabled = false
            refreshState()
            toast("No se pudo crear el archivo de captura")
        }
    }

    private fun stopCapture() {
        repository.stopSession()
        preferences.captureEnabled = false
        refreshState()
        toast("Captura detenida")
    }

    private fun shareLatestLog() {
        val file = repository.latestLog()
        if (file == null) {
            toast("Todavía no hay un registro para compartir")
            return
        }
        runCatching { ShareUtils.shareLog(this, file) }
            .onFailure { toast("No se pudo abrir el menú para compartir") }
    }

    private fun refreshState() {
        if (preferences.captureEnabled && repository.activeSessionId() == null) {
            preferences.captureEnabled = false
        }
        state = InspectorUiState(
            serviceEnabled = AccessibilityUtils.isInspectorServiceEnabled(this),
            captureEnabled = preferences.captureEnabled,
            captureMode = preferences.captureMode,
            selectedPackage = preferences.selectedPackage,
            lastObservedPackage = preferences.lastObservedPackage,
            currentLogSize = repository.currentSize(),
            hasLog = repository.latestLog() != null,
        )
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

private data class InspectorUiState(
    val serviceEnabled: Boolean = false,
    val captureEnabled: Boolean = false,
    val captureMode: CaptureMode = CaptureMode.ALL_APPS,
    val selectedPackage: String = "",
    val lastObservedPackage: String = "",
    val currentLogSize: Long = 0,
    val hasLog: Boolean = false,
)

@Composable
private fun InspectorScreen(
    state: InspectorUiState,
    onOpenSettings: () -> Unit,
    onModeChange: (CaptureMode) -> Unit,
    onSelectedPackageChange: (String) -> Unit,
    onUseObservedPackage: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit,
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Accessibility Inspector", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Inspector local de metadatos de accesibilidad. No pulsa, escribe ni automatiza.",
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider()
            Text("Servicio de accesibilidad", style = MaterialTheme.typography.titleMedium)
            StatusText(
                enabled = state.serviceEnabled,
                enabledText = "Habilitado",
                disabledText = "Deshabilitado",
            )
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir ajustes de accesibilidad")
            }

            HorizontalDivider()
            Text("Captura", style = MaterialTheme.typography.titleMedium)
            StatusText(
                enabled = state.captureEnabled,
                enabledText = "GRABANDO",
                disabledText = "Detenida",
            )

            CaptureModeOption(
                label = "Todas las aplicaciones",
                selected = state.captureMode == CaptureMode.ALL_APPS,
                enabled = !state.captureEnabled,
                onClick = { onModeChange(CaptureMode.ALL_APPS) },
            )
            CaptureModeOption(
                label = "Solo Cabify",
                selected = state.captureMode == CaptureMode.CABIFY_ONLY,
                enabled = !state.captureEnabled,
                onClick = { onModeChange(CaptureMode.CABIFY_ONLY) },
            )

            Text(
                "Último paquete observado: ${state.lastObservedPackage.ifBlank { "ninguno" }}",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = onUseObservedPackage,
                enabled = !state.captureEnabled && state.lastObservedPackage.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Usar el paquete observado")
            }
            OutlinedTextField(
                value = state.selectedPackage,
                onValueChange = onSelectedPackageChange,
                enabled = !state.captureEnabled,
                label = { Text("Paquete de Cabify") },
                supportingText = { Text("Ejemplo ilustrativo: com.empresa.aplicacion") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onStart,
                    enabled = state.serviceEnabled && !state.captureEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Iniciar")
                }
                Button(
                    onClick = onStop,
                    enabled = state.captureEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Detener")
                }
            }

            Text(
                "Tamaño del registro actual: " +
                    Formatter.formatShortFileSize(androidx.compose.ui.platform.LocalContext.current, state.currentLogSize),
            )
            OutlinedButton(
                onClick = onShare,
                enabled = state.hasLog && !state.captureEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Compartir registro")
            }
            OutlinedButton(
                onClick = { showClearConfirmation = true },
                enabled = state.hasLog && !state.captureEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Borrar registros")
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Los registros permanecen privados en este teléfono hasta que eliges Compartir.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Borrar registro") },
            text = { Text("¿Eliminar los registros de diagnóstico de esta aplicación?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClear()
                    },
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun CaptureModeOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun StatusText(enabled: Boolean, enabledText: String, disabledText: String) {
    Text(
        text = if (enabled) enabledText else disabledText,
        color = if (enabled) Color(0xFF087F23) else MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.titleSmall,
    )
}
