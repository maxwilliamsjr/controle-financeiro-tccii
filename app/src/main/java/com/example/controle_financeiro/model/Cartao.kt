package com.example.controle_financeiro.model

import com.google.firebase.Timestamp

data class Cartao(
    val id: String = "",
    var nome: String = "",
    var banco: String = "",
    var tipo: String = "",
    var vencimento: String = "",
    var bandeira: String = "",
    var faturaAtual: Double = 0.0,
    var dataFechamento: Timestamp? = null,
    var userId: String = ""
)
