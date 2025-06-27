package com.example.controle_financeiro.model

data class Periodicidade(
    val diaFixo: Int = 1,
    val mesInicio: Int? = null,
    val anoInicio: Int? = null,
    val mesFim: Int? = null,
    val anoFim: Int? = null
)
