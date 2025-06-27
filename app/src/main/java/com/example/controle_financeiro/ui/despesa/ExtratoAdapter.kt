package com.example.controle_financeiro.ui.despesa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import com.example.controle_financeiro.R
import com.example.controle_financeiro.model.Despesa
import java.text.SimpleDateFormat
import java.util.*

class ExtratoAdapter(
    private val despesas: List<Despesa>,
    private val onClick: (Despesa) -> Unit
) : RecyclerView.Adapter<ExtratoAdapter.ViewHolder>() {

    private val sdfExibicao = SimpleDateFormat("dd MMM/yy", Locale("pt", "BR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_despesa, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = despesas.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val despesa = despesas[position]

        val dataFormatada = despesa.data?.toDate()?.let {
            sdfExibicao.format(it)
        } ?: "Sem data"

        holder.tvData.text = dataFormatada
        holder.tvNome.text = despesa.nome
        holder.tvValor.text = "R$ %.2f".format(despesa.valor)

        holder.itemView.setOnClickListener {
            onClick(despesa)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvData: TextView = view.findViewById(R.id.tvData)
        val tvNome: TextView = view.findViewById(R.id.tvNomeDespesa)
        val tvValor: TextView = view.findViewById(R.id.tvValorDespesa)
    }
}
