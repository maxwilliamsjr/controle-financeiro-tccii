package com.example.controle_financeiro.ui.despesa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.controle_financeiro.R
import com.example.controle_financeiro.model.Despesa
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class FaturaAdapter(
    private val listaDespesas: List<Despesa>
) : RecyclerView.Adapter<FaturaAdapter.FaturaViewHolder>() {

    private val nf = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val sdf = SimpleDateFormat("dd MMM/yyyy", Locale("pt", "BR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaturaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fatura, parent, false)
        return FaturaViewHolder(view)
    }

    override fun onBindViewHolder(holder: FaturaViewHolder, position: Int) {
        val despesa = listaDespesas[position]

        holder.textNome.text = despesa.nome.ifEmpty { "Sem nome" }

        holder.textValor.text = nf.format(despesa.valor)

        val dataFormatada = despesa.data?.toDate()?.let { sdf.format(it) } ?: "-"
        holder.textData.text = dataFormatada
    }

    override fun getItemCount(): Int = listaDespesas.size

    class FaturaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textData: TextView = itemView.findViewById(R.id.textData)
        val textNome: TextView = itemView.findViewById(R.id.textNome)
        val textValor: TextView = itemView.findViewById(R.id.textValor)
    }
}
