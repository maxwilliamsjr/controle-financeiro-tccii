package com.example.controle_financeiro.ui.metodopagamento

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import android.os.Build
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.controle_financeiro.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CadastrarCartaoActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var editNomeCartao: EditText
    private lateinit var editBancoCartao: EditText
    private lateinit var spinnerTipo: Spinner
    private lateinit var spinnerBandeira: Spinner
    private lateinit var editVencimento: EditText
    private lateinit var editDataInicio: EditText
    private lateinit var txtTituloDataInicio: TextView
    private lateinit var txtInfoLimiteData: TextView
    private lateinit var txtTituloDataFechamento: TextView
    private lateinit var btnSalvarCartao: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnVoltar: ImageView

    private var tipoSelecionado = "Crédito"
    private var bandeiraSelecionada = "Visa"
    private var dataVencimentoSelecionada: Calendar? = null
    private var dataInicioSelecionada: Calendar? = null

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_cadastrar_cartao)
        supportActionBar?.title = "Cadastrar Cartão"

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        editNomeCartao = findViewById(R.id.editNomeCartao)
        editBancoCartao = findViewById(R.id.editBancoCartao)
        spinnerTipo = findViewById(R.id.spinnerTipo)
        spinnerBandeira = findViewById(R.id.spinnerBandeira)
        editVencimento = findViewById(R.id.editVencimento)
        editDataInicio = findViewById(R.id.editDataInicio)
        txtTituloDataInicio = findViewById(R.id.txtTituloDataInicio)
        txtTituloDataFechamento = findViewById(R.id.txtTituloDataFechamento)
        txtInfoLimiteData = findViewById(R.id.txtInfoLimiteData)
        btnSalvarCartao = findViewById(R.id.btnSalvarCartao)
        btnCancelar = findViewById(R.id.btnCancelarCartao)
        btnVoltar = findViewById(R.id.btnVoltar)

        txtInfoLimiteData.text = "Só é aceita uma data de fechamento até 30 dias atrás."

        txtTituloDataInicio.visibility = TextView.GONE
        editDataInicio.visibility = EditText.GONE

        editDataInicio.isFocusable = false
        editDataInicio.isClickable = false
        editDataInicio.setTextColor(Color.GRAY)

        editVencimento.setOnClickListener { mostrarDatePickerFechamento() }
        editDataInicio.setOnClickListener { /* Não faz nada, está desabilitado */ }

        val tipos = listOf("Crédito", "Débito")
        spinnerTipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                tipoSelecionado = tipos[position]
                if (tipoSelecionado == "Crédito") {
                    editVencimento.visibility = EditText.VISIBLE
                    txtTituloDataFechamento.visibility = TextView.VISIBLE
                    txtInfoLimiteData.visibility = TextView.VISIBLE

                    txtTituloDataInicio.visibility = TextView.GONE
                    editDataInicio.visibility = EditText.GONE
                } else {
                    editVencimento.visibility = EditText.GONE
                    txtTituloDataFechamento.visibility = TextView.GONE
                    txtInfoLimiteData.visibility = TextView.GONE

                    txtTituloDataInicio.visibility = TextView.GONE
                    editDataInicio.visibility = EditText.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val bandeiras = listOf("Visa", "Mastercard", "Elo", "American Express", "Hipercard")
        spinnerBandeira.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bandeiras).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerBandeira.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                bandeiraSelecionada = bandeiras[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnSalvarCartao.setOnClickListener { salvarCartao() }
        btnCancelar.setOnClickListener { finish() }
    }

    private fun mostrarDatePickerFechamento() {
        val hoje = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            dataVencimentoSelecionada = Calendar.getInstance().apply { set(year, month, day) }
            editVencimento.setText(sdf.format(dataVencimentoSelecionada!!.time))

            txtTituloDataInicio.visibility = TextView.VISIBLE
            editDataInicio.visibility = EditText.VISIBLE

            dataInicioSelecionada = Calendar.getInstance().apply {
                time = dataVencimentoSelecionada!!.time
                add(Calendar.DAY_OF_MONTH, -30)
            }
            editDataInicio.setText(sdf.format(dataInicioSelecionada!!.time))

            editDataInicio.isFocusable = false
            editDataInicio.isClickable = false
            editDataInicio.setTextColor(Color.GRAY)

        }, hoje.get(Calendar.YEAR), hoje.get(Calendar.MONTH), hoje.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun salvarCartao() {
        val nome = editNomeCartao.text.toString().trim()
        val banco = editBancoCartao.text.toString().trim()
        val userId = auth.currentUser?.uid ?: run {
            mostrarDialog("Erro", "Usuário não autenticado.")
            return
        }

        if (nome.isEmpty() || banco.isEmpty()) {
            mostrarDialog("Atenção", "Preencha todos os campos.")
            return
        }

        var timestampFechamento: Timestamp? = null
        var timestampInicio: Timestamp? = null

        if (tipoSelecionado == "Crédito") {
            if (dataVencimentoSelecionada == null || dataInicioSelecionada == null) {
                mostrarDialog("Atenção", "Selecione a data de fechamento.")
                return
            }

            val umMesAtras = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -30) }
            if (dataVencimentoSelecionada!!.before(umMesAtras)) {
                mostrarDialog("Data inválida", "Só é aceita uma data de fechamento até 30 dias atrás.")
                return
            }

            timestampFechamento = Timestamp(dataVencimentoSelecionada!!.time)
            timestampInicio = Timestamp(dataInicioSelecionada!!.time)
        }

        val id = UUID.randomUUID().toString()
        val cartao = hashMapOf<String, Any>(
            "id" to id,
            "userId" to userId,
            "nome" to nome,
            "banco" to banco,
            "tipo" to tipoSelecionado,
            "bandeira" to bandeiraSelecionada,
            "faturaAtual" to 0.0
        )

        if (tipoSelecionado == "Crédito" && timestampFechamento != null) {
            cartao["dataFechamento"] = timestampFechamento
        }

        firestore.collection("cartoes")
            .document(id)
            .set(cartao)
            .addOnSuccessListener {
                if (tipoSelecionado == "Crédito" && timestampInicio != null && timestampFechamento != null) {
                    salvarFechamentoInicial(id, timestampInicio, timestampFechamento)
                } else {
                    mostrarDialogFinal("Cartão cadastrado com sucesso!")
                }
            }
            .addOnFailureListener {
                mostrarDialog("Erro", "Erro ao salvar cartão: ${it.message}")
            }
    }

    private fun salvarFechamentoInicial(cartaoId: String, inicio: Timestamp, fechamento: Timestamp) {
        val ajuste = hashMapOf<String, Any>(
            "dataInicio" to inicio,
            "dataFechamento" to fechamento,
            "criadoEm" to Timestamp.now(),
            "numero" to "1",
            "numeroInt" to 1
        )

        firestore.collection("cartoes")
            .document(cartaoId)
            .collection("fechamentos")
            .add(ajuste)
            .addOnSuccessListener {
                val mensagem = if (fechamento.toDate().before(Calendar.getInstance().time)) {
                    "Cartão cadastrado com sucesso!\n\nATENÇÃO: A fatura deste cartão já está fechada. Por favor, atualize a data de fechamento."
                } else {
                    "Cartão cadastrado com sucesso!"
                }
                mostrarDialogFinal(mensagem)
            }
            .addOnFailureListener {
                mostrarDialog("Erro", "Cartão criado, mas falha ao salvar fechamento: ${it.message}")
            }
    }

    private fun mostrarDialogFinal(mensagem: String) {
        AlertDialog.Builder(this)
            .setTitle("Sucesso")
            .setMessage(mensagem)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(this, ListarMetodoPagamentoActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun mostrarDialog(titulo: String, mensagem: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensagem)
            .setCancelable(false)
            .setPositiveButton("OK", null)
            .show()
    }
}
