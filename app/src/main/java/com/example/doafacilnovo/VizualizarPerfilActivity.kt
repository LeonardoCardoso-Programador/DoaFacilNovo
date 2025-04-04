package com.example.doafacilnovo

import CustomAdapter
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.doafacilnovo.Login_Registro.LoginActivity
import com.example.doafacilnovo.databinding.ActivityVizualizarPerfilBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.util.ArrayList

class VizualizarPerfilActivity : AppCompatActivity() {

    private val binding by lazy { ActivityVizualizarPerfilBinding.inflate(layoutInflater) }
    private val listaDoacao = ArrayList<InformacoesDoacao>()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private val db = FirebaseFirestore.getInstance()  // Inicializa o Firestore
    private var currentImageName: String? = null // Variável para armazenar o nome da imagem atual
    private lateinit var UserSelecionadoUid: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

     UserSelecionadoUid = intent.getStringExtra("UserSelecionadoUid").toString()

        // Inicializando o DrawerLayout e NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

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

        // Ação do botão Menu (abre o DrawerLayout à direita)
        binding.ButtonMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)  // Abrindo o menu à direita
        }

        // Configurando o listener do NavigationView para os itens do menu
        navigationView.setNavigationItemSelectedListener { item ->
            handleNavigationItemSelected(item)
            drawerLayout.closeDrawer(GravityCompat.END)  // Fechando o menu ao selecionar um item
            true
        }

        fetchUser()
        fetchDoacoes()
    }

    private fun handleNavigationItemSelected(item: MenuItem) {

        val userUid = intent.getStringExtra("USER_UID") ?: "UID nao disponivel"
        when (item.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }

            R.id.nav_doacoes -> {
                val intent = Intent(this, MinhaDoacaoActitity::class.java)
                intent.putExtra("USER_UID", userUid)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }

            R.id.nav_MapaLoc -> {

                val intent = Intent(this, LocalizacaoActivity::class.java)
                intent.putExtra("USER_UID", userUid)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()

            }

            R.id.nav_Perfil -> {
                val intent = Intent(this, PerfilActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
            R.id.nav_Vizualizar_Perfil -> {

            }
            R.id.nav_Solicitacoes -> {
                val intent = Intent(this, SolicitacoesRecebidasActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
            R.id.nav_SolicitacoesFeitas -> {
                val intent = Intent(this, SolicitacoesFeitasActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
            }
            R.id.nav_Logout ->{
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }


            else -> {
                // Caso não haja nenhuma ação definida para o item, nada acontece
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END) // Fecha o drawer se estiver aberto
        } else {
            super.onBackPressed() // Comporta-se normalmente se o drawer estiver fechado
        }
    }

    private fun fetchUser() {
        db.collection("users").whereEqualTo("uid", UserSelecionadoUid)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.w("Doação", "Nenhuma doação encontrada com o título fornecido.")
                } else {
                    val user = result.documents.firstOrNull()
                    if (user != null) {
                        val nome = user.getString("nome")
                        val telefone = user.getString("telefone")
                        val biografia = user.getString("biografia") ?: "Preencha este campo (opicional)"
                        currentImageName = user.getString("IdfotoPerfil") // Nome da imagem armazenada
                        val nDoacoes = user.getDouble("nDoacoes")

                        // Carregar a imagem do Firebase Storage, se o nome da imagem for encontrado
                        if (!currentImageName.isNullOrEmpty()) {
                            loadImageFromFirebaseStorage(currentImageName!!)
                        }

                        // Atualizar os campos de texto
                        binding.userName.setText(nome)
                        binding.userContact.setText(telefone)
                        binding.userBio.setText(biografia)
                        binding.progressBarImpact.max = 100 // Define um limite máximo

                        if (nDoacoes != null) {
                            binding.progressBarImpact.progress = (nDoacoes.toInt() ?: 0).coerceIn(0, binding.progressBarImpact.max)
                        }

                        binding.listingsOffered.setText("Doações realizdas "+ nDoacoes)

                    } else {
                        Log.w("Doação", "Documento de doação não encontrado.")
                    }
                }
            }
    }

    private fun loadImageFromFirebaseStorage(imageName: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("fotosUsuarios/$UserSelecionadoUid/$imageName")

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

    private fun fetchDoacoes() {
        db.collection("doacoes").whereEqualTo("userUid", UserSelecionadoUid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                listaDoacao.clear()

                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
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
                    }

                    (binding.listDoacao.adapter as? CustomAdapter)?.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Nenhuma doação disponível.", Toast.LENGTH_SHORT).show()
                }

            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar doações: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}