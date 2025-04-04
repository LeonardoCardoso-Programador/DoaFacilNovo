package com.example.doafacilnovo

import android.app.Activity
import android.app.DownloadManager
import android.app.ProgressDialog
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.doafacilnovo.databinding.ActivityAdicionarDoacaoBinding
import com.example.doafacilnovo.databinding.ActivityLocalizacaoBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class AdicionarDoacaoActivity : AppCompatActivity(), OnMapReadyCallback {
    private val binding by lazy { ActivityAdicionarDoacaoBinding.inflate(layoutInflater) }
    private var googleMap: GoogleMap? = null
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var progressDialog: ProgressDialog // Declare o ProgressDialog

    // Declarando SharedPreferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userUid: String

    private val selectedImageUris = mutableListOf<Uri>()
    private var selectedImageUri: Uri? = null
    private var dataSelecionada: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)


        // Inicializando SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        userUid = getUserUid() ?: ""

        if (userUid.isEmpty()) {
            Toast.makeText(this, "Erro: UID do usuário não encontrado", Toast.LENGTH_SHORT).show()
            finish() // Fecha a activity se não tiver um UID válido
            return
        }

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
            val horarioEntrega = binding!!.editHorarioEntrega.text.toString()

            if (titulo.isEmpty() || descricao.isEmpty() || data.isNullOrEmpty() ||horarioEntrega.isNullOrEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            } else {
                uploadImagesToFirebase()
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
            atualizarImageView()
            updateImagePreview()
        }
    }

    private fun selecionarImagem() {
        multipleImagePickerLauncher.launch(arrayOf("image/*"))
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        val IMAGE_PICK_CODE = 0
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    // Resultado da seleção da imagem
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val IMAGE_PICK_CODE = null
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
            binding?.myImageView?.setImageURI(selectedImageUris[0])
        } else {
            binding?.myImageView?.setImageResource(R.drawable.ic_adicionar)
        }
    }

    private fun updateImagePreview() {
        binding?.imageContainer?.removeAllViews()
        for (i in 1 until selectedImageUris.size) {
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
        val horarioEntrega = binding!!.editHorarioEntrega.text.toString().trim()


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

        // Gerar um ID único para a pasta das imagens
        val pastaID = UUID.randomUUID().toString()
        val pastaDoacao = "imagensDoacoes/$pastaID"
        var primeiraImagemUrl: String? = null

        // Criando o ProgressDialog
        val progressDialog = android.app.ProgressDialog(this)
        progressDialog.setMessage("Carregando as imagens e criando a doação...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        for (uri in selectedImageUris) {
            val imageRef = storageRef.child("$pastaDoacao/${UUID.randomUUID()}.jpg")

            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        imageUrls.add(downloadUri.toString())

                        if (primeiraImagemUrl == null) {
                            primeiraImagemUrl = downloadUri.toString()
                        }

                        if (imageUrls.size == selectedImageUris.size) {
                            salvarDoacaoComImagens(pastaID, tituloDoacao, imageUrls, primeiraImagemUrl, progressDialog, horarioEntrega)
                        }
                    }
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Erro ao enviar imagem", Toast.LENGTH_SHORT).show()
                }
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.uiSettings.isZoomControlsEnabled = true

        fetchUserLocation()
    }

    private fun fetchUserLocation() {
        if (userUid.isEmpty()) {
            Toast.makeText(this, "UID do usuário não disponível", Toast.LENGTH_SHORT).show()
            Log.e("AdicionarDoacaoActivity", "UID vazio.")
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

    private fun salvarDoacaoComImagens(pastaID: String, titulo: String, imagemUrls: List<String>, primeiraImagemUrl: String?, progressDialog: android.app.ProgressDialog, horarioEntrega : String) {
        val descricao = binding?.editDescricao?.text.toString().trim()
        val data = dataSelecionada ?: ""
        val adicionarContagemDoacoes = 1

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
                            "pastaID" to pastaID,
                            "titulo" to titulo,
                            "descricao" to descricao,
                            "data" to data,
                            "userUid" to userUid,
                            "latitude" to latitude,
                            "longitude" to longitude,
                            "nome" to nome,
                            "disponibilidade" to "disponível",
                            "imagemUrls" to imagemUrls,
                            "primeiraImagemUrl" to primeiraImagemUrl,
                            "dataExpiracao" to dataExpiracao,
                            "horarioEntrega" to horarioEntrega
                        )

                        // Atualiza a contagem de doações do usuário
                        firestore.collection("users")
                            .document(userUid)
                            .update("nDoacoes", FieldValue.increment(1))
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Erro ao atualizar nDoacoes: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        firestore.collection("doacoes").add(doacaoData)
                            .addOnSuccessListener {
                                progressDialog.dismiss() // Fechar o ProgressDialog após sucesso
                                Toast.makeText(
                                    this,
                                    "Doação criada com sucesso!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Troca para a nova Activity após sucesso
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { exception ->
                                progressDialog.dismiss() // Fechar o ProgressDialog em caso de erro
                                Toast.makeText(
                                    this,
                                    "Erro ao salvar doação: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
            }
    }

    // Função para obter o UID do usuário salvo no SharedPreferences
    private fun getUserUid(): String? {
        return sharedPreferences.getString("USER_UID", null)
    }


}

