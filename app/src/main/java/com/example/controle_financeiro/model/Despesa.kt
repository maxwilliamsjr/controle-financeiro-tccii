package com.example.controle_financeiro.model

import com.google.firebase.Timestamp
import java.io.Serializable

data class Despesa(
    val id: String = "",
    val nome: String = "",
    val descricao: String = "",
    val valor: Double = 0.0,
    val data: Timestamp? = null,
    val categoria: String = "",
    val metodoPagamento: String = "",
    val userId: String = "",
    val destino: String = "",
    val cartaoId: String? = null,
    val timestampCriacao: Timestamp? = null
) : Serializable
