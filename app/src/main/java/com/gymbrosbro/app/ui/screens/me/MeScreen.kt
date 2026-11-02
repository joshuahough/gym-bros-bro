package com.gymbrosbro.app.ui.screens.me

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.avatar.AvatarConfig
import com.gymbrosbro.app.data.AwardsRepository
import com.gymbrosbro.app.data.ProgressRepository
import com.gymbrosbro.app.domain.progress.Level
import com.gymbrosbro.app.domain.progress.Trophies
import com.gymbrosbro.app.domain.progress.Xp
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.AvatarView
import com.gymbrosbro.app.ui.components.LevelBar
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.ScreenHeader
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MeViewModel(awards: AwardsRepository, progress: ProgressRepository) : ViewModel() {
    val avatar: StateFlow<AvatarConfig?> = awards.avatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val level: StateFlow<Level> = progress.xp.map { it.level }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Xp.level(0))

    val trophies: StateFlow<Int> = awards.trophies.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

@Composable
fun MeScreen(
    onOpenLocker: () -> Unit,
    onOpenTrophies: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel = appViewModel { c, _ -> MeViewModel(c.awardsRepository, c.progressRepository) }
    val avatar by viewModel.avatar.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()
    val trophies by viewModel.trophies.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("ME")
        PixelPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 60.dp, height = 111.dp)) {
                    avatar?.let { AvatarView(it, 111.dp) }
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text("PLAYER 1", style = MaterialTheme.typography.titleLarge, color = GbbColors.Chalk)
                    LevelBar(level, Modifier.fillMaxWidth().padding(top = 12.dp))
                }
            }
        }
        MenuPanel("LOCKER", "Dress up your bro with the gear you've unlocked.", onOpenLocker)
        MenuPanel("TROPHIES", "$trophies of ${Trophies.all.size} earned. See what's next.", onOpenTrophies)
        MenuPanel("HISTORY", "Every workout you've logged. Tap one to fix a set or change the date.", onOpenHistory)
        MenuPanel("SETTINGS", "Units, bodyweight, reminders, week start, sound and vibration.", onOpenSettings)
    }
}

@Composable
private fun MenuPanel(title: String, body: String, onClick: () -> Unit) {
    PixelPanel(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Column {
            Text("$title  >", style = MaterialTheme.typography.titleLarge, color = GbbColors.Gold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
