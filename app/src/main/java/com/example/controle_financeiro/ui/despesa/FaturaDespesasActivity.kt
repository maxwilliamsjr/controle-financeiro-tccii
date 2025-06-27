package com.example.controle_financeiro.ui.despesa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.os.Build
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.controle_financeiro.R
import com.example.controle_financeiro.model.Despesa
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class FaturaDespesasActivity : AppCompatActivity() {

    private lateinit var spinnerCartoes: Spinner
    private lateinit var mesesContainer: LinearLayout
    private lateinit var txtValorTotal: TextView
    private lateinit var txtPeriodo: TextView
    private lateinit var recyclerDespesas: RecyclerView
    private lateinit var layoutFatura: LinearLayout
    private lateinit var txtMensagem: TextView
    private lateinit var txtTituloLancamentos: TextView
    private lateinit var containerSpinnerCartao: LinearLayout

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sdfMesAno = SimpleDateFormat("MMM/yyyy", Locale("pt", "BR"))
    private val sdfDiaMesAno = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    private var listaCartoes = mutableListOf<Cartao>()
    private var listaFechamentos = mutableListOf<Pair<Date, Date>>()
    private var cartaoSelecionadoId: String? = null
    private var faturaSelecionadaIndice = 0

    data class Cartao(val id: String, val nome: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_fatura_despesas)

        val btnVoltar = findViewById<ImageView>(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            finish()
        }
        Log.d("FaturaDespesasActivity", "onCreate iniciado")

        spinnerCartoes = findViewById(R.id.spinnerCartoes)
        mesesContainer = findViewById(R.id.mesesContainer)
        txtValorTotal = findViewById(R.id.txtValorTotal)
        txtPeriodo = findViewById(R.id.txtPeriodo)
        recyclerDespesas = findViewById(R.id.recyclerDespesas)
        layoutFatura = findViewById(R.id.layoutFaturaCompleta)
        txtMensagem = findViewById(R.id.txtMensagemNenhumCartao)
        txtTituloLancamentos = findViewById(R.id.txtTituloLancamentos)
        containerSpinnerCartao = findViewById(R.id.containerSpinnerCartao)

        recyclerDespesas.layoutManager = LinearLayoutManager(this)

        carregarCartoesUsuario()
    }

    private fun carregarCartoesUsuario() {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            mostrarMensagem("Usuário não autenticado")
            return
        }
        db.collection("cartoes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("tipo", "Crédito")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    mostrarSemCartoes()
                } else {
                    listaCartoes.clear()
                    for (doc in snapshot.documents) {
                        val id = doc.id
                        val nome = doc.getString("nome") ?: "Cartão"
                        listaCartoes.add(Cartao(id, nome))
                    }
                    configurarSpinnerCartoes()
                    mostrarLayoutFatura()
                }
            }
            .addOnFailureListener {
                mostrarMensagem("Erro ao carregar cartões: ${it.message}")
            }
    }

    private fun configurarSpinnerCartoes() {
        val nomes = listaCartoes.map { it.nome }
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, nomes)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCartoes.adapter = adapterSpinner

        spinnerCartoes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                cartaoSelecionadoId = listaCartoes[position].id
                carregarFechamentos(cartaoSelecionadoId!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        spinnerCartoes.setSelection(0)
    }

    private fun mostrarSemCartoes() {
        containerSpinnerCartao.visibility = View.GONE
        mesesContainer.visibility = View.GONE
        recyclerDespesas.visibility = View.GONE
        txtTituloLancamentos.visibility = View.GONE
        txtValorTotal.visibility = View.GONE
        txtPeriodo.visibility = View.GONE
        txtMensagem.visibility = View.VISIBLE
        txtMensagem.text = "Nenhum cartão de crédito cadastrado.\nPor favor, cadastre um cartão para visualizar suas faturas."
    }

    private fun mostrarLayoutFatura() {
        containerSpinnerCartao.visibility = View.VISIBLE
        mesesContainer.visibility = View.VISIBLE
        recyclerDespesas.visibility = View.VISIBLE
        txtTituloLancamentos.visibility = View.VISIBLE
        txtValorTotal.visibility = View.VISIBLE
        txtPeriodo.visibility = View.VISIBLE

        txtMensagem.visibility = View.GONE
    }

    private fun carregarFechamentos(cartaoId: String) {
        db.collection("cartoes")
            .document(cartaoId)
            .collection("fechamentos")
            .get()
            .addOnSuccessListener { snapshot ->
                val versoes = snapshot.documents.mapNotNull { doc ->
                    val numero = doc.getString("numero") ?: return@mapNotNull null
                    val partes = numero.split(".")
                    val intParte = partes[0].toIntOrNull() ?: return@mapNotNull null
                    val subParte = if (partes.size > 1) partes[1].toIntOrNull() ?: 0 else 0
                    Triple(intParte, subParte, doc)
                }.sortedWith(compareByDescending<Triple<Int, Int, *>> { it.first }.thenByDescending { it.second })

                if (versoes.isNotEmpty()) {
                    val fechamentosPorNumero = versoes.groupBy { it.first }
                        .mapValues { entry -> entry.value.maxByOrNull { it.second }!! }
                        .toSortedMap()

                    val datasFaturas = fechamentosPorNumero.values.mapNotNull { triple ->
                        val doc = triple.third
                        val inicio = doc.getTimestamp("dataInicio")?.toDate()
                        val fim = doc.getTimestamp("dataFechamento")?.toDate()
                        if (inicio != null && fim != null) Pair(inicio, fim) else null
                    }.toMutableList()

                    val ultFechamento = datasFaturas.lastOrNull()
                    if (ultFechamento != null) {
                        val cal = Calendar.getInstance().apply { time = ultFechamento.second }
                        for (i in 1..3) {
                            cal.add(Calendar.DAY_OF_MONTH, 1)
                            val novaInicio = cal.time
                            cal.add(Calendar.DAY_OF_MONTH, 29)
                            val novaFim = cal.time
                            datasFaturas.add(Pair(novaInicio, novaFim))
                        }
                    }

                    listaFechamentos = datasFaturas
                    gerarScrollMeses()
                    selecionarFatura(0)
                } else {
                    listaFechamentos.clear()
                    mesesContainer.removeAllViews()
                    txtPeriodo.text = ""
                    txtValorTotal.text = "Total: R$ 0,00"
                    recyclerDespesas.adapter = null
                    txtTituloLancamentos.text = "Nenhuma fatura encontrada."
                }
            }
            .addOnFailureListener {
                mostrarMensagem("Erro ao carregar fechamentos: ${it.message}")
            }
    }

    private fun gerarScrollMeses() {
        mesesContainer.removeAllViews()
        listaFechamentos.forEachIndexed { index, (inicio, fim) ->
            val texto = sdfMesAno.format(fim).replaceFirstChar { it.uppercase() }
            val btn = Button(this).apply {
                text = texto
                setOnClickListener { selecionarFatura(index) }
            }
            mesesContainer.addView(btn)
        }
    }

    private fun selecionarFatura(indice: Int) {
        if (indice !in listaFechamentos.indices) return
        faturaSelecionadaIndice = indice
        val (inicio, fim) = listaFechamentos[indice]

        val inicioDia = startOfDay(inicio)
        val fimDia = endOfDay(fim)

        val periodoFormatado = "Período: ${sdfDiaMesAno.format(inicioDia)} até ${sdfDiaMesAno.format(fimDia)}"
        txtPeriodo.text = periodoFormatado

        carregarDespesasFatura(inicioDia, fimDia)
    }

    private fun carregarDespesasFatura(inicio: Date, fim: Date) {
        val userId = auth.currentUser?.uid
        val cartaoId = cartaoSelecionadoId
        if (userId.isNullOrEmpty() || cartaoId.isNullOrEmpty()) {
            mostrarMensagem("Erro: usuário ou cartão não definidos")
            return
        }

        db.collection("despesas")
            .whereEqualTo("userId", userId)
            .whereEqualTo("cartaoId", cartaoId)
            .whereEqualTo("destino", "fatura")
            .whereGreaterThanOrEqualTo("data", Timestamp(inicio))
            .whereLessThanOrEqualTo("data", Timestamp(fim))
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("FaturaDespesasActivity", "Despesas encontradas: ${snapshot.size()}")
                if (snapshot.isEmpty) {
                    txtTituloLancamentos.text = "Nenhuma despesa na fatura."
                    recyclerDespesas.adapter = null
                    txtValorTotal.text = "Total: R$ 0,00"
                    return@addOnSuccessListener
                }

                val despesas = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val nome = doc.getString("nome")
                    val descricao = doc.getString("descricao") ?: ""
                    val valor = doc.getDouble("valor")
                    val data = doc.getTimestamp("data")
                    val categoria = doc.getString("categoria") ?: ""
                    val metodoPagamento = doc.getString("metodoPagamento") ?: ""
                    val userIdDespesa = doc.getString("userId") ?: ""
                    val destino = doc.getString("destino") ?: ""
                    val cartaoIdDespesa = doc.getString("cartaoId")
                    val timestampCriacao = doc.getTimestamp("timestampCriacao") ?: Timestamp.now()

                    if (nome != null && valor != null && data != null && userIdDespesa.isNotEmpty()) {
                        Despesa(
                            id = id,
                            nome = nome,
                            descricao = descricao,
                            valor = valor,
                            data = data,
                            categoria = categoria,
                            metodoPagamento = metodoPagamento,
                            userId = userIdDespesa,
                            destino = destino,
                            cartaoId = cartaoIdDespesa,
                            timestampCriacao = timestampCriacao
                        )
                    } else null
                }.sortedBy { it.data }

                recyclerDespesas.adapter = DespesasAdapter(despesas) { despesaClicada ->
                    val intent = Intent(this, EditarDespesaActivity::class.java)
                    intent.putExtra("ID_DESPESA", despesaClicada.id)
                    startActivity(intent)
                }

                val total = despesas.sumOf { it.valor }
                val totalFormatado = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(total)

                txtValorTotal.text = "Total: $totalFormatado"
                txtTituloLancamentos.text = "Lançamentos na fatura"
            }
            .addOnFailureListener { e ->
                mostrarMensagem("Erro ao carregar despesas: ${e.message}")
            }
    }

    private fun mostrarMensagem(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun startOfDay(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun endOfDay(date: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }
}
