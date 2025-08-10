package com.cocibolka.elbanquito.ui.configuracion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cocibolka.elbanquito.MainActivity
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.data.UsuarioDao
import com.cocibolka.elbanquito.models.Usuarios
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InformacionNegocioFragment : Fragment() {

    private lateinit var editTextNombreEmpresa: EditText
    private lateinit var editTextNombreUsuario: EditText
    private lateinit var editTextApellidoUsuario: EditText
    private lateinit var editTextTelefonoUsuario: EditText
    private lateinit var editTextCorreoUsuario: EditText
    private lateinit var editTextDireccionNegocio: EditText
    private lateinit var editTextSitioWeb: EditText
    private lateinit var btnGuardar: Button
    private lateinit var imageViewFotoPerfil: CircleImageView
    private lateinit var btnCambiarFoto: ImageView
    private lateinit var usuarioDao: UsuarioDao

    private var imagenPerfilPath: String? = null
    private var currentPhotoPath: String = ""

    // Registrar lanzador para el selector de imágenes
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                    imageViewFotoPerfil.setImageBitmap(bitmap)

                    // Guardar la imagen en el almacenamiento interno
                    guardarImagenEnAlmacenamiento(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Registrar lanzador para la cámara
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val file = File(currentPhotoPath)
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                imageViewFotoPerfil.setImageBitmap(bitmap)

                // Guardar la imagen en el almacenamiento interno
                guardarImagenEnAlmacenamiento(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Para Android 13+ (API 33+), necesitamos estos permisos específicos
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            abrirCamara()
        } else {
            Toast.makeText(requireContext(), "Se necesita permiso para usar la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    // Para almacenamiento en Android 11+ usamos el permiso READ_MEDIA_IMAGES
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            abrirSelectorDeImagen()
        } else {
            Toast.makeText(requireContext(), "Se necesita permiso para acceder a la galería", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_informacion_negocio, container, false)

        // Inicializar vistas
        editTextNombreEmpresa = view.findViewById(R.id.editTextNombreEmpresa)
        editTextNombreUsuario = view.findViewById(R.id.editTextNombreUsuario)
        editTextApellidoUsuario = view.findViewById(R.id.editTextApellidoUsuario)
        editTextTelefonoUsuario = view.findViewById(R.id.editTextTelefonoUsuario)
        editTextCorreoUsuario = view.findViewById(R.id.editTextCorreoUsuario)
        editTextDireccionNegocio = view.findViewById(R.id.editTextDireccionNegocio)
        editTextSitioWeb = view.findViewById(R.id.editTextSitioWeb)
        btnGuardar = view.findViewById(R.id.btnGuardar)
        imageViewFotoPerfil = view.findViewById(R.id.imageViewFotoPerfil)
        btnCambiarFoto = view.findViewById(R.id.btnCambiarFoto)

        // Inicializar DAO
        usuarioDao = UsuarioDao(requireContext())

        // Configurar TextWatchers para formatear entradas
        setupTextWatchers()

        // Cargar datos existentes al abrir el fragmento
        cargarDatosGuardados()

        // Cargar imagen de perfil si existe
        cargarImagenPerfil()

        // Configurar el botón de cambiar foto
        btnCambiarFoto.setOnClickListener {
            mostrarOpcionesFoto()
        }

        // También permitir clic en la imagen para cambiarla
        imageViewFotoPerfil.setOnClickListener {
            mostrarOpcionesFoto()
        }

        // Configurar el botón de guardar
        btnGuardar.setOnClickListener {
            guardarDatos()
        }

        return view
    }

    private fun mostrarOpcionesFoto() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_opciones_foto, null)

        val btnGaleria = bottomSheetView.findViewById<LinearLayout>(R.id.btnGaleria)
        val btnCamera = bottomSheetView.findViewById<LinearLayout>(R.id.btnCamera)
        val btnEliminarFoto = bottomSheetView.findViewById<LinearLayout>(R.id.btnEliminarFoto)

        // Mostrar u ocultar la opción de eliminar basado en si hay una foto
        val fotoExiste = File(imagenPerfilPath ?: "").exists()
        btnEliminarFoto.visibility = if (fotoExiste) View.VISIBLE else View.GONE

        btnGaleria.setOnClickListener {
            // Para Android 13+, usamos READ_MEDIA_IMAGES
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        abrirSelectorDeImagen()
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) -> {
                        // Mostrar explicación de por qué se necesita el permiso
                        Toast.makeText(requireContext(), "Se necesita permiso para acceder a tus fotos", Toast.LENGTH_SHORT).show()
                        requestStoragePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                    else -> {
                        // Solicitar permiso
                        requestStoragePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
            }
            // Para Android 10-12, usamos READ_EXTERNAL_STORAGE
            else {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        abrirSelectorDeImagen()
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                        // Mostrar explicación de por qué se necesita el permiso
                        Toast.makeText(requireContext(), "Se necesita permiso para acceder a tus fotos", Toast.LENGTH_SHORT).show()
                        requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    else -> {
                        // Solicitar permiso
                        requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }

            bottomSheetDialog.dismiss()
        }

        btnCamera.setOnClickListener {
            // Verificar permiso para usar la cámara
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    abrirCamara()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                    // Mostrar explicación de por qué se necesita el permiso
                    Toast.makeText(requireContext(), "Se necesita permiso para usar la cámara", Toast.LENGTH_SHORT).show()
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                else -> {
                    // Solicitar permiso
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }

            bottomSheetDialog.dismiss()
        }

        btnEliminarFoto.setOnClickListener {
            // Eliminar la foto
            eliminarFotoPerfil()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun eliminarFotoPerfil() {
        // Eliminar archivo si existe
        val archivo = File(imagenPerfilPath ?: "")
        if (archivo.exists()) {
            archivo.delete()
        }

        // Notificar a MainActivity para actualizar nav_header
        (requireActivity() as MainActivity).actualizarNavHeader()

        // Restaurar imagen predeterminada
        imageViewFotoPerfil.setImageResource(R.drawable.ic_user)
        imagenPerfilPath = null

        Toast.makeText(requireContext(), "Foto de perfil eliminada", Toast.LENGTH_SHORT).show()
    }

    private fun abrirSelectorDeImagen() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Asegurarse de que hay una app de cámara disponible
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            // Crear archivo para guardar la foto
            val photoFile = try {
                crearArchivoImagen()
            } catch (ex: IOException) {
                Toast.makeText(requireContext(), "Error al crear archivo para la foto", Toast.LENGTH_SHORT).show()
                null
            }

            // Continuar solo si el archivo se creó correctamente
            photoFile?.also { file ->
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.cocibolka.elbanquito.fileprovider",
                    file
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                cameraLauncher.launch(intent)
            }
        } else {
            Toast.makeText(requireContext(), "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun crearArchivoImagen(): File {
        // Crear nombre de archivo único basado en timestamp
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val directorio = File(requireContext().filesDir, "perfiles")
        if (!directorio.exists()) {
            directorio.mkdirs()
        }

        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            directorio
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun guardarImagenEnAlmacenamiento(bitmap: Bitmap) {
        val directorio = File(requireContext().filesDir, "perfiles")
        if (!directorio.exists()) {
            directorio.mkdirs()
        }

        val archivo = File(directorio, "perfil_usuario.jpg")
        imagenPerfilPath = archivo.absolutePath

        try {
            val fos = FileOutputStream(archivo)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarImagenPerfil() {
        val archivo = File(requireContext().filesDir, "perfiles/perfil_usuario.jpg")
        if (archivo.exists()) {
            imagenPerfilPath = archivo.absolutePath
            try {
                val bitmap = BitmapFactory.decodeFile(archivo.absolutePath)
                imageViewFotoPerfil.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun guardarDatos() {
        val usuario = Usuarios(
            id = 0,
            nombre_empresa = editTextNombreEmpresa.text.toString().trim(),
            nombre_usuario = editTextNombreUsuario.text.toString().trim(),
            apellido_usuario = editTextApellidoUsuario.text.toString().trim(),
            telefono_usuario = editTextTelefonoUsuario.text.toString().trim(),
            direccion_negocio = editTextDireccionNegocio.text.toString().trim(),
            sitio_web = editTextSitioWeb.text.toString().trim(),
            correo_usuario = editTextCorreoUsuario.text.toString().trim(),
            foto_perfil_path = imagenPerfilPath
        )

        val resultado = usuarioDao.reemplazarUsuario(usuario)
        if (resultado != -1L) {
            Toast.makeText(requireContext(), "Datos guardados correctamente.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Error al guardar los datos.", Toast.LENGTH_SHORT).show()
        }

        // Actualizar el nav_header_main
        val mainActivity = requireActivity() as MainActivity
        mainActivity.actualizarNavHeader()

        // Navegar si se guarda correctamente
        if (resultado != -1L) {
            findNavController().navigate(R.id.nav_inicio)
        }
    }

    private fun cargarDatosGuardados() {
        val usuarios = usuarioDao.obtenerUsuarios()
        if (usuarios.isNotEmpty()) {
            val usuario = usuarios.first() // Usa el primer usuario como ejemplo
            editTextNombreEmpresa.setText(usuario.nombre_empresa ?: "")
            editTextNombreUsuario.setText(usuario.nombre_usuario ?: "")
            editTextApellidoUsuario.setText(usuario.apellido_usuario ?: "")
            editTextTelefonoUsuario.setText(usuario.telefono_usuario ?: "")
            editTextDireccionNegocio.setText(usuario.direccion_negocio ?: "")
            editTextSitioWeb.setText(usuario.sitio_web ?: "")
            editTextCorreoUsuario.setText(usuario.correo_usuario ?: "")
            imagenPerfilPath = usuario.foto_perfil_path
        }
    }

    private fun setupTextWatchers() {
        // TextWatcher para Nombre de Empresa
        editTextNombreEmpresa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                editTextNombreEmpresa.apply {
                    val formattedText = formatText(text.toString())
                    if (formattedText != text.toString()) {
                        setText(formattedText)
                        setSelection(formattedText.length)
                    }
                }
            }
        })

        // TextWatcher para Nombre de Usuario
        editTextNombreUsuario.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                editTextNombreUsuario.apply {
                    val formattedText = formatText(text.toString())
                    if (formattedText != text.toString()) {
                        setText(formattedText)
                        setSelection(formattedText.length)
                    }
                }
            }
        })

        // TextWatcher para Apellido de Usuario
        editTextApellidoUsuario.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                editTextApellidoUsuario.apply {
                    val formattedText = formatText(text.toString())
                    if (formattedText != text.toString()) {
                        setText(formattedText)
                        setSelection(formattedText.length)
                    }
                }
            }
        })

        // TextWatcher para Teléfono
        editTextTelefonoUsuario.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                editTextTelefonoUsuario.apply {
                    val formattedText = formatPhoneNumber(text.toString())
                    if (formattedText != text.toString()) {
                        setText(formattedText)
                        setSelection(formattedText.length)
                    }
                }
            }
        })
    }

    private fun formatText(input: String): String {
        return input.split(" ").joinToString(" ") { it.capitalize() }
    }

    private fun formatPhoneNumber(input: String): String {
        return input.replace(" ", "").chunked(4).joinToString(" ")
    }
}