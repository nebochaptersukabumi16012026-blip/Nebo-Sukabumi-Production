package com.example.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.CicilanAnggota
import java.text.NumberFormat
import java.util.Locale

class CicilanAdapter(
    private var listAnggota: MutableList<CicilanAnggota> = mutableListOf(),
    private val onItemClick: ((CicilanAnggota) -> Unit)? = null
) : RecyclerView.Adapter<CicilanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNamaAnggota: TextView = view.findViewById(R.id.tvNamaAnggota)
        val tvNraAnggota: TextView = view.findViewById(R.id.tvNraAnggota)
        val tvSisaCicilan: TextView = view.findViewById(R.id.tvSisaCicilan)
        val tvHargaBarang: TextView = view.findViewById(R.id.tvHargaBarang)
        val tvSudahDibayar: TextView = view.findViewById(R.id.tvSudahDibayar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cicilan_anggota, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listAnggota[position]

        holder.tvNamaAnggota.text = item.nama?.ifBlank { "Anggota Nebo" } ?: "Anggota Nebo"
        holder.tvNraAnggota.text = "NRA: ${item.nra?.ifBlank { "-" } ?: "-"}"
        holder.tvSisaCicilan.text = formatRupiah(item.sisa_cicilan)
        holder.tvHargaBarang.text = formatRupiah(item.harga_barang)

        val dibayar = if (item.total_dibayar > 0.0) item.total_dibayar else item.sudah_dibayar
        holder.tvSudahDibayar.text = formatRupiah(dibayar)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = listAnggota.size

    fun updateData(newList: List<CicilanAnggota>) {
        listAnggota.clear()
        listAnggota.addAll(newList)
        notifyDataSetChanged()
    }

    private fun formatRupiah(amount: Double): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
                maximumFractionDigits = 0
            }
            formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
        } catch (e: Exception) {
            "Rp " + String.format(Locale("in", "ID"), "%,.0f", amount)
        }
    }
}
