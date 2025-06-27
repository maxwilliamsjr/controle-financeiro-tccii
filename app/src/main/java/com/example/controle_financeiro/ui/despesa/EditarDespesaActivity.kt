package com.example.controle_financeiro.ui.despesa

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.*
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.controle_financeiro.R
import android.os.Build
import com.example.controle_financeiro.model.Despesa
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class EditarDespesaActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var editNome: EditText
    private lateinit var editDescricao: EditText
    private lateinit var editValor: EditText
    private lateinit var editData: EditText
    private lateinit var autoCompleteCategoria: AutoCompleteTextView
    private lateinit var autoCompleteMetodoPagamento: AutoCompleteTextView
    private lateinit var parcelasDropdown: AutoCompleteTextView

    private lateinit var btnSalvar: Button
    private lateinit var btnExcluir: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnVoltar: ImageView
    private lateinit var btnDropdownCategoria: ImageButton
    private lateinit var btnDropdownMetodo: ImageButton
    private lateinit var btnAddCategoria: ImageButton

    private var idDespesa: String? = null
    private val categorias = mutableListOf<String>()
    private val metodosPagamentoList = mutableListOf<MetodoPagamento>()

    private val metodosFixos = listOf("Pix", "Boleto", "Transferência", "Dinheiro", "Débito automático")

    private data class MetodoPagamento(val id: String?, val nomeExibicao: String)

    private val sdfData = SimpleDateFormat("yyyy-MM-dd", Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_editar_despesa)
        supportActionBar?.title = "Editar Despesa"

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        editNome = findViewById(R.id.editNome)
        editDescricao = findViewById(R.id.editDescricao)
        editValor = findViewById(R.id.editValor)
        editData = findViewById(R.id.editData)
        autoCompleteCategoria = findViewById(R.id.editCategoria)
        autoCompleteMetodoPagamento = findViewById(R.id.editMetodoPagamento)
        parcelasDropdown = findViewById(R.id.dropdownParcelas)

        btnSalvar = findViewById(R.id.btnSalvar)
        btnExcluir = findViewById(R.id.btnExcluir)
        btnCancelar = findViewById(R.id.btnCancelar)
        btnVoltar = findViewById(R.id.btnVoltar)
        btnDropdownCategoria = findViewById(R.id.btnDropdownCategoria)
        btnDropdownMetodo = findViewById(R.id.btnDropdownMetodo)
        btnAddCategoria = findViewById(R.id.btnAddCategoria)

        idDespesa = intent.getStringExtra("ID_DESPESA")

        if (idDespesa == null) {
            Toast.makeText(this, "Erro ao carregar despesa", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        editData.inputType = android.text.InputType.TYPE_NULL
        editData.setOnClickListener { mostrarDatePicker() }

        btnDropdownCategoria.setOnClickListener { autoCompleteCategoria.showDropDown() }
        btnDropdownMetodo.setOnClickListener { autoCompleteMetodoPagamento.showDropDown() }
        btnAddCategoria.setOnClickListener {
            startActivity(Intent(this, com.example.controle_financeiro.ui.categoria.CategoriaActivity::class.java))
        }

        editValor.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    editValor.removeTextChangedListener(this)
                    val cleanString = s.toString().replace("[R$,.\\s]".toRegex(), "")
                    if (cleanString.isNotEmpty()) {
                        val parsed = cleanString.toDouble() / 100
                        val formatted = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(parsed)
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
        })

        val parcelasList = (1..24).map { "${it}x" }
        val parcelasAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, parcelasList)
        parcelasDropdown.setAdapter(parcelasAdapter)
        parcelasDropdown.setOnClickListener { parcelasDropdown.showDropDown() }
        parcelasDropdown.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) parcelasDropdown.showDropDown() }
        parcelasDropdown.setText("1x", false)
        parcelasDropdown.visibility = View.GONE

        autoCompleteMetodoPagamento.setOnItemClickListener { _, _, _, _ ->
            val metodo = autoCompleteMetodoPagamento.text.toString()
            if (metodo.contains("Crédito", ignoreCase = true)) {
                parcelasDropdown.visibility = View.VISIBLE
                val valor = obterValorDecimal()
                parcelasDropdown.setText("1x R$ ${obterValorParcela(1, valor)}", false)
            } else {
                parcelasDropdown.visibility = View.GONE
                parcelasDropdown.setText("1x", false)
            }
        }

        parcelasDropdown.setOnItemClickListener { _, _, _, _ ->
            val valor = obterValorDecimal()
            atualizarTextoParcelas(valor)
        }

        carregarCategorias()
        carregarMetodosPagamento()
        carregarDespesa()

        btnSalvar.setOnClickListener { atualizarDespesa() }
        btnExcluir.setOnClickListener { confirmarExclusao() }
        btnCancelar.setOnClickListener { finish() }
        btnVoltar.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        carregarCategorias()
        carregarMetodosPagamento()
    }

    private fun mostrarDatePicker() {
        val calendario = Calendar.getInstance()
        val ano = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, a, m, d ->
            val dataFormatada = String.format("%04d-%02d-%02d", a, m + 1, d)
            editData.setText(dataFormatada)
        }, ano, mes, dia).show()
    }

    private fun carregarCategorias() {
        firestore.collection("categorias")
            .get()
            .addOnSuccessListener { result ->
                categorias.clear()
                categorias.addAll(result.documents.mapNotNull { it.getString("nomeCategoria") })
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias)
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
                    val nomeComposto = "$tipo $banco $nome $bandeira".trim().replace("\\s+".toRegex(), " ")
                    metodosPagamentoList.add(MetodoPagamento(doc.id, nomeComposto))
                }

                val nomesExibicao = metodosPagamentoList.map { it.nomeExibicao }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nomesExibicao)
                autoCompleteMetodoPagamento.setAdapter(adapter)
            }
    }

    private fun carregarDespesa() {
        firestore.collection("despesas").document(idDespesa!!)
            .get()
            .addOnSuccessListener { doc ->
                val despesa = doc.toObject(Despesa::class.java)
                val userId = auth.currentUser?.uid
                if (despesa != null && despesa.userId == userId) {
                    editNome.setText(despesa.nome)
                    editDescricao.setText(despesa.descricao)

                    val valorFormatado = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(despesa.valor)
                    editValor.setText(valorFormatado)

                    val dataString = when (val dataField = despesa.data) {
                        is Timestamp -> sdfData.format(dataField.toDate())
                        is Date -> sdfData.format(dataField)
                        is String -> dataField
                        else -> ""
                    }
                    editData.setText(dataString)

                    autoCompleteCategoria.setText(despesa.categoria, false)
                    autoCompleteMetodoPagamento.setText(despesa.metodoPagamento, false)

                    if (despesa.metodoPagamento.contains("Crédito", ignoreCase = true)) {
                        parcelasDropdown.visibility = View.VISIBLE
                        parcelasDropdown.setText("1x", false)
                    } else {
                        parcelasDropdown.visibility = View.GONE
                        parcelasDropdown.setText("1x", false)
                    }
                } else {
                    Toast.makeText(this, "Despesa não encontrada ou acesso negado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar: ${it.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun obterValorDecimal(): Double {
        val valorStr = editValor.text.toString().replace("[R$,.\\s]".toRegex(), "")
        return valorStr.toDoubleOrNull()?.div(100) ?: 0.0
    }

    private fun obterValorParcela(parcelas: Int, valorTotal: Double): String {
        if (parcelas <= 0 || valorTotal <= 0) return "0,00"
        val valorParcela = valorTotal / parcelas
        return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valorParcela)
    }

    private fun atualizarTextoParcelas(valorTotal: Double) {
        val parcelasText = parcelasDropdown.text.toString().split("x").firstOrNull()?.trim()
        val parcelas = parcelasText?.toIntOrNull() ?: 1
        if (parcelas > 0 && valorTotal > 0) {
            val valorParcelaFormatado = obterValorParcela(parcelas, valorTotal)
            parcelasDropdown.setText("${parcelas}x $valorParcelaFormatado", false)
        } else {
            parcelasDropdown.setText("1x R$ 0,00", false)
        }
    }

    private fun atualizarDespesa() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val nome = editNome.text.toString().trim()
        val descricao = editDescricao.text.toString().trim()
        val valor = obterValorDecimal()
        val dataString = editData.text.toString().trim()
        val categoria = autoCompleteCategoria.text.toString().trim()
        val metodoNome = autoCompleteMetodoPagamento.text.toString().trim()
        val metodoSelecionado = metodosPagamentoList.find { it.nomeExibicao == metodoNome }
        val metodoIdCartao = metodoSelecionado?.id
        val isCartaoCredito = metodoNome.contains("Crédito", ignoreCase = true)
        val qtdParcelas = if (isCartaoCredito) {
            parcelasDropdown.text.toString().split("x").firstOrNull()?.trim()?.toIntOrNull() ?: 1
        } else 1

        if (nome.isBlank() || valor <= 0.0 || TextUtils.isEmpty(dataString) || categoria.isBlank() || metodoNome.isBlank()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios corretamente", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Selecione um cartão válido para cartão de crédito", Toast.LENGTH_SHORT).show()
            return
        }

        val dataTimestamp = try {
            val date = sdfData.parse(dataString)
            if (date != null) Timestamp(date) else null
        } catch (e: Exception) {
            null
        }

        if (dataTimestamp == null) {
            Toast.makeText(this, "Data inválida", Toast.LENGTH_SHORT).show()
            return
        }

        val novaDespesa = Despesa(
            id = idDespesa!!,
            nome = nome,
            descricao = descricao,
            valor = valor,
            data = dataTimestamp,
            categoria = categoria,
            metodoPagamento = metodoNome,
            userId = userId,
            cartaoId = metodoIdCartao
        )

        firestore.collection("despesas").document(idDespesa!!)
            .set(novaDespesa)
            .addOnSuccessListener {
                Toast.makeText(this, "Despesa atualizada", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao atualizar: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmarExclusao() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar exclusão")
            .setMessage("Tem certeza que deseja excluir esta despesa?")
            .setPositiveButton("Sim") { _, _ -> excluirDespesa() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirDespesa() {
        firestore.collection("despesas").document(idDespesa!!)
            .delete()
            .addOnSuccessListener {
                AlertDialog.Builder(this)
                    .setTitle("Despesa excluída")
                    .setMessage("A despesa foi removida com sucesso.")
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ ->
                        val intent = Intent(this, com.example.controle_financeiro.ui.despesa.ListarDespesasActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao excluir: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
