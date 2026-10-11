
package com.softwareparatodos.ladria

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SavedObservation(val id: String, val timestamp: Long, val dogName: String, val situation: String, val posture: String, val text: String)

/** Registros locales: solo texto, nunca multimedia. Android backup desactivado. */
object LocalNotebook {
    private const val PREFS = "ladria_private_notebook_v1"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun dogs(ctx: Context): List<String> = try {
        val arr = JSONArray(prefs(ctx).getString("dogs", "[]"))
        (0 until arr.length()).map { arr.optString(it).trim().take(40) }.filter { it.isNotEmpty() }.distinct()
    } catch (_: Exception) { emptyList() }

    fun saveDog(ctx: Context, name: String): List<String> {
        val trimmed = name.trim().take(40)
        if (trimmed.isEmpty()) return dogs(ctx)
        val updated = (dogs(ctx) + trimmed).distinctBy { it.lowercase(Locale.ROOT) }.take(20)
        prefs(ctx).edit().putString("dogs", JSONArray(updated).toString()).apply()
        return updated
    }

    fun deleteDog(ctx: Context, name: String): List<String> {
        val updated = dogs(ctx).filterNot { it.equals(name, ignoreCase=true) }
        prefs(ctx).edit().putString("dogs", JSONArray(updated).toString()).apply()
        return updated
    }

    fun records(ctx: Context): List<SavedObservation> = try {
        val arr = JSONArray(prefs(ctx).getString("records", "[]"))
        (0 until arr.length()).mapNotNull { i ->
            val row = arr.optJSONObject(i) ?: return@mapNotNull null
            SavedObservation(row.optString("id"), row.optLong("timestamp"),
                row.optString("dogName"), row.optString("situation"),
                row.optString("posture"), row.optString("text").take(4000))
        }
    } catch (_: Exception) { emptyList() }

    @Synchronized
    fun add(ctx: Context, name: String, situation: String, posture: String, report: VideoReport, tone: TranslationTone = TranslationTone.TIERNO): List<SavedObservation> {
        val assessment = ObservationGuide.build(
            report.dogFrames > 0, report.sounds.map { it.label }, situation, posture)
        val voice = DogTalk.imagine(report.sounds, situation, posture, tone)?.let {
            "\nVoz imaginaria (ficción, no traducción): “" + it.phrase + "”" +
                "\n" + it.caution
        } ?: ""
        val evidence = assessment + voice + "\nCuadros revisados: " + report.framesChecked +
            ". Segundos de audio: " + report.audioSecondsInspected + "." +
            (if (report.audioError != null) "\nAudio: " + report.audioError.take(80) else "") +
            (if (report.visualError != null) "\nImagen: " + report.visualError.take(80) else "")
        val item = SavedObservation(UUID.randomUUID().toString(), System.currentTimeMillis(),
            name.trim().take(40), situation.take(80), posture.take(80), evidence.take(4000))
        val all = (listOf(item) + records(ctx)).take(60)
        val arr = JSONArray()
        all.forEach {
            arr.put(JSONObject().put("id",it.id).put("timestamp",it.timestamp)
                .put("dogName",it.dogName).put("situation",it.situation)
                .put("posture",it.posture).put("text",it.text))
        }
        prefs(ctx).edit().putString("records", arr.toString()).apply()
        return all
    }

    fun clear(ctx: Context): List<SavedObservation> {
        prefs(ctx).edit().remove("records").apply()
        return emptyList()
    }

    fun shareText(item: SavedObservation): String {
        val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es","AR")).format(Date(item.timestamp))
        return "LadrIA - Observación local\nFecha: " + stamp +
            "\nPerro: " + item.dogName.ifBlank { "Sin nombre" } +
            "\nContexto (manual): " + item.situation + "\nPostura (manual): " +
            item.posture + "\n\n" + item.text +
            "\n\nAnálisis experimental, no es una traducción ni diagnóstico."
    }
}
