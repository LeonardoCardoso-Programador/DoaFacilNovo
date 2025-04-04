package com.example.doafacilnovo

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.doafacilnovo.Login_Registro.LoginActivity
import com.example.doafacilnovo.databinding.ActivityLocalizacaoBinding
import com.google.android.material.navigation.NavigationView

class LocalizacaoActivity : AppCompatActivity() {
    private val binding by lazy { ActivityLocalizacaoBinding.inflate(layoutInflater) }
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        supportFragmentManager.beginTransaction()
            .replace(R.id.mapFragmentLocalização, MapsFragment())
            .commit()

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
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
            }
            R.id.nav_MapaLoc -> {}

            R.id.nav_Perfil -> {
                val intent = Intent(this, PerfilActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
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
}
