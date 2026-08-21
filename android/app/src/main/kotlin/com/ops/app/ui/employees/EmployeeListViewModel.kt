package com.ops.app.ui.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.EmployeeEntity
import com.ops.app.data.repository.EmployeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Employees list — reached from Business Profile/Settings (see
 * BusinessProfileScreen). Just a staff contact list: who works for the
 * business, alphabetical (EmployeeDao already orders by name), no filters. */
@HiltViewModel
class EmployeeListViewModel @Inject constructor(
    employeeRepository: EmployeeRepository,
) : ViewModel() {

    val employees: StateFlow<List<EmployeeEntity>> = employeeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
