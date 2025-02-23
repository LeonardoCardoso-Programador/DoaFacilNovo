package com.example.doafacilnovo

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.doafacilnovo.databinding.ActivityAdicionarDoacaoBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class AdicionarDoacaoActivity : AppCompatActivity(), OnMapReadyCallback {
    private var binding: ActivityAdicionarDoacaoBinding? = null
    private var googleMap: GoogleMap? = null
    private val firestore = FirebaseFirestore.getInstance()

    // Declarando SharedPreferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userUid: String
    private val storage = FirebaseStorage.getInstance()

    private val selectedImageUris = mutableListOf<Uri>()
    private var selectedImageUri: Uri? = null
    private var dataSelecionada: String? = null

    // Defina o REQUEST_CODE para a solicitação de permissão
    private val REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializando SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        userUid = getUserUid() ?: ""

        if (userUid.isEmpty()) {
            Toast.makeText(this, "Erro: UID do usuário não encontrado", Toast.LENGTH_SHORT).show()
            finish() // Fecha a activity se não tiver um UID válido
            return
        }

        // Configuração do Binding
        binding = ActivityAdicionarDoacaoBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        // Inicializar o fragmento do mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapa) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Configurar o Spinner
        configureSpinner()

        // Configurar botão voltar
        binding!!.buttonVoltar.setOnClickListener { finish() }

        // Configurar botão de submeter
        binding!!.buttonSubmeter.setOnClickListener {
            val titulo = binding!!.editTitulo.text.toString()
            val descricao = binding!!.editDescricao.text.toString()
            val data = dataSelecionada

            if (titulo.isEmpty() || descricao.isEmpty() || data.isNullOrEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            } else {
                uploadImagesToFirebase() // Enviar múltiplas imagens
            }
        }

        // Configurar o clique na imagem para abrir a galeria
        binding!!.myImageView.setOnClickListener {
            // Verificar se a permissão de leitura do armazenamento foi concedida
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Caso a permissão não tenha sido concedida, solicita a permissão
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
            } else {
                // Caso a permissão já tenha sido concedida, abre a galeria
                openGallery()
            }
        }

        binding!!.myImageView.setOnClickListener {
            selecionarImagem()
        }


    }
    private val multipleImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            selectedImageUris.clear()
            selectedImageUris.addAll(uris)

            // Atualiza a visualização com a primeira imagem
            atualizarImageView()
            updateImagePreview()  // Atualiza o `imageContainer` com todas as imagens

        }
    }


    // Função para abrir a galeria
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    // Resultado da seleção da imagem
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            selectedImageUri = data?.data
            binding!!.myImageView.setImageURI(selectedImageUri)  // Exibir a primeira imagem no myImageView
            selectedImageUris.clear() // Limpar a lista de imagens
            selectedImageUris.addAll(data?.data?.let { listOf(it) } ?: emptyList()) // Adicionar a imagem selecionada à lista

            updateImagePreview()  // Atualiza o imageContainer com as outras imagens
        }
    }


    private fun atualizarImageView() {
        if (selectedImageUris.isNotEmpty()) {
            binding?.myImageView?.setImageURI(selectedImageUris[0]) // Exibe a primeira imagem
        } else {
            binding?.myImageView?.setImageResource(R.drawable.ic_adicionar) // Imagem padrão
        }
    }


    private fun selecionarImagem() {
        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE
            )
        } else {
            multipleImagePickerLauncher.launch(arrayOf("image/*"))
        }


    }




    private fun updateImagePreview() {
        // Remove todas as imagens do container
        binding?.imageContainer?.removeAllViews()

        // Exibir as imagens, mas sem a primeira (já exibida em myImageView)
        for (i in 1 until selectedImageUris.size) {  // Começa a partir de 1 para pular a primeira imagem
            val uri = selectedImageUris[i]

            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                    setMargins(8, 8, 8, 8)
                }
                setImageURI(uri)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            binding?.imageContainer?.addView(imageView)
        }
    }




    private fun uploadImagesToFirebase() {
        val tituloDoacao = binding?.editTitulo?.text.toString().trim()

        if (tituloDoacao.isEmpty()) {
            Toast.makeText(this, "O título da doação não pode estar vazio!", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Por favor, selecione pelo menos uma imagem!", Toast.LENGTH_SHORT).show()
            return
        }

        val imageUrls = mutableListOf<String>()
        val storageRef = FirebaseStorage.getInstance().reference

        // 🔥 Gerar um ID único para a pasta das imagens
        val pastaID = UUID.randomUUID().toString()
        val pastaDoacao = "imagensDoacoes/$pastaID" // 📂 Pasta com ID aleatório
        var primeiraImagemUrl: String? = null  // Variável para armazenar a URL da primeira imagem

        for (uri in selectedImageUris) {
            val imageRef = storageRef.child("$pastaDoacao/${UUID.randomUUID()}.jpg")

            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        imageUrls.add(downloadUri.toString())

                        // Armazenar a URL da primeira imagem
                        if (primeiraImagemUrl == null) {
                            primeiraImagemUrl = downloadUri.toString()
                        }

                        // Se todas as imagens foram enviadas, salvar a doação
                        if (imageUrls.size == selectedImageUris.size) {
                            salvarDoacaoComImagens(pastaID, tituloDoacao, imageUrls, primeiraImagemUrl)
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao enviar imagem", Toast.LENGTH_SHORT).show()
                }
        }
    }



    fun startDownload(uri: Uri) {
        // Crie um arquivo de destino no dispositivo (por exemplo, no diretório de Downloads)
        val request = DownloadManager.Request(uri)
            .setTitle("Imagem para download")
            .setDescription("Baixando imagem do Firebase")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "nome_da_imagem.jpg")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        // Obtenha o sistema de download e inicie o download
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        // Opcionalmente, você pode mostrar um Toast informando o usuário
        Toast.makeText(this, "Download iniciado!", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Verificar se a permissão foi concedida
        if (requestCode == REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permissão concedida, pode iniciar o download
            val storageReference = FirebaseStorage.getInstance().reference
            val imageReference = storageReference.child("images/nome_da_imagem.jpg") // Substitua pelo caminho correto da sua imagem

            imageReference.downloadUrl.addOnSuccessListener { uri ->
                // Iniciar o download
                startDownload(uri)
            }.addOnFailureListener { exception ->
                // Tratar falha no download
                Toast.makeText(this, "Falha ao obter URL: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Permissão negada, mostrar uma mensagem
            Toast.makeText(this, "Permissão para acessar o armazenamento negada.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Buscar localização do usuário no Firestore
        fetchUserLocation()
    }

    private fun fetchUserLocation() {
        if (userUid.isEmpty()) {
            Toast.makeText(this, "UID do usuário não disponível", Toast.LENGTH_SHORT).show()
            Log.e("AdicionarDoacaoActivity", "UID vazio.")
            return
        }
        firestore.collection("users")
            .whereEqualTo("uid", userUid)  // Filtra os documentos onde o campo "userUid" é igual ao valor fornecido
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.first()
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")

                    if (latitude != null && longitude != null) {
                        val userLocation = LatLng(latitude, longitude)

                        googleMap?.clear()
                        googleMap?.addMarker(
                            MarkerOptions()
                                .position(userLocation)
                                .title("Localização do usuário")
                        )
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                    } else {
                        Toast.makeText(this, "Coordenadas não encontradas no documento.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Nenhum documento encontrado para o UID: $userUid", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar localização: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configureSpinner() {
        val options = Array(15) { "${it + 1} Dias" }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            options
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding!!.spinnerData.adapter = adapter

        binding!!.spinnerData.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                dataSelecionada = options[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                dataSelecionada = ""
            }
        }
    }

    private fun salvarDoacaoComImagens(pastaID: String, titulo: String, imagemUrls: List<String>, primeiraImagemUrl: String?) {
        val descricao = binding?.editDescricao?.text.toString().trim()
        val data = dataSelecionada ?: ""


        val diasSelecionados = data.replace(" Dias", "").toIntOrNull() ?: return

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, diasSelecionados) // Adiciona os dias selecionados
        val dataExpiracao = calendar.time

        if (titulo.isEmpty() || descricao.isEmpty() || data.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users")
            .whereEqualTo("uid", userUid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.first()
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")
                    val nome = document.getString("nome")

                    if (latitude != null && longitude != null && nome != null) {
                        val doacaoData = mapOf(
                            "pastaID" to pastaID, // 🆕 Salvar ID da pasta
                            "titulo" to titulo,
                            "descricao" to descricao,
                            "data" to data,
                            "userUid" to userUid,
                            "latitude" to latitude,
                            "longitude" to longitude,
                            "nome" to nome,
                            "disponibilidade" to "disponível",
                            "imagemUrls" to imagemUrls, // Lista de imagens
                            "primeiraImagemUrl" to primeiraImagemUrl, // URL da primeira imagem
                            "dataExpiracao" to dataExpiracao // Salvar a data de expiração
                        )

                        firestore.collection("doacoes").add(doacaoData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Doação criada com sucesso!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Erro ao salvar doação: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
    }



    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    // Função para obter o UID do usuário salvo no SharedPreferences
    private fun getUserUid(): String? {
        return sharedPreferences.getString("USER_UID", null)
    }

    companion object {
        const val IMAGE_PICK_CODE = 1000
        const val REQUEST_CODE = 1001
    }
}
