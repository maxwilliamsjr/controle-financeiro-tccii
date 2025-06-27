package com.example.controle_financeiro.ui.despesa

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.controle_financeiro.R
import com.example.controle_financeiro.model.Despesa
import com.google.firebase.Timestamp
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class ExtratoDespesasActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var btnInicio: Button
    private lateinit var btnFim: Button
    private lateinit var btnAplicar: Button
    private lateinit var spinnerCategoria: Spinner
    private lateinit var spinnerMetodo: Spinner
    private lateinit var txtValorTotalExtrato: TextView

    private var dataInicio: Date? = null
    private var dataFim: Date? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    private val metodosFixos = listOf("Pix", "Dinheiro", "Boleto", "Transferência", "Débito automático")
    private val metodosPermitidos = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_extrato_despesas)

        recycler = findViewById(R.id.recyclerExtrato)
        recycler.layoutManager = LinearLayoutManager(this)

        btnInicio = findViewById(R.id.btnDataInicio)
        btnFim = findViewById(R.id.btnDataFim)
        btnAplicar = findViewById(R.id.btnAplicarFiltros)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        spinnerMetodo = findViewById(R.id.spinnerMetodo)
        txtValorTotalExtrato = findViewById(R.id.txtValorTotalExtrato)

        configurarFiltros()

        btnInicio.setOnClickListener { abrirDatePicker(true) }
        btnFim.setOnClickListener { abrirDatePicker(false) }

        btnAplicar.setOnClickListener { carregarDespesasFiltradas() }

        val btnVoltar = findViewById<ImageView>(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            val intent = Intent(this, MenuDespesasActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun abrirDatePicker(inicio: Boolean) {
        val cal = Calendar.getInstance()
        val listener = DatePickerDialog.OnDateSetListener { _, y, m, d ->
            cal.set(y, m, d, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (inicio) {
                dataInicio = cal.time
                btnInicio.text = "Início: ${sdf.format(dataInicio!!)}"
            } else {
                dataFim = cal.time
                btnFim.text = "Fim: ${sdf.format(dataFim!!)}"
            }
        }
        DatePickerDialog(this, listener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun configurarFiltros() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("categorias")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val categorias = mutableListOf("Todas")
                categorias.addAll(result.documents.mapNotNull { it.getString("nomeCategoria") })

                val adapterCategoria = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
                adapterCategoria.setDropDownViewResource(R.layout.spinner_dropdown_item)
                spinnerCategoria.adapter = adapterCategoria
            }

        db.collection("cartoes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("tipo", "Débito")
            .get()
            .addOnSuccessListener { snapshot ->
                val metodos = mutableListOf("Todos")
                metodos.addAll(metodosFixos)
                metodosPermitidos.clear()
                metodosPermitidos.addAll(metodosFixos)

                for (document in snapshot.documents) {
                    val banco = document.getString("banco") ?: ""
                    val nome = document.getString("nome") ?: ""
                    val bandeira = document.getString("bandeira") ?: ""
                    val metodo = "Débito $banco $nome $bandeira".trim().replace("\\s+".toRegex(), " ")
                    metodos.add(metodo)
                    metodosPermitidos.add(metodo)
                }

                val adapterMetodo = ArrayAdapter(this, android.R.layout.simple_spinner_item, metodos)
                adapterMetodo.setDropDownViewResource(R.layout.spinner_dropdown_item)
                spinnerMetodo.adapter = adapterMetodo

                carregarUltimasDespesas()
            }
    }

    private fun carregarUltimasDespesas() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("despesas")
            .whereEqualTo("userId", userId)
            .orderBy("data", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { snapshot ->
                val despesas = snapshot.documents.mapNotNull { doc ->
                    val desp = doc.toObject(Despesa::class.java)
                    if (desp != null && desp.metodoPagamento in metodosPermitidos) desp else null
                }

                val despesasOrdenadas = despesas.sortedWith(
                    compareByDescending<Despesa> { it.data }
                        .thenByDescending { it.timestampCriacao?.toDate() ?: Date(0) }
                ).take(45)

                recycler.adapter = ExtratoAdapter(despesasOrdenadas) { despesa ->
                    val intent = Intent(this, EditarDespesaActivity::class.java)
                    intent.putExtra("ID_DESPESA", despesa.id)
                    startActivity(intent)
                }

                atualizarTotal(despesasOrdenadas)
            }
    }

    private fun carregarDespesasFiltradas() {
        val userId = auth.currentUser?.uid ?: return

        var query = db.collection("despesas").whereEqualTo("userId", userId)

        dataInicio?.let {
            Log.d("FiltroData", "Data início filtro: $it")
            query = query.whereGreaterThanOrEqualTo("data", Timestamp(it))
        }
        dataFim?.let {
            val cal = Calendar.getInstance()
            cal.time = it
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            Log.d("FiltroData", "Data fim filtro ajustada: ${cal.time}")
            query = query.whereLessThanOrEqualTo("data", Timestamp(cal.time))
        }

        query.get().addOnSuccessListener { snapshot ->
            val filtroCategoria = spinnerCategoria.selectedItem.toString()
            val filtroMetodo = spinnerMetodo.selectedItem.toString()

            val despesas = snapshot.documents.mapNotNull { doc ->
                val desp = doc.toObject(Despesa::class.java)
                if (
                    desp != null &&
                    desp.metodoPagamento in metodosPermitidos &&
                    (filtroCategoria == "Todas" || desp.categoria == filtroCategoria) &&
                    (filtroMetodo == "Todos" || desp.metodoPagamento == filtroMetodo)
                ) desp else null
            }

            val despesasOrdenadas = despesas.sortedWith(
                compareByDescending<Despesa> { it.data }
                    .thenByDescending { it.timestampCriacao?.toDate() ?: Date(0) }
            )

            recycler.adapter = ExtratoAdapter(despesasOrdenadas) { despesa ->
                val intent = Intent(this, EditarDespesaActivity::class.java)
                intent.putExtra("ID_DESPESA", despesa.id)
                startActivity(intent)
            }

            atualizarTotal(despesasOrdenadas)
        }
    }

    private fun atualizarTotal(despesas: List<Despesa>) {
        val total = despesas.sumOf { it.valor ?: 0.0 }
        val formatoBR = java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        txtValorTotalExtrato.text = "Total: ${formatoBR.format(total)}"
    }
}
