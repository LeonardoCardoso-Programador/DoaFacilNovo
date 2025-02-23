package com.example.doafacilnovo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class DetalhesDoacaoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.item_lista) // Certifique-se de que esse é o XML correto

        // Obtendo referência ao TextView
        val statusTextView: TextView = findViewById(R.id.statusDisponibilidade)

        // Pegando o status enviado pelo MainActivity
        val statusRecebido = intent.getStringExtra("STATUS_DOACAO") ?: "Disponível"

        // Configurando o status inicial
        statusTextView.text = statusRecebido
        statusTextView.setTextColor(
            if (statusRecebido == "Disponível")
                ContextCompat.getColor(this, R.color.verde)
            else
                ContextCompat.getColor(this, R.color.vermelho)
        )

        // Alternando entre Disponível e Indisponível ao clicar
        statusTextView.setOnClickListener { view ->
            if (statusTextView.text == "Disponível") {
                statusTextView.text = "Indisponível"
                statusTextView.setTextColor(ContextCompat.getColor(this, R.color.vermelho))
            } else {
                statusTextView.text = "Disponível"
                statusTextView.setTextColor(ContextCompat.getColor(this, R.color.verde))
            }

            // Permite que o clique continue sendo propagado para o elemento pai
            view.performClick()
        }
    }
}
