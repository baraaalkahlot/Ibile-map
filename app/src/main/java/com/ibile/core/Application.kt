package com.ibile.core

import android.app.Application
import com.airbnb.mvrx.mock.MvRxMocks
import com.google.android.libraries.places.api.Places
import com.ibile.R
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        MvRxMocks.install(applicationContext)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_api_key))
        }
        startKoin {
            androidLogger()
            androidContext(this@Application)
            modules(appModule)
        }
    }

}
