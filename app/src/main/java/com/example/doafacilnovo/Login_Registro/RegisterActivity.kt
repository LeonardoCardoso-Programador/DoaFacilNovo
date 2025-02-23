package com.example.doafacilnovo.Login_Registro

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doafacilnovo.databinding.ActivityRegisterBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

class RegisterActivity : AppCompatActivity() {
    private val binding by lazy { ActivityRegisterBinding.inflate(layoutInflater) }
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLatitude: Double? = null
    private var userLongitude: Double? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Inicializar o FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Verificar permissão de localização
        checkLocationPermission()

        binding.buttonEntrar.setOnClickListener {
            val nome = binding.editNome.text.toString()
            val telefone = binding.editTelefone.text.toString()
            val email = binding.editEmail.text.toString()
            val pass = binding.editPass.text.toString()

            if (nome.isEmpty() || telefone.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { cadastro ->
                        if (cadastro.isSuccessful) {
                            val user = auth.currentUser
                            user?.let {
                                saveUserToFirestoreWithUid(nome, telefone, email)
                            }
                            Toast.makeText(this, "Sucesso ao cadastrar usuário", Toast.LENGTH_SHORT).show()
                            binding.editNome.setText("")
                            binding.editTelefone.setText("")
                            binding.editEmail.setText("")
                            binding.editPass.setText("")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Erro ao cadastrar: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        binding.buttonVoltar.setOnClickListener {
            finish()
        }
    }

    private fun saveUserToFirestoreWithUid(nome: String, telefone: String, email: String) {
        val user = auth.currentUser
        val uid = user?.uid

        if (uid == null) {
            Toast.makeText(this, "Erro: UID do usuário não encontrado", Toast.LENGTH_SHORT).show()
            return
        }

        // Dados do usuário a serem salvos no Firestore
        val userMap = hashMapOf(
            "uid" to uid,
            "nome" to nome,
            "telefone" to telefone,
            "email" to email,
            "latitude" to userLatitude,
            "longitude" to userLongitude
        )

        // Salvar o documento no Firestore com o UID como ID do documento
        firestore.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Usuário salvo com localização.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao salvar usuário: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    userLatitude = it.latitude
                    userLongitude = it.longitude
                    Toast.makeText(this, "Localização obtida: $userLatitude, $userLongitude", Toast.LENGTH_SHORT).show()
                } ?: Toast.makeText(this, "Não foi possível obter a localização.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation()
            } else {
                Toast.makeText(this, "Permissão de localização negada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
