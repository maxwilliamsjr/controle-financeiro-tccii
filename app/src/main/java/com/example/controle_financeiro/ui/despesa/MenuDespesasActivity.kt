package com.example.controle_financeiro.ui.despesa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.example.controle_financeiro.R
import android.widget.ImageView

class MenuDespesasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_menu_despesas)
        supportActionBar?.title = "Menu de Despesas"

        val btnVoltar = findViewById<ImageView>(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            finish()
        }

        val btnCadastrar = findViewById<Button>(R.id.btnCadastrarDespesa)
        val btnListar = findViewById<Button>(R.id.btnListarDespesas)

        btnCadastrar.setOnClickListener {
            startActivity(Intent(this, DespesaActivity::class.java))
        }

        btnListar.setOnClickListener {
            startActivity(Intent(this, ListarDespesasActivity::class.java))
        }
    }
}


