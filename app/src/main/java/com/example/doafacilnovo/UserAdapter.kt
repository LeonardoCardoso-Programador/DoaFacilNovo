package com.example.doafacilnovo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(
    private val context: Context,
    private val userList: List<Usuario>,
    private val onItemClick: (Usuario) -> Unit // Função de clique como parâmetro
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_usuario, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        // Preenchendo os dados do usuário nas views
        holder.userName.text = user.nome
        holder.userDoacoes.text = user.contagemDoacoes

        // Definindo o clique no item
        holder.itemView.setOnClickListener {
            onItemClick(user) // Chama a função de clique passando o usuário
        }
    }

    override fun getItemCount(): Int = userList.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userName)
        val userDoacoes: TextView = itemView.findViewById(R.id.userDoacoes)
    }
}
