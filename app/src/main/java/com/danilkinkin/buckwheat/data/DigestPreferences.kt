package com.danilkinkin.buckwheat.data

import androidx.datastore.preferences.core.booleanPreferencesKey

/** Stored in the settings DataStore; opt-in, so a fresh install has no digest scheduled. */
val digestEnabledKey = booleanPreferencesKey("weekly-digest-enabled")
