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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

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

@androidx.compose.runtime.Composable
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
            text = "Gelir ve giderlerini kolayca takip et.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Bu Ay",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Toplam Gelir: ${String.format(Locale.getDefault(), "%.2f", gelirValue)} ₺"
                )

                Text(
                    text = "Toplam Gider: ${String.format(Locale.getDefault(), "%.2f", giderValue)} ₺"
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Kalan: ${String.format(Locale.getDefault(), "%.2f", kalan)} ₺",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        OutlinedTextField(
            value = gelir,
            onValueChange = { gelir = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Maaş / Gelir") },
            placeholder = { Text("66565") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            singleLine = true
        )

        OutlinedTextField(
            value = gider,
            onValueChange = { gider = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Gider / Ödeme") },
            placeholder = { Text("15000") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = {
                    gelir = ""
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("+ Gelir")
            }

            Button(
                onClick = {
                    gider = ""
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("+ Ödeme")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Yaklaşan Ödemeler",
            style = MaterialTheme.typography.titleMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Henüz yaklaşan ödeme eklenmedi.",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
