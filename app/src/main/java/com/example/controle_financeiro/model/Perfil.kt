package com.example.controle_financeiro.model

data class Perfil(
    val idUsuario: String = "",
    var nome: String = "",
    var sobrenome: String = "",
    var nascimento: String = "",
    var cidade: String = "",
    var estado: String = "",
    var sexo: String = "",
    var email: String = "",
    var preferencias: Map<String, Any> = emptyMap()
)
