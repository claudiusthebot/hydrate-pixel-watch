package rocks.claudiusthebot.watertracker.phone.ui

import android.content.Context

/**
 * Tiny SharedPreferences wrapper for the last-visited peer tab.
 *
 * Goal: when the user re-launches Hydrate (regardless of whether the
 * Android process was kept warm or killed), they land on the same top-level
 * destination they were on last time — Today, History, or Settings — instead
 * of always being kicked back to Today.
 *
 * Sub-screens (e.g. Diagnostics) are intentionally NOT persisted: relaunching
 * always lands on a peer tab, never on a deep child.  We use SharedPreferences
 * (not DataStore) so the read can be synchronous on initial composition,
 * which keeps `NavHost`'s `startDestination` straightforward.
 */
internal object LastTabPrefs {
    private const val FILE = "last_tab_prefs"
    private const val KEY = "route"

    /** Synchronous read used to seed `NavHost(startDestination = …)`. */
    fun read(context: Context, default: String): String =
        context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, default) ?: default

    /** Fire-and-forget write — `apply()` schedules a background flush. */
    fun write(context: Context, route: String) {
        context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, route)
            .apply()
    }
}
