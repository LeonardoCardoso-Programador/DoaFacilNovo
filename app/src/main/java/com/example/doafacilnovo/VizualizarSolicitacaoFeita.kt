package com.example.doafacilnovo

import CustomAdapter
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.doafacilnovo.databinding.ActivityVizualizarSolicitacaoFeitaBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class VizualizarSolicitacaoFeita : AppCompatActivity() {

    private val binding by lazy { ActivityVizualizarSolicitacaoFeitaBinding.inflate(layoutInflater) }
    private val listaDoacao = ArrayList<InformacoesDoacao>()
    private val db = FirebaseFirestore.getInstance()  // Inicializa o Firestore
    private var currentImageName: String? = null // Variável para armazenar o nome da imagem atual
    private lateinit var solicitacaoUid: String
    private lateinit var progressDialog: ProgressDialog // Declare o ProgressDialog
    private lateinit var uidBeneficiario: String
    private lateinit var uidDoador: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        solicitacaoUid = intent.getStringExtra("solicitacaoUid").toString()

        // Inicialize o ProgressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Carregando dados...") // Mensagem do ProgressDialog
        progressDialog.setCancelable(false) // Impede o cancelamento ao tocar fora do dialog


        // Criando e configurando o adapter
        val adapter = CustomAdapter(this, listaDoacao)
        binding.listDoacao.adapter = adapter

        // Ação do clique na lista (abre a activity SolicitarDoacaoActivity)
        binding.listDoacao.setOnItemClickListener { _, _, position, _ ->
            val listaDoacaoSelecionada = listaDoacao[position]
            val intent = Intent(this, SolicitarDoacaoActivity::class.java).apply {
                putExtra("title", listaDoacaoSelecionada.titulo)
                putExtra("name", listaDoacaoSelecionada.nome)
                putExtra("desc", listaDoacaoSelecionada.descricao)

            }
            startActivity(intent)  // Código atualizado para Android 13+ sem `startActivityForResult`
        }
        // Fechar activity
        binding.buttonVoltar.setOnClickListener {
            finish()
        }
        binding.buttonApagar.setOnClickListener{
            deletarDocumento(solicitacaoUid)
        }

        fetchSolicitacoes(solicitacaoUid)
    }

    private fun fetchSolicitacoes(solicitacaoUid: String) {
        db.collection("solicitacaoDoacao").document(solicitacaoUid)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.w("Solicitação", "Nenhuma solicitação encontrada com este ID.")
                    return@addOnSuccessListener
                }

                val uidSolicitacao = document.id
                val uidDoacao = document.getString("idDoacaoSolicitada") ?: "Desconhecido"
                uidBeneficiario = document.getString("userUidBeneficiario") ?: "Desconhecido"
                uidDoador = document.getString("userUidDoador").toString()

                fetchDoacoes(uidDoacao)
                fetchUserBeneficiario(uidDoador)

                // Criar objeto da solicitação
                val solicitacao = Solicitacoes(uidSolicitacao, uidDoacao, uidBeneficiario)

                Log.d("Firestore", "Solicitação carregada: $uidSolicitacao")
                Log.d("Firestore", "Doacao carregada: $uidDoacao")

            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Erro ao buscar solicitação: ${exception.message}")
                Toast.makeText(this, "Erro ao carregar os dados", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchDoacoes(uidDoacao: String) {
        // Exibe o ProgressDialog enquanto os dados estão sendo carregados
        progressDialog.show()

        db.collection("doacoes")
            .document(uidDoacao) // Obtém um único documento pelo UID
            .get()
            .addOnSuccessListener { document ->
                listaDoacao.clear()

                if (document.exists()) { // ✅ Verifica se o documento existe
                    val titulo = document.getString("titulo") ?: "Título desconhecido"
                    val descricao = document.getString("descricao") ?: "Sem descrição"
                    val data = document.getString("data")
                    val nomeUsuario = document.getString("nome") ?: "Usuário desconhecido"
                    val disponibilidade = document.getString("disponibilidade") ?: "Indisponível"
                    val latitude = document.getDouble("latitude") ?: 0.0
                    val longitude = document.getDouble("longitude") ?: 0.0
                    val primeiraImagemUrl = document.getString("primeiraImagemUrl")

                    val doacao = InformacoesDoacao(
                        titulo, nomeUsuario, descricao, longitude, latitude, data, disponibilidade, primeiraImagemUrl
                    )
                    listaDoacao.add(doacao)

                    // Atualiza o adapter
                    (binding.listDoacao.adapter as? CustomAdapter)?.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Nenhuma doação encontrada.", Toast.LENGTH_SHORT).show()
                }

                progressDialog.dismiss()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar doação: ${exception.message}", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
    }


    private fun fetchUserBeneficiario(uidDoador: String) {
        db.collection("users").document(uidDoador) // Obtém um único documento pelo UID
            .get()
            .addOnSuccessListener { result ->
                // Verifique se o documento existe
                if (result.exists()) {
                    val user = result
                    val nome = user.getString("nome")
                    val telefone = user.getString("telefone")
                    val biografia = user.getString("biografia") ?: "Preencha este campo (opcional)"
                    currentImageName = user.getString("IdfotoPerfil") // Nome da imagem armazenada

                    // Carregar a imagem do Firebase Storage, se o nome da imagem for encontrado
                    if (!currentImageName.isNullOrEmpty()) {
                        loadImageFromFirebaseStorage(currentImageName!!, uidDoador)
                    }


                    binding.userName.setText(nome ?: "")
                    binding.userContact.setText(telefone ?: "")
                    binding.userBio.setText(biografia)

                } else {
                    Log.w("Doação", "Documento de doação não encontrado.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Doação", "Erro ao buscar usuário: ${exception.message}")
            }
    }

    private fun loadImageFromFirebaseStorage(imageName: String, uidDoador: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("fotosUsuarios/$uidDoador/$imageName")

        val MAX_SIZE: Long = 1024 * 1024 * 5

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


    private fun deletarDocumento(documentoId: String) {
    val db = FirebaseFirestore.getInstance()

    db.collection("solicitacaoDoacao").document(documentoId)
        .delete()
        .addOnSuccessListener {
            Log.d("Firestore", "Documento deletado com sucesso: $documentoId")
            Toast.makeText(this, "Documento deletado com sucesso!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Erro ao deletar documento: ${e.message}")
            Toast.makeText(this, "Erro ao deletar documento", Toast.LENGTH_SHORT).show()
        }
}


}