package com.ops.app.ui.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.SupplierEntity
import com.ops.app.data.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Suppliers list — reached from the Money tab (see MoneyScreen). Just a
 * contact list: who the business buys from, alphabetical (SupplierDao
 * already orders by name), no filters. */
@HiltViewModel
class SupplierListViewModel @Inject constructor(
    supplierRepository: SupplierRepository,
) : ViewModel() {

    val suppliers: StateFlow<List<SupplierEntity>> = supplierRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
