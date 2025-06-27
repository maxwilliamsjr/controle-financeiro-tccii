package com.example.controle_financeiro.ui.metodopagamento

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.os.Build
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.controle_financeiro.R

class MenuMetodoPagamentoActivity : AppCompatActivity() {

    private lateinit var btnListarMetodos: Button
    private lateinit var btnCadastrarCartao: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_menu_metodo_pagamento)
        supportActionBar?.title = "Menu Método de Pagamento"

        btnListarMetodos = findViewById(R.id.btnListarMetodos)
        btnCadastrarCartao = findViewById(R.id.btnCadastrarCartao)

        btnListarMetodos.setOnClickListener {
            startActivity(Intent(this, ListarMetodoPagamentoActivity::class.java))
        }

        btnCadastrarCartao.setOnClickListener {
            startActivity(Intent(this, CadastrarCartaoActivity::class.java))
        }

        checarMensagemDaIntent()

        val btnVoltar = findViewById<ImageView>(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            finish()
        }
    }



    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checarMensagemDaIntent()
    }

    private fun checarMensagemDaIntent() {
        val nomeCompleto = intent.getStringExtra("NOME_COMPLETO_CARTAO") ?: return
        val faturaFechada = intent.getBooleanExtra("CARTAO_FATURA_FECHADA", false)
        val cartaoSalvo = intent.getBooleanExtra("CARTAO_SALVO", false)

        val mensagem = StringBuilder()

        if (mensagem.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Aviso")
                .setMessage(mensagem.toString())
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        intent.removeExtra("NOME_COMPLETO_CARTAO")
        intent.removeExtra("CARTAO_FATURA_FECHADA")
        intent.removeExtra("CARTAO_SALVO")
    }
}
