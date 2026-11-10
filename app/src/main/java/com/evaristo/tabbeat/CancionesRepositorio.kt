package com.evaristo.tabbeat

import com.evaristo.tabbeat.TipoGolpe.*

/**
 * "object" = existe una sola copia en toda la app.
 * Aquí vive nuestra lista estática de canciones (sin base de datos).
 */
object CancionesRepositorio {

    val canciones: List<Cancion> = listOf(
        Cancion(
            id = 1,
            titulo = "Creep",
            artista = "Radiohead",
            tono = "G",
            bpm = 92,
            acordesPrincipales = listOf("G", "B", "C", "Cm"),
            rasgueo = listOf(ABAJO, PAUSA, ABAJO, ARRIBA, PAUSA, ARRIBA, ABAJO, ARRIBA),
            archivoContenido = R.raw.creep
        ),
        Cancion(
            id = 2,
            titulo = "Lamento Boliviano",
            artista = "Los Enanitos Verdes",
            tono = "Em",
            bpm = 123,
            acordesPrincipales = listOf("Em", "Bm", "Am", "B7"),
            rasgueo = listOf(BAJEO, ABAJO, ARRIBA, ABAJO, ARRIBA, ABAJO, ARRIBA),
            archivoContenido = R.raw.lamento_boliviano
        ),
        Cancion(
            id = 3,
            titulo = "Chachachá",
            artista = "Jósean Log",
            tono = "F (forma de C)",
            capo = 5,
            bpm = 110,
            acordesPrincipales = listOf("C", "Am", "Em", "E", "F", "G"),
            rasgueo = listOf(BAJEO, ABAJO, BAJEO, ABAJO, ARRIBA),
            archivoContenido = R.raw.chachacha
        ),
        Cancion(
            id = 4,
            titulo = "La Flaca",
            artista = "Jarabe de Palo",
            tono = "Am",
            bpm = 103,
            acordesPrincipales = listOf("Am", "E7", "D7", "C", "F", "G"),
            rasgueo = listOf(ABAJO, ABAJO, ARRIBA, ARRIBA, ABAJO, ARRIBA),
            archivoContenido = R.raw.la_flaca
        )
    )

    /** Busca una canción por su id. Devuelve null si no existe. */
    fun buscarPorId(id: Int): Cancion? = canciones.find { it.id == id }
}