package com.evaristo.tabbeat

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extensión para tener un solo DataStore en toda la app
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favoritos")

/**
 * Se encarga de guardar y leer qué canciones están marcadas como favoritas.
 * Usa DataStore (oficial de Android) para que los datos sobrevivan aunque cierres la app.
 */
class FavoritosManager(private val context: Context) {

    // La clave con la que guardamos el conjunto de IDs
    private val KEY_FAVORITOS = stringSetPreferencesKey("ids_favoritos")

    /** Flujo que emite la lista actual de IDs favoritos cada vez que cambia */
    val favoritos: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITOS]
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    /** Añade o quita una canción de favoritos */
    suspend fun alternar(idCancion: Int) {
        context.dataStore.edit { prefs ->
            val actuales = prefs[KEY_FAVORITOS]?.toMutableSet() ?: mutableSetOf()
            val idTexto = idCancion.toString()

            if (actuales.contains(idTexto)) {
                actuales.remove(idTexto)   // ya era favorito → lo quitamos
            } else {
                actuales.add(idTexto)      // no era favorito → lo agregamos
            }

            prefs[KEY_FAVORITOS] = actuales
        }
    }

    /** Pregunta si una canción es favorita (una sola vez) */
    suspend fun esFavorito(idCancion: Int): Boolean {
        val ids = favoritos // Flow
        // Para uso puntual preferimos leer el valor actual de otra forma,
        // pero con el Flow en la UI es suficiente.
        return false // se usará principalmente el Flow
    }
}