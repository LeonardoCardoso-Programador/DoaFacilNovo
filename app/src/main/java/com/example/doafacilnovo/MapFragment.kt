package com.example.doafacilnovo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class MapsFragment : Fragment(R.layout.fragment_maps), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var userCountTextView: TextView  // Adiciona uma referência ao TextView

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragmentLocalização) as? SupportMapFragment
        mapFragment?.getMapAsync(this) ?: run {
            Log.e("MapsFragment", "SupportMapFragment não encontrado!")
        }



        // Obtém a referência do TextView para mostrar a contagem de usuários
        userCountTextView = activity?.findViewById(R.id.userCount) ?: throw IllegalStateException("TextView não encontrado!")
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        // Sempre centraliza no Porto
        centerMapOnPorto()

        // Carrega os usuários no mapa
        loadUsersOnMap()
    }

    private fun centerMapOnPorto() {
        // Coordenadas da cidade do Porto, Portugal (coordenadas fixas)
        val portoLocation = LatLng(41.14961, -8.61099)  // Coordenadas do Porto

        Log.d("MapsFragment", "Centralizando no Porto com coordenadas: $portoLocation")

        // Centraliza o mapa no Porto com um zoom adequado para a cidade
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(portoLocation, 9f))  // Ajuste o zoom conforme necessário
    }

    private fun loadUsersOnMap() {
        // Coordenadas da cidade do Porto, Portugal (coordenadas fixas)
        val portoLocation = LatLng(41.14961, -8.61099)  // Coordenadas do Porto
        var latSum = 0.0
        var lngSum = 0.0
        var userCount = 0

        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.size() > 0) { // Verifica se há usuários
                    documents.forEach { document ->
                        val latitude = document.getDouble("latitude") ?: portoLocation.latitude
                        val longitude = document.getDouble("longitude") ?: portoLocation.longitude
                        val userLocation = LatLng(latitude, longitude)

                        // Cria a bolinha e redimensiona
                        val markerBitmap = BitmapFactory.decodeResource(resources, android.R.drawable.presence_online) // Ícone padrão (bolinha)
                        val smallMarkerBitmap = Bitmap.createScaledBitmap(markerBitmap, 50, 50, false)  // Redimensiona para 50x50px

                        // Adiciona um marcador com o ícone redimensionado
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(userLocation)
                                .title("Usuário")
                                .icon(BitmapDescriptorFactory.fromBitmap(smallMarkerBitmap))  // Usando o ícone redimensionado
                        )

                        // Soma as coordenadas para calcular a média posteriormente
                        latSum += latitude
                        lngSum += longitude
                        userCount++
                    }

                    // Define a contagem de usuários no TextView
                    userCountTextView.text = userCount.toString()

                } else {
                    // Se não houver usuários, centraliza no Porto
                    centerMapOnPorto()

                    // Atualiza o TextView com zero usuários
                    userCountTextView.text = "0"
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapsFragment", "Erro ao buscar usuários", e)
                // Em caso de erro, centraliza no Porto
                centerMapOnPorto()

                // Atualiza o TextView com zero usuários em caso de erro
                userCountTextView.text = "0"
            }
    }
}
