package com.example.doafacilnovo

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.doafacilnovo.databinding.ActivitySolicitarDoacaoBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SolicitarDoacaoActivity : AppCompatActivity(), OnMapReadyCallback {
    private val binding by lazy { ActivitySolicitarDoacaoBinding.inflate(layoutInflater) }
    private val userUid by lazy { intent.getStringExtra("USER_UID") ?: "" }
    private var googleMap: GoogleMap? = null

    private val db = FirebaseFirestore.getInstance()

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

        binding.tituloHeader.text = titulo
        binding.textTitulo.text = titulo
        binding.textNomeUtilizador.text = nome
        binding.textDesc.text = descricao

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

                    // Recuperando a latitude, longitude, e data de expiração
                    val latitude = doacao?.getDouble("latitude")
                    val longitude = doacao?.getDouble("longitude")
                    val dataTimestamp = doacao?.getTimestamp("dataExpiracao")
                    val imagemUrls = doacao?.get("imagemUrls") as? List<String> ?: emptyList()  // Pegando a lista de URLs

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

    private fun formatDateToString(date: Date?): String {
        if (date == null) return "Data não disponível"

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())  // Formato para dia, mês e ano
        return sdf.format(date)  // Retorna a data formatada como string
    }

}
