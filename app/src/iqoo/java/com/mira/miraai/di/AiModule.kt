package com.mira.miraai.di

import android.util.Log
import com.google.mediapipe.tasks.core.Delegate
import com.mira.miraai.agent.freestyle.ActionSchemaClassifier
import com.mira.miraai.perception.MediaPipePoseEstimator
import com.mira.miraai.perception.PoseEstimator
import com.mira.miraai.voice.FallbackTTSProvider
import com.mira.miraai.voice.LLMProvider
import com.mira.miraai.voice.PiperTTSProvider
import com.mira.miraai.voice.STTProvider
import com.mira.miraai.voice.StubSTTProvider
import com.mira.miraai.voice.SystemTTSProvider
import com.mira.miraai.voice.TTSProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * iqoo flavor DI bindings — build-architecture.md Section 2 table.
 *
 * `Delegate.GPU` stands in for the true QNN/Hexagon-NPU delegate until Phase 12's QNN wiring;
 * MediaPipe's tasks-vision `Delegate` enum only exposes CPU/GPU today, so GPU is the closer
 * available proxy for "not CPU" on this flavor. STT still binds the same placeholder as
 * devPhone (real Whisper-Tiny wiring is unscheduled) — only the pose delegate differs for now.
 */
val aiModule = module {
    single(named("poseDelegate")) { Delegate.GPU }
    single<PoseEstimator> {
        MediaPipePoseEstimator(androidContext(), isFrontCamera = true, delegate = get(named("poseDelegate")))
    }
    // Piper primary (QNN-accelerated once wired), system TTS as the explicit fallback — see
    // devPhone's aiModule for the shared FallbackTTSProvider rationale (Section 2.1).
    single<TTSProvider> {
        FallbackTTSProvider(
            primary = PiperTTSProvider(androidContext()),
            fallback = SystemTTSProvider(androidContext()),
            onFallback = { error -> Log.w("Mira.TTS", "Piper unavailable, using system TTS fallback", error) },
        )
    }
    // Phase 8: keyword ActionSchema classifier — see devPhone's aiModule / the class doc for
    // why this satisfies Section 12.5's "lightweight separate classifier" fallback option.
    single<LLMProvider> { ActionSchemaClassifier() }
    single<STTProvider> { StubSTTProvider() }
}
