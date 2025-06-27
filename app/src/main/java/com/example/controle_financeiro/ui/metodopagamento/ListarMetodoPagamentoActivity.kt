package com.example.controle_financeiro.ui.metodopagamento

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.os.Build
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.controle_financeiro.R
import com.example.controle_financeiro.model.Cartao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ListarMetodoPagamentoActivity : AppCompatActivity() {

    private lateinit var listViewMetodos: ListView
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val metodosFixos = listOf("Pix", "Boleto", "Transferência", "Dinheiro", "Débito automático")
    private val listaMetodosExibicao = mutableListOf<String>()
    private val listaCartoes = mutableListOf<Cartao>()

    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_listar_metodos_pagamento)
        supportActionBar?.title = "Métodos de Pagamento"

        val btnVoltar = findViewById<ImageView>(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            finish()
        }

        listViewMetodos = findViewById(R.id.listViewMetodos)

        adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listaMetodosExibicao) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)

                if (position >= metodosFixos.size) {
                    val indiceCartao = position - metodosFixos.size
                    val cartao = listaCartoes.getOrNull(indiceCartao)
                    if (cartao != null) {
                        val hoje = Calendar.getInstance()
                        val fechamento = cartao.dataFechamento?.toDate()?.let {
                            Calendar.getInstance().apply { time = it }
                        }

                        if (fechamento != null && hoje.after(fechamento)) {
                            view.setBackgroundColor(Color.parseColor("#FFCDD2"))
                        } else {
                            view.setBackgroundColor(Color.WHITE)
                        }
                    } else {
                        view.setBackgroundColor(Color.WHITE)
                    }
                } else {
                    view.setBackgroundColor(Color.WHITE)
                }

                return view
            }
        }

        listViewMetodos.adapter = adapter

        listViewMetodos.setOnItemClickListener { _, _, position, _ ->
            val itemSelecionado = listaMetodosExibicao[position]

            if (metodosFixos.contains(itemSelecionado)) {
                Toast.makeText(this, "'$itemSelecionado' é um método fixo e não pode ser editado", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            val indiceCartao = position - metodosFixos.size
            if (indiceCartao in listaCartoes.indices) {
                val cartaoSelecionado = listaCartoes[indiceCartao]
                val dataFormatada = cartaoSelecionado.dataFechamento?.toDate()?.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                } ?: ""

                val intent = Intent(this, EditarCartaoActivity::class.java).apply {
                    putExtra("id", cartaoSelecionado.id)
                    putExtra("nome", cartaoSelecionado.nome)
                    putExtra("banco", cartaoSelecionado.banco)
                    putExtra("tipo", cartaoSelecionado.tipo)
                    putExtra("bandeira", cartaoSelecionado.bandeira)
                    putExtra("vencimento", dataFormatada)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Erro: cartão não encontrado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        carregarMetodos()
    }

    private fun carregarMetodos() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("cartoes")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { resultado ->
                listaCartoes.clear()
                listaMetodosExibicao.clear()

                listaMetodosExibicao.addAll(metodosFixos)

                for (documento in resultado) {
                    val id = documento.id
                    val nome = documento.getString("nome") ?: continue
                    val banco = documento.getString("banco") ?: ""
                    val tipo = documento.getString("tipo") ?: "Crédito"
                    val bandeira = documento.getString("bandeira") ?: "Visa"
                    val dataFechamento = documento.getTimestamp("dataFechamento")
                    val userIdDoc = documento.getString("userId") ?: ""

                    val cartao = Cartao(
                        id = id,
                        nome = nome,
                        banco = banco,
                        tipo = tipo,
                        bandeira = bandeira,
                        vencimento = "",
                        faturaAtual = 0.0,
                        dataFechamento = dataFechamento,
                        userId = userIdDoc
                    )

                    listaCartoes.add(cartao)
                    val display = "$tipo $banco $nome $bandeira"
                    listaMetodosExibicao.add(display)
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar cartões: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
