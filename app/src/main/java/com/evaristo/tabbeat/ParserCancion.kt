package com.evaristo.tabbeat

/**
 * Cada "pieza" en la que dividimos una canción.
 * "sealed class" = una lista CERRADA de tipos posibles. Kotlin nos obliga
 * a manejar todos los casos en un "when", así no se nos olvida ninguno.
 */
sealed class Bloque {
    data class Seccion(val nombre: String) : Bloque()        // [Intro], [Estribillo]...
    data class Acordes(val texto: String) : Bloque()         // "  G      B"
    data class Letra(val texto: String) : Bloque()           // "When you were here..."
    data class Tablatura(val titulo: String?, val lineas: List<String>) : Bloque()
    object Espacio : Bloque()                                // Línea en blanco
}

object ParserCancion {

    // [Nombre]  y opcionalmente algo después, ej: "[Intro] G B C Cm"
    private val regexSeccion = Regex("""^\s*\[(.+?)\]\s*(.*)$""")

    // Línea de tablatura: empieza con una cuerda y "|", ej: "E|---3---"
    private val regexLineaTab = Regex("""^\s*[A-Ga-g][#b]?\|""")

    // Un acorde: G, Cm, B7, F#m, Cmaj7, Dsus4, E7(9-), F/A, E/G#...
    private val regexAcorde =
        Regex("""^[A-G][#b]?(m|maj|min|dim|aug|sus|add)?\d*(\([^)]*\))?(/[A-G][#b]?)?$""")

    /** Recibe el texto completo del .txt y devuelve la lista de bloques */
    fun analizar(texto: String): List<Bloque> {
        val bloques = mutableListOf<Bloque>()
        var lineasTab = mutableListOf<String>()
        var tituloTab: String? = null

        // Guarda la tablatura que se venía juntando (si hay)
        fun cerrarTablatura() {
            if (lineasTab.isNotEmpty()) {
                bloques.add(Bloque.Tablatura(tituloTab, lineasTab))
                lineasTab = mutableListOf()
                tituloTab = null
            }
        }

        for (lineaOriginal in texto.lines()) {
            val linea = lineaOriginal.trimEnd()

            // 1) ¿Es línea de tablatura? La juntamos con las demás
            if (regexLineaTab.containsMatchIn(linea)) {
                // Si justo antes había un acorde suelto (ej: " G"), lo usamos de título
                if (lineasTab.isEmpty()) {
                    val anterior = bloques.lastOrNull()
                    if (anterior is Bloque.Acordes) {
                        tituloTab = anterior.texto.trim()
                        bloques.removeAt(bloques.lastIndex)
                    }
                }
                lineasTab.add(linea.trim())
                continue
            }

            // Si llegamos aquí, la tablatura (si había) ya terminó
            cerrarTablatura()

            val seccion = regexSeccion.find(linea)
            when {
                // 2) Línea vacía (evitamos dos espacios seguidos)
                linea.isBlank() -> {
                    if (bloques.lastOrNull() !is Bloque.Espacio) bloques.add(Bloque.Espacio)
                }
                // 3) Sección, y si trae acordes al lado, también los agregamos
                seccion != null -> {
                    bloques.add(Bloque.Seccion(seccion.groupValues[1].trim()))
                    val resto = seccion.groupValues[2]
                    if (resto.isNotBlank()) bloques.add(Bloque.Acordes(resto.trim()))
                }
                // 4) Línea de puros acordes (conservamos los espacios para alinear)
                esLineaDeAcordes(linea) -> bloques.add(Bloque.Acordes(linea))
                // 5) Todo lo demás es letra
                else -> bloques.add(Bloque.Letra(linea))
            }
        }
        cerrarTablatura()
        return bloques
    }

    /** true si TODAS las palabras de la línea son acordes */
    private fun esLineaDeAcordes(linea: String): Boolean {
        val palabras = linea.trim()
            .split(Regex("""\s+"""))
            .filter { it != "(" && it != ")" }   // ignora "( C Am Em E )"
        return palabras.isNotEmpty() && palabras.all { regexAcorde.matches(it) }
    }
}