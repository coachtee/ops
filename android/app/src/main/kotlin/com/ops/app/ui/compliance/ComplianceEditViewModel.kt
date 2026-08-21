package com.ops.app.ui.compliance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.repository.ComplianceItemRepository
import com.ops.app.ui.navigation.OpsDestinations.orNull
import com.ops.coredomain.ComplianceCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** A pre-filled but unsaved suggestion for the *next* occurrence of a
 * recurring item, offered once the current one is marked done — the owner
 * must explicitly confirm it (see [ComplianceEditViewModel.confirmNextItem]);
 * nothing is ever created silently in the background. */
data class NextItemSuggestion(
    val category: String,
    val title: String,
    val dueDate: String,
)

data class ComplianceEditUiState(
    val itemId: String? = null,
    val category: String = ComplianceCategory.OTHER.wire,
    val title: String = "",
    val dueDate: String = LocalDate.now().toString(),
    val completedDate: String? = null,
    val isRecurring: Boolean = true,
    val notes: String = "",
    val syncState: String? = null,
    val syncError: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val titleError: String? = null,
    val nextItemSuggestion: NextItemSuggestion? = null,
)

/**
 * One screen for create, edit, and view (delete too) — same "always
 * editable, no separate edit mode" spirit as SupplierEditScreen. Marking a
 * recurring item done offers to pre-fill the next occurrence at a
 * category-typical interval — a pure client-side UX nudge, computed here,
 * never on the server (see ComplianceItem's model doc comment). The owner
 * must tap "Add" for it to actually be created; declining does nothing.
 */
@HiltViewModel
class ComplianceEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val complianceItemRepository: ComplianceItemRepository,
) : ViewModel() {

    private val routeItemId: String? = (savedStateHandle.get<String>("complianceItemId")).orNull()

    private val _uiState = MutableStateFlow(ComplianceEditUiState(itemId = routeItemId))
    val uiState: StateFlow<ComplianceEditUiState> = _uiState.asStateFlow()

    /** Whether the item being edited was already marked done before this
     * screen's own edits — used to detect a not-done -> done transition on
     * save, which is the only time the next-item suggestion makes sense. */
    private var wasCompletedOnLoad = false

    init {
        viewModelScope.launch { loadInitial() }
    }

    private suspend fun loadInitial() {
        val id = routeItemId
        val existing = id?.let { complianceItemRepository.getById(it) }
        if (existing != null) {
            wasCompletedOnLoad = existing.completedDate != null
            _uiState.update {
                it.copy(
                    itemId = existing.id,
                    category = existing.category,
                    title = existing.title,
                    dueDate = existing.dueDate,
                    completedDate = existing.completedDate,
                    isRecurring = existing.isRecurring,
                    notes = existing.notes,
                    syncState = existing.syncState,
                    syncError = existing.syncError,
                    isLoading = false,
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun update(transform: (ComplianceEditUiState) -> ComplianceEditUiState) = _uiState.update(transform)

    /** Mirrors ComplianceItemSerializer.validate_title server-side. */
    private fun validate(state: ComplianceEditUiState): ComplianceEditUiState =
        state.copy(titleError = if (state.title.isBlank()) "Title is required" else null)

    fun save(onSaved: () -> Unit) {
        val validated = validate(_uiState.value)
        _uiState.value = validated
        if (validated.titleError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val id = complianceItemRepository.save(
                id = validated.itemId,
                category = validated.category,
                title = validated.title,
                dueDate = validated.dueDate,
                completedDate = validated.completedDate,
                isRecurring = validated.isRecurring,
                notes = validated.notes,
            )
            val justMarkedDone = !wasCompletedOnLoad && validated.completedDate != null
            val suggestion = if (justMarkedDone && validated.isRecurring) {
                suggestedNextItem(validated.category, validated.title, validated.dueDate)
            } else {
                null
            }
            wasCompletedOnLoad = validated.completedDate != null
            _uiState.update { it.copy(isSaving = false, itemId = id, nextItemSuggestion = suggestion) }
            onSaved()
        }
    }

    /** Owner tapped "Add" on the next-item suggestion — creates it as a
     * genuinely new item (a fresh id, PENDING like any manual create). */
    fun confirmNextItem() {
        val suggestion = _uiState.value.nextItemSuggestion ?: return
        viewModelScope.launch {
            complianceItemRepository.save(
                id = null,
                category = suggestion.category,
                title = suggestion.title,
                dueDate = suggestion.dueDate,
                completedDate = null,
                isRecurring = true,
                notes = "",
            )
            _uiState.update { it.copy(nextItemSuggestion = null) }
        }
    }

    fun dismissNextItemSuggestion() {
        _uiState.update { it.copy(nextItemSuggestion = null) }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _uiState.value.itemId ?: return
        viewModelScope.launch {
            complianceItemRepository.delete(id)
            onDeleted()
        }
    }
}

/** A category-typical interval for "add the next one" — a plain client-side
 * convenience, not a claim about actual SARS/CIPC deadlines (which vary by
 * business and aren't computed anywhere in this app). [ComplianceCategory.OTHER]
 * has no sensible default interval, so no suggestion is offered for it. */
private fun suggestedNextItem(category: String, title: String, previousDueDate: String): NextItemSuggestion? {
    val previous = runCatching { LocalDate.parse(previousDueDate) }.getOrNull() ?: return null
    val nextDate = when (category) {
        ComplianceCategory.VAT_RETURN.wire -> previous.plusMonths(2)
        ComplianceCategory.PAYE_UIF_SDL.wire -> previous.plusMonths(1)
        ComplianceCategory.PROVISIONAL_TAX.wire -> previous.plusMonths(6)
        ComplianceCategory.CIPC_ANNUAL_RETURN.wire -> previous.plusYears(1)
        else -> return null
    }
    return NextItemSuggestion(category = category, title = title, dueDate = nextDate.toString())
}
