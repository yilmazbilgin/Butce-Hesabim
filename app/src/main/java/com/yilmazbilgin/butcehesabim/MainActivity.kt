package com.yilmazbilgin.butcehesabim

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class BudgetRecord(
    val date: String,
    val type: String,
    val amount: Double,
    val note: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                BudgetApp()
            }
        }
    }
}

@Composable
fun BudgetApp() {

    val context = androidx.compose.ui.platform.LocalContext.current

    val prefs = remember {
        context.getSharedPreferences("butce_hesabim", 0)
    }

    val records = remember {
        mutableStateListOf<BudgetRecord>().apply {
            val saved = prefs.getString("records", "[]") ?: "[]"

            try {
                val array = JSONArray(saved)

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)

                    add(
                        BudgetRecord(
                            date = obj.getString("date"),
                            type = obj.getString("type"),
                            amount = obj.getDouble("amount"),
                            note = obj.getString("note")
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    fun saveRecords() {

        val array = JSONArray()

        records.forEach { record ->

            val obj = JSONObject()

            obj.put("date", record.date)
            obj.put("type", record.type)
            obj.put("amount", record.amount)
            obj.put("note", record.note)

            array.put(obj)
        }

        prefs.edit()
            .putString("records", array.toString())
            .apply()
    }

    var selectedMonthText by rememberSaveable {
        mutableStateOf(YearMonth.now().toString())
    }

    val selectedMonth = YearMonth.parse(selectedMonthText)

    var selectedDateText by rememberSaveable {
        mutableStateOf(LocalDate.now().toString())
    }

    val selectedDate = LocalDate.parse(selectedDateText)

    var salaryText by rememberSaveable {
        mutableStateOf(
            prefs.getString(
                "salary_${selectedMonth.year}_${selectedMonth.monthValue}",
                ""
            ) ?: ""
        )
    }

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var dialogType by remember {
        mutableStateOf("Gider")
    }

    var dialogAmount by remember {
        mutableStateOf("")
    }

    var dialogNote by remember {
        mutableStateOf("")
    }

    val monthPrefix =
        "${selectedMonth.year}-${String.format("%02d", selectedMonth.monthValue)}"

    val monthRecords = records.filter {
        it.date.startsWith(monthPrefix)
    }

    val salary =
        salaryText.replace(",", ".").toDoubleOrNull() ?: 0.0

    val recordIncome =
        monthRecords
            .filter { it.type == "Gelir" }
            .sumOf { it.amount }

    val totalIncome = salary + recordIncome

    val totalExpense =
        monthRecords
            .filter { it.type == "Gider" }
            .sumOf { it.amount }

    val remaining = totalIncome - totalExpense

    val monthFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale("tr", "TR"))

    val displayMonth =
        selectedMonth.atDay(1).format(monthFormatter)
            .replaceFirstChar { it.uppercase(Locale("tr", "TR")) }

    fun money(value: Double): String {
        return NumberFormat
            .getNumberInstance(Locale("tr", "TR"))
            .apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            .format(value) + " ₺"
    }

    fun changeMonth(amount: Long) {

        val newMonth = selectedMonth.plusMonths(amount)

        selectedMonthText = newMonth.toString()

        salaryText =
            prefs.getString(
                "salary_${newMonth.year}_${newMonth.monthValue}",
                ""
            ) ?: ""

        selectedDateText =
            newMonth.atDay(1).toString()
    }

    fun saveSalary() {

        prefs.edit()
            .putString(
                "salary_${selectedMonth.year}_${selectedMonth.monthValue}",
                salaryText
            )
            .apply()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7FA))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "💰 Bütçe Hesabım",
                fontSize = 31.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Gelir ve giderlerini kolayca takip et.",
                fontSize = 16.sp,
                color = Color(0xFF5F5B63)
            )
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEDE8F0)
                )
            ) {

                Column(
                    modifier = Modifier.padding(22.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = displayMonth,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row {

                            TextButton(
                                onClick = {
                                    changeMonth(-1)
                                }
                            ) {
                                Text(
                                    text = "‹",
                                    fontSize = 30.sp
                                )
                            }

                            TextButton(
                                onClick = {
                                    changeMonth(1)
                                }
                            ) {
                                Text(
                                    text = "›",
                                    fontSize = 30.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Toplam Gelir",
                        fontSize = 16.sp,
                        color = Color(0xFF5F5B63)
                    )

                    Text(
                        text = money(totalIncome),
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Toplam Gider",
                        fontSize = 16.sp,
                        color = Color(0xFF5F5B63)
                    )

                    Text(
                        text = money(totalExpense),
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Kalan",
                        fontSize = 16.sp,
                        color = Color(0xFF5F5B63)
                    )

                    Text(
                        text = money(remaining),
                        fontSize = 31.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining >= 0)
                            Color(0xFF218B4B)
                        else
                            Color(0xFFD32F2F)
                    )
                }
            }
        }

        item {

            CalendarCard(
                month = selectedMonth,
                records = records,
                selectedDate = selectedDate,
                onDateSelected = {
                    selectedDateText = it.toString()
                }
            )
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEDE8F0)
                )
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "💰 Maaş / Sabit Gelir",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$displayMonth için aylık maaşını gir.",
                        color = Color(0xFF65606A)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = salaryText,
                        onValueChange = {
                            salaryText = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Aylık maaş / sabit gelir")
                        },
                        placeholder = {
                            Text("Örn: 66565")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            saveSalary()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Maaşı Kaydet")
                    }
                }
            }
        }

        item {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = {
                        dialogType = "Gelir"
                        dialogAmount = ""
                        dialogNote = ""
                        showAddDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("+ Gelir")
                }

                Button(
                    onClick = {
                        dialogType = "Gider"
                        dialogAmount = ""
                        dialogNote = ""
                        showAddDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("+ Gider")
                }
            }
        }

        item {

            Text(
                text = "📅 Yaklaşan Ödemeler",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            val today = LocalDate.now()

            val upcoming =
                records
                    .filter {
                        it.type == "Gider" &&
                        LocalDate.parse(it.date).isAfter(today)
                    }
                    .sortedBy {
                        LocalDate.parse(it.date)
                    }
                    .take(5)

            if (upcoming.isEmpty()) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEDE8F0)
                    )
                ) {

                    Text(
                        text = "Yaklaşan bekleyen ödeme yok.",
                        modifier = Modifier.padding(20.dp),
                        fontSize = 16.sp
                    )
                }

            } else {

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    upcoming.forEach { record ->

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Column {

                                    Text(
                                        text = record.note.ifBlank {
                                            "Gider"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )

                                    Text(
                                        text = formatDate(record.date),
                                        color = Color.Gray
                                    )
                                }

                                Text(
                                    text = money(record.amount),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {

            Text(
                text = "📋 Bu Ayın Kayıtları",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            if (monthRecords.isEmpty()) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEDE8F0)
                    )
                ) {

                    Text(
                        text = "Bu ay henüz kayıt yok.",
                        modifier = Modifier.padding(20.dp)
                    )
                }

            } else {

                monthRecords
                    .sortedByDescending {
                        LocalDate.parse(it.date)
                    }
                    .forEach { record ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    records.remove(record)
                                    saveRecords()
                                },
                            shape = RoundedCornerShape(18.dp)
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = record.note.ifBlank {
                                            record.type
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )

                                    Text(
                                        text = formatDate(record.date),
                                        color = Color.Gray
                                    )
                                }

                                Text(
                                    text =
                                        if (record.type == "Gelir")
                                            "+ ${money(record.amount)}"
                                        else
                                            "- ${money(record.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        if (record.type == "Gelir")
                                            Color(0xFF218B4B)
                                        else
                                            Color(0xFFD32F2F)
                                )
                            }
                        }
                    }

                Text(
                    text = "Kayıt silmek için karta dokunabilirsin.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showAddDialog) {

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
            },
            title = {
                Text(
                    text = if (dialogType == "Gelir")
                        "Yeni Gelir"
                    else
                        "Yeni Gider"
                )
            },
            text = {

                Column {

                    Text(
                        text = "Tarih: ${formatDate(selectedDate.toString())}",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = dialogAmount,
                        onValueChange = {
                            dialogAmount = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Tutar")
                        },
                        placeholder = {
                            Text("Örn: 15000")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dialogNote,
                        onValueChange = {
                            dialogNote = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Açıklama")
                        },
                        placeholder = {
                            Text(
                                if (dialogType == "Gelir")
                                    "Örn: Ek ödeme"
                                else
                                    "Örn: Kredi kartı"
                            )
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {

                            val date = selectedDate

                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    selectedDateText =
                                        LocalDate.of(
                                            year,
                                            month + 1,
                                            day
                                        ).toString()
                                },
                                date.year,
                                date.monthValue - 1,
                                date.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tarihi Değiştir")
                    }
                }
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        val amount =
                            dialogAmount
                                .replace(",", ".")
                                .toDoubleOrNull()

                        if (amount != null && amount > 0) {

                            records.add(
                                BudgetRecord(
                                    date = selectedDate.toString(),
                                    type = dialogType,
                                    amount = amount,
                                    note = dialogNote.trim()
                                )
                            )

                            saveRecords()

                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showAddDialog = false
                    }
                ) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun CalendarCard(
    month: YearMonth,
    records: List<BudgetRecord>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {

    val days = month.lengthOfMonth()

    val firstDay =
        month.atDay(1).dayOfWeek.value % 7

    val incomeDays =
        records
            .filter {
                it.type == "Gelir"
            }
            .map {
                LocalDate.parse(it.date)
            }
            .toSet()

    val expenseDays =
        records
            .filter {
                it.type == "Gider"
            }
            .map {
                LocalDate.parse(it.date)
            }
            .toSet()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEDE8F0)
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = "📆 Aylık Takvim",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                listOf(
                    "Pzt",
                    "Sal",
                    "Çar",
                    "Per",
                    "Cum",
                    "Cmt",
                    "Paz"
                ).forEach {

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = it,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            var day = 1

            for (week in 0..5) {

                if (day > days) break

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {

                    for (column in 0..6) {

                        val position = week * 7 + column

                        if (position < firstDay || day > days) {

                            Spacer(
                                modifier = Modifier.weight(1f)
                            )

                        } else {

                            val currentDay =
                                month.atDay(day)

                            val isSelected =
                                currentDay == selectedDate

                            val hasIncome =
                                currentDay in incomeDays

                            val hasExpense =
                                currentDay in expenseDays

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected)
                                            Color(0xFFD9C9EA)
                                        else
                                            Color.Transparent
                                    )
                                    .clickable {
                                        onDateSelected(
                                            currentDay
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {

                                Column(
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally,
                                    verticalArrangement =
                                        Arrangement.Center
                                ) {

                                    Text(
                                        text = day.toString(),
                                        fontSize = 16.sp,
                                        fontWeight =
                                            if (isSelected)
                                                FontWeight.Bold
                                            else
                                                FontWeight.Normal
                                    )

                                    Row {

                                        if (hasIncome) {

                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Color(0xFF72B943)
                                                    )
                                            )
                                        }

                                        if (hasIncome && hasExpense) {
                                            Spacer(
                                                modifier = Modifier.width(3.dp)
                                            )
                                        }

                                        if (hasExpense) {

                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Color(0xFFF44336)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }

                            day++
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF72B943))
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text("Gelir")

                Spacer(modifier = Modifier.width(18.dp))

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF44336))
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text("Gider")
            }
        }
    }
}

fun formatDate(date: String): String {

    return try {

        val parsed = LocalDate.parse(date)

        String.format(
            Locale("tr", "TR"),
            "%02d.%02d.%04d",
            parsed.dayOfMonth,
            parsed.monthValue,
            parsed.year
        )

    } catch (_: Exception) {

        date
    }
}
