package com.example.doafacilnovo

data class Solicitacoes(
    val nome: String,
    val tituloDoacao : String,
    val solicitacaoUid: String
){
    override fun toString(): String {
        return nome
    }
}
