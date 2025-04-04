package com.example.doafacilnovo

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.doafacilnovo.databinding.ActivityEnviarSolicitacaoBinding
import com.example.doafacilnovo.databinding.ActivitySolicitarDoacaoBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EnviarSolicitacaoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEnviarSolicitacaoBinding.inflate(layoutInflater) }
    private lateinit var userUidDoador: String
    private val db = FirebaseFirestore.getInstance()
    private var currentImageName: String? = null // Variável para armazenar o nome da imagem atual
    private lateinit var doacaoSolicitadaId: String // Variável para armazenar o ID do documento
    // Declarando SharedPreferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userUidBeneficiario: String



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        doacaoSolicitadaId = intent.getStringExtra("IdDoacaoSolicitada").toString()

        // Inicializando SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        userUidBeneficiario = getUserUid() ?: ""

        fetchDoacaoSolicitada(doacaoSolicitadaId)

        binding.buttonEnviarSolicitacao.setOnClickListener{
            salvarSolicitacao()
        }

        binding.buttonVoltar.setOnClickListener{
            finish()
        }
    }

    // Função para obter o UID do usuário salvo no SharedPreferences
    private fun getUserUid(): String? {
        return sharedPreferences.getString("USER_UID", null)
    }

    //Busca as informações do doador
    private fun fetchUserDoador() {
        db.collection("users").whereEqualTo("uid", userUidDoador)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.w("Doação", "Nenhuma doação encontrada com o título fornecido.")
                } else {
                    val user = result.documents.firstOrNull()
                    if (user != null) {
                        currentImageName = user.getString("IdfotoPerfil") // Nome da imagem armazenada

                        // Carregar a imagem do Firebase Storage, se o nome da imagem for encontrado
                        if (!currentImageName.isNullOrEmpty()) {
                            loadImageFromFirebaseStorage(currentImageName!!)
                        }

                    } else {
                        Log.w("Doação", "Documento de doação não encontrado.")
                    }
                }
            }
    }

    //Busca imagem do Doador
    private fun loadImageFromFirebaseStorage(imageName: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("fotosUsuarios/$userUidDoador/$imageName")

        val MAX_SIZE: Long = 1024 * 1024 * 5 // 5MB, ajuste conforme necessário

        storageRef.getBytes(MAX_SIZE)
            .addOnSuccessListener { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding.profileImage.setImageBitmap(bitmap)
            }
            .addOnFailureListener { exception ->
                Log.e("Image Load", "Erro ao carregar a imagem: ${exception.message}")
                Toast.makeText(this, "Erro ao carregar a imagem", Toast.LENGTH_SHORT).show()
            }
    }


    private fun fetchDoacaoSolicitada(doacaoSolicitadaId: String) {
        // Referencia a coleção 'doacoes' e acessa um documento específico pelo nome
        db.collection("doacoes").document(doacaoSolicitadaId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Toast.makeText(this, "Dados não encontrados", Toast.LENGTH_SHORT).show()
                } else {
                    val titulo = document.getString("titulo") ?: "Título não disponível"
                    val horarioRecolha = document.getString("horarioEntrega") ?: "Horário não disponível"
                    userUidDoador = document.getString("userUid").toString()

                    fetchUserDoador() // Buscar os dados do doador

                    // Atualizando os dados na interface
                    binding.textHorario.text = horarioRecolha
                    binding.textNomeItem.text = titulo

                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar dados: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun salvarSolicitacao() {
        val horarioRecolha = binding.editPickupTime.text.trim().toString()
        val menssagem = binding.editMessage.text.trim().toString()
        val estadoSolicitacao = "Em espera"

        // Verificar se os campos obrigatórios estão preenchidos
        if (horarioRecolha.isEmpty() || menssagem.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return
        } else {
            val solicitacaoData: MutableMap<String, Any> = hashMapOf(
                "userUidBeneficiario" to userUidBeneficiario,
                "userUidDoador" to userUidDoador,
                "horarioRecolha" to horarioRecolha,
                "menssagem" to menssagem,
                "estadoSolicitacao" to estadoSolicitacao,
                "idDoacaoSolicitada" to doacaoSolicitadaId
            )

            // Adiciona a solicitação à coleção "solicitacaoDoacao"
            db.collection("solicitacaoDoacao")
                .add(solicitacaoData)
                .addOnSuccessListener { documentReference ->
                    // Sucesso ao adicionar
                    Toast.makeText(this, "Solicitação enviada com sucesso", Toast.LENGTH_SHORT).show()

                    // Atualiza os contadores de solicitações para o doador e beneficiário
                    atualizarContadorDoador(userUidDoador)
                    atualizarContadorBeneficiario(userUidBeneficiario)
                }
                .addOnFailureListener { e ->
                    // Falha ao adicionar
                    Toast.makeText(this, "Erro ao enviar solicitação: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()        }
    }

    // Função para atualizar o contador de doações recebidas para o doador
    private fun atualizarContadorDoador(userUidDoador: String) {
        val userRefDoador = db.collection("users").document(userUidDoador)

        // Atualiza o campo 'doacoesRecebidas'
        userRefDoador.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentCount = document.getLong("solicitacoesRecebidas") ?: 0
                    userRefDoador.update("solicitacoesRecebidas", currentCount + 1)
                        .addOnSuccessListener {
                            Log.d("Update", "Contador de doações recebidas atualizado com sucesso")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Update", "Erro ao atualizar contador de doações recebidas: ${e.message}")
                        }
                } else {
                    Log.w("Update", "Doador não encontrado para atualização de doações recebidas")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Update", "Erro ao buscar o doador: ${e.message}")
            }
    }

    // Função para atualizar o contador de solicitações feitas para o beneficiário
    private fun atualizarContadorBeneficiario(userUidBeneficiario: String) {
        val userRefBeneficiario = db.collection("users").document(userUidBeneficiario)

        // Atualiza o campo 'solicitacoesFeitas'
        userRefBeneficiario.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentCount = document.getLong("solicitacoesFeitas") ?: 0
                    userRefBeneficiario.update("solicitacoesFeitas", currentCount + 1)
                        .addOnSuccessListener {
                            Log.d("Update", "Contador de solicitações feitas atualizado com sucesso")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Update", "Erro ao atualizar contador de solicitações feitas: ${e.message}")
                        }
                } else {
                    Log.w("Update", "Beneficiário não encontrado para atualização de solicitações feitas")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Update", "Erro ao buscar o beneficiário: ${e.message}")
            }
    }




}