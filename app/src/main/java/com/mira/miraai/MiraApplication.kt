package com.mira.miraai

import android.app.Application
import com.mira.miraai.di.aiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** Starts Koin with the active flavor's [aiModule] (`devPhone` or `iqoo` — see `di/AiModule.kt`). */
class MiraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MiraApplication)
            modules(aiModule)
        }
    }
}
