package com.example.controle_financeiro.ui.despesa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.controle_financeiro.R

class ListarDespesasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        showPopupSelecionarTipo()
    }

    private fun showPopupSelecionarTipo() {
        val view = layoutInflater.inflate(R.layout.popup_selecionar_tipo_despesa, null)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        val btnExtrato = view.findViewById<Button>(R.id.btnExtrato)
        val btnFatura = view.findViewById<Button>(R.id.btnFatura)

        btnExtrato.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, ExtratoDespesasActivity::class.java))
        }

        btnFatura.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, FaturaDespesasActivity::class.java))
        }

        dialog.show()
    }
}
