package com.example.controle_financeiro.ui.categoria

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.controle_financeiro.R

class MenuCategoriaActivity : AppCompatActivity() {

    private lateinit var btnCadastrarCategoria: Button
    private lateinit var btnListarCategorias: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_menu_categoria)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        btnCadastrarCategoria = findViewById(R.id.btnCadastrarCategoria)
        btnListarCategorias = findViewById(R.id.btnListarCategorias)

        btnCadastrarCategoria.setOnClickListener {
            startActivity(Intent(this, CategoriaActivity::class.java))
        }

        btnListarCategorias.setOnClickListener {
            startActivity(Intent(this, ListarCategoriaActivity::class.java))
        }
    }
}
