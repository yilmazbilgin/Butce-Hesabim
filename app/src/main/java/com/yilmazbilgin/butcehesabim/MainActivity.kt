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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BudgetRecord(
    val id: Long,
    val type: String,
    val title: String,
    val amount: Double,
    val date: String,
    val installment: String,
    val paid: Boolean
)

class MainActivity : ComponentActivity() {

    private val preferences by lazy {
        getSharedPreferences("butce_hesabim", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF6B4FA3),
                    secondary = Color(0xFF7659A8)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ButceHesabim()
                }
            }
        }
    }

    private fun loadRecords(): MutableList<BudgetRecord> {
        val result = mutableListOf<BudgetRecord>()

        val text = preferences.getString("records", "[]") ?: "[]"

        try {
            val array = JSONArray(text)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                result.add(
                    BudgetRecord(
                        id = obj.optLong("id"),
                        type = obj.optString("type"),
                        title = obj.optString("title"),
                        amount = obj.optDouble("amount"),
                        date = obj.optString("date"),
                        installment = obj.optString("installment"),
                        paid = obj.optBoolean("paid")
                    )
                )
            }
        } catch (_: Exception) {
        }

        return result
    }

    private fun saveRecords(records: List<BudgetRecord>) {
        val array = JSONArray()

        records.forEach { record ->
            val obj = JSONObject()

            obj.put("id", record.id)
            obj.put("type", record.type)
            obj.put("title", record.title)
            obj.put("amount", record.amount)
            obj.put("date", record.date)
            obj.put("installment", record.installment)
            obj.put("paid", record.paid)

            array.put(obj)
        }

        preferences.edit()
            .putString("records", array.toString())
            .apply()
    }

    private fun getIncome(): Double {
        return preferences.getFloat("income", 0f).toDouble()
    }

    private fun setIncome(value: Double) {
        preferences.edit()
            .putFloat("income", value.toFloat())
            .apply()
    }

    private fun formatMoney(value: Double): String {
        return String.format(
            Locale("tr", "TR"),
            "%,.2f ₺",
            value
        )
    }

    private fun formatDate(date: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val output = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

            output.format(input.parse(date)!!)
        } catch (_: Exception) {
            date
        }
    }

    private fun monthKey(calendar: Calendar): String {
        return String.format(
            Locale.US,
            "%04d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }

    private fun monthName(calendar: Calendar): String {
        return SimpleDateFormat(
            "MMMM yyyy",
            Locale("tr", "TR")
        ).format(calendar.time)
            .replaceFirstChar { it.uppercase() }
    }

    @Composable
    private fun ButceHesabim() {

        val records = remember {
            mutableStateListOf<BudgetRecord>().apply {
                addAll(loadRecords())
            }
        }

        var selectedMonth by remember {
            mutableStateOf(Calendar.getInstance())
        }

        var showIncomeDialog by remember {
            mutableStateOf(false)
        }

        var showExpenseDialog by remember {
            mutableStateOf(false)
        }

        var showAllRecords by remember {
            mutableStateOf(false)
        }

        val currentMonth = monthKey(selectedMonth)

        val monthRecords = records.filter {
            it.date.startsWith(currentMonth)
        }

        val incomeRecords = monthRecords.filter {
            it.type == "income"
        }

        val expenseRecords = monthRecords.filter {
            it.type == "expense"
        }

        val savedSalary = getIncome()

        val totalIncome =
            savedSalary + incomeRecords.sumOf { it.amount }

        val totalExpense =
            expenseRecords.sumOf { it.amount }

        val remaining =
            totalIncome - totalExpense

        val upcomingPayments = records
            .filter {
                it.type == "expense" &&
                        !it.paid &&
                        isUpcoming(it.date)
            }
            .sortedBy { it.date }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F7FA))
        ) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Column(
                        modifier = Modifier.padding(
                            horizontal = 20.dp
                        )
                    ) {

                        Text(
                            text = "💰 Bütçe Hesabım",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Gelir ve giderlerini kolayca takip et.",
                            fontSize = 16.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                item {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE9E4ED)
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween,
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text = monthName(selectedMonth),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row {

                                    TextButton(
                                        onClick = {
                                            val newCalendar =
                                                selectedMonth.clone()
                                                    as Calendar

                                            newCalendar.add(
                                                Calendar.MONTH,
                                                -1
                                            )

                                            selectedMonth =
                                                newCalendar
                                        }
                                    ) {
                                        Text("‹")
                                    }

                                    TextButton(
                                        onClick = {
                                            val newCalendar =
                                                selectedMonth.clone()
                                                    as Calendar

                                            newCalendar.add(
                                                Calendar.MONTH,
                                                1
                                            )

                                            selectedMonth =
                                                newCalendar
                                        }
                                    ) {
                                        Text("›")
                                    }
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                text = "Toplam Gelir",
                                fontSize = 15.sp,
                                color = Color.DarkGray
                            )

                            Text(
                                text = formatMoney(totalIncome),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Toplam Gider",
                                fontSize = 15.sp,
                                color = Color.DarkGray
                            )

                            Text(
                                text = formatMoney(totalExpense),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Divider()

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                text = "Kalan",
                                fontSize = 16.sp
                            )

                            Text(
                                text = formatMoney(remaining),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remaining >= 0)
                                    Color(0xFF247A45)
                                else
                                    Color(0xFFC62828)
                            )
                        }
                    }
                }

                item {

                    CalendarCard(
                        calendar = selectedMonth,
                        records = records
                    )
                }

                item {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = "💰 Maaş / Gelir",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            var salaryText by remember {
                                mutableStateOf(
                                    if (savedSalary == 0.0)
                                        ""
                                    else
                                        savedSalary.toString()
                                )
                            }

                            OutlinedTextField(
                                value = salaryText,
                                onValueChange = {
                                    salaryText = it
                                        .filter {
                                            it.isDigit() ||
                                                    it == '.' ||
                                                    it == ','
                                        }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = {
                                    Text("Aylık maaş / sabit gelir")
                                },
                                placeholder = {
                                    Text("Örn: 66565")
                                }
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Button(
                                onClick = {

                                    val value =
                                        salaryText
                                            .replace(",", ".")
                                            .toDoubleOrNull()
                                            ?: 0.0

                                    setIncome(value)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Maaşı Kaydet")
                            }
                        }
                    }
                }

                item {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        Button(
                            onClick = {
                                showIncomeDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Gelir")
                        }

                        Button(
                            onClick = {
                                showExpenseDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Gider")
                        }
                    }
                }

                item {

                    Text(
                        text = "📅 Yaklaşan Ödemeler",
                        modifier = Modifier.padding(
                            horizontal = 20.dp
                        ),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (upcomingPayments.isEmpty()) {

                    item {

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {

                            Text(
                                text = "Yaklaşan bekleyen ödeme yok.",
                                modifier = Modifier.padding(18.dp),
                                fontSize = 16.sp
                            )
                        }
                    }

                } else {

                    items(
                        upcomingPayments.take(5),
                        key = { it.id }
                    ) { record ->

                        PaymentCard(
                            record = record,
                            onPaid = {

                                val index =
                                    records.indexOfFirst {
                                        it.id == record.id
                                    }

                                if (index >= 0) {

                                    records[index] =
                                        records[index].copy(
                                            paid = true
                                        )

                                    saveRecords(records)
                                }
                            },
                            onDelete = {

                                records.removeAll {
                                    it.id == record.id
                                }

                                saveRecords(records)
                            }
                        )
                    }
                }

                item {

                    TextButton(
                        onClick = {
                            showAllRecords = !showAllRecords
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {

                        Text(
                            if (showAllRecords)
                                "Kayıtları Gizle"
                            else
                                "📋 Tüm Kayıtları Göster"
                        )
                    }
                }

                if (showAllRecords) {

                    items(
                        monthRecords
                            .sortedByDescending { it.date },
                        key = { it.id }
                    ) { record ->

                        PaymentCard(
                            record = record,
                            onPaid = {

                                val index =
                                    records.indexOfFirst {
                                        it.id == record.id
                                    }

                                if (index >= 0) {

                                    records[index] =
                                        records[index].copy(
                                            paid = true
                                        )

                                    saveRecords(records)
                                }
                            },
                            onDelete = {

                                records.removeAll {
                                    it.id == record.id
                                }

                                saveRecords(records)
                            }
                        )
                    }
                }

                item {
                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
        }

        if (showIncomeDialog) {

            AddRecordDialog(
                title = "Gelir Ekle",
                type = "income",
                onDismiss = {
                    showIncomeDialog = false
                },
                onSave = { title, amount, date, installment ->

                    records.add(
                        BudgetRecord(
                            id = System.currentTimeMillis(),
                            type = "income",
                            title = title,
                            amount = amount,
                            date = date,
                            installment = installment,
                            paid = true
                        )
                    )

                    saveRecords(records)
                    showIncomeDialog = false
                }
            )
        }

        if (showExpenseDialog) {

            AddRecordDialog(
                title = "Gider / Ödeme Ekle",
                type = "expense",
                onDismiss = {
                    showExpenseDialog = false
                },
                onSave = { title, amount, date, installment ->

                    records.add(
                        BudgetRecord(
                            id = System.currentTimeMillis(),
                            type = "expense",
                            title = title,
                            amount = amount,
                            date = date,
                            installment = installment,
                            paid = false
                        )
                    )

                    saveRecords(records)
                    showExpenseDialog = false
                }
            )
        }
    }

    private fun isUpcoming(date: String): Boolean {

        return try {

            val format =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            val selected =
                format.parse(date) ?: return false

            val today =
                format.parse(
                    format.format(Date())
                ) ?: return false

            selected >= today

        } catch (_: Exception) {
            false
        }
    }

    @Composable
    private fun PaymentCard(
        record: BudgetRecord,
        onPaid: () -> Unit,
        onDelete: () -> Unit
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    if (record.paid)
                        Color(0xFFE7F4EA)
                    else
                        Color.White
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                if (record.type == "expense")
                                    "💸 ${record.title}"
                                else
                                    "💰 ${record.title}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text = formatMoney(record.amount),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "📅 ${formatDate(record.date)}"
                        )

                        if (record.installment.isNotBlank()) {

                            Text(
                                text =
                                    "🔢 Taksit: ${record.installment}"
                            )
                        }
                    }

                    Text(
                        text =
                            if (record.paid)
                                "🟢 Ödendi"
                            else
                                "🔴 Bekliyor",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!record.paid &&
                    record.type == "expense"
                ) {

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Button(
                            onClick = onPaid,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ödendi")
                        }

                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sil")
                        }
                    }

                } else {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kaydı Sil")
                    }
                }
            }
        }
    }

    @Composable
    private fun AddRecordDialog(
        title: String,
        type: String,
        onDismiss: () -> Unit,
        onSave: (
            String,
            Double,
            String,
            String
        ) -> Unit
    ) {

        var name by remember {
            mutableStateOf("")
        }

        var amountText by remember {
            mutableStateOf("")
        }

        var date by remember {
            mutableStateOf(
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(Date())
            )
        }

        var installment by remember {
            mutableStateOf("")
        }

        var error by remember {
            mutableStateOf("")
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {

                Column {

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text(
                                if (type == "expense")
                                    "Ödeme adı / kurum"
                                else
                                    "Gelir adı"
                            )
                        },
                        placeholder = {
                            Text(
                                if (type == "expense")
                                    "Örn: Kredi kartı"
                                else
                                    "Örn: Ek gelir"
                            )
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                                .filter {
                                    it.isDigit() ||
                                            it == '.' ||
                                            it == ','
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text("Tutar")
                        },
                        placeholder = {
                            Text("Örn: 15000")
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedButton(
                        onClick = {

                            val calendar =
                                Calendar.getInstance()

                            DatePickerDialog(
                                this@MainActivity,
                                { _, year, month, day ->

                                    date =
                                        String.format(
                                            Locale.US,
                                            "%04d-%02d-%02d",
                                            year,
                                            month + 1,
                                            day
                                        )
                                },
                                calendar.get(
                                    Calendar.YEAR
                                ),
                                calendar.get(
                                    Calendar.MONTH
                                ),
                                calendar.get(
                                    Calendar.DAY_OF_MONTH
                                )
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "📅 Tarih: ${formatDate(date)}"
                        )
                    }

                    if (type == "expense") {

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        OutlinedTextField(
                            value = installment,
                            onValueChange = {
                                installment = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = {
                                Text("Taksit")
                            },
                            placeholder = {
                                Text("Örn: 3/6")
                            }
                        )
                    }

                    if (error.isNotBlank()) {

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = error,
                            color = Color.Red
                        )
                    }
                }
            },
            confirmButton = {

                Button(
                    onClick = {

                        val amount =
                            amountText
                                .replace(",", ".")
                                .toDoubleOrNull()

                        if (name.isBlank()) {

                            error =
                                "Lütfen açıklama gir."

                            return@Button
                        }

                        if (amount == null ||
                            amount <= 0
                        ) {

                            error =
                                "Lütfen geçerli bir tutar gir."

                            return@Button
                        }

                        onSave(
                            name,
                            amount,
                            date,
                            installment
                        )
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Vazgeç")
                }
            }
        )
    }

    @Composable
    private fun CalendarCard(
        calendar: Calendar,
        records: List<BudgetRecord>
    ) {

        val year =
            calendar.get(Calendar.YEAR)

        val month =
            calendar.get(Calendar.MONTH)

        val firstDay =
            Calendar.getInstance().apply {
                set(
                    year,
                    month,
                    1
                )
            }

        val daysInMonth =
            firstDay.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )

        val firstWeekDay =
            (firstDay.get(Calendar.DAY_OF_WEEK) + 5) % 7

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier.padding(14.dp)
            ) {

                Text(
                    text = "📆 Aylık Takvim",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

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

                        Text(
                            text = it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                var day = 1

                while (day <= daysInMonth) {

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        for (column in 0..6) {

                            if (
                                (day == 1 &&
                                        column < firstWeekDay) ||
                                day > daysInMonth
                            ) {

                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                )

                            } else {

                                val dateKey =
                                    String.format(
                                        Locale.US,
                                        "%04d-%02d-%02d",
                                        year,
                                        month + 1,
                                        day
                                    )

                                val dayRecords =
                                    records.filter {
                                        it.date == dateKey
                                    }

                                val hasExpense =
                                    dayRecords.any {
                                        it.type ==
                                                "expense"
                                    }

                                val hasIncome =
                                    dayRecords.any {
                                        it.type ==
                                                "income"
                                    }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .padding(2.dp)
                                        .background(
                                            if (dayRecords.isNotEmpty())
                                                Color(0xFFEDE7F6)
                                            else
                                                Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {

                                    Text(
                                        text = day.toString(),
                                        fontSize = 13.sp,
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Row {

                                        if (hasIncome) {
                                            Text(
                                                text = "●",
                                                color =
                                                    Color(0xFF2E7D32),
                                                fontSize = 9.sp
                                            )
                                        }

                                        if (hasExpense) {
                                            Text(
                                                text = "●",
                                                color =
                                                    Color(0xFFC62828),
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }

                                day++
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "🟢 Gelir    🔴 Gider",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}
