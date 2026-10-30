package com.ivanko.phonedna

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PhoneRule(
    val id: String,
    val title: String,
    val trigger: String,
    val time: String = "21:00",
    val action: String = "notification",
    val enabled: Boolean = true,
    val lastRunDay: String = ""
)

object RuleStore {
    private const val PREFS = "phone_dna_rules"
    private const val KEY = "rules"

    fun all(context: Context): List<PhoneRule> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                PhoneRule(
                    id = o.optString("id"), title = o.optString("title"),
                    trigger = o.optString("trigger", "time"), time = o.optString("time", "21:00"),
                    action = o.optString("action", "notification"), enabled = o.optBoolean("enabled", true),
                    lastRunDay = o.optString("lastRunDay", "")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun save(context: Context, rules: List<PhoneRule>) {
        val array = JSONArray()
        rules.forEach { r ->
            array.put(JSONObject().put("id", r.id).put("title", r.title).put("trigger", r.trigger)
                .put("time", r.time).put("action", r.action).put("enabled", r.enabled)
                .put("lastRunDay", r.lastRunDay))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    fun parse(text: String): PhoneRule? {
        val value = text.trim()
        if (value.isBlank()) return null
        val regex = Regex("(?:в|at)\\s*(\\d{1,2}:\\d{2})", RegexOption.IGNORE_CASE)
        val time = regex.find(value)?.groupValues?.get(1)?.let {
            val parts = it.split(":")
            val h = parts[0].toIntOrNull() ?: return@let null
            val m = parts[1].toIntOrNull() ?: return@let null
            if (h in 0..23 && m in 0..59) "%02d:%02d".format(h, m) else null
        }
        val trigger = if (time != null || value.contains("каждый день", true) || value.contains("every day", true)) "time" else return null
        val title = value.replace(Regex("(?i)каждый день\\s*"), "")
            .replace(Regex("(?i)напомни( мне)?"), "").trim().ifBlank { "Напоминание Phone DNA" }
        return PhoneRule(java.util.UUID.randomUUID().toString(), title, trigger, time ?: "21:00")
    }
}
