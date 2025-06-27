package com.example.controle_financeiro

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editSenha = findViewById<EditText>(R.id.editSenha)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnCadastrar = findViewById<Button>(R.id.btnCadastrar)
        val imgToggleSenha = findViewById<ImageView>(R.id.imgToggleSenha)

        var senhaVisivel = false
        imgToggleSenha.setOnClickListener {
            senhaVisivel = !senhaVisivel
            if (senhaVisivel) {
                editSenha.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                imgToggleSenha.setImageResource(R.drawable.ic_eye_off)
            } else {
                editSenha.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                imgToggleSenha.setImageResource(R.drawable.ic_eye)
            }
            editSenha.setSelection(editSenha.text.length)
        }

        btnLogin.setOnClickListener {
            val email = editEmail.text.toString().trim()
            val senha = editSenha.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editEmail.error = "Email inválido"
                editEmail.requestFocus()
                return@setOnClickListener
            }

            if (senha.length < 6) {
                editSenha.error = "Senha deve ter ao menos 8 caracteres"
                editSenha.requestFocus()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            if (user.isEmailVerified) {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Por favor, confirme seu e-mail antes de continuar.", Toast.LENGTH_LONG).show()
                                auth.signOut()
                            }
                        }
                    } else {
                        val exception = task.exception
                        when {
                            exception is FirebaseAuthException -> {
                                when (exception.errorCode) {
                                    "ERROR_USER_NOT_FOUND" -> {
                                        Toast.makeText(this, "Usuário não encontrado. Verifique o email cadastrado.", Toast.LENGTH_LONG).show()
                                        editEmail.requestFocus()
                                    }
                                    "ERROR_WRONG_PASSWORD" -> {
                                        Toast.makeText(this, "Senha incorreta. Tente novamente.", Toast.LENGTH_LONG).show()
                                        editSenha.requestFocus()
                                    }
                                    "ERROR_INVALID_EMAIL" -> {
                                        Toast.makeText(this, "Formato de e-mail inválido.", Toast.LENGTH_LONG).show()
                                        editEmail.requestFocus()
                                    }
                                    "ERROR_USER_DISABLED" -> {
                                        Toast.makeText(this, "Usuário desativado. Contate o suporte.", Toast.LENGTH_LONG).show()
                                    }
                                    else -> {
                                        Toast.makeText(this, "Erro ao fazer login: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            else -> {
                                Toast.makeText(this, "Erro ao fazer login: ${exception?.localizedMessage ?: "Tente novamente mais tarde."}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
        }

        btnCadastrar.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }
}
