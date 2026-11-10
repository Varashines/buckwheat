package com.danilkinkin.buckwheat.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.danilkinkin.buckwheat.MainActivity
import com.danilkinkin.buckwheat.ShortcutRouter
import com.danilkinkin.buckwheat.R

/**
 * The one number worth seeing without opening the app: today's spend.
 *
 * A dynamic launcher shortcut, because it needs no widget host, no extra process and no refresh
 * schedule. The static `today` shortcut in shortcuts.xml is the fallback before the first run;
 * this one overrides it with the current total.
 */
object TodayShortcut {
    private const val ID = "today"

    fun update(context: Context, total: String?) {
        val app = context.applicationContext
        val intent = Intent(app, ShortcutRouter::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.EXTRA_SHORTCUT, MainActivity.ROUTE_TODAY)
        }
        val short = app.getString(R.string.shortcut_today)
        val long = if (total != null) "$short · $total" else short

        val shortcut = ShortcutInfoCompat.Builder(app, ID)
            .setShortLabel(short)
            .setLongLabel(long.take(60))
            .setIcon(IconCompat.createWithResource(app, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()

        runCatching { ShortcutManagerCompat.setDynamicShortcuts(app, listOf(shortcut)) }
    }
}
