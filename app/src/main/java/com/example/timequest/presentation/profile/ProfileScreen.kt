package com.example.timequest.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.timequest.R
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.presentation.tasks.TaskViewModel

@Composable
fun ProfileScreen(taskViewModel: TaskViewModel) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val completedTasks = tasks.filter { it.isCompleted }
    val totalXp = completedTasks.sumOf(::calculateXp)
    val level = totalXp / 100 + 1
    val levelProgress = (totalXp % 100) / 100f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineMedium
        )

        SectionCard {
            Text(
                text = stringResource(R.string.level_xp_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.level_value, level),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.xp_value, totalXp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { levelProgress },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = stringResource(R.string.completed_tasks),
                value = completedTasks.size.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.current_streak),
                value = stringResource(R.string.streak_placeholder),
                modifier = Modifier.weight(1f)
            )
        }

        SectionCard {
            Text(
                text = stringResource(R.string.achievements_title),
                style = MaterialTheme.typography.titleMedium
            )
            AchievementLine(
                title = stringResource(R.string.achievement_first_done),
                isUnlocked = completedTasks.isNotEmpty()
            )
            AchievementLine(
                title = stringResource(R.string.achievement_three_done),
                isUnlocked = completedTasks.size >= 3
            )
            AchievementLine(
                title = stringResource(R.string.achievement_seven_days),
                isUnlocked = false
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun AchievementLine(
    title: String,
    isUnlocked: Boolean
) {
    val status = if (isUnlocked) {
        stringResource(R.string.achievement_unlocked)
    } else {
        stringResource(R.string.achievement_locked)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun calculateXp(task: TaskEntity): Int {
    val baseXp = when (task.difficulty) {
        "hard" -> 40
        "medium" -> 20
        else -> 10
    }
    val priorityBonus = if (task.priority == "high") 10 else 0
    val dueDateBonus = if (
        task.dueDate != null &&
        task.completedAt != null &&
        task.completedAt <= task.dueDate
    ) {
        5
    } else {
        0
    }
    return baseXp + priorityBonus + dueDateBonus
}
