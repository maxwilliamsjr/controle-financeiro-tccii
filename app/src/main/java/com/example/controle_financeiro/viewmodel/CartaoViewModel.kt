package com.example.controle_financeiro.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.controle_financeiro.model.Cartao

class CartaoViewModel : ViewModel() {

    private val _cartoes = MutableLiveData<MutableList<Cartao>>(mutableListOf())
    val cartoes: LiveData<MutableList<Cartao>> = _cartoes

    fun adicionarCartao(cartao: Cartao) {
        _cartoes.value?.add(cartao)
        _cartoes.value = _cartoes.value
    }

    fun removerCartao(id: String) {
        _cartoes.value = _cartoes.value?.filter { it.id != id }?.toMutableList()
    }
}
