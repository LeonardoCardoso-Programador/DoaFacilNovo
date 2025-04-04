package com.example.doafacilnovo

import CustomAdapter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.doafacilnovo.databinding.ActivityEditarDoacaoBinding
import com.example.doafacilnovo.databinding.ActivityMinhaDoacaoBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditarDoacaoActivity : AppCompatActivity() {

    private val binding by lazy { ActivityEditarDoacaoBinding.inflate(layoutInflater) }

    private val db = FirebaseFirestore.getInstance()  // Inicializa o Firestore
    private var dataSelecionada: String? = null
    private var dataExpiracaoOriginal: Date? = null // Armazena a data original


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        //Buscar a variavel titulo para pesquisa
        val titulo = intent.getStringExtra("title") ?: "Titulo não disponível"

        fetchDoacoes(titulo)
        binding.buttonVoltar.setOnClickListener { finish() }




        binding.buttonEditar.setOnClickListener {
            atualizarDoacaoNoFirestore(titulo)
        }

        binding.buttonEliminar.setOnClickListener { confirmarEliminacao(titulo) }


    }

    private fun atualizarDoacaoNoFirestore(titulo: String) {
        val novoTitulo = binding.editTitulo.text.toString().trim()
        val novaDescricao = binding.editDescricao.text.toString().trim()
        val novaDataExpiracaoStr = binding.editDataExpiracao.text.toString().trim()

        // Converte a data de String para Timestamp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val novaDataExpiracao: Date? = try {
            dateFormat.parse(novaDataExpiracaoStr)
        } catch (e: ParseException) {
            null
        }

        if (novaDataExpiracao == null) {
            Toast.makeText(this, "Data de expiração inválida!", Toast.LENGTH_SHORT).show()
            return
        }

        val atualizacoes = hashMapOf(
            "titulo" to novoTitulo,
            "descricao" to novaDescricao,
            "dataExpiracao" to Timestamp(novaDataExpiracao)
        )

        db.collection("doacoes")
            .whereEqualTo("titulo", titulo) // Usa o título original para buscar o documento
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val documento = result.documents.first() // Obtém o primeiro documento encontrado
                    val documentoId = documento.id // Pega o ID do documento

                    db.collection("doacoes").document(documentoId)
                        .update(atualizacoes as Map<String, Any>)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Doação atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erro ao atualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Nenhuma doação encontrada para atualizar.", Toast.LENGTH_SHORT).show()
                }

            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar doação: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun fetchDoacoes(titulo: String) {
        db.collection("doacoes")
            .whereEqualTo("titulo", titulo)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.w("Doação", "Nenhuma doação encontrada com o título fornecido.")
                } else {
                    val doacao = result.documents.firstOrNull()
                    if (doacao != null) {
                        val tituloDoacao = doacao.getString("titulo")
                        val descricao = doacao.getString("descricao")
                        val dataTimestamp = doacao.getTimestamp("dataExpiracao")

                        val imagemUrls = doacao?.get("imagemUrls") as? List<String> ?: emptyList()  // Pegando a lista de URLs


                        // Verifica se há imagens e atualiza o carrossel
                        if (imagemUrls.isNotEmpty()) {
                            val adapter = ImagePagerAdapter(imagemUrls)
                            binding.viewPager.adapter = adapter

                            // Conectar o TabLayout ao ViewPager2
                            TabLayoutMediator(binding.tabIndicator, binding.viewPager) { tab, position ->
                                tab.text = (position + 1).toString() // Você pode mudar isso para um texto mais adequado
                            }.attach()
                        } else {
                            Toast.makeText(this, "Nenhuma imagem disponível", Toast.LENGTH_SHORT).show()
                        }

                        // Converte Timestamp para Date
                        dataExpiracaoOriginal = dataTimestamp?.toDate()

                        // Formata a data inicial para exibição
                        val dataExpiracaoString = dataExpiracaoOriginal?.let { date ->
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            dateFormat.format(date)
                        } ?: "Data não disponível"

                        // Atualiza os campos
                        binding.editTitulo.setText(tituloDoacao)
                        binding.editDescricao.setText(descricao)
                        binding.editDataExpiracao.setText(dataExpiracaoString)

                        // Configura o Spinner
                        configureSpinner()
                    } else {
                        Log.w("Doação", "Documento de doação não encontrado.")
                    }
                }
            }
    }

    private fun confirmarEliminacao(titulo: String) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza de que deseja excluir esta doação?")
            .setPositiveButton("Sim") { _, _ -> eliminarDoacaoNoFirestore(titulo) }
            .setNegativeButton("Cancelar", null)
            .create()

        alertDialog.show()
    }

    private fun eliminarDoacaoNoFirestore(titulo: String) {
        db.collection("doacoes")
            .whereEqualTo("titulo", titulo)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val documentoId = result.documents.first().id

                    db.collection("doacoes").document(documentoId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Doação eliminada com sucesso!", Toast.LENGTH_SHORT).show()
                            finish() // Fecha a activity após a exclusão
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erro ao eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Nenhuma doação encontrada para eliminar.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar doação: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun configureSpinner() {
        val options = Array(15) { "$it Dias" } // Inclui "0 Dias" até "14 Dias"

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            options
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerData.adapter = adapter

        binding.spinnerData.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                val diasSelecionados = position // Mantém 0 como opção válida
                atualizarDataExpiracao(diasSelecionados)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun adicionarDiasExpiracao(dias: Int) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Pegar a data atual do campo editDataExpiracao
        val dataAtualStr = binding.editDataExpiracao.text.toString()

        try {
            val dataAtual = dateFormat.parse(dataAtualStr) ?: return
            val calendar = Calendar.getInstance()
            calendar.time = dataAtual
            calendar.add(Calendar.DAY_OF_MONTH, dias) // Adiciona os dias selecionados

            // Atualizar o EditText com a nova data
            val novaData = dateFormat.format(calendar.time)
            binding.editDataExpiracao.setText(novaData)

        } catch (e: ParseException) {
            Log.e("DataErro", "Erro ao converter data: ${e.message}")
        }
    }

    private fun atualizarDataExpiracao(dias: Int) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Usa sempre a data original
        dataExpiracaoOriginal?.let { dataOriginal ->
            val calendar = Calendar.getInstance()
            calendar.time = dataOriginal
            calendar.add(Calendar.DAY_OF_MONTH, dias) // Soma os dias ao original

            val novaData = dateFormat.format(calendar.time)
            binding.editDataExpiracao.setText(novaData)
        }
    }

}