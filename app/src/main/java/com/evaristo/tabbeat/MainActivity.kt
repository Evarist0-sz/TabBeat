package com.evaristo.tabbeat

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var favoritosManager: FavoritosManager
    private lateinit var adapter: CancionAdapter
    private lateinit var txtTituloPantalla: TextView
    private lateinit var txtSubtitulo: TextView

    // true = mostrando solo favoritos
    private var mostrandoFavoritos = false
    private var idsFavoritosActuales: Set<Int> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        favoritosManager = FavoritosManager(this)
        txtTituloPantalla = findViewById(R.id.txtTituloPantalla)
        txtSubtitulo = findViewById(R.id.txtSubtitulo)

        configurarLista()
        configurarBottomNav()
        observarFavoritos()
    }

    private fun configurarLista() {
        val rvCanciones = findViewById<RecyclerView>(R.id.rvCanciones)
        rvCanciones.layoutManager = LinearLayoutManager(this)

        adapter = CancionAdapter(
            canciones = CancionesRepositorio.canciones,
            idsFavoritos = emptySet(),
            alHacerClic = { cancion ->
                val intent = Intent(this, DetalleActivity::class.java)
                intent.putExtra(DetalleActivity.EXTRA_ID_CANCION, cancion.id)
                startActivity(intent)
            },
            alClicFavorito = { cancion ->
                lifecycleScope.launch {
                    favoritosManager.alternar(cancion.id)
                }
            }
        )

        rvCanciones.adapter = adapter
    }

    private fun configurarBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    mostrandoFavoritos = false
                    txtTituloPantalla.text = "TabBeat"
                    txtSubtitulo.text = "Encuentra tus canciones favoritas"
                    actualizarLista()
                    true
                }
                R.id.nav_favoritos -> {
                    mostrandoFavoritos = true
                    txtTituloPantalla.text = "Favoritos"
                    txtSubtitulo.text = "Tus canciones guardadas"
                    actualizarLista()
                    true
                }
                else -> false
            }
        }

        // Por defecto empieza en Inicio
        bottomNav.selectedItemId = R.id.nav_inicio
    }

    private fun observarFavoritos() {
        lifecycleScope.launch {
            favoritosManager.favoritos.collectLatest { ids ->
                idsFavoritosActuales = ids
                actualizarLista()
            }
        }
    }

    /** Decide qué canciones mostrar según la pestaña actual */
    private fun actualizarLista() {
        val lista = if (mostrandoFavoritos) {
            CancionesRepositorio.canciones.filter { it.id in idsFavoritosActuales }
        } else {
            CancionesRepositorio.canciones
        }
        adapter.actualizar(lista, idsFavoritosActuales)
    }
}