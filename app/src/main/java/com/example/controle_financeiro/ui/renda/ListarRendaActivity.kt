package com.example.controle_financeiro.ui.renda

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controle_financeiro.R
import com.example.controle_financeiro.model.RendaSimplificada
import com.example.controle_financeiro.ui.theme.ControlefinanceiroTheme
import com.google.firebase.firestore.FirebaseFirestore

class ListarRendaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = 0
        }

        setContent {
            ControlefinanceiroTheme {
                ListarRendaScreen(onBackClick = { finish() })
            }
        }
    }
}

@Composable
fun ListarRendaScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var rendas by remember { mutableStateOf<List<RendaSimplificada>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("rendas")
            .get()
            .addOnSuccessListener { result ->
                rendas = result.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: return@mapNotNull null
                        val tipo = doc.getString("tipo") ?: ""
                        val fonte = doc.getString("fontePagadora") ?: ""
                        val valor = doc.getDouble("valor") ?: 0.0
                        val data = doc.getString("dataRecebimento") ?: ""
                        val categoriaNome = (doc.get("categoria") as? Map<*, *>)?.get("nome") as? String ?: ""
                        val descricao = doc.getString("descricao")
                        val periodicidade = doc.get("periodicidade") as? Map<*, *>

                        val diaFixo = (periodicidade?.get("diaFixo") as? Long)?.toInt()
                        val mesInicio = (periodicidade?.get("mesInicio") as? Long)?.toInt()
                        val anoInicio = (periodicidade?.get("anoInicio") as? Long)?.toInt()
                        val mesFim = (periodicidade?.get("mesFim") as? Long)?.toInt()
                        val anoFim = (periodicidade?.get("anoFim") as? Long)?.toInt()

                        RendaSimplificada(
                            id = id,
                            tipo = tipo,
                            fontePagadora = fonte,
                            valor = valor,
                            dataRecebimento = data,
                            categoriaNome = categoriaNome,
                            descricao = descricao,
                            diaFixo = diaFixo,
                            mesInicio = mesInicio,
                            anoInicio = anoInicio,
                            mesFim = mesFim,
                            anoFim = anoFim
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopBarListarRenda(onBackClick = onBackClick)
        },
        containerColor = colorResource(id = R.color.background)
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rendas) { renda ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(
                                    Intent(context, EditarRendaActivity::class.java).apply {
                                        putExtra("rendaId", renda.id)
                                    }
                                )
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Tipo: ${renda.tipo}", style = MaterialTheme.typography.titleMedium)
                            Text("Fonte: ${renda.fontePagadora}")
                            Text("Valor: R$ %.2f".format(renda.valor))
                            Text("Data: ${renda.dataRecebimento}")
                            Text("Categoria: ${renda.categoriaNome}")
                            if (!renda.descricao.isNullOrBlank()) {
                                Text("Descrição: ${renda.descricao}")
                            }
                            if (renda.diaFixo != null && renda.mesInicio != null && renda.mesFim != null) {
                                Text("Periodicidade: Dia ${renda.diaFixo}, de ${renda.mesInicio} até ${renda.mesFim}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopBarListarRenda(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(colorResource(id = R.color.primary))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back),
                contentDescription = "Voltar",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Rendas Cadastradas",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
    }
}
