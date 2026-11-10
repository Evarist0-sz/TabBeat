package com.evaristo.tabbeat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * El adaptador es el "puente" entre la lista de canciones y el RecyclerView.
 * Ahora también maneja el estado de favoritos.
 */
class CancionAdapter(
    private var canciones: List<Cancion>,
    private var idsFavoritos: Set<Int> = emptySet(),
    private val alHacerClic: (Cancion) -> Unit,
    private val alClicFavorito: (Cancion) -> Unit
) : RecyclerView.Adapter<CancionAdapter.CancionViewHolder>() {

    class CancionViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val txtTono: TextView = vista.findViewById(R.id.txtTono)
        val txtTitulo: TextView = vista.findViewById(R.id.txtTitulo)
        val txtArtista: TextView = vista.findViewById(R.id.txtArtista)
        val txtAcordes: TextView = vista.findViewById(R.id.txtAcordes)
        val btnFavorito: ImageView = vista.findViewById(R.id.btnFavorito)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cancion, parent, false)
        return CancionViewHolder(vista)
    }

    override fun onBindViewHolder(holder: CancionViewHolder, position: Int) {
        val cancion = canciones[position]
        val esFavorito = idsFavoritos.contains(cancion.id)

        holder.txtTono.text = cancion.tono.substringBefore(" ")
        holder.txtTitulo.text = cancion.titulo

        holder.txtArtista.text =
            if (cancion.capo > 0) "${cancion.artista}  ·  Capo ${cancion.capo}"
            else cancion.artista

        holder.txtAcordes.text = cancion.acordesPrincipales.joinToString("  ·  ")

        // Cambia el icono del corazón según si es favorito o no
        holder.btnFavorito.setImageResource(
            if (esFavorito) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_outline
        )

        // Clic en toda la tarjeta → abrir detalle
        holder.itemView.setOnClickListener { alHacerClic(cancion) }

        // Clic solo en el corazón → alternar favorito
        holder.btnFavorito.setOnClickListener { alClicFavorito(cancion) }
    }

    override fun getItemCount(): Int = canciones.size

    /** Actualiza la lista de canciones y/o los favoritos y refresca la pantalla */
    fun actualizar(nuevasCanciones: List<Cancion>, nuevosFavoritos: Set<Int>) {
        canciones = nuevasCanciones
        idsFavoritos = nuevosFavoritos
        notifyDataSetChanged()
    }
}