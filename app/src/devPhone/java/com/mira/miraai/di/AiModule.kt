package com.mira.miraai.di

import com.google.mediapipe.tasks.core.Delegate
import com.mira.miraai.voice.LLMProvider
import com.mira.miraai.voice.STTProvider
import com.mira.miraai.voice.StubLLMProvider
import com.mira.miraai.voice.StubSTTProvider
import com.mira.miraai.voice.SystemTTSProvider
import com.mira.miraai.voice.TTSProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * devPhone flavor DI bindings — build-architecture.md Section 2 table. Business logic never
 * sees this module directly, only the interfaces it binds.
 */
val aiModule = module {
    // LiteRT GPU/CPU delegate on devPhone; CPU is the safe default until GPU is profiled.
    single(named("poseDelegate")) { Delegate.CPU }
    single<TTSProvider> { SystemTTSProvider(androidContext()) }
    single<LLMProvider> { StubLLMProvider() }
    single<STTProvider> { StubSTTProvider() }
}
