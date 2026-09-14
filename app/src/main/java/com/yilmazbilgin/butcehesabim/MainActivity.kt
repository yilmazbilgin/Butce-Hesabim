package com.yilmazbilgin.butcehesabim

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun dateText(date: String): String {
    return try {
        val d = LocalDate.parse(date)
        "%02d.%02d.%04d".format(
            Locale("tr", "TR"),
            d.dayOfMonth,
            d.monthValue,
            d.year
        )
    } catch (_: Exception) {
        date
    }
}

data class BudgetRecord(
    val id: Long,
    val date: String,
    val type: String,
    val amount: Double,
    val note: String,
    val paid: Boolean = false,
    val installment: String = ""
)

class MainActivity : ComponentActivity() {

    private val prefs by lazy {
        getSharedPreferences("butce_hesabim", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BudgetApp()
                }
            }
        }
    }

    private fun loadRecords(): MutableList<BudgetRecord> {
        val result = mutableListOf<BudgetRecord>()
        val saved = prefs.getString("records", "[]") ?: "[]"

        try {
            val array = JSONArray(saved)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                result.add(
                    BudgetRecord(
                        id = o.optLong("id", System.currentTimeMillis() + i),
                        date = o.optString("date"),
                        type = o.optString("type"),
                        amount = o.optDouble("amount", 0.0),
                        note = o.optString("note"),
                        paid = o.optBoolean("paid", false),
                        installment = o.optString("installment")
                    )
                )
            }
        } catch (_: Exception) {
        }
        expandInstallments(result)
        if (result.isNotEmpty()) saveRecords(result)
        return result
    }

    private fun expandInstallments(records: MutableList<BudgetRecord>) {
        val original = records.toList()
        val additions = mutableListOf<BudgetRecord>()

        for (record in original) {
            if (record.type != "Gider") continue

            val raw = record.installment.trim()
            if (raw.isBlank()) continue

            val slash = raw.indexOf("/")
            val totalParts: Int
            val currentPart: Int

            if (slash >= 0) {
                currentPart = raw.substring(0, slash).trim().toIntOrNull() ?: 1
                totalParts = raw.substring(slash + 1).trim().toIntOrNull() ?: continue
            } else {
                // A plain "8" means 8 total installments and this record is 1/8.
                currentPart = 1
                totalParts = raw.toIntOrNull() ?: continue
            }

            if (totalParts <= 1 || currentPart !in 1..totalParts) continue

            val baseDate = try {
                LocalDate.parse(record.date)
            } catch (_: Exception) {
                continue
            }

            val normalized = "$currentPart/$totalParts"
            val originalIndex = records.indexOfFirst { it.id == record.id }
            if (originalIndex >= 0 && record.installment != normalized) {
                records[originalIndex] = record.copy(installment = normalized)
            }

            for (part in (currentPart + 1)..totalParts) {
                val futureDate =
                    baseDate.plusMonths((part - currentPart).toLong())
                val installmentLabel = "$part/$totalParts"

                val alreadyExists =
                    records.any {
                        it.type == "Gider" &&
                        it.date == futureDate.toString() &&
                        it.note == record.note &&
                        kotlin.math.abs(it.amount - record.amount) < 0.01 &&
                        it.installment == installmentLabel
                    } ||
                    additions.any {
                        it.type == "Gider" &&
                        it.date == futureDate.toString() &&
                        it.note == record.note &&
                        kotlin.math.abs(it.amount - record.amount) < 0.01 &&
                        it.installment == installmentLabel
                    }

                if (!alreadyExists) {
                    additions.add(
                        record.copy(
                            id = System.currentTimeMillis() +
                                additions.size.toLong() + part.toLong() * 1000000L,
                            date = futureDate.toString(),
                            paid = false,
                            installment = installmentLabel
                        )
                    )
                }
            }
        }

        records.addAll(additions)
    }


    private fun saveRecords(records: List<BudgetRecord>) {
        val array = JSONArray()
        records.forEach { r ->
            array.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("date", r.date)
                    put("type", r.type)
                    put("amount", r.amount)
                    put("note", r.note)
                    put("paid", r.paid)
                    put("installment", r.installment)
                }
            )
        }
        prefs.edit().putString("records", array.toString()).apply()
    }

    private fun money(value: Double): String =
        NumberFormat.getNumberInstance(Locale("tr", "TR")).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }.format(value) + " \u20ba"

    @Composable
    private fun BudgetApp() {
        val records = remember {
            mutableStateListOf<BudgetRecord>().apply { addAll(loadRecords()) }
        }

        var monthText by rememberSaveable {
            mutableStateOf(YearMonth.now().toString())
        }
        var selectedDateText by rememberSaveable {
            mutableStateOf(LocalDate.now().toString())
        }
        var showDialog by remember { mutableStateOf(false) }
        var dialogType by remember { mutableStateOf("Gider") }
        var dialogAmount by remember { mutableStateOf("") }
        var dialogNote by remember { mutableStateOf("") }
        var dialogInstallment by remember { mutableStateOf("") }

        val month = YearMonth.parse(monthText)
        val selectedDate = LocalDate.parse(selectedDateText)
        val prefix = month.toString()

        LaunchedEffect(Unit) {
            val before = records.size
            expandInstallments(records)
            if (records.size != before) {
                saveRecords(records)
            } else {
                // Also persist normalization such as 8 -> 1/8.
                saveRecords(records)
            }
        }

        val monthRecords = records.filter { it.date.startsWith(prefix) }
        val expenses = monthRecords.filter { it.type == "Gider" }
        val incomes = monthRecords.filter { it.type == "Gelir" }

        var salaryText by remember(monthText) {
            mutableStateOf(
                prefs.getString(
                    "salary_${month.year}_${month.monthValue}",
                    ""
                ) ?: ""
            )
        }

        val salary = salaryText.replace(",", ".").toDoubleOrNull() ?: 0.0
        val totalIncome = salary + incomes.sumOf { it.amount }
        val totalExpense = expenses.sumOf { it.amount }
        val remaining = totalIncome - totalExpense

        val monthTitle = month.atDay(1).format(
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale("tr", "TR"))
        ).replaceFirstChar { it.uppercase(Locale("tr", "TR")) }

        fun changeMonth(delta: Long) {
            expandInstallments(records)
            saveRecords(records)
            val next = month.plusMonths(delta)
            monthText = next.toString()
            selectedDateText = next.atDay(1).toString()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F6F8))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    "B\u00fct\u00e7e Hesab\u0131m",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Gelir, gider ve \u00f6demelerini takip et",
                    fontSize = 10.sp,
                    color = Color(0xFF66636A)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(11.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEAE6EE)
                    )
                ) {
                    Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                monthTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton({ changeMonth(-1) }) {
                                    Text("\u2039", fontSize = 24.sp)
                                }
                                TextButton({ changeMonth(1) }) {
                                    Text("\u203a", fontSize = 24.sp)
                                }
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            SummaryBox(
                                "Gelir",
                                money(totalIncome),
                                Color(0xFF2E7D32),
                                Modifier.weight(1f)
                            )
                            SummaryBox(
                                "Gider",
                                money(totalExpense),
                                Color(0xFFC62828),
                                Modifier.weight(1f)
                            )
                            SummaryBox(
                                "Kalan",
                                money(remaining),
                                if (remaining >= 0) Color(0xFF2E7D32)
                                else Color(0xFFC62828),
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                CalendarCard(
                    month = month,
                    records = records,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDateText = it.toString() }
                )
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(
                            "Maa\u015f / Sabit Gelir",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        OutlinedTextField(
                            value = salaryText,
                            onValueChange = { salaryText = it },
                            Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Ayl\u0131k gelir") },
                            placeholder = { Text("\u00d6rn. 66565") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                        Spacer(Modifier.height(3.dp))
                        Button(
                            onClick = {
                                prefs.edit()
                                    .putString(
                                        "salary_${month.year}_${month.monthValue}",
                                        salaryText
                                    )
                                    .apply()
                            },
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Maa\u015f\u0131 Kaydet", fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            dialogType = "Gelir"
                            dialogAmount = ""
                            dialogNote = ""
                            dialogInstallment = ""
                            showDialog = true
                        },
                        Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Gelir", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            dialogType = "Gider"
                            dialogAmount = ""
                            dialogNote = ""
                            dialogInstallment = ""
                            showDialog = true
                        },
                        Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Gider", fontSize = 13.sp)
                    }
                }
            }

            item {
                SectionTitle("Yakla\u015fan \u00d6demeler")

                val today = LocalDate.now()
                val upcoming = records
                    .filter {
                        it.type == "Gider" &&
                        !it.paid &&
                        !LocalDate.parse(it.date).isBefore(today)
                    }
                    .sortedBy { it.date }
                    .take(5)

                if (upcoming.isEmpty()) {
                    EmptyCard("Yakla\u015fan bekleyen \u00f6deme yok.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        upcoming.forEach { record ->
                            PaymentRow(
                                record = record,
                                onPaid = {
                                    val index =
                                        records.indexOfFirst { it.id == record.id }
                                    if (index >= 0) {
                                        records[index] =
                                            records[index].copy(paid = true)
                                        saveRecords(records)
                                    }
                                },
                                onDelete = {
                                    records.removeAll { it.id == record.id }
                                    saveRecords(records)
                                }
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("Bu Ay\u0131n Kay\u0131tlar\u0131")

                if (monthRecords.isEmpty()) {
                    EmptyCard("Bu ay hen\u00fcz kay\u0131t yok.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        monthRecords
                            .sortedByDescending { it.date }
                            .forEach { record ->
                                PaymentRow(
                                    record = record,
                                    onPaid = {
                                        val index =
                                            records.indexOfFirst { it.id == record.id }
                                        if (index >= 0) {
                                            records[index] =
                                                records[index].copy(paid = true)
                                            saveRecords(records)
                                        }
                                    },
                                    onDelete = {
                                        records.removeAll { it.id == record.id }
                                        saveRecords(records)
                                    }
                                )
                            }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        if (showDialog) {
            AddRecordDialog(
                type = dialogType,
                amount = dialogAmount,
                note = dialogNote,
                installment = dialogInstallment,
                selectedDate = selectedDate,
                onAmountChange = { dialogAmount = it },
                onNoteChange = { dialogNote = it },
                onInstallmentChange = { dialogInstallment = it },
                onDateChange = { selectedDateText = it.toString() },
                onDismiss = { showDialog = false },
                onSave = {
                    val amount =
                        dialogAmount.replace(",", ".").toDoubleOrNull()

                    if (amount != null && amount > 0) {
                        records.add(
                            BudgetRecord(
                                id = System.currentTimeMillis(),
                                date = selectedDate.toString(),
                                type = dialogType,
                                amount = amount,
                                note = dialogNote.trim(),
                                paid = dialogType == "Gelir",
                                installment = dialogInstallment.trim()
                            )
                        )
                        expandInstallments(records)
                        saveRecords(records)
                        showDialog = false
                    }
                }
            )
        }
    }
}

@Composable
private fun SummaryBox(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                title,
                fontSize = 10.sp,
                color = Color(0xFF66636A)
            )
            Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(3.dp))
}

@Composable
private fun EmptyCard(text: String) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEAE6EE)
        )
    ) {
        Text(
            text,
            Modifier.padding(15.dp),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PaymentRow(
    record: BudgetRecord,
    onPaid: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = record.type == "Gelir"
    val statusText = when {
        isIncome -> "GEL\u0130R"
        record.paid -> "\u00d6DEND\u0130"
        else -> "BEKL\u0130YOR"
    }
    val statusColor = when {
        isIncome -> Color(0xFF2E7D32)
        record.paid -> Color(0xFF2E7D32)
        else -> Color(0xFFC62828)
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (record.paid || isIncome)
                Color(0xFFEAF4EC)
            else
                Color.White
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    record.note.ifBlank {
                        if (isIncome) "Gelir" else "\u00d6deme"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        dateText(record.date),
                        fontSize = 10.sp,
                        color = Color(0xFF77737A)
                    )
                    if (record.installment.isNotBlank()) {
                        Text(
                            "  \u2022  ${record.installment}",
                            fontSize = 10.sp,
                            color = Color(0xFF77737A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    statusText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(Modifier.width(6.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    if (isIncome)
                        "+ ${moneyStatic(record.amount)}"
                    else
                        "- ${moneyStatic(record.amount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome)
                        Color(0xFF2E7D32)
                    else
                        Color(0xFFC62828),
                    maxLines = 1
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isIncome && !record.paid) {
                        OutlinedButton(
                            onClick = onPaid,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp,
                                vertical = 0.dp
                            ),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("\u00d6dendi", fontSize = 10.sp)
                        }
                    }

                    TextButton(
                        onClick = onDelete,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 7.dp,
                            vertical = 0.dp
                        ),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Sil", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private fun moneyStatic(value: Double): String =
    NumberFormat.getNumberInstance(Locale("tr", "TR")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }.format(value) + " \u20ba"

@Composable
private fun CalendarCard(
    month: YearMonth,
    records: List<BudgetRecord>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val days = month.lengthOfMonth()
    val firstColumn = month.atDay(1).dayOfWeek.value - 1

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEAE6EE)
        )
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(
                "Ayl\u0131k Takvim",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(2.dp))

            Row(Modifier.fillMaxWidth()) {
                listOf("Pzt", "Sal", "\u00c7ar", "Per", "Cum", "Cmt", "Paz")
                    .forEach {
                        Box(
                            Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                it,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
            }

            Spacer(Modifier.height(3.dp))

            var day = 1
            while (day <= days) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(29.dp)
                ) {
                    for (column in 0..6) {
                        val position =
                            if (day == 1) column else firstColumn + day - 1

                        if (position < firstColumn && day == 1) {
                            Spacer(Modifier.weight(1f))
                        } else if (day <= days) {
                            val current = month.atDay(day)
                            val selected = current == selectedDate
                            val hasIncome = records.any {
                                it.date == current.toString() &&
                                    it.type == "Gelir"
                            }
                            val hasExpense = records.any {
                                it.date == current.toString() &&
                                    it.type == "Gider"
                            }

                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .padding(1.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected)
                                            Color(0xFFD7C6E5)
                                        else
                                            Color.Transparent
                                    )
                                    .clickable {
                                        onDateSelected(current)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        day.toString(),
                                        fontSize = 10.sp,
                                        fontWeight =
                                            if (selected)
                                                FontWeight.Bold
                                            else
                                                FontWeight.Normal
                                    )
                                    Row(
                                        horizontalArrangement =
                                            Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (hasIncome) Dot(Color(0xFF65A93B))
                                        if (hasExpense) Dot(Color(0xFFE53935))
                                    }
                                }
                            }
                            day++
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(Color(0xFF65A93B))
                Spacer(Modifier.width(4.dp))
                Text("Gelir", fontSize = 10.sp)
                Spacer(Modifier.width(12.dp))
                Dot(Color(0xFFE53935))
                Spacer(Modifier.width(4.dp))
                Text("Gider", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun AddRecordDialog(
    type: String,
    amount: String,
    note: String,
    installment: String,
    selectedDate: LocalDate,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onInstallmentChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (type == "Gelir") "Gelir Ekle" else "\u00d6deme Ekle",
                fontSize = 19.sp
            )
        },
        text = {
            Column {
                Text(
                    "Tarih: ${dateText(selectedDate.toString())}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Tutar") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )

                Spacer(Modifier.height(3.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text(
                            if (type == "Gelir")
                                "Gelir ad\u0131"
                            else
                                "Kime / ne i\u00e7in?"
                        )
                    }
                )

                if (type == "Gider") {
                    Spacer(Modifier.height(3.dp))
                    OutlinedTextField(
                        value = installment,
                        onValueChange = onInstallmentChange,
                        Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Taksit (iste\u011fe ba\u011fl\u0131)") },
                        placeholder = { Text("\u00d6rn. 3/6") }
                    )
                }

                Spacer(Modifier.height(3.dp))

                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                onDateChange(
                                    LocalDate.of(year, month + 1, day)
                                )
                            },
                            selectedDate.year,
                            selectedDate.monthValue - 1,
                            selectedDate.dayOfMonth
                        ).show()
                    },
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Tarihi De\u011fi\u015ftir", fontSize = 10.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("\u0130ptal")
            }
        }
    )
}
