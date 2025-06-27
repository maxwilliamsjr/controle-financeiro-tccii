package com.example.controle_financeiro.ui.planejamento

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.controle_financeiro.R

class PlanejamentoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planejamento)
        supportActionBar?.title = "Planejamento Mensal"
    }
}
