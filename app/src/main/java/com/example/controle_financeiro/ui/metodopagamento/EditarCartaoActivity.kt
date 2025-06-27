package com.example.controle_financeiro.ui.metodopagamento

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import android.os.Build
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.controle_financeiro.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EditarCartaoActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    private lateinit var editNomeCartao: EditText
    private lateinit var editBancoCartao: EditText
    private lateinit var editVencimento: EditText
    private lateinit var editDataInicio: EditText
    private lateinit var txtAvisoFaturaFechada: TextView
    private lateinit var txtTituloDataInicio: TextView
    private lateinit var txtTituloDataFechamento: TextView
    private lateinit var txtInfoLimiteData: TextView

    private lateinit var btnAtualizarDataFechamento: Button
    private lateinit var btnCorrigirCadastroAnterior: Button
    private lateinit var btnSalvar: Button
    private lateinit var btnExcluir: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnVoltar: ImageView

    private var idMetodo: String = ""
    private var dataFechamentoSelecionada: Calendar? = null
    private var dataInicioSelecionada: Calendar? = null
    private var ultimaDataFechamentoSalva: Calendar? = null
    private var tipoCartao: String = ""

    private val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_editar_cartao)

        firestore = FirebaseFirestore.getInstance()

        editNomeCartao = findViewById(R.id.editNomeCartao)
        editBancoCartao = findViewById(R.id.editBancoCartao)
        editVencimento = findViewById(R.id.editVencimento)
        editDataInicio = findViewById(R.id.editDataInicio)
        txtAvisoFaturaFechada = findViewById(R.id.txtAvisoFaturaFechada)
        txtTituloDataInicio = findViewById(R.id.txtTituloDataInicio)
        txtTituloDataFechamento = findViewById(R.id.txtTituloDataFechamento)
        txtInfoLimiteData = findViewById(R.id.txtInfoLimiteData)

        btnAtualizarDataFechamento = findViewById(R.id.btnAtualizarDataFechamento)
        btnCorrigirCadastroAnterior = findViewById(R.id.btnCorrigirCadastroAnterior)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnExcluir = findViewById(R.id.btnExcluir)
        btnCancelar = findViewById(R.id.btnCancelarCartao)
        btnVoltar = findViewById(R.id.btnVoltar)

        idMetodo = intent.getStringExtra("id") ?: return finish()

        editBancoCartao.isEnabled = false
        editBancoCartao.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        editVencimento.isEnabled = false
        editVencimento.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        editDataInicio.isEnabled = false
        editDataInicio.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        firestore.collection("cartoes").document(idMetodo).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val nome = doc.getString("nome") ?: ""
                    val banco = doc.getString("banco") ?: ""
                    tipoCartao = doc.getString("tipo") ?: ""
                    editNomeCartao.setText(nome)
                    editBancoCartao.setText(banco)

                    if (tipoCartao.equals("Débito", ignoreCase = true)) {
                        esconderCamposDeDatas()
                    } else if (tipoCartao.equals("Crédito", ignoreCase = true)) {
                        carregarUltimaVersaoFechamento()
                        btnAtualizarDataFechamento.setOnClickListener { abrirDatePickerAtualizarData() }
                        btnCorrigirCadastroAnterior.setOnClickListener { abrirPopupCorrecao() }
                    } else {
                        esconderCamposDeDatas()
                    }

                    btnSalvar.setOnClickListener { salvarAtualizacao() }

                } else {
                    mostrarDialogSimples("Cartão não encontrado.")
                    finish()
                }
            }
            .addOnFailureListener {
                mostrarDialogSimples("Erro ao carregar cartão: ${it.message}")
                finish()
            }

        btnCancelar.setOnClickListener {
            val intent = Intent(this, ListarMetodoPagamentoActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        btnExcluir.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Excluir Cartão")
                .setMessage("Tem certeza que deseja excluir este cartão?")
                .setPositiveButton("Sim") { _, _ ->
                    firestore.collection("cartoes").document(idMetodo).delete()
                        .addOnSuccessListener {
                            AlertDialog.Builder(this)
                                .setTitle("Excluído")
                                .setMessage("Cartão excluído com sucesso.")
                                .setPositiveButton("OK") { _, _ ->
                                    val intent = Intent(this, ListarMetodoPagamentoActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .show()
                        }
                        .addOnFailureListener {
                            mostrarDialogSimples("Erro ao excluir cartão: ${it.message}")
                        }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun esconderCamposDeDatas() {
        editVencimento.visibility = View.GONE
        editDataInicio.visibility = View.GONE
        txtAvisoFaturaFechada.visibility = View.GONE
        txtTituloDataInicio.visibility = View.GONE
        txtTituloDataFechamento.visibility = View.GONE
        txtInfoLimiteData.visibility = View.GONE
        btnAtualizarDataFechamento.visibility = View.GONE
        btnCorrigirCadastroAnterior.visibility = View.GONE
    }

    private fun carregarUltimaVersaoFechamento() {
        val fechamentosRef = firestore.collection("cartoes").document(idMetodo).collection("fechamentos")

        fechamentosRef.get().addOnSuccessListener { snapshot ->
            val ultimaVersao = snapshot.documents
                .mapNotNull { doc ->
                    val numero = doc.getString("numero") ?: return@mapNotNull null
                    val partes = numero.split(".")
                    val intParte = partes[0].toIntOrNull() ?: return@mapNotNull null
                    val subParte = if (partes.size > 1) partes[1].toIntOrNull() ?: 0 else 0
                    Triple(intParte, subParte, doc)
                }
                .maxWithOrNull(compareBy({ it.first }, { it.second }))?.third

            ultimaVersao?.let { doc ->
                val inicio = doc.getTimestamp("dataInicio")?.toDate()
                val fechamento = doc.getTimestamp("dataFechamento")?.toDate()
                if (inicio != null && fechamento != null) {
                    val calInicio = Calendar.getInstance().apply { time = inicio }
                    val calFechamento = Calendar.getInstance().apply { time = fechamento }

                    dataInicioSelecionada = calInicio
                    dataFechamentoSelecionada = calFechamento
                    ultimaDataFechamentoSalva = calFechamento.clone() as Calendar

                    editDataInicio.setText(formatoData.format(calInicio.time))
                    editVencimento.setText(formatoData.format(calFechamento.time))

                    mostrarAvisoFaturaSeFechada()
                }
            }
        }
    }

    private fun abrirDatePickerAtualizarData() {
        val hoje = Calendar.getInstance()
        val picker = DatePickerDialog(this, { _, y, m, d ->
            val novaFech = Calendar.getInstance().apply { set(y, m, d) }
            val novaInicio = Calendar.getInstance().apply {
                time = ultimaDataFechamentoSalva?.time ?: Date()
                add(Calendar.DAY_OF_MONTH, 1)
            }

            dataFechamentoSelecionada = novaFech
            dataInicioSelecionada = novaInicio

            editVencimento.setText(formatoData.format(novaFech.time))
            editDataInicio.setText(formatoData.format(novaInicio.time))

            AlertDialog.Builder(this)
                .setTitle("Confirmar Atualização")
                .setMessage("Deseja confirmar a nova data de fechamento?")
                .setPositiveButton("Confirmar") { _, _ -> salvarAtualizacaoComVersao() }
                .setNegativeButton("Cancelar", null)
                .show()
        }, hoje.get(Calendar.YEAR), hoje.get(Calendar.MONTH), hoje.get(Calendar.DAY_OF_MONTH))

        picker.setButton(DatePickerDialog.BUTTON_POSITIVE, "Confirmar", picker)
        picker.show()
    }

    private fun mostrarAvisoFaturaSeFechada() {
        val hoje = Calendar.getInstance()
        txtAvisoFaturaFechada.visibility =
            if (dataFechamentoSelecionada?.before(hoje) == true) View.VISIBLE else View.GONE
    }

    private fun salvarAtualizacao() {
        if (tipoCartao.equals("Débito", ignoreCase = true)) {
            salvarSomenteNome()
        } else {
            salvarAtualizacaoComVersao()
        }
    }

    private fun salvarSomenteNome() {
        val nome = editNomeCartao.text.toString().trim()
        if (nome.isEmpty()) {
            mostrarDialogSimples("Por favor, informe o nome do cartão.")
            return
        }
        val dados = mapOf("nome" to nome)
        firestore.collection("cartoes").document(idMetodo).update(dados)
            .addOnSuccessListener {
                mostrarDialogComRetorno("Sucesso", "Nome do cartão atualizado com sucesso!")
            }
            .addOnFailureListener {
                mostrarDialogSimples("Erro ao atualizar nome: ${it.message}")
            }
    }

    private fun salvarAtualizacaoComVersao() {
        val nome = editNomeCartao.text.toString().trim()
        val banco = editBancoCartao.text.toString().trim()
        val fechamento = dataFechamentoSelecionada
        val inicio = dataInicioSelecionada

        if (nome.isEmpty() || banco.isEmpty() || fechamento == null || inicio == null) {
            mostrarDialogSimples("Preencha todos os campos.")
            return
        }

        val hoje = Calendar.getInstance()
        if (fechamento.before(hoje.apply { add(Calendar.DAY_OF_MONTH, -30) })) {
            mostrarDialogSimples("Só é aceita uma data de fechamento até 30 dias atrás.")
            return
        }

        val dados = mapOf(
            "nome" to nome,
            "banco" to banco,
            "dataFechamento" to Timestamp(fechamento.time)
        )

        firestore.collection("cartoes").document(idMetodo).update(dados)
            .addOnSuccessListener {
                salvarNovaVersaoFechamento(inicio, fechamento)
            }
            .addOnFailureListener {
                mostrarDialogSimples("Erro ao atualizar: ${it.message}")
            }
    }

    private fun salvarNovaVersaoFechamento(inicio: Calendar, fim: Calendar) {
        val fechamentosRef = firestore.collection("cartoes").document(idMetodo).collection("fechamentos")

        fechamentosRef.orderBy("numeroInt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val maiorNumeroInt = snapshot.documents.firstOrNull()?.getLong("numeroInt")?.toInt() ?: 0
                val novoNumeroInt = maiorNumeroInt + 1

                val dados = mapOf(
                    "dataInicio" to Timestamp(inicio.time),
                    "dataFechamento" to Timestamp(fim.time),
                    "criadoEm" to Timestamp.now(),
                    "numero" to novoNumeroInt.toString(),
                    "numeroInt" to novoNumeroInt
                )

                fechamentosRef.add(dados)
                    .addOnSuccessListener {
                        mostrarDialogComRetorno("Sucesso", "Cartão atualizado com sucesso!")
                    }
                    .addOnFailureListener {
                        mostrarDialogSimples("Erro ao salvar fechamento: ${it.message}")
                    }
            }
    }

    private fun mostrarDialogSimples(msg: String) {
        AlertDialog.Builder(this)
            .setTitle("Atenção")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun mostrarDialogComRetorno(titulo: String, mensagem: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
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

    private fun abrirPopupCorrecao() {
        val popupView = layoutInflater.inflate(R.layout.fechamento_fatura_popup, null)
        val btnCancelarPopup = popupView.findViewById<Button>(R.id.btnCancelarPopup)
        val btnConfirmarPopup = popupView.findViewById<Button>(R.id.btnAdicionarPopup)
        val editInicio = popupView.findViewById<EditText>(R.id.editPopupDataInicio)
        val editFechamento = popupView.findViewById<EditText>(R.id.editPopupDataFechamento)

        val fechamentosRef = firestore.collection("cartoes").document(idMetodo).collection("fechamentos")

        fechamentosRef.get().addOnSuccessListener { snapshot ->
            val ultimaVersao = snapshot.documents
                .mapNotNull { doc ->
                    val numero = doc.getString("numero") ?: return@mapNotNull null
                    val partes = numero.split(".")
                    val intParte = partes[0].toIntOrNull() ?: return@mapNotNull null
                    val subParte = if (partes.size > 1) partes[1].toIntOrNull() ?: 0 else 0
                    Triple(intParte, subParte, doc)
                }
                .maxWithOrNull(compareBy({ it.first }, { it.second }))?.third

            ultimaVersao?.let { doc ->
                val inicio = doc.getTimestamp("dataInicio")?.toDate()
                val fechamento = doc.getTimestamp("dataFechamento")?.toDate()

                val calInicio = Calendar.getInstance().apply { time = inicio ?: Date() }
                val calFechamento = Calendar.getInstance().apply { time = fechamento ?: Date() }

                editInicio.setText(formatoData.format(calInicio.time))
                editFechamento.setText(formatoData.format(calFechamento.time))

                editInicio.isEnabled = false
                editInicio.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

                editFechamento.setOnClickListener {
                    DatePickerDialog(this, { _, y, m, d ->
                        calFechamento.set(y, m, d)
                        editFechamento.setText(formatoData.format(calFechamento.time))
                    }, calFechamento.get(Calendar.YEAR), calFechamento.get(Calendar.MONTH), calFechamento.get(Calendar.DAY_OF_MONTH)).show()
                }

                val dialog = AlertDialog.Builder(this)
                    .setView(popupView)
                    .setCancelable(false)
                    .create()

                btnCancelarPopup.setOnClickListener { dialog.dismiss() }

                btnConfirmarPopup.setOnClickListener {
                    val baseNumeroInt = ultimaVersao.getLong("numeroInt")?.toInt() ?: return@setOnClickListener

                    fechamentosRef.whereEqualTo("numeroInt", baseNumeroInt.toLong()).get().addOnSuccessListener { subSnap ->
                        val maioresSubversoes = subSnap.documents.mapNotNull {
                            val n = it.getString("numero")
                            n?.split(".")?.getOrNull(1)?.toIntOrNull() ?: 0
                        }
                        val proximaSubversao = (maioresSubversoes.maxOrNull() ?: 0) + 1
                        val novoNumero = "$baseNumeroInt.$proximaSubversao"

                        val dados = mapOf(
                            "dataInicio" to Timestamp(calInicio.time),
                            "dataFechamento" to Timestamp(calFechamento.time),
                            "criadoEm" to Timestamp.now(),
                            "numero" to novoNumero,
                            "numeroInt" to baseNumeroInt
                        )

                        fechamentosRef.add(dados).addOnSuccessListener {
                            mostrarDialogComRetorno("Sucesso", "Correção salva com sucesso!")
                            dialog.dismiss()
                        }.addOnFailureListener {
                            mostrarDialogSimples("Erro ao salvar correção: ${it.message}")
                        }
                    }
                }

                dialog.show()
            }
        }
    }
}
