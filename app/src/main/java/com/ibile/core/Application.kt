package com.ibile.core

import android.app.Application
import com.airbnb.mvrx.mock.MvRxMocks
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        MvRxMocks.install(applicationContext)
        startKoin {
            androidLogger()
            androidContext(this@Application)
            modules(appModule)
        }
    }

}
