package com.danilkinkin.buckwheat.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.danilkinkin.buckwheat.data.entities.Transaction
import com.danilkinkin.buckwheat.util.join
import com.danilkinkin.buckwheat.util.tryConvertStringToNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date
import javax.inject.Inject

enum class EditMode { ADD, EDIT }
enum class EditStage { IDLE, CREATING_SPENT, EDIT_SPENT, COMMITTING_SPENT }

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    var mode = MutableStateFlow(EditMode.ADD)
    var stage = MutableStateFlow(EditStage.IDLE)

    var editedTransaction: Transaction? = null
    var currentDate: Date = Date()
    var currentSpent: BigDecimal = BigDecimal.ZERO
    var currentComment = MutableStateFlow("")
    var currentCategoryId = MutableStateFlow<Int?>(null)
    var rawSpentValue = MutableStateFlow("")

    fun startEditingSpent(transaction: Transaction) {
        editedTransaction = transaction
        currentSpent = transaction.value
        currentDate = transaction.date
        currentComment.value = transaction.comment
        currentCategoryId.value = transaction.categoryId
        rawSpentValue.value = tryConvertStringToNumber(transaction.value.toString()).join(third = false)

        stage.value = EditStage.EDIT_SPENT
        mode.value = EditMode.EDIT
    }

    fun startCreatingSpent() {
        currentSpent = BigDecimal.ZERO

        stage.value = EditStage.CREATING_SPENT
    }

    fun modifyEditingSpent(value: BigDecimal) {
        currentSpent = value

        stage.value = EditStage.EDIT_SPENT
    }

    fun resetEditingSpent() {
        currentSpent = BigDecimal.ZERO
        currentDate = Date()
        currentComment.value = ""
        currentCategoryId.value = null
        rawSpentValue.value = ""

        stage.value = EditStage.IDLE
        mode.value = EditMode.ADD
        editedTransaction = null
    }

    fun canCommitEditingSpent(): Boolean {
        if (stage.value !== EditStage.EDIT_SPENT) return false

        val formatSpent = currentSpent
            .setScale(2, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
            .toPlainString()

        return formatSpent != "0"
    }
}