package com.yilmazbilgin.butcehesabim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                ButceHesabim()
            }
        }
    }
}

@Composable
fun ButceHesabim() {

    var gelir by remember { mutableStateOf("") }
    var gider by remember { mutableStateOf("") }

    val gelirValue = gelir.toDoubleOrNull() ?: 0.0
    val giderValue = gider.toDoubleOrNull() ?: 0.0
    val kalan = gelirValue - giderValue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "💰 Bütçe Hesabım",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Gelir ve giderlerini kolayca takip et",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text("Bu Ay", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Text("Toplam Gelir: ₺${"%.2f".format(gelirValue)}")
                Text("Toplam Gider: ₺${"%.2f".format(giderValue)}")

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Kalan: ₺${"%.2f".format(kalan)}",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        OutlinedTextField(
            value = gelir,
            onValueChange = { gelir = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Maaş / Gelir") },
            placeholder = { Text("66565") }
        )

        OutlinedTextField(
            value = gider,
            onValueChange = { gider = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Gider / Ödeme") },
            placeholder = { Text("15000") }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = { },
                modifier = Modifier.weight(1f)
            ) {
                Text("+ Gelir")
            }

            Button(
                onClick = { },
                modifier = Modifier.weight(1f)
            ) {
                Text("+ Ödeme")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Yaklaşan Ödemeler",
            style =
