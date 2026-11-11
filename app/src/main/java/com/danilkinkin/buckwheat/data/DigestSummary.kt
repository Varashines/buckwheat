package com.danilkinkin.buckwheat.data

import com.danilkinkin.buckwheat.data.entities.Category
import com.danilkinkin.buckwheat.data.entities.Transaction
import com.danilkinkin.buckwheat.data.entities.TransactionType
import com.danilkinkin.buckwheat.util.toLocalDate
import java.math.BigDecimal
import java.time.LocalDate

/** What a weekly digest says, independent of notifications and storage. */
data class DigestSummary(
    val total: BigDecimal,
    val count: Int,
    val topCategory: String?,
    val topAmount: BigDecimal,
)

/**
 * The seven days ending today, inclusive.
 *
 * Excludes transfers and income by construction: only SPENT rows count. Top category is by total,
 * not by count, so one large purchase does not lose to many small ones.
 */
fun composeDigestSummary(
    transactions: List<Transaction>,
    categories: List<Category>,
    today: LocalDate,
): DigestSummary {
    val start = today.minusDays(6)
    val inWindow = transactions.filter { transaction ->
        transaction.type == TransactionType.SPENT &&
            !transaction.date.toLocalDate().isBefore(start) &&
            !transaction.date.toLocalDate().isAfter(today)
    }

    if (inWindow.isEmpty()) {
        return DigestSummary(
            total = BigDecimal.ZERO,
            count = 0,
            topCategory = null,
            topAmount = BigDecimal.ZERO,
        )
    }

    val byCategory = inWindow
        .groupBy { it.categoryId }
        .mapValues { (_, group) ->
            group.fold(BigDecimal.ZERO) { sum, transaction -> sum + transaction.value }
        }

    val top = byCategory.maxByOrNull { it.value }
    val namesById = categories.associate { it.id to it.name }

    return DigestSummary(
        total = inWindow.fold(BigDecimal.ZERO) { sum, transaction -> sum + transaction.value },
        count = inWindow.size,
        topCategory = top?.key?.let { namesById[it] },
        topAmount = top?.value ?: BigDecimal.ZERO,
    )
}
