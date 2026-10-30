package com.ivanko.phonedna

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleWorker()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { PhoneDNAApp() }
    }

    private fun scheduleWorker() {
        val request = PeriodicWorkRequestBuilder<RuleWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("phone_dna_rule_worker", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

@Composable
private fun PhoneDNAApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var rules by remember { mutableStateOf(RuleStore.all(context)) }
    var phrase by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    var trigger by remember { mutableStateOf("time") }
    var action by remember { mutableStateOf("notification") }
    var title by remember { mutableStateOf("Напомнить о важном") }
    var time by remember { mutableStateOf("21:00") }
    val bg = Color(0xFF090D18)
    val card = Color(0xFF171E30)
    val accent = Color(0xFF9A8CFF)

    MaterialTheme(colorScheme = darkColorScheme(
        primary = accent, secondary = Color(0xFF71E1D0),
        background = bg, surface = card, onBackground = Color(0xFFF2F3FF),
        onSurface = Color(0xFFF2F3FF)
    )) {
        Scaffold(containerColor = bg, bottomBar = {
            NavigationBar(containerColor = card) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.AutoAwesome, null) }, label = { Text("Правила") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Конструктор") })
            }
        }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF27284B), Color(0xFF101A2A))), RoundedCornerShape(26.dp)).padding(22.dp)) {
                        Text("PHONE DNA", style = MaterialTheme.typography.labelLarge, color = Color(0xFFBDB5FF))
                        Spacer(Modifier.height(8.dp))
                        Text(if (tab == 0) "Твой телефон. Твои правила." else "Создай автоматизацию.", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(6.dp))
                        Text("Локальные сценарии без root", color = Color(0xFFB9C1D8))
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = Color(0xFF71E1D0))
                            Spacer(Modifier.width(8.dp))
                            Text("${rules.count { it.enabled }} активных правил")
                        }
                    }
                }
                if (tab == 0) {
                    item { Text("Твои правила", style = MaterialTheme.typography.titleLarge) }
                    if (rules.isEmpty()) item {
                        Column(Modifier.fillMaxWidth().background(card, RoundedCornerShape(20.dp)).padding(20.dp)) {
                            Text("Пока здесь тихо", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("Создай первое правило — например, ежедневное напоминание в 21:00.", color = Color(0xFFB9C1D8))
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { tab = 1 }) { Text("Создать правило") }
                        }
                    }
                    items(rules, key = { it.id }) { rule ->
                        Card(colors = CardDefaults.cardColors(containerColor = card), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(rule.title, style = MaterialTheme.typography.titleMedium)
                                        Text(if (rule.trigger == "time") "Каждый день · ${rule.time}" else rule.trigger, color = Color(0xFFB9C1D8))
                                    }
                                    Switch(checked = rule.enabled, onCheckedChange = { enabled ->
                                        rules = rules.map { if (it.id == rule.id) it.copy(enabled = enabled) else it }
                                        RuleStore.save(context, rules)
                                    })
                                    IconButton(onClick = { rules = rules.filterNot { it.id == rule.id }; RuleStore.save(context, rules) }) {
                                        Icon(Icons.Default.Delete, "Удалить правило", tint = Color(0xFFFF8797))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Text("Быстрое создание", style = MaterialTheme.typography.titleLarge)
                        Text("Напиши правило своими словами. Пока поддерживаются ежедневные напоминания со временем, например: «Каждый день в 21:00 напомни проверить заряд».", color = Color(0xFFB9C1D8))
                        OutlinedTextField(value = phrase, onValueChange = { phrase = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Опиши правило") }, minLines = 2, shape = RoundedCornerShape(16.dp))
                        Button(onClick = {
                            val parsed = RuleStore.parse(phrase)
                            if (parsed != null) {
                                rules = rules + parsed
                                RuleStore.save(context, rules)
                                phrase = ""
                                tab = 0
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Разобрать и сохранить") }
                        Divider(color = Color(0xFF303951))
                    }
                    item { Text("Или собери из блоков", style = MaterialTheme.typography.titleLarge) }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = card), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("КОГДА", color = Color(0xFFBDB5FF), style = MaterialTheme.typography.labelLarge)
                                Text("Каждый день в указанное время")
                                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Время (ЧЧ:ММ)") }, singleLine = true)
                                Text("ВЫПОЛНИТЬ", color = Color(0xFF71E1D0), style = MaterialTheme.typography.labelLarge)
                                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Текст напоминания") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                Button(onClick = {
                                    val valid = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$").matches(time)
                                    if (valid && title.isNotBlank()) {
                                        val normalized = time.split(":").let { "%02d:%02d".format(it[0].toInt(), it[1].toInt()) }
                                        val rule = PhoneRule(java.util.UUID.randomUUID().toString(), title.trim(), "time", normalized)
                                        rules = rules + rule
                                        RuleStore.save(context, rules)
                                        title = "Напомнить о важном"
                                        tab = 0
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Добавить правило") }
                                Text("События времени проверяются периодически; доставка может задерживаться из-за энергосбережения Android.", color = Color(0xFFB9C1D8), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item { Text("Phone DNA · MVP 0.1.0", color = Color(0xFF77819D), style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}
