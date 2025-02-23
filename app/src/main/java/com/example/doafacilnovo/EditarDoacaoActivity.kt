package com.example.doafacilnovo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.doafacilnovo.databinding.ActivityEditarDoacaoBinding
import com.example.doafacilnovo.databinding.ActivityMinhaDoacaoBinding

class EditarDoacaoActivity : AppCompatActivity() {

    private val binding by lazy { ActivityEditarDoacaoBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)


        binding.buttonVoltar.setOnClickListener { finish() }


        binding.statusDisponibilidade.setOnClickListener { view ->

            if (binding.statusDisponibilidade.text == "Disponível") {
                binding.statusDisponibilidade.text = "Indisponível"
                binding.statusDisponibilidade.setTextColor(ContextCompat.getColor(this, R.color.vermelho))
            } else {
                binding.statusDisponibilidade.text = "Disponível"
                binding.statusDisponibilidade.setTextColor(ContextCompat.getColor(this, R.color.verde))
            }

        }




    }
}