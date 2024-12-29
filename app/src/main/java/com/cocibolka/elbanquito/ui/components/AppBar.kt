package com.cocibolka.elbanquito.ui.components

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cocibolka.elbanquito.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert

@Composable
fun AppBar() {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.app_name)) },
        actions = {
            IconButton(onClick = { /* Acción del ícono de ajustes */ }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ajustes",
                    tint = Color.White
                )
            }
            IconButton(onClick = { /* Acción del ícono de menú */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menú",
                    tint = Color.White
                )
            }
        },
        backgroundColor = Color(0xFF283593), // Azul oscuro
        contentColor = Color.White,
        elevation = 4.dp
    )
}

@Preview(showBackground = true)
@Composable
fun AppBarPreview() {
    AppBar()
}
