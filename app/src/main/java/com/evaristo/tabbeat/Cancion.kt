package com.evaristo.tabbeat

import androidx.annotation.RawRes

/**
 * Tipos de golpe que puede tener un patrón de rasgueo.
 * Cada uno se dibujará como una cajita distinta en la pantalla de detalle.
 */
enum class TipoGolpe {
    ABAJO,   // ↓  Flecha grande hacia abajo
    ARRIBA,  // ↑  Flecha grande hacia arriba
    BAJEO,   // •  Golpe al bajo (flecha pequeña / punto)
    PAUSA    // ·  Silencio, no se toca
}

/**
 * Molde de una canción.
 * "data class" significa que solo sirve para guardar datos.
 */
data class Cancion(
    val id: Int,                          // Identificador único (lo usaremos en el Intent)
    val titulo: String,
    val artista: String,
    val tono: String,                     // Ej: "G", "Em"
    val capo: Int = 0,                    // Traste del capo (0 = sin capo)
    val bpm: Int = 90,                    // ← NUEVO: tempo por defecto
    val acordesPrincipales: List<String>, // Ej: listOf("G", "B", "C", "Cm")
    val rasgueo: List<TipoGolpe>,         // Secuencia del patrón de rasgueo
    @RawRes val archivoContenido: Int     // Archivo .txt con letra, acordes y tablaturas
)