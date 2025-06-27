package com.example.controle_financeiro.ui.categoria

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.os.Build
import android.widget.Toast
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.controle_financeiro.R
import com.example.controle_financeiro.model.Categoria
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ListarCategoriaActivity : AppCompatActivity() {

    private lateinit var listViewCategorias: ListView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var listaCategorias: MutableList<Categoria>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_listar_categorias)
        supportActionBar?.title = "Lista de Categorias"

        listViewCategorias = findViewById(R.id.listViewCategorias)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        listaCategorias = mutableListOf()

        carregarCategorias()

        val btnVoltar = findViewById<ImageView>(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            finish()
        }
    }

    private fun carregarCategorias() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("categorias")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                listaCategorias.clear()
                for (document in result) {
                    val id = document.id
                    val nome = document.getString("nomeCategoria") ?: ""
                    val categoria = Categoria(id = id, nome = nome)
                    listaCategorias.add(categoria)
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    listaCategorias.map { it.nome }
                )
                listViewCategorias.adapter = adapter

                listViewCategorias.setOnItemClickListener { _: AdapterView<*>, _, position: Int, _ ->
                    val categoriaSelecionada = listaCategorias[position]
                    val intent = Intent(this, EditarCategoriaActivity::class.java).apply {
                        putExtra("id", categoriaSelecionada.id)
                        putExtra("nomeCategoria", categoriaSelecionada.nome)
                    }
                    startActivity(intent)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
