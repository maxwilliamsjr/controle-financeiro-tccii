package com.example.controle_financeiro.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.controle_financeiro.model.Renda

class RendaViewModel : ViewModel() {

    private val _listaRendas = MutableLiveData<MutableList<Renda>>(mutableListOf())
    val listaRendas: LiveData<MutableList<Renda>> = _listaRendas

    fun adicionarRenda(renda: Renda) {
        _listaRendas.value?.add(renda)
        _listaRendas.value = _listaRendas.value
    }

    fun removerRenda(id: String) {
        _listaRendas.value = _listaRendas.value?.filter { it.id != id }?.toMutableList()
    }

    fun limparRendas() {
        _listaRendas.value = mutableListOf()
    }
}
