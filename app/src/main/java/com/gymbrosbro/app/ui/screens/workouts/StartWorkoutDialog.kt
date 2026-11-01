package com.gymbrosbro.app.ui.screens.workouts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gymbrosbro.app.data.db.entity.WorkoutSessionEntity
import com.gymbrosbro.app.ui.components.PixelButton
import com.gymbrosbro.app.ui.components.PixelDialog
import com.gymbrosbro.app.ui.theme.GbbColors

/**
 * Remembers which start was blocked by an in-progress workout and shows the
 * resume / discard choice. Call [request] with a template id (null = quick).
 */
class StartWorkoutController(
    private val viewModel: WorkoutsViewModel,
    private val onOpenSession: (Long) -> Unit,
) {
    var pending by mutableStateOf<Pair<WorkoutSessionEntity, Long?>?>(null)
        private set

    fun request(templateId: Long?) {
        viewModel.start(templateId) { result ->
            when (result) {
                is StartResult.Started -> onOpenSession(result.sessionId)
                is StartResult.Blocked -> pending = result.inProgress to templateId
            }
        }
    }

    fun resume() {
        val (session, _) = pending ?: return
        pending = null
        onOpenSession(session.id)
    }

    fun discardAndStart() {
        val (session, templateId) = pending ?: return
        pending = null
        viewModel.discardAndStart(session.id, templateId, onOpenSession)
    }

    fun dismiss() {
        pending = null
    }
}

@Composable
fun rememberStartWorkoutController(viewModel: WorkoutsViewModel, onOpenSession: (Long) -> Unit) =
    remember(viewModel) { StartWorkoutController(viewModel, onOpenSession) }

@Composable
fun StartWorkoutDialog(controller: StartWorkoutController) {
    val (session, _) = controller.pending ?: return
    PixelDialog("WORKOUT IN PROGRESS", onDismiss = controller::dismiss) {
        Text(
            "\"${session.name}\" is still going. Pick up where you left off, or toss it and start fresh?",
            style = MaterialTheme.typography.bodyLarge,
            color = GbbColors.Chalk,
        )
        PixelButton("RESUME", onClick = controller::resume, modifier = Modifier.fillMaxWidth())
        PixelButton(
            "DISCARD & START NEW",
            onClick = controller::discardAndStart,
            color = GbbColors.Red,
            modifier = Modifier.fillMaxWidth(),
        )
        PixelButton(
            "CANCEL",
            onClick = controller::dismiss,
            color = GbbColors.Steel,
            contentColor = GbbColors.Chalk,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
