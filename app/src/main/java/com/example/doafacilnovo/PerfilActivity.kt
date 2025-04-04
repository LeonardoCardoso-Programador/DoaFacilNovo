package com.example.doafacilnovo

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.doafacilnovo.Login_Registro.LoginActivity
import com.example.doafacilnovo.databinding.ActivityMainBinding
import com.example.doafacilnovo.databinding.ActivityPerfilBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class PerfilActivity : AppCompatActivity() {
    private val binding by lazy { ActivityPerfilBinding.inflate(layoutInflater) }
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private val db = FirebaseFirestore.getInstance()  // Inicializa o Firestore
    private val storage = FirebaseStorage.getInstance()

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userUid: String

    private var selectedImageUri: Uri? = null // Variável para armazenar a imagem selecionada
    private var currentImageName: String? = null // Variável para armazenar o nome da imagem atual

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        userUid = getUserUid() ?: ""

        // Inicializando o DrawerLayout e NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

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

        // Abrir a galeria quando o CircleImageView for clicado
        binding.profileImage.setOnClickListener {
            openGallery()
        }

        binding.btnSalvar.setOnClickListener {
            // Chama o método de salvar (upload da imagem e dados)
            saveUserData()
        }

        fetchUser()
    }

    private fun getUserUid(): String? {
        return sharedPreferences.getString("USER_UID", null)
    }

    // Função que trata as ações de navegação
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
            }
            R.id.nav_Vizualizar_Perfil -> {
                val intent = Intent(this, ListagensUserActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
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

    private fun fetchUser() {
        db.collection("users").whereEqualTo("uid", userUid)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.w("Doação", "Nenhuma doação encontrada com o título fornecido.")
                } else {
                    val user = result.documents.firstOrNull()
                    if (user != null) {
                        val nome = user.getString("nome")
                        val email = user.getString("email")
                        val telefone = user.getString("telefone")
                        val biografia = user.getString("biografia") ?: "Preencha este campo (opicional)"
                        currentImageName = user.getString("IdfotoPerfil") // Nome da imagem armazenada

                        // Carregar a imagem do Firebase Storage, se o nome da imagem for encontrado
                        if (!currentImageName.isNullOrEmpty()) {
                            loadImageFromFirebaseStorage(currentImageName!!)
                        }

                        // Atualizar os campos de texto
                        binding.etNome.setText(nome)
                        binding.etEmail.setText(email)
                        binding.etTelefone.setText(telefone)
                        binding.editInformacaoExtra.setText(biografia)
                    } else {
                        Log.w("Doação", "Documento de doação não encontrado.")
                    }
                }
            }
    }

    private fun loadImageFromFirebaseStorage(imageName: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("fotosUsuarios/$userUid/$imageName")

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

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    // Função para lidar com o resultado da seleção de imagem da galeria
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == GALLERY_REQUEST_CODE) {
            data?.data?.let { imageUri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    binding.profileImage.setImageBitmap(bitmap)

                    // Armazenar a URI da imagem selecionada para uso posterior
                    selectedImageUri = imageUri

                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Erro ao carregar a imagem", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveUserData() {
        val nome = binding.etNome.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val telefone = binding.etTelefone.text.toString().trim()
        val biografia = binding.editInformacaoExtra.text.toString().trim()

        // Verificar se os campos obrigatórios estão preenchidos
        if (nome.isEmpty() || email.isEmpty() || telefone.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }

        // Atualizar dados no Firestore
        val userData: MutableMap<String, Any> = hashMapOf(
            "nome" to nome,
            "email" to email,
            "telefone" to telefone,
            "biografia" to biografia
        )

        // Se o usuário escolheu uma imagem, faz o upload
        if (selectedImageUri != null) {
            // Se a imagem foi alterada, substitui a existente
            if (!currentImageName.isNullOrEmpty()) {
                deleteOldImage(currentImageName!!) // Apagar a imagem antiga
            }
            uploadImageToStorage(selectedImageUri!!)
        } else {
            // Se não há imagem, apenas atualizamos os dados no Firestore
            db.collection("users").document(userUid)
                .update(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Perfil atualizado com sucesso", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore Update", "Erro ao atualizar perfil: ${e.message}")
                    Toast.makeText(this, "Erro ao atualizar perfil", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteOldImage(imageName: String) {
        // Referência para a imagem antiga no Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference.child("fotosUsuarios/$userUid/$imageName")

        storageRef.delete()
            .addOnSuccessListener {
                Log.d("Delete Image", "Imagem antiga deletada com sucesso")
            }
            .addOnFailureListener { exception ->
                Log.e("Delete Image", "Erro ao deletar imagem antiga: ${exception.message}")
            }
    }

    private fun uploadImageToStorage(imageUri: Uri) {
        // Usa o nome da imagem antiga para sobrescrever a imagem existente
        val imageName = currentImageName ?: UUID.randomUUID().toString() + ".jpg"
        val storageRef = storage.reference.child("fotosUsuarios/$userUid/$imageName")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveImageUrlToFirestore(uri.toString(), imageName)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Upload Image", "Erro ao subir a imagem: ${e.message}")
                Toast.makeText(this, "Erro ao fazer upload da imagem", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageUrlToFirestore(imageUrl: String, imageName: String) {
        val userData: MutableMap<String, Any> = hashMapOf(
            "nome" to binding.etNome.text.toString(),
            "email" to binding.etEmail.text.toString(),
            "telefone" to binding.etTelefone.text.toString(),
            "biografia" to binding.editInformacaoExtra.text.toString(),
            "IdfotoPerfil" to imageName // Salvando o nome da imagem no campo IdfotoPerfil
        )

        db.collection("users").document(userUid)
            .update(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Perfil atualizado com sucesso", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore Update", "Erro ao atualizar perfil: ${e.message}")
                Toast.makeText(this, "Erro ao atualizar perfil", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val GALLERY_REQUEST_CODE = 1001
    }
}
