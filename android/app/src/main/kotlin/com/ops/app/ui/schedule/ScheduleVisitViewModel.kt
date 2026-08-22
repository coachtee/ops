package com.ops.app.ui.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.EmployeeEntity
import com.ops.app.data.repository.EmployeeRepository
import com.ops.app.data.repository.VisitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ScheduleVisitUiState(
    val jobId: String = "",
    val employeeId: String? = null,
    val scheduledDate: String = LocalDate.now().toString(),
    val startTime: String? = null,
    val isSaving: Boolean = false,
)

@HiltViewModel
class ScheduleVisitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val visitRepository: VisitRepository,
    employeeRepository: EmployeeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleVisitUiState(jobId = checkNotNull(savedStateHandle["jobId"])))
    val uiState: StateFlow<ScheduleVisitUiState> = _uiState.asStateFlow()

    val employees: StateFlow<List<EmployeeEntity>> = employeeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun update(transform: (ScheduleVisitUiState) -> ScheduleVisitUiState) = _uiState.update(transform)

    fun save(onSaved: (String) -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val id = visitRepository.schedule(state.jobId, state.employeeId, state.scheduledDate, state.startTime)
            _uiState.update { it.copy(isSaving = false) }
            onSaved(id)
        }
    }
}
