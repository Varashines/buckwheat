package com.danilkinkin.buckwheat.digest

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.danilkinkin.buckwheat.MainActivity
import com.danilkinkin.buckwheat.R
import com.danilkinkin.buckwheat.budgetDataStore
import com.danilkinkin.buckwheat.data.DigestSummary
import com.danilkinkin.buckwheat.data.ExtendCurrency
import com.danilkinkin.buckwheat.data.composeDigestSummary
import com.danilkinkin.buckwheat.di.currencyStoreKey
import com.danilkinkin.buckwheat.di.DatabaseModule
import com.danilkinkin.buckwheat.di.SeedDefaultCategoriesCallback
import com.danilkinkin.buckwheat.util.numberFormat
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "weekly-digest"

/**
 * A Sunday-evening summary of the last seven days.
 *
 * Read-only, like the subscription check: it opens the database directly rather than pulling
 * Hilt into WorkManager, and posts nothing when there is no spending or no notification
 * permission.
 */
class DigestWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val database = Room.databaseBuilder(
            applicationContext,
            DatabaseModule::class.java,
            "buckwheat-db",
        )
            .addMigrations(*DatabaseModule.MANUAL_MIGRATIONS)
            .addCallback(SeedDefaultCategoriesCallback)
            .build()

        return try {
            val transactions = database.transactionDao().getAllOnce()
            val categories = database.categoryDao().getAllOnce()
            val summary = composeDigestSummary(transactions, categories, LocalDate.now())

            if (summary.count == 0) {
                Result.success()
            } else {
                val currencyCode = applicationContext.budgetDataStore.data
                    .first()[currencyStoreKey]
                val currency = ExtendCurrency.getInstance(currencyCode)

                ensureChannel()
                notify(summary, currency)
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        } finally {
            database.close()
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.weekly_digest_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
    }

    private fun notify(
        summary: DigestSummary,
        currency: ExtendCurrency,
    ) {
        val amount = numberFormat(
            applicationContext,
            summary.total,
            currency,
            trimDecimalPlaces = true,
        )
        val topName = summary.topCategory
            ?: applicationContext.getString(R.string.no_category)
        val topAmount = numberFormat(
            applicationContext,
            summary.topAmount,
            currency,
            trimDecimalPlaces = true,
        )

        val text = buildString {
            append(
                applicationContext.getString(
                    R.string.weekly_digest_text,
                    amount,
                    summary.count,
                )
            )
            append(" · ")
            append(
                applicationContext.getString(
                    R.string.weekly_digest_top,
                    topName,
                    topAmount,
                )
            )
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_SHORTCUT, MainActivity.ROUTE_STATS)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_analytics)
            .setContentTitle(applicationContext.getString(R.string.weekly_digest_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(WEEKLY_NOTIFICATION_ID, notification)
    }

    private companion object {
        const val WEEKLY_NOTIFICATION_ID = 4101
    }
}

/** Keeps the weekly work anchored to Sunday 19:00 local, not to install time. */
object DigestScheduler {
    private const val WORK_NAME = "weekly-digest"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DigestWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextDigest(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    internal fun millisUntilNextDigest(now: ZonedDateTime = ZonedDateTime.now()): Long {
        var next = now
            .with(DayOfWeek.SUNDAY)
            .withHour(19)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        if (!next.isAfter(now)) next = next.plusWeeks(1)

        return Duration.between(now, next).toMillis()
    }
}
