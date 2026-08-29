package com.mira.miraai.di

import com.mira.miraai.data.MiraDatabase
import com.mira.miraai.data.SessionRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Room/local-storage bindings — identical on both flavors (no delegate/NPU concerns here,
 * unlike `di/AiModule.kt`), so this lives in `main/` rather than being duplicated per flavor.
 */
val dataModule = module {
    single { MiraDatabase.build(androidContext()) }
    single { SessionRepository(get()) }
}
