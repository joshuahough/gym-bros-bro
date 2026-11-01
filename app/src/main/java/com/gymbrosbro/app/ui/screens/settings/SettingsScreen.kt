package com.gymbrosbro.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.data.BodyweightRepository
import com.gymbrosbro.app.data.db.entity.BodyweightEntryEntity
import com.gymbrosbro.app.data.settings.SettingsRepository
import com.gymbrosbro.app.domain.units.UnitSystem
import com.gymbrosbro.app.domain.units.Units
import com.gymbrosbro.app.ui.LocalUnitSystem
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.NumberInputDialog
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelChip
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.PixelTopBar
import com.gymbrosbro.app.ui.display
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val bodyweight: BodyweightRepository,
) : ViewModel() {
    val keepScreenOn: StateFlow<Boolean> = settings.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val latestBodyweight: StateFlow<BodyweightEntryEntity?> = bodyweight.observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setUnits(system: UnitSystem) = viewModelScope.launch { settings.setUnitSystem(system) }
    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { settings.setKeepScreenOn(enabled) }
    fun recordBodyweight(weightKg: Double) = viewModelScope.launch { bodyweight.record(weightKg) }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c, _ -> SettingsViewModel(c.settings, c.bodyweightRepository) }
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val latest by viewModel.latestBodyweight.collectAsStateWithLifecycle()
    var editBodyweight by remember { mutableStateOf(false) }
    val units = LocalUnitSystem.current

    Column(Modifier.fillMaxSize()) {
        PixelTopBar("SETTINGS", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingPanel("UNITS", "Changing units converts everything; nothing is lost.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelChip("Pounds · miles", units == UnitSystem.IMPERIAL, { viewModel.setUnits(UnitSystem.IMPERIAL) })
                    PixelChip("Kilograms · km", units == UnitSystem.METRIC, { viewModel.setUnits(UnitSystem.METRIC) })
                }
            }
            SettingPanel("BODYWEIGHT", "Used for calisthenics volume. Each workout keeps the weight you were at that day.") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val l = latest
                    Text(
                        if (l == null) "Not set" else "${Units.formatWeight(l.weightKg, units)}  ·  ${l.date.display()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = GbbColors.Chalk,
                        modifier = Modifier.weight(1f),
                    )
                    PixelButton("UPDATE", onClick = { editBodyweight = true }, small = true)
                }
            }
            SettingPanel("KEEP SCREEN AWAKE", "Stops the screen locking during a workout.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelChip("On", keepScreenOn, { viewModel.setKeepScreenOn(true) })
                    PixelChip("Off", !keepScreenOn, { viewModel.setKeepScreenOn(false) })
                }
            }
        }
    }

    if (editBodyweight) {
        NumberInputDialog(
            title = "BODYWEIGHT",
            unitLabel = Units.weightUnit(units),
            initial = latest?.let { Units.formatNumber(Units.kgToDisplay(it.weightKg, units), 1) }.orEmpty(),
            onConfirm = { value ->
                viewModel.recordBodyweight(Units.displayToKg(value, units))
                editBodyweight = false
            },
            onDismiss = { editBodyweight = false },
        )
    }
}

@Composable
private fun SettingPanel(title: String, description: String, content: @Composable () -> Unit) {
    PixelPanel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = GbbColors.Cyan)
            Text(description, style = MaterialTheme.typography.bodySmall, color = GbbColors.Fog)
            content()
        }
    }
}
