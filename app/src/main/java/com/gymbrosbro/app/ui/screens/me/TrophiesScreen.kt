package com.gymbrosbro.app.ui.screens.me

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbrosbro.app.data.AwardsRepository
import com.gymbrosbro.app.data.ProgressRepository
import com.gymbrosbro.app.domain.progress.Trophies
import com.gymbrosbro.app.domain.progress.Trophy
import com.gymbrosbro.app.sprites.SpriteImage
import com.gymbrosbro.app.sprites.Sprites
import com.gymbrosbro.app.ui.LocalUnitSystem
import com.gymbrosbro.app.ui.appViewModel
import com.gymbrosbro.app.ui.components.PixelPanel
import com.gymbrosbro.app.ui.components.PixelProgressBar
import com.gymbrosbro.app.ui.components.PixelTopBar
import com.gymbrosbro.app.ui.description
import com.gymbrosbro.app.ui.display
import com.gymbrosbro.app.ui.format
import com.gymbrosbro.app.ui.localDate
import com.gymbrosbro.app.ui.reward
import com.gymbrosbro.app.ui.theme.GbbColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

data class TrophyRow(val trophy: Trophy, val earnedAt: Instant?, val progress: Double)

class TrophiesViewModel(awards: AwardsRepository, progress: ProgressRepository) : ViewModel() {
    val rows: StateFlow<List<TrophyRow>?> = combine(awards.trophies, progress.progress) { earned, p ->
        val earnedAt = earned.associate { it.trophyId to it.earnedAt }
        // Earned first, newest on top; then the closest to done.
        Trophies.all.map { TrophyRow(it, earnedAt[it.id], p.lifetime.value(it.metric)) }
            .sortedWith(
                compareBy<TrophyRow> { it.earnedAt == null }
                    .thenByDescending { it.earnedAt }
                    .thenByDescending { it.progress / it.trophy.target },
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun TrophiesScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c, _ -> TrophiesViewModel(c.awardsRepository, c.progressRepository) }
    val rows by viewModel.rows.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        PixelTopBar("TROPHIES", onBack = onBack)
        val list = rows ?: return@Column
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "${list.count { it.earnedAt != null }} of ${list.size} earned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GbbColors.Fog,
                )
            }
            items(list, key = { it.trophy.id }) { TrophyCard(it) }
        }
    }
}

@Composable
private fun TrophyCard(row: TrophyRow) {
    val units = LocalUnitSystem.current
    val earned = row.earnedAt != null
    val t = row.trophy
    PixelPanel(Modifier.fillMaxWidth(), border = if (earned) GbbColors.Gold else GbbColors.Steel) {
        Row(verticalAlignment = Alignment.Top) {
            SpriteImage(
                Sprites.Star.recolor('f' to if (earned) GbbColors.Gold else GbbColors.Dusk),
                contentDescription = if (earned) "Earned" else "Not earned yet",
                size = 28.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(t.name, style = MaterialTheme.typography.titleSmall, color = if (earned) GbbColors.Gold else GbbColors.Chalk)
                Text(t.description(units), style = MaterialTheme.typography.bodyMedium, color = GbbColors.Fog)
                if (earned) {
                    Text("Earned ${row.earnedAt!!.localDate().display()}", style = MaterialTheme.typography.bodySmall, color = GbbColors.Green)
                } else {
                    PixelProgressBar((row.progress / t.target).toFloat(), GbbColors.Cyan, Modifier.fillMaxWidth().padding(top = 4.dp))
                    Text(
                        "${t.metric.format(row.progress.coerceAtMost(t.target), units)} / ${t.metric.format(t.target, units)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GbbColors.Fog,
                    )
                }
                t.reward?.let { Text("Unlocks: ${it.name}", style = MaterialTheme.typography.bodySmall, color = GbbColors.Cyan) }
            }
        }
    }
}
