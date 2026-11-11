package com.danilkinkin.buckwheat

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Launcher shortcuts must target an exported component, but MainActivity deliberately is not
 * exported (the launcher aliases are). This activity is the smallest possible bridge: it forwards
 * the whitelisted route to MainActivity and finishes without drawing anything.
 */
class ShortcutRouter : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val route = intent?.getStringExtra(MainActivity.EXTRA_SHORTCUT)
        if (route == MainActivity.ROUTE_ADD ||
            route == MainActivity.ROUTE_HISTORY ||
            route == MainActivity.ROUTE_TODAY ||
            route == MainActivity.ROUTE_STATS
        ) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_SHORTCUT, route)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }

        finish()
    }
}
