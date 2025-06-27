package com.example.controle_financeiro.ui.renda

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import android.os.Build
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controle_financeiro.R
import com.example.controle_financeiro.model.Categoria
import com.example.controle_financeiro.model.Periodicidade
import com.example.controle_financeiro.model.Renda
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class RendaActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContent {
            RendaScreen(onBack = { finish() })
        }
    }

    @Composable
    fun RendaScreen(onBack: () -> Unit) {
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

        val calendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun showDatePicker() {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    dataRecebimento = dateFormatter.format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        fun calcularMesesPeriodicidade(): Int? {
            val mi = mesInicio.toIntOrNull()
            val ai = anoInicio.toIntOrNull()
            val mf = mesFim.toIntOrNull()
            val af = anoFim.toIntOrNull()
            if (mi != null && ai != null && mf != null && af != null) {
                val meses = (af - ai) * 12 + (mf - mi) + 1
                return if (meses >= 0) meses else null
            }
            return null
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.background))
        ) {
            TopBar(title = "Cadastrar Renda", onBack = onBack)

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
                    onValueChange = { input ->
                        val cleanInput = input.replace("[^\\d]".toRegex(), "")
                        valor = if (cleanInput.isNotEmpty()) {
                            val parsed = cleanInput.toDouble() / 100
                            String.format("%,.2f", parsed)
                        } else ""
                    },
                    label = { Text("Valor") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = interactionSource, indication = null) {
                            showDatePicker()
                        }
                ) {
                    OutlinedTextField(
                        value = dataRecebimento,
                        onValueChange = {},
                        label = { Text("Data de Recebimento") },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                Text("Periodicidade", style = MaterialTheme.typography.titleMedium)

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
                    label = { Text("Mês de Início (1-12)") },
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
                    label = { Text("Mês de Fim (1-12)") },
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

                val mesesPeriodicidade = calcularMesesPeriodicidade()
                if (mesesPeriodicidade != null) {
                    Text("Periodicidade: $mesesPeriodicidade meses", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        "Informe mês e ano de início e fim válidos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        if (tipo.isBlank() || fontePagadora.isBlank() || valor.isBlank() || dataRecebimento.isBlank() || categoria.isBlank()) {
                            Toast.makeText(context, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val valorNumerico = valor.replace(".", "").replace(",", ".").toDoubleOrNull()
                        if (valorNumerico == null) {
                            Toast.makeText(context, "Valor inválido", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val diaFixoInt = diaFixo.toIntOrNull() ?: 1

                        val renda = Renda(
                            id = UUID.randomUUID().toString(),
                            tipo = tipo,
                            fontePagadora = fontePagadora,
                            valor = valorNumerico,
                            dataRecebimento = dataRecebimento,
                            categoria = Categoria(id = "", nome = categoria, descricao = ""),
                            descricao = descricao,
                            periodicidade = Periodicidade(
                                diaFixo = diaFixoInt,
                                mesInicio = mesInicio.toIntOrNull(),
                                anoInicio = anoInicio.toIntOrNull(),
                                mesFim = mesFim.toIntOrNull(),
                                anoFim = anoFim.toIntOrNull()
                            )
                        )

                        firestore.collection("rendas").document(renda.id).set(renda)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Renda cadastrada com sucesso", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Erro ao cadastrar", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary),
                        contentColor = Color.White
                    )
                ) {
                    Text("Salvar")
                }
            }
        }
    }

    @Composable
    fun TopBar(title: String, onBack: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colorResource(id = R.color.primary))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "Voltar",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
    }
}
