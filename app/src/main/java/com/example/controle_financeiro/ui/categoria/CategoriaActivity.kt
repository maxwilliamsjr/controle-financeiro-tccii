package com.example.controle_financeiro.ui.categoria

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.controle_financeiro.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CategoriaActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var editNomeCategoria: EditText
    private lateinit var btnSalvarCategoria: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_categoria)
        supportActionBar?.title = "Cadastrar Categoria"

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        editNomeCategoria = findViewById(R.id.editNomeCategoria)
        btnSalvarCategoria = findViewById(R.id.btnSalvarCategoria)

        val btnVoltar = findViewById<ImageView>(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            finish()
        }

        btnSalvarCategoria.setOnClickListener {
            val nome = editNomeCategoria.text.toString().trim()
            val userId = auth.currentUser?.uid

            if (nome.isEmpty()) {
                Toast.makeText(this, "Informe o nome da categoria", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId == null) {
                Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val id = UUID.randomUUID().toString()
            val categoria = hashMapOf(
                "id" to id,
                "nomeCategoria" to nome,
                "userId" to userId
            )

            firestore.collection("categorias")
                .document(id)
                .set(categoria)
                .addOnSuccessListener {
                    Toast.makeText(this, "Categoria salva com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao salvar categoria: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
