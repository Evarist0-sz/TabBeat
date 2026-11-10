package com.evaristo.tabbeat

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorRes
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetalleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID_CANCION = "extra_id_cancion"
        const val MOSTRAR_TABS_AL_INICIO = true
    }

    // Variables de instancia (aquí sí van)
    private lateinit var favoritosManager: FavoritosManager
    private var cancionActual: Cancion? = null
    private var metronomo: Metronomo? = null
    private val cajasRasgueo = mutableListOf<View>()
    private var totalGolpesRasgueo = 0
    private var bpmActual = 90

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val idCancion = intent.getIntExtra(EXTRA_ID_CANCION, -1)
        val cancion = CancionesRepositorio.buscarPorId(idCancion)
        if (cancion == null) {
            Toast.makeText(this, "Canción no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.btnVolver).setOnClickListener { finish() }

        favoritosManager = FavoritosManager(this)
        cancionActual = cancion

        // Corazón de favorito
        val btnFavorito = findViewById<ImageView>(R.id.btnFavorito)
        btnFavorito.setOnClickListener {
            lifecycleScope.launch {
                favoritosManager.alternar(cancion.id)
            }
        }
        lifecycleScope.launch {
            favoritosManager.favoritos.collectLatest { ids ->
                val esFavorito = ids.contains(cancion.id)
                btnFavorito.setImageResource(
                    if (esFavorito) R.drawable.ic_heart_filled
                    else R.drawable.ic_heart_outline
                )
            }
        }

        // Metrónomo
        configurarMetronomo(cancion)

        mostrarEncabezado(cancion)
        mostrarRasgueo(cancion.rasgueo)
        mostrarContenido(cancion)
    }

    // ==================== ENCABEZADO ====================

    private fun mostrarEncabezado(cancion: Cancion) {
        findViewById<TextView>(R.id.txtTitulo).text = cancion.titulo
        findViewById<TextView>(R.id.txtArtista).text = cancion.artista
        findViewById<TextView>(R.id.txtTono).text = "Tono: ${cancion.tono}"
        findViewById<TextView>(R.id.txtAcordes).text =
            cancion.acordesPrincipales.joinToString("  ·  ")

        val txtCapo = findViewById<TextView>(R.id.txtCapo)
        txtCapo.isVisible = cancion.capo > 0
        txtCapo.text = "Capo: ${cancion.capo}"
    }

    // ==================== RASGUEO ====================

    private fun mostrarRasgueo(rasgueo: List<TipoGolpe>) {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorRasgueo)
        contenedor.removeAllViews()
        cajasRasgueo.clear()
        totalGolpesRasgueo = rasgueo.size

        for ((indice, golpe) in rasgueo.withIndex()) {
            val caja = layoutInflater.inflate(R.layout.item_golpe, contenedor, false)
            val txtFlecha = caja.findViewById<TextView>(R.id.txtFlecha)
            val txtNumero = caja.findViewById<TextView>(R.id.txtNumero)

            when (golpe) {
                TipoGolpe.ABAJO -> estilizarGolpe(txtFlecha, "↓", 30f, R.color.verde_celeste, R.drawable.bg_tono)
                TipoGolpe.ARRIBA -> estilizarGolpe(txtFlecha, "↑", 30f, R.color.verde_celeste, R.drawable.bg_tono)
                TipoGolpe.BAJEO -> estilizarGolpe(txtFlecha, "•", 26f, R.color.gota_lluvia, R.drawable.bg_golpe_bajeo)
                TipoGolpe.PAUSA -> estilizarGolpe(txtFlecha, "–", 20f, R.color.texto_niebla, R.drawable.bg_golpe_pausa)
            }

            txtNumero.text = (indice + 1).toString()
            contenedor.addView(caja)
            cajasRasgueo.add(caja)
        }
    }

    private fun resaltarGolpe(indice: Int) {
        // Quitar resaltado de todas
        for (caja in cajasRasgueo) {
            caja.alpha = 0.45f
            caja.scaleX = 1f
            caja.scaleY = 1f
        }

        // indice -1 = detener → dejar todas normales
        if (indice < 0 || cajasRasgueo.isEmpty()) {
            for (caja in cajasRasgueo) {
                caja.alpha = 1f
            }
            return
        }

        // Resaltar el golpe actual (cíclico)
        val pos = indice % cajasRasgueo.size
        val actual = cajasRasgueo[pos]
        actual.alpha = 1f
        actual.scaleX = 1.15f
        actual.scaleY = 1.15f
    }

    private fun estilizarGolpe(
        vista: TextView,
        simbolo: String,
        tamano: Float,
        @ColorRes colorId: Int,
        fondo: Int
    ) {
        vista.text = simbolo
        vista.textSize = tamano
        vista.setTextColor(color(colorId))
        vista.setBackgroundResource(fondo)
    }

    // ==================== CONTENIDO ====================

    private fun mostrarContenido(cancion: Cancion) {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorContenido)
        val bloques = ParserCancion.analizar(leerArchivo(cancion.archivoContenido))

        for (bloque in bloques) {
            val vista = when (bloque) {
                is Bloque.Seccion -> crearChipSeccion(bloque.nombre)
                is Bloque.Acordes -> crearLinea(bloque.texto, R.color.verde_celeste, negrita = true)
                is Bloque.Letra -> crearLinea(bloque.texto, R.color.texto_principal, negrita = false)
                is Bloque.Tablatura -> crearTarjetaTablatura(bloque, contenedor)
                is Bloque.Espacio -> crearEspacio()
            }
            contenedor.addView(vista)
        }
    }

    private fun crearChipSeccion(nombre: String): TextView {
        val chip = TextView(this)
        chip.text = nombre
        chip.textSize = 13f
        chip.setTypeface(chip.typeface, Typeface.BOLD)
        chip.setTextColor(color(R.color.texto_principal))
        chip.setBackgroundResource(R.drawable.bg_seccion)
        chip.setPadding(dp(14), dp(6), dp(14), dp(6))

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = dp(20)
        params.bottomMargin = dp(8)
        chip.layoutParams = params
        return chip
    }

    private fun crearLinea(texto: String, @ColorRes colorId: Int, negrita: Boolean): TextView {
        val linea = TextView(this)
        linea.text = texto
        linea.textSize = 14f
        linea.typeface = Typeface.create(
            Typeface.MONOSPACE,
            if (negrita) Typeface.BOLD else Typeface.NORMAL
        )
        linea.setTextColor(color(colorId))
        return linea
    }

    private fun crearEspacio(): View {
        val espacio = View(this)
        espacio.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(8)
        )
        return espacio
    }

    private fun crearTarjetaTablatura(tab: Bloque.Tablatura, padre: ViewGroup): View {
        val tarjeta = layoutInflater.inflate(R.layout.item_tablatura, padre, false)
        val encabezado = tarjeta.findViewById<View>(R.id.encabezadoTab)
        val txtTitulo = tarjeta.findViewById<TextView>(R.id.txtTituloTab)
        val txtAlternar = tarjeta.findViewById<TextView>(R.id.txtAlternarTab)
        val scroll = tarjeta.findViewById<View>(R.id.scrollTab)
        val txtLineas = tarjeta.findViewById<TextView>(R.id.txtLineasTab)

        txtTitulo.text = tab.titulo ?: "Tablatura"
        txtLineas.text = colorearTablatura(tab.lineas)

        fun cambiarVisibilidad(visible: Boolean) {
            scroll.isVisible = visible
            txtAlternar.text = if (visible) "Ocultar ▲" else "Mostrar ▼"
        }

        cambiarVisibilidad(MOSTRAR_TABS_AL_INICIO)
        encabezado.setOnClickListener { cambiarVisibilidad(!scroll.isVisible) }
        return tarjeta
    }

    private fun colorearTablatura(lineas: List<String>): CharSequence {
        val resultado = SpannableStringBuilder()

        for ((i, linea) in lineas.withIndex()) {
            if (i > 0) resultado.append(if (i % 6 == 0) "\n\n" else "\n")

            val posicionBarra = linea.indexOf('|')

            for ((j, caracter) in linea.withIndex()) {
                val inicio = resultado.length
                resultado.append(caracter)

                val esCuerda = j < posicionBarra
                val colorId = when {
                    esCuerda -> R.color.esmeralda_claro
                    caracter.isDigit() -> R.color.verde_celeste
                    caracter == '-' || caracter == '|' -> R.color.linea_cuerda
                    else -> R.color.texto_niebla
                }
                resultado.setSpan(
                    ForegroundColorSpan(color(colorId)),
                    inicio,
                    inicio + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                if (esCuerda || caracter.isDigit()) {
                    resultado.setSpan(
                        StyleSpan(Typeface.BOLD),
                        inicio,
                        inicio + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        return resultado
    }

    // ==================== METRÓNOMO ====================

    private fun configurarMetronomo(cancion: Cancion) {
        bpmActual = cancion.bpm

        val txtBpm = findViewById<TextView>(R.id.txtBpm)
        val btnMenos = findViewById<TextView>(R.id.btnBpmMenos)
        val btnMas = findViewById<TextView>(R.id.btnBpmMas)
        val btnPlay = findViewById<TextView>(R.id.btnMetronomo)

        val txtVolumen = findViewById<TextView>(R.id.txtVolumen)
        val btnVolMenos = findViewById<TextView>(R.id.btnVolMenos)
        val btnVolMas = findViewById<TextView>(R.id.btnVolMas)

        txtBpm.text = bpmActual.toString()

        // Volumen inicial 90%
        var volActual = 0.9f
        txtVolumen.text = "90%"

        metronomo = Metronomo { golpeIndex ->
            // Se ejecuta en cada tick → resaltar golpe
            runOnUiThread {
                resaltarGolpe(golpeIndex)
            }
        }
        metronomo?.cambiarVolumen(volActual)

        // BPM −
        btnMenos.setOnClickListener {
            if (bpmActual > 40) {
                bpmActual -= 5
                txtBpm.text = bpmActual.toString()
                metronomo?.cambiarBpm(bpmActual)
            }
        }

        // BPM +
        btnMas.setOnClickListener {
            if (bpmActual < 240) {
                bpmActual += 5
                txtBpm.text = bpmActual.toString()
                metronomo?.cambiarBpm(bpmActual)
            }
        }

        // Volumen −
        btnVolMenos.setOnClickListener {
            if (volActual > 0.1f) {
                volActual = (volActual - 0.1f).coerceAtLeast(0.1f)
                val porcentaje = (volActual * 100).toInt()
                txtVolumen.text = "$porcentaje%"
                metronomo?.cambiarVolumen(volActual)
            }
        }

        // Volumen +
        btnVolMas.setOnClickListener {
            if (volActual < 1f) {
                volActual = (volActual + 0.1f).coerceAtMost(1f)
                val porcentaje = (volActual * 100).toInt()
                txtVolumen.text = "$porcentaje%"
                metronomo?.cambiarVolumen(volActual)
            }
        }

        // Play / Stop
        btnPlay.setOnClickListener {
            val m = metronomo ?: return@setOnClickListener
            if (m.estaCorriendo()) {
                m.detener()
                btnPlay.text = "▶"
                resaltarGolpe(-1) // quitar resaltado
            } else {
                m.iniciar(bpmActual)
                btnPlay.text = "■"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        metronomo?.detener()
    }

    // ==================== UTILIDADES ====================

    private fun leerArchivo(@RawRes idArchivo: Int): String =
        resources.openRawResource(idArchivo)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()

    private fun color(@ColorRes id: Int): Int =
        ContextCompat.getColor(this, id)
}