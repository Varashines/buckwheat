package com.danilkinkin.buckwheat

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.glance.appwidget.updateAll
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.danilkinkin.buckwheat.subscriptions.SubscriptionCheckWorker
import com.danilkinkin.buckwheat.di.SpendsRepository
import com.danilkinkin.buckwheat.ui.TodayShortcut
import com.danilkinkin.buckwheat.ui.applyLauncherIcon
import com.danilkinkin.buckwheat.widget.extend.ExtendWidget
import com.danilkinkin.buckwheat.widget.minimal.MinimalWidget
import com.danilkinkin.buckwheat.data.ExtendCurrency
import com.danilkinkin.buckwheat.util.isSameDay
import com.danilkinkin.buckwheat.util.numberFormat
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class Application : Application() {
    @Inject
    lateinit var spendsRepository: SpendsRepository

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        scheduleSubscriptionChecks()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

            }

            override fun onActivityStarted(activity: Activity) {

            }

            override fun onActivityResumed(activity: Activity) {

            }

            override fun onActivityPaused(activity: Activity) {
                val context = activity.applicationContext
                widgetScope.launch {
                    ExtendWidget().updateAll(context)
                    MinimalWidget().updateAll(context)

                    val spends = runCatching {
                        spendsRepository.getAllSpends().first()
                    }.getOrDefault(emptyList())
                    val currency = runCatching {
                        spendsRepository.getCurrency().first()
                    }.getOrDefault(ExtendCurrency.none())
                    val today = Date()
                    val total = spends
                        .filter { isSameDay(it.date, today) }
                        .fold(BigDecimal.ZERO) { sum, spend -> sum + spend.value }

                    TodayShortcut.update(
                        context = context,
                        total = numberFormat(context, total, currency),
                    )
                }

                // The launcher is not visible while the app is in front, so the alias switch
                // happens here instead of mid-theme-change. Enabling the new alias before
                // disabling the old one keeps the app launchable throughout.
                applyLauncherIcon(
                    activity.applicationContext,
                    activity.applicationContext.appThemePreset,
                )
            }

            override fun onActivityStopped(activity: Activity) {

            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

            }

            override fun onActivityDestroyed(activity: Activity) {

            }
        })
    }

    /**
     * One daily check; WorkManager persists it across reboots. The first run is shifted to the
     * next 10:00 local so a subscription due today does not fire in the middle of the night.
     */
    private fun scheduleSubscriptionChecks() {
        val request = PeriodicWorkRequestBuilder<SubscriptionCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntil(10), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "subscription-daily-check",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun millisUntil(hourOfDay: Int): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }

        return next.timeInMillis - now.timeInMillis
    }
}
