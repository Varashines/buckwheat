package com.danilkinkin.buckwheat.digest

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class DigestSchedulerTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun `sunday before seven waits until the evening`() {
        val now = ZonedDateTime.of(2026, 11, 8, 18, 0, 0, 0, zone)

        assertEquals(
            Duration.ofHours(1).toMillis(),
            DigestScheduler.millisUntilNextDigest(now),
        )
    }

    @Test
    fun `sunday after seven waits a full week`() {
        val now = ZonedDateTime.of(2026, 11, 8, 20, 0, 0, 0, zone)

        assertEquals(
            Duration.ofDays(7).minusHours(1).toMillis(),
            DigestScheduler.millisUntilNextDigest(now),
        )
    }

    @Test
    fun `monday waits until the coming sunday`() {
        val now = ZonedDateTime.of(2026, 11, 9, 12, 0, 0, 0, zone)

        assertEquals(
            Duration.ofDays(6).plusHours(7).toMillis(),
            DigestScheduler.millisUntilNextDigest(now),
        )
    }
}
