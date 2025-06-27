package com.example.controle_financeiro.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.controle_financeiro.model.Despesa

class DespesaViewModel : ViewModel() {

    private val _listaDespesas = MutableLiveData<MutableList<Despesa>>(mutableListOf())
    val listaDespesas: LiveData<MutableList<Despesa>> = _listaDespesas

    fun adicionarDespesa(despesa: Despesa) {
        _listaDespesas.value?.add(despesa)
        _listaDespesas.value = _listaDespesas.value // Força atualização
    }

    fun removerDespesa(id: String) {
        _listaDespesas.value = _listaDespesas.value?.filter { it.id != id }?.toMutableList()
    }

    fun limparDespesas() {
        _listaDespesas.value = mutableListOf()
    }
}
