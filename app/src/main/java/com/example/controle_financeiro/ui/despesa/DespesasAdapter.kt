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
import java.util.*

class DespesasAdapter(
    private val listaDespesas: List<Despesa>,
    private val onItemClick: ((Despesa) -> Unit)? = null
) : RecyclerView.Adapter<DespesasAdapter.DespesaViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val nf = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DespesaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_despesa, parent, false)
        return DespesaViewHolder(view)
    }

    override fun onBindViewHolder(holder: DespesaViewHolder, position: Int) {
        val despesa = listaDespesas[position]

        holder.tvNomeDespesa.text = despesa.nome
        holder.tvValorDespesa.text = nf.format(despesa.valor)

        val dataDate = despesa.data?.toDate()
        holder.tvData.text = if (dataDate != null) sdf.format(dataDate) else ""

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(despesa)
        }
    }

    override fun getItemCount(): Int = listaDespesas.size

    class DespesaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNomeDespesa: TextView = itemView.findViewById(R.id.tvNomeDespesa)
        val tvValorDespesa: TextView = itemView.findViewById(R.id.tvValorDespesa)
        val tvData: TextView = itemView.findViewById(R.id.tvData)
    }
}
