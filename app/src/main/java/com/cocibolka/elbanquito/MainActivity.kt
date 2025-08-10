package com.cocibolka.elbanquito

import androidx.core.view.GravityCompat
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import android.widget.Toast
import com.cocibolka.elbanquito.data.DatabaseHelper
import com.cocibolka.elbanquito.databinding.ActivityMainBinding
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.cocibolka.elbanquito.data.UsuarioDao
import com.cocibolka.elbanquito.models.DataModel
import com.cocibolka.elbanquito.repositories.MonedaRepository
import com.cocibolka.elbanquito.utils.DataCache
import com.cocibolka.elbanquito.utils.ThemeManager
import com.cocibolka.elbanquito.workers.CurrencyExchangeWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import android.view.View
import android.net.Uri
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    // Bandera para evitar la recursión en la navegación
    private var isUpdatingBottomNav = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar el tema guardado antes de inflar la vista
        val savedTheme = ThemeManager.getTheme(this)
        Log.d("MainActivity", "Aplicando tema guardado: $savedTheme")
        ThemeManager.applyTheme(savedTheme)

        // Iniciar el worker para actualizar tasas de cambio
        CurrencyExchangeWorker.enqueuePeriodicWork(this)

        // Inicializar monedas y worker
        initializeMonedas()

        super.onCreate(savedInstanceState)

        // Inicializar binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // Configurar el NavController
        navController = findNavController(R.id.nav_host_fragment_content_main)

        // Configurar NavigationView y DrawerLayout
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_inicio, R.id.nav_slideshow, R.id.nav_clientes, R.id.nav_prestamos,
                R.id.nav_cobros, R.id.nav_contratos, R.id.nav_configuracion, R.id.nav_acerca_de
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        // Primero configura con el controlador y luego asigna el listener personalizado
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener(this)

        // Configurar el BottomNavigationView estándar
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Configurar para que solo muestre el texto en el ítem seleccionado
        bottomNavigationView.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_SELECTED

        // Configurar el listener del BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener { item ->
            // Manejar la navegación solo si no estamos ya en ese destino
            when (item.itemId) {
                R.id.nav_inicio -> {
                    if (navController.currentDestination?.id != R.id.nav_inicio) {
                        navController.popBackStack(R.id.nav_inicio, false)
                        navController.navigate(R.id.nav_inicio)
                    }
                    true
                }
                R.id.nav_clientes -> {
                    if (navController.currentDestination?.id != R.id.nav_clientes) {
                        navController.popBackStack()
                        navController.navigate(R.id.nav_clientes)
                    }
                    true
                }
                R.id.nav_prestamos -> {
                    if (navController.currentDestination?.id != R.id.nav_prestamos) {
                        navController.popBackStack()
                        navController.navigate(R.id.nav_prestamos)
                    }
                    true
                }
                R.id.nav_cobros -> {
                    if (navController.currentDestination?.id != R.id.nav_cobros) {
                        navController.popBackStack()
                        navController.navigate(R.id.nav_cobros)
                    }
                    true
                }
                R.id.nav_contratos -> {
                    if (navController.currentDestination?.id != R.id.nav_contratos) {
                        navController.popBackStack()
                        navController.navigate(R.id.nav_contratos)
                    }
                    true
                }
                else -> false
            }
        }

        // Observar los cambios en la navegación para actualizar el BottomNavigationView
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Evitar actualizar el BottomNav si ya estamos en proceso de actualización
            if (!isUpdatingBottomNav) {
                isUpdatingBottomNav = true

                // Actualizar el elemento seleccionado en el bottom navigation
                when (destination.id) {
                    R.id.nav_inicio -> {
                        bottomNavigationView.selectedItemId = R.id.nav_inicio
                    }
                    R.id.nav_clientes -> {
                        bottomNavigationView.selectedItemId = R.id.nav_clientes
                    }
                    R.id.nav_prestamos -> {
                        bottomNavigationView.selectedItemId = R.id.nav_prestamos
                    }
                    R.id.nav_cobros -> {
                        bottomNavigationView.selectedItemId = R.id.nav_cobros
                    }
                    R.id.nav_contratos -> {
                        bottomNavigationView.selectedItemId = R.id.nav_contratos
                    }
                }

                isUpdatingBottomNav = false
            }
        }

        // Cargar datos
        if (!DataCache.isDataLoaded()) {
            loadData()
        }

        // Configurar clic en el header
        val headerView = navView.getHeaderView(0)
        headerView.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            navController.navigate(R.id.nav_informacion_negocio)
        }

        // Cargar datos actualizados en el nav_header
        actualizarNavHeader()
    }


    private fun loadData() {
        // Simula la carga de datos
        val data = listOf(
            DataModel("1", "Cliente 1"),
            DataModel("2", "Cliente 2"),
            DataModel("3", "Cliente 3")
        )
        DataCache.setData(data) // Guardar los datos en el caché
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Limpiar backstack antes de navegar para evitar problemas de navegación
        when (item.itemId) {
            R.id.nav_inicio -> {
                // Navega al fragmento de inicio, limpiando el backstack
                if (navController.currentDestination?.id != R.id.nav_inicio) {
                    navController.popBackStack(R.id.nav_inicio, false)
                    navController.navigate(R.id.nav_inicio)
                }
            }
            R.id.nav_prestamos -> {
                // Navega a Préstamos, limpiando el backstack
                if (navController.currentDestination?.id != R.id.nav_prestamos) {
                    navController.popBackStack()
                    navController.navigate(R.id.nav_prestamos)
                }
            }
            R.id.nav_cobros -> {
                // Navega a Cobros, limpiando el backstack
                if (navController.currentDestination?.id != R.id.nav_cobros) {
                    navController.popBackStack()
                    navController.navigate(R.id.nav_cobros)
                }
            }
            R.id.nav_acerca_de -> {
                // Navega hacia AcercaDeFragment, limpiando el backstack
                if (navController.currentDestination?.id != R.id.nav_acerca_de) {
                    navController.popBackStack()
                    navController.navigate(R.id.nav_acerca_de)
                }
            }
            R.id.nav_clientes -> {
                // Navega a Clientes, limpiando el backstack
                if (navController.currentDestination?.id != R.id.nav_clientes) {
                    navController.popBackStack()
                    navController.navigate(R.id.nav_clientes)
                }
            }
            R.id.nav_contratos -> {
                // Navega a Contratos, limpiando el backstack
                if (navController.currentDestination?.id != R.id.nav_contratos) {
                    navController.popBackStack()
                    navController.navigate(R.id.nav_contratos)
                }
            }
            R.id.nav_configuracion -> {
                // Navega a Configuración, limpiando el backstack
                if (navController.currentDestination?.id != R.id.nav_configuracion) {
                    navController.popBackStack()
                    navController.navigate(R.id.nav_configuracion)
                }
            }
            R.id.nav_slideshow -> {
                // Navega a Slideshow, limpiando el backstack
                if (navController.currentDestination?.id != R.id.nav_slideshow) {
                    navController.popBackStack()
                    navController.navigate(R.id.nav_slideshow)
                }
            }
            R.id.nav_eliminar_base_datos -> {
                mostrarDialogoEliminarBD()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // Función para obtener las iniciales del nombre y apellido
    private fun obtenerIniciales(nombre: String, apellido: String?): String {
        val nombreParts = nombre.trim().split("\\s+".toRegex())
        val apellidoParts = apellido?.trim()?.split("\\s+".toRegex()) ?: listOf()

        val inicialNombre = if (nombreParts.isNotEmpty() && nombreParts[0].isNotEmpty())
            nombreParts[0][0].uppercaseChar() else ' '
        val inicialApellido = if (apellidoParts.isNotEmpty() && apellidoParts[0].isNotEmpty())
            apellidoParts[0][0].uppercaseChar() else ' '

        return "$inicialNombre$inicialApellido".trim()
    }

    // Función actualizarNavHeader en MainActivity.kt
    fun actualizarNavHeader() {
        val navView: NavigationView = binding.navView
        val headerView = navView.getHeaderView(0)
        val textViewNombreEmpresa = headerView.findViewById<TextView>(R.id.textViewNombreEmpresa)
        val textViewNombreUsuario = headerView.findViewById<TextView>(R.id.textViewNombreUsuario)
        val textViewCorreoUsuario = headerView.findViewById<TextView>(R.id.textViewCorreoUsuario)
        val imageViewFotoPerfil = headerView.findViewById<CircleImageView>(R.id.imageViewFotoPerfil)
        val textViewUserInitial = headerView.findViewById<TextView>(R.id.textViewUserInitial)

        // Obtener los datos actualizados de la base de datos
        val usuarioDao = UsuarioDao(this)
        val usuario = usuarioDao.obtenerUsuarios().firstOrNull()

        if (usuario != null) {
            textViewNombreEmpresa.text = usuario.nombre_empresa

            val nombre = usuario.nombre_usuario.trim()
            val apellido = usuario.apellido_usuario?.trim()
            textViewNombreUsuario.text = "$nombre $apellido".trim()

            // Verificar si hay correo y configurar visibilidad
            if (usuario.correo_usuario.isNullOrBlank()) {
                textViewCorreoUsuario.visibility = View.GONE
            } else {
                textViewCorreoUsuario.visibility = View.VISIBLE
                textViewCorreoUsuario.text = usuario.correo_usuario
            }

            // Configurar foto de perfil o iniciales
            val fotoPerfilPath = usuario.foto_perfil_path
            if (!fotoPerfilPath.isNullOrEmpty()) {
                val file = File(fotoPerfilPath)
                if (file.exists()) {
                    imageViewFotoPerfil.setImageURI(Uri.fromFile(file))
                    imageViewFotoPerfil.visibility = View.VISIBLE
                    textViewUserInitial.visibility = View.GONE
                } else {
                    // Si el archivo no existe, mostrar iniciales
                    val iniciales = obtenerIniciales(nombre, apellido)
                    textViewUserInitial.text = iniciales
                    textViewUserInitial.visibility = View.VISIBLE
                    imageViewFotoPerfil.visibility = View.GONE
                }
            } else {
                // Si no hay ruta de foto, mostrar iniciales
                val iniciales = obtenerIniciales(nombre, apellido)
                textViewUserInitial.text = iniciales
                textViewUserInitial.visibility = View.VISIBLE
                imageViewFotoPerfil.visibility = View.GONE
            }
        } else {
            textViewNombreEmpresa.text = "Nombre de la empresa"
            textViewNombreUsuario.text = "Usuario desconocido"
            textViewCorreoUsuario.visibility = View.GONE
            textViewUserInitial.text = "NA"
            textViewUserInitial.visibility = View.VISIBLE
            imageViewFotoPerfil.visibility = View.GONE
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_nuevo_cliente -> {
                navController.navigate(R.id.nav_nuevo_cliente)
                true
            }
            R.id.action_nuevo_prestamo -> {
                navController.navigate(R.id.nav_nuevo_prestamo)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun mostrarDialogoEliminarBD() {
        MaterialAlertDialogBuilder(this, R.style.DialogoEliminarDatabase)
            .setTitle("Confirmación")
            .setMessage("¡Se eliminarán todos los datos de la aplicación! ¿Desea continuar?")
            .setPositiveButton("Sí") { _, _ -> eliminarBaseDeDatos() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun eliminarBaseDeDatos() {
        val dbFile = getDatabasePath(DatabaseHelper.DATABASE_NAME)
        if (dbFile.exists()) {
            val deleted = dbFile.delete() // Eliminar el archivo de la base de datos
            if (deleted) {
                Toast.makeText(this, "Base de datos eliminada correctamente", Toast.LENGTH_SHORT).show()

                // Reiniciar la actividad
                reiniciarActividad()
            } else {
                Toast.makeText(this, "Error al eliminar la base de datos", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "La base de datos no existe", Toast.LENGTH_SHORT).show()

            // Reiniciar la actividad
            reiniciarActividad()
        }
    }

    private fun initializeMonedas() {
        lifecycleScope.launch {
            try {
                val repository = MonedaRepository(this@MainActivity)

                // Verificar si ya existen monedas, si no, inicializar las predeterminadas
                val monedasExistentes = repository.getAllMonedasList()
                if (monedasExistentes.isEmpty()) {
                    repository.initializeDefaultCurrencies()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error inicializando monedas: ${e.message}")
            }
        }
    }

    private fun reiniciarActividad() {
        val intent = intent
        finish()
        startActivity(intent)
    }
}