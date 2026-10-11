
package com.softwareparatodos.ladria

/** La postura y el contexto los elige una persona, no los detecta la IA. */
object ObservationGuide {
    val situations = listOf("Sin contexto", "Jugando", "Durante paseo", "Visitantes", "Descansando", "Otro")
    val postures = listOf("No observada", "Cuerpo relajado", "Cuerpo rígido", "Cola en movimiento", "Orejas hacia atrás")

    fun build(dogSeen: Boolean, soundNames: List<String>, situation: String, posture: String): String {
        val observed = soundNames.distinct().take(4)
        val audio = if (observed.isEmpty()) "No se detectaron vocalizaciones distinguibles." else
            "Audio detectado por el modelo: " + observed.joinToString(", ") + "."
        val video = if (dogSeen) "Se reconoció probable presencia de perro en cuadros."
            else "No se confirmó presencia de perro en los cuadros examinados."
        val userSituation = if (situation in situations && situation != "Sin contexto")
            "Contexto informado por la persona: " + situation + "."
            else "No se informó el contexto."
        val userPosture = if (posture in postures && posture != "No observada")
            "Postura informada manualmente: " + posture + "."
            else "La postura no fue evaluada."
        val combined = if (observed.isEmpty() || !dogSeen)
            "Evidencia conjunta insuficiente para relacionar sonidos e imágenes."
            else "Audio e imágenes son compatibles con un perro, pero pueden corresponder a distintos animales."
        return listOf(audio, video, userSituation, userPosture, combined,
            "No se infieren emociones, agresividad, miedo, intención ni estado médico.").joinToString("\n")
    }
}
