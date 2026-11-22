package com.tixi.app.ui.importflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tixi.app.domain.model.ImportRecord
import com.tixi.app.domain.repository.ImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportHistoryViewModel @Inject constructor(
    private val importRepository: ImportRepository,
) : ViewModel() {

    val records: StateFlow<List<ImportRecord>> = importRepository.observeRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(record: ImportRecord) {
        viewModelScope.launch { importRepository.deleteRecord(record.id) }
    }
}
