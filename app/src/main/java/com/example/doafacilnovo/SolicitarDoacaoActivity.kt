package com.example.doafacilnovo

import android.R.attr.button
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doafacilnovo.databinding.ActivitySolicitarDoacaoBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class SolicitarDoacaoActivity : AppCompatActivity(), OnMapReadyCallback {
    private val binding by lazy { ActivitySolicitarDoacaoBinding.inflate(layoutInflater) }
    private lateinit var userUidDoador: String
    private var googleMap: GoogleMap? = null
    private var currentImageName: String? = null // Variável para armazenar o nome da imagem atual
    private val db = FirebaseFirestore.getInstance()
    private var doacaoSolicitadaId: String? = null  // Variável para armazenar o ID do documento


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Inicializar o fragmento do mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapa) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        val titulo = intent.getStringExtra("title") ?: "Titulo não disponível"
        val nome = intent.getStringExtra("name") ?: "Nome não disponível"
        val descricao = intent.getStringExtra("desc") ?: "Descricao não disponível"
        val location = intent.getStringExtra("location") ?: "Localização não disponível"
        val position = intent.getIntExtra("position", -1)

        binding.buttonVoltar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.textTitulo.text = titulo
        binding.textNomeUtilizador.text = nome + " está doando"
        binding.textDesc.text = descricao

        binding.profileImage.setOnClickListener{
            val intent = Intent(this, VizualizarPerfilActivity::class.java)
            intent.putExtra("UserDoadorUid", userUidDoador)
            startActivity(intent)
        }

        binding.buttonEnviarSolicitacao.setOnClickListener{


            val intent = Intent(this, EnviarSolicitacaoActivity::class.java).apply {

                putExtra("UserSelecionadoUid", userUidDoador)
                putExtra("IdDoacaoSolicitada", doacaoSolicitadaId)
            }
            startActivity(intent)
        }

       /* binding.buttonMenssagem.setOnClickListener() {
            if (::nomeDoador.isInitialized && ::telefoneDoador.isInitialized) {
                val mensagem = "Olá $nomeDoador, tenho interesse na sua doação $titulo!!!" // Mensagem opcional
                val url = "https://api.whatsapp.com/send?phone=$telefoneDoador&text=${Uri.encode(mensagem)}"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setData(Uri.parse(url))

                // Verifica se o WhatsApp está instalado
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(applicationContext, "O WhatsApp não está instalado no dispositivo.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(applicationContext, "Dados do doador não carregados. Tente novamente.", Toast.LENGTH_SHORT).show()
            }
        }*/



        // Buscar a data da doação no Firestore
        fetchDoacaoData(titulo)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Buscar localização do usuário no Firestore
        fetchDoacaoData(intent.getStringExtra("title") ?: "")
    }

    private fun fetchDoacaoData(titulo: String) {
        // Referencia a coleção 'doacoes' e realiza a busca com base no título
        db.collection("doacoes")
            .whereEqualTo("titulo", titulo)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    binding.textData.text = "Data não encontrada"
                } else {

                    val doacao = result.documents.firstOrNull()
                    doacaoSolicitadaId = doacao?.id // Armazena o ID do documento selecionado


                    // Recuperando a latitude, longitude, e data de expiração
                    val latitude = doacao?.getDouble("latitude")
                    val longitude = doacao?.getDouble("longitude")
                    val dataTimestamp = doacao?.getTimestamp("dataExpiracao")
                    val horarioRecolha = doacao?.getString("horarioEntrega")
                    val imagemUrls = doacao?.get("imagemUrls") as? List<String> ?: emptyList()  // Pegando a lista de URLs
                    userUidDoador = doacao?.getString("userUid").toString()


                    fetchUser()


                    //Colocando valor no horario de recolha

                    binding.textHorarioRecolha.setText(horarioRecolha)

                    // Formatando a data
                    val data = dataTimestamp?.toDate()
                    val dataString = formatDateToString(data)
                    binding.textData.text = dataString


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



                    // Verificando se as coordenadas foram encontradas
                    if (latitude != null && longitude != null) {
                        val userLocation = LatLng(latitude, longitude)

                        // Limpar o mapa e adicionar um novo marcador
                        googleMap?.clear()

                        // Adicionar um marcador para as coordenadas
                        googleMap?.addMarker(
                            MarkerOptions()
                                .position(userLocation)
                                .title("Localização do usuário")
                        )

                        // Adicionando um círculo ao redor da localização
                        val circleOptions = CircleOptions()
                            .center(userLocation) // Definir o centro do círculo
                            .radius(500.0) // Raio de 500 metros (ajuste conforme necessário)
                            .strokeColor(0x5500FF00) // Cor da borda do círculo (verde)
                            .fillColor(0x5500FF00) // Cor do preenchimento do círculo (verde claro)
                            .strokeWidth(2f)

                        googleMap?.addCircle(circleOptions)

                        // Move a câmera para mostrar a zona ao redor da localização
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 13f)) // Ajuste o zoom conforme necessário
                    } else {
                        Toast.makeText(this, "Coordenadas não encontradas no documento.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { exception ->
                binding.textData.text = "Erro ao buscar dados"
                Toast.makeText(this, "Erro ao buscar dados: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun fetchUser() {
        db.collection("users").whereEqualTo("uid", userUidDoador)
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

                    } else {
                        Log.w("Doação", "Documento de doação não encontrado.")
                    }
                }
            }
    }

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


    private fun formatDateToString(date: Date?): String {
        if (date == null) return "Data não disponível"

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())  // Formato para dia, mês e ano
        return sdf.format(date)  // Retorna a data formatada como string
    }

}
