package com.example.controle_financeiro.ui.despesa

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.os.Build
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.controle_financeiro.R
import com.example.controle_financeiro.model.Despesa
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DespesaActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var editNome: EditText
    private lateinit var editDescricao: EditText
    private lateinit var editValor: EditText
    private lateinit var editData: EditText
    private lateinit var parcelasDropdown: AutoCompleteTextView
    private lateinit var autoCompleteCategoria: AutoCompleteTextView
    private lateinit var autoCompleteMetodoPagamento: AutoCompleteTextView
    private lateinit var btnSalvar: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnDropdownCategoria: ImageButton
    private lateinit var btnDropdownMetodo: ImageButton
    private lateinit var btnAddCategoria: ImageButton

    private val categorias = mutableListOf<String>()

    private data class MetodoPagamento(val id: String?, val nomeExibicao: String)

    private val metodosPagamentoList = mutableListOf<MetodoPagamento>()

    private val metodosFixos =
        listOf("Pix", "Boleto", "Transferência", "Dinheiro", "Débito automático")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_despesa)
        supportActionBar?.title = "Cadastrar Despesa"

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        editNome = findViewById(R.id.editNome)
        editDescricao = findViewById(R.id.editDescricao)
        editValor = findViewById(R.id.editValor)
        editData = findViewById(R.id.editData)
        parcelasDropdown = findViewById(R.id.dropdownParcelas)
        autoCompleteCategoria = findViewById(R.id.autoCompleteCategoria)
        autoCompleteMetodoPagamento = findViewById(R.id.autoCompleteMetodoPagamento)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnCancelar = findViewById(R.id.btnCancelar)
        btnDropdownCategoria = findViewById(R.id.btnDropdownCategoria)
        btnDropdownMetodo = findViewById(R.id.btnDropdownMetodo)
        btnAddCategoria = findViewById(R.id.btnAddCategoria)

        editData.inputType = android.text.InputType.TYPE_NULL
        editData.setOnClickListener { mostrarDatePicker() }

        btnDropdownCategoria.setOnClickListener { autoCompleteCategoria.showDropDown() }
        btnDropdownMetodo.setOnClickListener { autoCompleteMetodoPagamento.showDropDown() }

        btnAddCategoria.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    com.example.controle_financeiro.ui.categoria.CategoriaActivity::class.java
                )
            )
        }

        editValor.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    editValor.removeTextChangedListener(this)
                    val cleanString = s.toString().replace("[R$,.\\s]".toRegex(), "")
                    if (cleanString.isNotEmpty()) {
                        val parsed = cleanString.toDouble() / 100
                        val formatted =
                            NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(parsed)
                        current = formatted
                        editValor.setText(formatted)
                        editValor.setSelection(formatted.length)
                        atualizarTextoParcelas(parsed)
                    } else {
                        current = ""
                        editValor.setText("")
                        atualizarTextoParcelas(0.0)
                    }
                    editValor.addTextChangedListener(this)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val parcelasList = (1..24).map { "${it}x" }
        val parcelasAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, parcelasList)
        parcelasDropdown.setAdapter(parcelasAdapter)
        parcelasDropdown.setText("1x R$ 0,00", false)
        parcelasDropdown.setOnClickListener { parcelasDropdown.showDropDown() }
        parcelasDropdown.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) parcelasDropdown.showDropDown() }
        parcelasDropdown.visibility = View.GONE

        autoCompleteMetodoPagamento.setOnItemClickListener { _, _, _, _ ->
            val metodo = autoCompleteMetodoPagamento.text.toString()
            if (metodo.contains("Crédito", ignoreCase = true)) {
                parcelasDropdown.visibility = View.VISIBLE
                parcelasDropdown.setText("1x R$ ${obterValorParcela(1)}", false)
            } else {
                parcelasDropdown.visibility = View.GONE
                parcelasDropdown.setText("", false)
            }
        }

        parcelasDropdown.setOnItemClickListener { _, _, _, _ ->
            atualizarTextoParcelas(obterValorDecimal())
        }

        carregarCategorias()
        carregarMetodosPagamento()

        btnSalvar.setOnClickListener { salvarDespesa() }
        btnCancelar.setOnClickListener { finish() }

        val btnVoltar = findViewById<ImageView>(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            finish()
        }
    }

    private fun obterValorDecimal(): Double {
        val valorStr = editValor.text.toString().replace("[R$,.\\s]".toRegex(), "")
        return valorStr.toDoubleOrNull()?.div(100) ?: 0.0
    }

    private fun obterValorParcela(parcelas: Int): String {
        val valorTotal = obterValorDecimal()
        if (parcelas <= 0 || valorTotal <= 0) return "0,00"
        val valorParcela = valorTotal / parcelas
        return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valorParcela)
    }

    private fun atualizarTextoParcelas(valorTotal: Double) {
        val parcelasText = parcelasDropdown.text.toString().split("x").firstOrNull()?.trim()
        val parcelas = parcelasText?.toIntOrNull() ?: 1
        if (parcelas > 0 && valorTotal > 0) {
            val valorParcelaFormatado = obterValorParcela(parcelas)
            parcelasDropdown.setText("${parcelas}x $valorParcelaFormatado", false)
        } else {
            parcelasDropdown.setText("1x R$ 0,00", false)
        }
    }

    override fun onResume() {
        super.onResume()
        carregarCategorias()
        carregarMetodosPagamento()
    }

    private fun mostrarDatePicker() {
        val calendario = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, ano, mes, dia ->
                val dataFormatada = String.format("%04d-%02d-%02d", ano, mes + 1, dia)
                editData.setText(dataFormatada)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun carregarCategorias() {
        firestore.collection("categorias")
            .get()
            .addOnSuccessListener { result ->
                categorias.clear()
                categorias.addAll(result.documents.mapNotNull { it.getString("nomeCategoria") })
                val adapter =
                    ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)
                autoCompleteCategoria.setAdapter(adapter)
            }
    }

    private fun carregarMetodosPagamento() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("cartoes")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { resultado ->
                metodosPagamentoList.clear()
                for (fixo in metodosFixos) {
                    metodosPagamentoList.add(MetodoPagamento(null, fixo))
                }

                for (doc in resultado) {
                    val nome = doc.getString("nome") ?: continue
                    val banco = doc.getString("banco") ?: ""
                    val tipo = doc.getString("tipo") ?: ""
                    val bandeira = doc.getString("bandeira") ?: ""
                    val nomeComposto = "$tipo $banco $nome $bandeira".trim()
                    metodosPagamentoList.add(MetodoPagamento(doc.id, nomeComposto))
                }

                val nomesExibicao = metodosPagamentoList.map { it.nomeExibicao }
                val adapter =
                    ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nomesExibicao)
                autoCompleteMetodoPagamento.setAdapter(adapter)
            }
    }

    private fun salvarDespesa() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val nome = editNome.text.toString().trim()
        val descricao = editDescricao.text.toString().trim()
        val valor = obterValorDecimal()
        val dataStr = editData.text.toString().trim()
        val categoria = autoCompleteCategoria.text.toString().trim()
        val metodoNome = autoCompleteMetodoPagamento.text.toString().trim()

        val metodoSelecionado = metodosPagamentoList.find { it.nomeExibicao == metodoNome }
        val metodoIdCartao = metodoSelecionado?.id
        val isCartaoCredito = metodoNome.contains("Crédito", ignoreCase = true)
        val qtdParcelas = if (isCartaoCredito) {
            parcelasDropdown.text.toString().split("x").firstOrNull()?.trim()?.toIntOrNull() ?: 1
        } else 1

        if (nome.isBlank() || valor <= 0.0 || dataStr.isBlank() || categoria.isBlank() || metodoNome.isBlank()) {
            Toast.makeText(
                this,
                "Preencha todos os campos obrigatórios corretamente",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!categorias.contains(categoria)) {
            Toast.makeText(this, "Categoria não cadastrada", Toast.LENGTH_SHORT).show()
            return
        }

        if (!metodosPagamentoList.any { it.nomeExibicao == metodoNome }) {
            Toast.makeText(this, "Método de pagamento não cadastrado", Toast.LENGTH_SHORT).show()
            return
        }

        if (isCartaoCredito && metodoIdCartao == null) {
            Toast.makeText(
                this,
                "Selecione um cartão válido para cartão de crédito",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dataOriginal = try {
            dateFormat.parse(dataStr) ?: throw IllegalArgumentException()
        } catch (e: Exception) {
            Toast.makeText(this, "Data inválida", Toast.LENGTH_SHORT).show()
            return
        }

        val batch = firestore.batch()
        val calendar = Calendar.getInstance()
        calendar.time = dataOriginal

        for (i in 1..qtdParcelas) {
            val id = UUID.randomUUID().toString()
            val nomeParcela = if (qtdParcelas > 1) "$nome ($i/$qtdParcelas)" else nome
            val destino = if (isCartaoCredito) "fatura" else "extrato"
            val dataTimestamp = Timestamp(calendar.time)

            val despesaMap = hashMapOf(
                "id" to id,
                "nome" to nomeParcela,
                "descricao" to descricao,
                "valor" to valor / qtdParcelas,
                "data" to dataTimestamp,
                "categoria" to categoria,
                "metodoPagamento" to metodoNome,
                "userId" to userId,
                "destino" to destino,
                "cartaoId" to metodoIdCartao,
                "timestampCriacao" to Timestamp.now()
            )

            val docRef = firestore.collection("despesas").document(id)
            batch.set(docRef, despesaMap)

            calendar.add(Calendar.MONTH, 1)
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Despesa(s) cadastrada(s) com sucesso!", Toast.LENGTH_SHORT)
                    .show()
                editNome.text.clear()
                editDescricao.text.clear()
                editValor.text.clear()
                editData.text.clear()
                autoCompleteCategoria.setText("")
                autoCompleteMetodoPagamento.setText("")
                parcelasDropdown.setText("1x R$ 0,00", false)
                parcelasDropdown.visibility = View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao salvar: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
