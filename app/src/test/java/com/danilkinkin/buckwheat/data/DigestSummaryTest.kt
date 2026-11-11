package com.danilkinkin.buckwheat.data

import com.danilkinkin.buckwheat.data.entities.Category
import com.danilkinkin.buckwheat.data.entities.Transaction
import com.danilkinkin.buckwheat.data.entities.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

class DigestSummaryTest {

    private val today = LocalDate.of(2026, 11, 10)
    private val food = Category(id = 1, name = "Food")
    private val travel = Category(id = 2, name = "Travel")

    private fun spend(daysAgo: Long, amount: String, categoryId: Int? = 1) = Transaction(
        type = TransactionType.SPENT,
        value = BigDecimal(amount),
        date = Date.from(
            today.minusDays(daysAgo).atStartOfDay(ZoneOffset.UTC).toInstant()
        ),
        categoryId = categoryId,
    )

    @Test
    fun `window covers today and the six days before it`() {
        val summary = composeDigestSummary(
            transactions = listOf(
                spend(0, "100"),
                spend(6, "200"),
                spend(7, "400"),
            ),
            categories = listOf(food),
            today = today,
        )

        assertEquals(2, summary.count)
        assertEquals(0, summary.total.compareTo(BigDecimal("300")))
    }

    @Test
    fun `top category is chosen by total, not count`() {
        val summary = composeDigestSummary(
            transactions = listOf(
                spend(1, "50", food.id),
                spend(2, "50", food.id),
                spend(3, "300", travel.id),
            ),
            categories = listOf(food, travel),
            today = today,
        )

        assertEquals("Travel", summary.topCategory)
        assertEquals(0, summary.topAmount.compareTo(BigDecimal("300")))
    }

    @Test
    fun `income does not enter the digest`() {
        val summary = composeDigestSummary(
            transactions = listOf(
                spend(1, "100"),
                Transaction(
                    type = TransactionType.INCOME,
                    value = BigDecimal("9999"),
                    date = Date.from(today.atStartOfDay(ZoneOffset.UTC).toInstant()),
                    categoryId = 1,
                ),
            ),
            categories = listOf(food),
            today = today,
        )

        assertEquals(1, summary.count)
        assertEquals(0, summary.total.compareTo(BigDecimal("100")))
    }

    @Test
    fun `empty week stays empty`() {
        val summary = composeDigestSummary(
            transactions = emptyList(),
            categories = emptyList(),
            today = today,
        )

        assertEquals(0, summary.count)
        assertEquals(null, summary.topCategory)
    }
}
