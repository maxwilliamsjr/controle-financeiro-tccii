package com.example.controle_financeiro

import android.os.Bundle
import android.text.InputType
import android.widget.*
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class PerfilActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var editNome: EditText
    private lateinit var editSobrenome: EditText
    private lateinit var editNascimento: EditText
    private lateinit var editCidade: EditText
    private lateinit var editEstado: EditText
    private lateinit var editEmail: EditText
    private lateinit var spinnerSexo: Spinner
    private lateinit var btnSalvar: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnExcluirConta: Button
    private lateinit var btnVoltar: ImageView

    private lateinit var editSenhaAtual: EditText
    private lateinit var editNovaSenha: EditText
    private lateinit var editConfirmarSenha: EditText

    private lateinit var imgToggleSenhaAtual: ImageView
    private lateinit var imgToggleNovaSenha: ImageView
    private lateinit var imgToggleConfirmarSenha: ImageView

    private lateinit var opcoesSexo: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_perfil)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        editNome = findViewById(R.id.editNome)
        editSobrenome = findViewById(R.id.editSobrenome)
        editNascimento = findViewById(R.id.editNascimento)
        editCidade = findViewById(R.id.editCidade)
        editEstado = findViewById(R.id.editEstado)
        editEmail = findViewById(R.id.editEmail)
        spinnerSexo = findViewById(R.id.spinnerSexo)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnCancelar = findViewById(R.id.btnCancelar)
        btnExcluirConta = findViewById(R.id.btnExcluirConta)
        btnVoltar = findViewById(R.id.btnVoltar)

        editSenhaAtual = findViewById(R.id.editSenhaAtual)
        editNovaSenha = findViewById(R.id.editNovaSenha)
        editConfirmarSenha = findViewById(R.id.editConfirmarSenha)

        imgToggleSenhaAtual = findViewById(R.id.imgToggleSenhaAtual)
        imgToggleNovaSenha = findViewById(R.id.imgToggleNovaSenha)
        imgToggleConfirmarSenha = findViewById(R.id.imgToggleConfirmarSenha)

        opcoesSexo = resources.getStringArray(R.array.opcoesSexo)
        val adapterSexo = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcoesSexo)
        adapterSexo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSexo.adapter = adapterSexo

        editEmail.isEnabled = false

        carregarDadosUsuario()
        configurarToggleSenhas()

        btnSalvar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirmar alterações")
                .setMessage("Deseja salvar as alterações?")
                .setPositiveButton("Sim") { _, _ -> salvarAlteracoes() }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnCancelar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cancelar alterações")
                .setMessage("Deseja descartar as alterações?")
                .setPositiveButton("Sim") { _, _ -> carregarDadosUsuario() }
                .setNegativeButton("Não", null)
                .show()
        }

        btnExcluirConta.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Excluir conta")
                .setMessage("Tem certeza que deseja excluir sua conta? Essa ação não poderá ser desfeita.")
                .setPositiveButton("Sim") { _, _ -> excluirConta() }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnVoltar.setOnClickListener { finish() }
    }

    private fun configurarToggleSenhas() {
        fun togglePasswordVisibility(editText: EditText, imageView: ImageView) {
            var senhaVisivel = false
            imageView.setOnClickListener {
                senhaVisivel = !senhaVisivel
                if (senhaVisivel) {
                    editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    imageView.setImageResource(R.drawable.ic_eye_off)
                } else {
                    editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    imageView.setImageResource(R.drawable.ic_eye)
                }
                editText.setSelection(editText.text.length)
            }
        }
        togglePasswordVisibility(editSenhaAtual, imgToggleSenhaAtual)
        togglePasswordVisibility(editNovaSenha, imgToggleNovaSenha)
        togglePasswordVisibility(editConfirmarSenha, imgToggleConfirmarSenha)
    }

    private fun carregarDadosUsuario() {
        val usuarioAtual = auth.currentUser
        if (usuarioAtual == null) {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uid = usuarioAtual.uid
        firestore.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    editNome.setText(doc.getString("nome") ?: "")
                    editSobrenome.setText(doc.getString("sobrenome") ?: "")
                    editNascimento.setText(doc.getString("nascimento") ?: "")
                    editCidade.setText(doc.getString("cidade") ?: "")
                    editEstado.setText(doc.getString("estado") ?: "")
                    editEmail.setText(doc.getString("email") ?: "")

                    val sexo = doc.getString("sexo") ?: opcoesSexo.last()
                    val pos = opcoesSexo.indexOf(sexo).takeIf { it >= 0 } ?: opcoesSexo.size - 1
                    spinnerSexo.setSelection(pos)

                    // Limpar campos de senha ao carregar dados
                    editSenhaAtual.text.clear()
                    editNovaSenha.text.clear()
                    editConfirmarSenha.text.clear()
                } else {
                    Toast.makeText(this, "Dados do usuário não encontrados", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar dados: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun salvarAlteracoes() {
        val usuarioAtual = auth.currentUser
        if (usuarioAtual == null) {
            Toast.makeText(this, "Usuário não logado", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = usuarioAtual.uid

        val nome = editNome.text.toString().trim()
        val sobrenome = editSobrenome.text.toString().trim()
        val nascimento = editNascimento.text.toString().trim()
        val cidade = editCidade.text.toString().trim()
        val estado = editEstado.text.toString().trim()
        val sexo = spinnerSexo.selectedItem.toString()

        val senhaAtual = editSenhaAtual.text.toString()
        val novaSenha = editNovaSenha.text.toString()
        val confirmarSenha = editConfirmarSenha.text.toString()

        val estaAlterandoSenha = senhaAtual.isNotEmpty() || novaSenha.isNotEmpty() || confirmarSenha.isNotEmpty()

        if (estaAlterandoSenha) {

            if (senhaAtual.isBlank() || novaSenha.isBlank() || confirmarSenha.isBlank()) {
                Toast.makeText(this, "Preencha todos os campos de senha para alterar a senha", Toast.LENGTH_SHORT).show()
                return
            }
            if (!isSenhaValida(novaSenha)) {
                Toast.makeText(this, "Nova senha inválida. Deve ter ao menos 8 caracteres, incluindo número e caractere especial.", Toast.LENGTH_LONG).show()
                return
            }
            if (novaSenha != confirmarSenha) {
                Toast.makeText(this, "Nova senha e confirmação não coincidem.", Toast.LENGTH_SHORT).show()
                return
            }

            val credential = EmailAuthProvider.getCredential(usuarioAtual.email ?: "", senhaAtual)
            usuarioAtual.reauthenticate(credential).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val usuarioAtualizado = hashMapOf(
                        "nome" to nome,
                        "sobrenome" to sobrenome,
                        "nascimento" to nascimento,
                        "cidade" to cidade,
                        "estado" to estado,
                        "sexo" to sexo
                    )
                    firestore.collection("usuarios").document(uid)
                        .update(usuarioAtualizado as Map<String, Any>)
                        .addOnSuccessListener {
                            usuarioAtual.updatePassword(novaSenha).addOnCompleteListener { updatePassTask ->
                                if (updatePassTask.isSuccessful) {
                                    Toast.makeText(this, "Dados e senha atualizados com sucesso", Toast.LENGTH_LONG).show()
                                    // Limpa campos de senha
                                    editSenhaAtual.text.clear()
                                    editNovaSenha.text.clear()
                                    editConfirmarSenha.text.clear()
                                } else {
                                    Toast.makeText(this, "Erro ao atualizar senha: ${updatePassTask.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Erro ao salvar dados: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Senha atual incorreta.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val usuarioAtualizado = hashMapOf(
                "nome" to nome,
                "sobrenome" to sobrenome,
                "nascimento" to nascimento,
                "cidade" to cidade,
                "estado" to estado,
                "sexo" to sexo
            )
            firestore.collection("usuarios").document(uid)
                .update(usuarioAtualizado as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Dados salvos com sucesso", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao salvar: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun isSenhaValida(senha: String): Boolean {
        if (senha.length < 8) return false
        if (!senha.any { it.isDigit() }) return false
        if (!senha.any { !it.isLetterOrDigit() }) return false
        return true
    }

    private fun excluirConta() {
        val user = auth.currentUser ?: return

        firestore.collection("usuarios").document(user.uid).delete().addOnCompleteListener {
            user.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Conta excluída", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Erro ao excluir conta", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
