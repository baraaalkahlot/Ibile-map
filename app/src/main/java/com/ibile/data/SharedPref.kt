package com.ibile.data

import android.content.Context
import android.content.SharedPreferences

class SharedPref(val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE)

    var subscriptionToken
        get() = sharedPreferences.getString(PREF_SUBSCRIPTION_TOKEN, null)
        set(value) {
            sharedPreferences.edit().putString(PREF_SUBSCRIPTION_TOKEN, value).apply()
        }

    companion object {
        const val SHARED_PREF_FILE_NAME = "com.ibile.PREF_FILE"

        const val PREF_SUBSCRIPTION_TOKEN = "PREF_SUBSCRIPTION_TOKEN"
    }
}
