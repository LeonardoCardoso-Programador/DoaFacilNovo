package com.example.doafacilnovo

data class InformacoesDoacao(
    val titulo: String,
    val nome: String,
    val descricao: String,
    val longitude: Double,
    val latitude: Double,
    val data: String?,
    val disponibilidade: String,
    val primeiraImagem: String?, // Apenas a primeira imagem em Bitmap
)
{
    override fun toString(): String {
        return titulo // Isto é importante para a ListView exibir o título
    }
}

