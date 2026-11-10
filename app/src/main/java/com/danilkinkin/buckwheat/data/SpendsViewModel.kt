package com.danilkinkin.buckwheat.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danilkinkin.buckwheat.data.entities.Category
import com.danilkinkin.buckwheat.data.entities.Transaction
import com.danilkinkin.buckwheat.di.SpendsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Everything the ledger screens read: spends, categories and the display currency.
 *
 * The app is a plain expense tracker, so there is no budget state, daily allowance or period to
 * keep in sync here. Those preferences still exist in storage for the widget, which reads them
 * through [SpendsRepository] directly.
 */
@HiltViewModel
class SpendsViewModel @Inject constructor(
    private val spendsRepository: SpendsRepository,
) : ViewModel() {
    val spends = spendsRepository.getAllSpends()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories = spendsRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currency = spendsRepository.getCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExtendCurrency.none())

    private val _lastRemovedTransaction = MutableStateFlow<Transaction?>(null)
    val lastRemovedTransaction = _lastRemovedTransaction.asStateFlow()

    // Spend handling

    fun addSpent(transactionForAdd: Transaction) {
        viewModelScope.launch {
            spendsRepository.addSpent(transactionForAdd)
        }
    }

    fun removeSpent(transactionForRemove: Transaction, silent: Boolean = false) {
        viewModelScope.launch {
            spendsRepository.removeSpent(transactionForRemove)

            if (!silent) {
                _lastRemovedTransaction.value = transactionForRemove
            }
        }
    }

    fun undoRemoveSpent() {
        viewModelScope.launch {
            _lastRemovedTransaction.value?.let {
                spendsRepository.addSpent(it)
            }
            _lastRemovedTransaction.value = null
        }
    }

    fun clearLastRemovedTransaction() {
        _lastRemovedTransaction.value = null
    }

    // Categories

    fun addCategory(name: String, icon: String, color: Long) {
        viewModelScope.launch {
            spendsRepository.addCategory(name, icon, color)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            spendsRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category, onDeleted: (List<Int>) -> Unit) {
        viewModelScope.launch {
            val affected = spendsRepository.deleteCategory(category.id)
            onDeleted(affected)
        }
    }

    fun restoreCategory(category: Category, transactionIds: List<Int>) {
        viewModelScope.launch {
            spendsRepository.restoreCategory(category, transactionIds)
        }
    }

    // Backup

    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            onReady(spendsRepository.exportBackup())
        }
    }

    fun restoreBackup(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { spendsRepository.restoreBackup(json) }.isSuccess)
        }
    }

    // Currency

    fun changeDisplayCurrency(currency: ExtendCurrency) {
        viewModelScope.launch {
            spendsRepository.changeDisplayCurrency(currency)
        }
    }
}
