package com.gymbrosbro.app.ui.screens.me

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.avatar.AvatarConfig
import com.gymbrosbro.app.avatar.Locker
import com.gymbrosbro.app.avatar.LockerItem
import com.gymbrosbro.app.avatar.LockerSection
import com.gymbrosbro.app.avatar.LockerSlot
import com.gymbrosbro.app.avatar.Unlock
import com.gymbrosbro.app.data.AwardsRepository
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.AvatarView
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.PixelTopBar
import com.gymbrosbro.app.ui.requirement
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A locked item being tried on: shown on the avatar, never saved. */
data class TryOn(val section: LockerSection, val item: LockerItem)

data class LockerUiState(
    val saved: AvatarConfig = AvatarConfig(),
    val unlocked: Set<String> = emptySet(),
    val tryOn: TryOn? = null,
) {
    val shown: AvatarConfig get() = tryOn?.let { it.section.set(saved, it.item.id) } ?: saved
    fun isUnlocked(item: LockerItem) = Locker.isUnlocked(item, unlocked)
}

class LockerViewModel(private val awards: AwardsRepository) : ViewModel() {
    private val tryOn = MutableStateFlow<TryOn?>(null)

    val state: StateFlow<LockerUiState?> = combine(awards.avatar, awards.unlockedIds, tryOn, ::LockerUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun pick(section: LockerSection, item: LockerItem) {
        val s = state.value ?: return
        if (s.isUnlocked(item)) {
            tryOn.value = null
            viewModelScope.launch { awards.setAvatar(section.set(s.saved, item.id)) }
        } else {
            tryOn.value = TryOn(section, item).takeIf { tryOn.value?.item != item }
        }
    }
}

@Composable
fun LockerScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c, _ -> LockerViewModel(c.awardsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return

    Column(Modifier.fillMaxSize()) {
        PixelTopBar("LOCKER", onBack = onBack)
        PixelPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarView(s.shown, 148.dp)
                Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tryOn = s.tryOn
                    if (tryOn != null) {
                        Text("LOCKED", style = MaterialTheme.typography.titleMedium, color = GbbColors.Red)
                        Text(tryOn.item.name, style = MaterialTheme.typography.titleSmall, color = GbbColors.Chalk)
                        Text(tryOn.item.requirement, style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
                    } else {
                        val earned = Locker.items.count { s.isUnlocked(it) }
                        Text("$earned/${Locker.items.size}", style = MaterialTheme.typography.headlineSmall, color = GbbColors.Gold)
                        Text("items unlocked", style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
                        Text(
                            "Tap locked gear to try it on.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GbbColors.Fog,
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Locker.sections.forEach { section ->
                SectionPicker(section, s, onPick = { viewModel.pick(section, it) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionPicker(section: LockerSection, state: LockerUiState, onPick: (LockerItem) -> Unit) {
    val current = section.get(state.shown)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(section.title, style = MaterialTheme.typography.titleMedium, color = GbbColors.Cyan)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Locker.inSlot(section.slot).forEach { item ->
                val selected = item.id == current
                val unlocked = state.isUnlocked(item)
                if (section.slot.isColor) {
                    Swatch(item, selected, unlocked) { onPick(item) }
                } else {
                    StyleChip(item, selected, unlocked) { onPick(item) }
                }
            }
        }
    }
}

private val LockerSlot.isColor: Boolean
    get() = this == LockerSlot.SKIN || this == LockerSlot.HAIR_COLOR || this == LockerSlot.GEAR_COLOR

/** Short lock tag for a chip: "LV 5" or "TROPHY". */
private val LockerItem.lockTag: String
    get() = when (val u = unlock) {
        Unlock.Starter -> ""
        is Unlock.AtLevel -> "LV ${u.level}"
        is Unlock.ByTrophy -> "TROPHY"
    }

@Composable
private fun StyleChip(item: LockerItem, selected: Boolean, unlocked: Boolean, onClick: () -> Unit) {
    PixelPanel(
        modifier = Modifier
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClick = onClick),
        fill = if (selected) GbbColors.Dusk else GbbColors.Night,
        border = when {
            selected && unlocked -> GbbColors.Gold
            selected -> GbbColors.Red
            else -> GbbColors.Steel
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.name,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    !unlocked -> GbbColors.Steel
                    selected -> GbbColors.Gold
                    else -> GbbColors.Chalk
                },
            )
            if (!unlocked) Text(item.lockTag, style = MaterialTheme.typography.labelSmall, color = GbbColors.Fog)
        }
    }
}

@Composable
private fun Swatch(item: LockerItem, selected: Boolean, unlocked: Boolean, onClick: () -> Unit) {
    val description = if (unlocked) item.name else "${item.name}, locked: ${item.requirement}"
    Box(
        modifier = Modifier
            .size(44.dp)
            .semantics {
                this.selected = selected
                contentDescription = description
            }
            .border(3.dp, if (selected) (if (unlocked) GbbColors.Gold else GbbColors.Red) else GbbColors.Steel)
            .padding(5.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().alpha(if (unlocked) 1f else 0.3f).background(item.swatch))
        if (!unlocked) {
            Box(Modifier.size(12.dp, 10.dp).background(GbbColors.Void), contentAlignment = Alignment.Center) {
                Box(Modifier.size(4.dp).background(GbbColors.Fog))
            }
        }
    }
}
