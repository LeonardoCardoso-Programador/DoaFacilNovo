package com.example.doafacilnovo

data class Usuario(
    val nome: String,
    val contagemDoacoes : String,
    val userUid: String
){
    override fun toString(): String {
        return nome // Isto é importante para a ListView exibir o título
    }
}
