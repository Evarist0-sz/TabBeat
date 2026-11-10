package com.evaristo.tabbeat

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.sin

class Metronomo(
    private val onTick: ((golpeIndex: Int) -> Unit)? = null
) {
    private val handler = Handler(Looper.getMainLooper())
    private var corriendo = false
    private var bpmActual = 90
    private var contadorGolpe = 0

    // Volumen interno (0.0 = silencio, 1.0 = máximo)
    private var volumen = 0.9

    private val runnable = object : Runnable {
        override fun run() {
            if (!corriendo) return

            val esTiempoFuerte = (contadorGolpe % 4 == 0)
            val frec = if (esTiempoFuerte) 1000.0 else 800.0
            val dur = if (esTiempoFuerte) 40 else 30
            val vol = if (esTiempoFuerte) volumen else volumen * 0.7

            reproducir(generarClick(frec, dur, vol))

            onTick?.invoke(contadorGolpe)
            contadorGolpe++

            val intervalo = (60_000L / bpmActual).coerceAtLeast(200L)
            handler.postDelayed(this, intervalo)
        }
    }

    fun iniciar(bpm: Int) {
        if (corriendo) return
        bpmActual = bpm.coerceIn(40, 240)
        contadorGolpe = 0
        corriendo = true
        handler.post(runnable)
    }

    fun detener() {
        corriendo = false
        handler.removeCallbacks(runnable)
        contadorGolpe = 0
        onTick?.invoke(-1) // -1 = quitar resaltado
    }

    fun cambiarBpm(nuevoBpm: Int) {
        bpmActual = nuevoBpm.coerceIn(40, 240)
    }

    /** volumen entre 0.0 y 1.0 */
    fun cambiarVolumen(nuevo: Float) {
        volumen = nuevo.coerceIn(0f, 1f).toDouble()
    }

    fun obtenerVolumen(): Float = volumen.toFloat()

    fun estaCorriendo(): Boolean = corriendo

    fun obtenerBpm(): Int = bpmActual

    private fun generarClick(frecuencia: Double, duracionMs: Int, vol: Double): ShortArray {
        val sampleRate = 44100
        val numSamples = (sampleRate * duracionMs) / 1000
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envolvente = 1.0 - (i.toDouble() / numSamples)
            val muestra = sin(2.0 * Math.PI * frecuencia * t) * envolvente * vol
            buffer[i] = (muestra * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun reproducir(samples: ShortArray) {
        val sampleRate = 44100
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()

            handler.postDelayed({
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) { }
            }, 120)
        } catch (_: Exception) { }
    }
}