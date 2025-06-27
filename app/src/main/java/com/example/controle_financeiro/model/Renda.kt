package com.example.controle_financeiro.model

data class Renda(
    val id: String = "",
    var tipo: String = "",
    var fontePagadora: String = "",
    var valor: Double = 0.0,
    var dataRecebimento: String = "",
    var categoria: Categoria = Categoria(),
    var descricao: String? = null,
    var periodicidade: Periodicidade? = null
)
