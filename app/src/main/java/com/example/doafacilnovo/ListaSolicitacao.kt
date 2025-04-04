package com.example.doafacilnovo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ListaSolicitacao(
    private val context: Context,
    private val solicitacoesList: List<Solicitacoes>,
    private val onItemClick: (Solicitacoes) -> Unit // Callback para clique no item
) : RecyclerView.Adapter<ListaSolicitacao.SolicitacaoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SolicitacaoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.lista_solcitacoes, parent, false)
        return SolicitacaoViewHolder(view)
    }

    override fun onBindViewHolder(holder: SolicitacaoViewHolder, position: Int) {
        val solicitacao = solicitacoesList[position]

        // Preenchendo os dados do item na lista
        holder.userName.text = solicitacao.nome
        holder.tituloDoacao.text = solicitacao.tituloDoacao

        // Definindo o clique no item
        holder.itemView.setOnClickListener {
            onItemClick(solicitacao)
        }
    }

    override fun getItemCount(): Int = solicitacoesList.size

    class SolicitacaoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.textUserNameBeneficiario)
        val tituloDoacao: TextView = itemView.findViewById(R.id.textTituloDoacao)
    }
}
