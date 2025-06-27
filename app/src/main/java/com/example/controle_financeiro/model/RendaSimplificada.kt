package com.example.controle_financeiro.model

data class RendaSimplificada(
    val id: String = "",
    val tipo: String = "",
    val fontePagadora: String = "",
    val valor: Double = 0.0,
    val dataRecebimento: String = "",
    val categoriaNome: String = "",
    val descricao: String? = null,
    val diaFixo: Int? = null,
    val mesInicio: Int? = null,
    val anoInicio: Int? = null,
    val mesFim: Int? = null,
    val anoFim: Int? = null
)
