package com.mira.miraai.di

import android.util.Log
import com.google.mediapipe.tasks.core.Delegate
import com.mira.miraai.perception.MediaPipePoseEstimator
import com.mira.miraai.perception.PoseEstimator
import com.mira.miraai.voice.FallbackTTSProvider
import com.mira.miraai.voice.LLMProvider
import com.mira.miraai.voice.PiperTTSProvider
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
    // Front camera only, per CameraXController's current DEFAULT_FRONT_CAMERA binding.
    single<PoseEstimator> {
        MediaPipePoseEstimator(androidContext(), isFrontCamera = true, delegate = get(named("poseDelegate")))
    }
    // Piper primary, system TTS as the explicit (not silent) fallback — build-architecture.md
    // Section 2.1. PiperTTSProvider always reports unavailable until a real voice model/runtime
    // is bundled (see its class doc) — this composition is what makes that swap a one-line change.
    single<TTSProvider> {
        FallbackTTSProvider(
            primary = PiperTTSProvider(androidContext()),
            fallback = SystemTTSProvider(androidContext()),
            onFallback = { error -> Log.w("Mira.TTS", "Piper unavailable, using system TTS fallback", error) },
        )
    }
    single<LLMProvider> { StubLLMProvider() }
    single<STTProvider> { StubSTTProvider() }
}
