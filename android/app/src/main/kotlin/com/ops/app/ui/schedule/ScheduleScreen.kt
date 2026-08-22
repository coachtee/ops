package com.ops.app.ui.schedule

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ops.app.ui.components.ActionableListRow
import com.ops.app.ui.components.EmptyState
import com.ops.app.ui.components.SectionHeader
import com.ops.app.ui.components.StatusBadge
import com.ops.app.ui.components.VISIT_STATUS_CHOICES
import com.ops.app.ui.components.formatDate
import com.ops.app.ui.components.labelFor
import com.ops.app.ui.components.visitStatusTone

@Composable
fun ScheduleScreen(
    onOpenVisit: (String) -> Unit,
    onOpenJob: (String) -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScheduleContent(uiState = uiState, onOpenVisit = onOpenVisit, onOpenJob = onOpenJob)
}

/** Stateless render of [ScheduleScreen] — split out for the screenshot pack
 * (see android/README.md); not called from navigation directly.
 *
 * The field-workflow entry point: what needs attention right now (Overdue),
 * what's happening today, what's coming up, and which open jobs haven't
 * been put on the schedule at all yet. New visits are scheduled from a
 * job's own detail screen (job context already known there) rather than a
 * separate job-picker on this screen — tapping an unscheduled job goes
 * straight there. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleContent(
    uiState: ScheduleUiState,
    onOpenVisit: (String) -> Unit,
    onOpenJob: (String) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Schedule") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (uiState.overdue.isNotEmpty()) {
                item { SectionHeader("Overdue") }
                items(uiState.overdue, key = { "overdue-${it.visit.id}" }) { row ->
                    VisitRow(row, onOpenVisit)
                }
            }

            item { SectionHeader("Today") }
            if (uiState.today.isEmpty()) {
                item { EmptyState("Nothing scheduled today", "Visits you schedule from a job's detail screen show up here.") }
            } else {
                items(uiState.today, key = { "today-${it.visit.id}" }) { row -> VisitRow(row, onOpenVisit) }
            }

            item { SectionHeader("Upcoming") }
            if (uiState.upcoming.isEmpty()) {
                item { EmptyState("Nothing else on the schedule", "") }
            } else {
                items(uiState.upcoming, key = { "upcoming-${it.visit.id}" }) { row -> VisitRow(row, onOpenVisit) }
            }

            if (uiState.unscheduledJobs.isNotEmpty()) {
                item { SectionHeader("Unscheduled jobs") }
                items(uiState.unscheduledJobs, key = { "job-${it.id}" }) { job ->
                    ActionableListRow(
                        primary = job.number ?: job.title,
                        secondary = "Not yet scheduled",
                        onClick = { onOpenJob(job.id) },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitRow(row: ScheduleVisitRow, onOpenVisit: (String) -> Unit) {
    ActionableListRow(
        primary = row.customerName.ifBlank { row.jobTitle },
        secondary = listOfNotNull(
            row.jobTitle.takeIf { it != row.customerName },
            "${formatDate(row.visit.scheduledDate)}${row.visit.startTime?.let { " · $it" } ?: ""}",
        ).joinToString(" · "),
        statusBadge = { StatusBadge(labelFor(VISIT_STATUS_CHOICES, row.visit.status), visitStatusTone(row.visit.status)) },
        onClick = { onOpenVisit(row.visit.id) },
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
