package com.example.controle_financeiro.ui.renda

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controle_financeiro.R
import com.example.controle_financeiro.model.Categoria
import com.example.controle_financeiro.model.Periodicidade
import com.example.controle_financeiro.model.Renda
import com.example.controle_financeiro.ui.theme.ControlefinanceiroTheme
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EditarRendaActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        val rendaId = intent.getStringExtra("rendaId") ?: return finish()

        setContent {
            ControlefinanceiroTheme {
                EditarRendaScreen(rendaId = rendaId, onBack = { finish() })
            }
        }
    }

    private fun formatarValor(valor: String): String {
        val clean = valor.filter { it.isDigit() }
        if (clean.isEmpty()) return ""
        val padded = clean.padStart(3, '0')
        val inteiro = padded.dropLast(2)
        val decimal = padded.takeLast(2)
        val inteiroFormatado = inteiro.reversed().chunked(3).joinToString(".").reversed()
        return "$inteiroFormatado,$decimal"
    }

    @Composable
    fun EditarRendaScreen(rendaId: String, onBack: () -> Unit) {
        val context = LocalContext.current
        val scrollState = rememberScrollState()

        var tipo by remember { mutableStateOf("") }
        var fontePagadora by remember { mutableStateOf("") }
        var valor by remember { mutableStateOf("") }
        var dataRecebimento by remember { mutableStateOf("") }
        var categoria by remember { mutableStateOf("") }
        var descricao by remember { mutableStateOf("") }
        var diaFixo by remember { mutableStateOf("") }
        var mesInicio by remember { mutableStateOf("") }
        var anoInicio by remember { mutableStateOf("") }
        var mesFim by remember { mutableStateOf("") }
        var anoFim by remember { mutableStateOf("") }

        var isLoading by remember { mutableStateOf(true) }

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val openDatePicker = {
            val date = try {
                dateFormat.parse(dataRecebimento) ?: Date()
            } catch (e: Exception) {
                Date()
            }
            calendar.time = date
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(context, { _, y, m, d ->
                calendar.set(y, m, d)
                dataRecebimento = dateFormat.format(calendar.time)
            }, year, month, day).show()
        }

        LaunchedEffect(rendaId) {
            firestore.collection("rendas").document(rendaId).get()
                .addOnSuccessListener { doc ->
                    val renda = doc.toObject(Renda::class.java)
                    if (renda != null) {
                        tipo = renda.tipo
                        fontePagadora = renda.fontePagadora
                        valor = formatarValor(renda.valor.toString())
                        dataRecebimento = renda.dataRecebimento
                        categoria = renda.categoria.nome
                        descricao = renda.descricao ?: ""
                        diaFixo = renda.periodicidade?.diaFixo?.toString() ?: ""
                        mesInicio = renda.periodicidade?.mesInicio?.toString() ?: ""
                        anoInicio = renda.periodicidade?.anoInicio?.toString() ?: ""
                        mesFim = renda.periodicidade?.mesFim?.toString() ?: ""
                        anoFim = renda.periodicidade?.anoFim?.toString() ?: ""
                    } else {
                        Toast.makeText(context, "Renda não encontrada", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Erro ao carregar renda", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TopBarEditar(title = "Editar Renda", onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = tipo,
                    onValueChange = { tipo = it },
                    label = { Text("Tipo (Ex: Salário, Bônus...)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fontePagadora,
                    onValueChange = { fontePagadora = it },
                    label = { Text("Fonte Pagadora") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = valor,
                    onValueChange = { valor = formatarValor(it) },
                    label = { Text("Valor") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dataRecebimento,
                    onValueChange = {},
                    label = { Text("Data de Recebimento") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openDatePicker() },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { openDatePicker() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Selecionar Data")
                        }
                    }
                )

                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoria") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()
                Text("Periodicidade", style = MaterialTheme.typography.bodyLarge)

                OutlinedTextField(
                    value = diaFixo,
                    onValueChange = { diaFixo = it },
                    label = { Text("Dia Fixo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mesInicio,
                    onValueChange = { mesInicio = it },
                    label = { Text("Mês de Início") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = anoInicio,
                    onValueChange = { anoInicio = it },
                    label = { Text("Ano de Início") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mesFim,
                    onValueChange = { mesFim = it },
                    label = { Text("Mês de Fim") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = anoFim,
                    onValueChange = { anoFim = it },
                    label = { Text("Ano de Fim") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (tipo.isBlank() || fontePagadora.isBlank() || valor.isBlank() || dataRecebimento.isBlank() || categoria.isBlank()) {
                            Toast.makeText(context, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val valorDouble = valor.replace(".", "").replace(",", ".").toDoubleOrNull()
                        if (valorDouble == null) {
                            Toast.makeText(context, "Valor inválido", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val rendaEditada = Renda(
                            id = rendaId,
                            tipo = tipo,
                            fontePagadora = fontePagadora,
                            valor = valorDouble,
                            dataRecebimento = dataRecebimento,
                            categoria = Categoria(id = "", nome = categoria, descricao = ""),
                            descricao = descricao,
                            periodicidade = Periodicidade(
                                diaFixo = diaFixo.toIntOrNull() ?: 1,
                                mesInicio = mesInicio.toIntOrNull(),
                                anoInicio = anoInicio.toIntOrNull(),
                                mesFim = mesFim.toIntOrNull(),
                                anoFim = anoFim.toIntOrNull()
                            )
                        )

                        firestore.collection("rendas").document(rendaId).set(rendaEditada)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Renda atualizada com sucesso", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Erro ao atualizar", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF115F22),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Salvar Alterações", fontSize = 16.sp)
                }
            }
        }
    }

    @Composable
    fun TopBarEditar(title: String, onBack: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF115F22))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "Voltar",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() },
                colorFilter = ColorFilter.tint(Color.White)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier
                    .weight(6f)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
