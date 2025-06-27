package com.example.controle_financeiro.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SaldoViewModel : ViewModel() {

    private val _saldo = MutableLiveData<Double>()
    val saldo: LiveData<Double> = _saldo

    init {
        _saldo.value = 0.0 // saldo inicial
    }

    fun adicionarValor(valor: Double) {
        _saldo.value = (_saldo.value ?: 0.0) + valor
    }

    fun subtrairValor(valor: Double) {
        _saldo.value = (_saldo.value ?: 0.0) - valor
    }

    fun definirSaldo(valor: Double) {
        _saldo.value = valor
    }}
