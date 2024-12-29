package com.cocibolka.elbanquito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cocibolka.elbanquito.ui.components.AppBar


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            throwable.printStackTrace()
        }
        setContent {
            MaterialTheme {
                InicioScreen()
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen() {
    Scaffold(
        topBar = { AppBar()
            CenterAlignedTopAppBar(
                title = { Text("Inicio") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text("Pantalla básica funcionando", color = Color.Black)
        }
    }
}



@Composable
fun GananciasCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("He ganado:", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "C\$ 149,868.16",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFC107)
            )
            Text("+49.89%", fontSize = 16.sp, color = Color(0xFF4CAF50))
            Text(
                "Octubre: C\$ 100,800.30",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Se han invertido ≈ 1,500,800.30 dólares hasta hoy.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PagosSection(title: String, count: String, names: List<String>, atrasado: Boolean = false) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (atrasado) Color(0xFFFFCDD2) else Color(0xFFBBDEFB)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$title $count", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            names.forEach { name ->
                Text(
                    text = "$name: C\$ 10,000.00",
                    fontSize = 14.sp,
                    color = if (atrasado) Color(0xFFD32F2F) else Color(0xFF388E3C)
                )
            }
        }
    }
}

@Composable
fun DestacadosSection() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Destacados:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Juan Pérez: #001", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("Cantidad: C\$ 10,000.00 - 5% al mes", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Flavio Josefo: #002", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("Cantidad: C\$ 5,000.00 - 4% al mes", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInicioScreen() {
    InicioScreen()
}
