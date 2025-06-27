package com.example.controle_financeiro

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.widget.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CadastroActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var editNome: EditText
    private lateinit var editSobrenome: EditText
    private lateinit var editNascimento: EditText
    private lateinit var editCidade: EditText
    private lateinit var editEstado: EditText
    private lateinit var editEmail: EditText
    private lateinit var editSenha: EditText
    private lateinit var editConfirmarSenha: EditText
    private lateinit var spinnerSexo: Spinner
    private lateinit var btnCadastrar: Button
    private lateinit var btnCancelar: Button
    private lateinit var tvSenhaCriterios: TextView
    private lateinit var imgToggleSenha: ImageView
    private lateinit var imgToggleConfirmarSenha: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContentView(R.layout.activity_cadastro)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Cadastro"

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        editNome = findViewById(R.id.editNome)
        editSobrenome = findViewById(R.id.editSobrenome)
        editNascimento = findViewById(R.id.editNascimento)
        editCidade = findViewById(R.id.editCidade)
        editEstado = findViewById(R.id.editEstado)
        editEmail = findViewById(R.id.editEmail)
        editSenha = findViewById(R.id.editSenha)
        editConfirmarSenha = findViewById(R.id.editConfirmarSenha)
        spinnerSexo = findViewById(R.id.spinnerSexo)
        btnCadastrar = findViewById(R.id.btnCadastrar)
        btnCancelar = findViewById(R.id.btnCancelar)
        tvSenhaCriterios = findViewById(R.id.tvSenhaCriterios)
        imgToggleSenha = findViewById(R.id.imgToggleSenha)
        imgToggleConfirmarSenha = findViewById(R.id.imgToggleConfirmarSenha)

        editNome.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        editSobrenome.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        editCidade.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        editEstado.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        editEmail.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        // Máscara para nascimento
        editNascimento.inputType = InputType.TYPE_CLASS_NUMBER
        editNascimento.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val mask = "##/##/####"

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) {
                    isUpdating = false
                    return
                }

                val str = s.toString().replace("[^\\d]".toRegex(), "")
                var formatted = ""
                var i = 0
                for (m in mask.toCharArray()) {
                    if (m != '#') {
                        formatted += m
                        continue
                    }
                    if (i >= str.length) break
                    formatted += str[i]
                    i++
                }

                isUpdating = true
                editNascimento.setText(formatted)
                editNascimento.setSelection(formatted.length)
            }
        })

        val opcoesSexo = listOf("Masculino", "Feminino", "Prefiro não informar")
        val adapterSexo = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcoesSexo)
        adapterSexo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSexo.adapter = adapterSexo

        var senhaVisivel = false
        imgToggleSenha.setOnClickListener {
            senhaVisivel = !senhaVisivel
            editSenha.inputType = if (senhaVisivel)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editSenha.setSelection(editSenha.text.length)
            imgToggleSenha.setImageResource(if (senhaVisivel) R.drawable.ic_eye_off else R.drawable.ic_eye)
        }

        var confirmarVisivel = false
        imgToggleConfirmarSenha.setOnClickListener {
            confirmarVisivel = !confirmarVisivel
            editConfirmarSenha.inputType = if (confirmarVisivel)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editConfirmarSenha.setSelection(editConfirmarSenha.text.length)
            imgToggleConfirmarSenha.setImageResource(if (confirmarVisivel) R.drawable.ic_eye_off else R.drawable.ic_eye)
        }

        btnCadastrar.setOnClickListener {
            val nome = editNome.text.toString().trim()
            val sobrenome = editSobrenome.text.toString().trim()
            val nascimento = editNascimento.text.toString().trim()
            val cidade = editCidade.text.toString().trim()
            val estado = editEstado.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val senha = editSenha.text.toString()
            val confirmarSenha = editConfirmarSenha.text.toString()
            val sexo = spinnerSexo.selectedItem.toString()

            if (
                nome.isBlank() || sobrenome.isBlank() || nascimento.isBlank() ||
                cidade.isBlank() || estado.isBlank() || email.isBlank() || senha.isBlank() || confirmarSenha.isBlank()
            ) {
                Toast.makeText(this, "Preencha todos os campos corretamente", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editEmail.error = "Email inválido"
                editEmail.requestFocus()
                return@setOnClickListener
            }

            if (!isSenhaValida(senha)) {
                editSenha.error = "Senha deve ter ao menos 8 caracteres, 1 número e 1 caractere especial"
                editSenha.requestFocus()
                return@setOnClickListener
            }

            if (senha != confirmarSenha) {
                editConfirmarSenha.error = "As senhas não coincidem"
                editConfirmarSenha.requestFocus()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, senha)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val uid = user.uid
                            val userData = hashMapOf(
                                "nome" to nome,
                                "sobrenome" to sobrenome,
                                "nascimento" to nascimento,
                                "cidade" to cidade,
                                "estado" to estado,
                                "email" to email,
                                "sexo" to sexo
                            )
                            firestore.collection("usuarios").document(uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    user.sendEmailVerification()
                                        .addOnCompleteListener { emailTask ->
                                            if (emailTask.isSuccessful) {
                                                Toast.makeText(this, "Verificação enviada para seu e-mail. Confirme para acessar o app.", Toast.LENGTH_LONG).show()
                                                auth.signOut()
                                                finish()
                                            } else {
                                                Toast.makeText(this, "Erro ao enviar verificação: ${emailTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Erro ao salvar dados: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Erro ao cadastrar: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun isSenhaValida(senha: String): Boolean {
        return senha.length >= 8 && senha.any { it.isDigit() } && senha.any { !it.isLetterOrDigit() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
