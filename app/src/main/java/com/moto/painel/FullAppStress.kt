package com.moto.painel

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import android.widget.Toast

/**
 * Measures full MainActivity frame callback cadence while the user exercises
 * the actual dashboard, map, routing and menus. No synthetic GPS or navigation.
 * Choreographer callback gaps are a proxy, NOT actual GPU-presented frame times.
 */
class FullAppStress(private val activity: Activity) {
    private val handler = Handler(Looper.getMainLooper())
    private val choreographer by lazy { Choreographer.getInstance() }
    private var running = false
    private var startedAt = 0L
    private var lastFrameNs = 0L
    private var callbacks = 0
    private var gaps34 = 0
    private var gaps100 = 0
    private var maxGapMs = 0.0
    private val samples = ArrayList<Double>()
    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            if (lastFrameNs != 0L) {
                val gap = (frameTimeNanos - lastFrameNs) / 1_000_000.0
                if (gap > 0) {
                    callbacks++
                    if (gap > 34) gaps34++
                    if (gap > 100) gaps100++
                    maxGapMs = maxOf(maxGapMs, gap)
                    if (samples.size < 20000) samples.add(gap)
                }
            }
            lastFrameNs = frameTimeNanos
            choreographer.postFrameCallback(this)
        }
    }
    private val finish = Runnable { stop("completed") }

    fun start(seconds: Int = 120) {
        if (running) return
        if (!Diagnostics.enabled(activity)) Diagnostics.setEnabled(activity, true)
        running = true
        startedAt = SystemClock.elapsedRealtime()
        lastFrameNs = 0L
        callbacks = 0
        gaps34 = 0
        gaps100 = 0
        maxGapMs = 0.0
        samples.clear()
        Diagnostics.event(activity, "full_app_stress_start", mapOf("seconds" to seconds))
        choreographer.postFrameCallback(frame)
        handler.postDelayed(finish, seconds * 1000L)
        Toast.makeText(activity, "Teste RAVO iniciado: use mapa, painel e menus por ${seconds}s", Toast.LENGTH_LONG).show()
    }

    fun stop(reason: String = "interrupted") {
        if (!running) return
        running = false
        handler.removeCallbacks(finish)
        choreographer.removeFrameCallback(frame)
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        val sorted = samples.sorted()
        val p95 = if (sorted.isEmpty()) 0.0 else sorted[((sorted.size - 1) * 0.95).toInt()]
        Diagnostics.event(activity, "full_app_stress_result", mapOf(
            "reason" to reason,
            "elapsed_ms" to elapsed,
            "frame_callback_count" to callbacks,
            "callback_gap_p95_ms" to (p95 * 10).toInt() / 10.0,
            "callback_gap_max_ms" to (maxGapMs * 10).toInt() / 10.0,
            "callback_gaps_over_34ms" to gaps34,
            "callback_gaps_over_100ms" to gaps100,
            "metric_note" to "Choreographer cadence, not GPU presented FPS"
        ))
        Diagnostics.memory(activity, "full_app", 0L)
        if (reason == "completed") Toast.makeText(activity, "Teste concluído! Compartilhe o diagnóstico nas configurações.", Toast.LENGTH_LONG).show()
    }
}
