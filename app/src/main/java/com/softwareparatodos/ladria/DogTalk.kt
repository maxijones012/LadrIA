package com.softwareparatodos.ladria

/**
 * "Traductor" divertido y EXPLÍCITAMENTE FICTICIO.
 * Usa etiquetas acústicas producidas por YAMNet y contexto seleccionado por una persona.
 * No reconoce palabras, emociones, necesidades clínicas ni intenciones del perro.
 * No hay llamadas de red, modelo generativo remoto ni consumo de créditos.
 */
enum class TranslationTone(val title: String) {
    TIERNO("Tierno"), DIVERTIDO("Divertido"), TRANQUILO("Tranquilo")
}

data class PlayfulTranslation(
    val phrase: String,
    val evidence: String,
    val caution: String,
    val kind: String
)

object DogTalk {
    fun imagine(
        sounds: List<SoundFinding>,
        context: String,
        posture: String,
        tone: TranslationTone
    ): PlayfulTranslation? {
        // La clase genérica Dog no es una vocalización: no fabricar una frase.
        val sound = sounds
            .filter { it.label != "Sonido de perro" }
            .filter { it.label in setOf("Ladridos", "Ladridos agudos", "Aullidos", "Gruñidos", "Gemidos") }
            .maxByOrNull { it.score }
            ?: return null

        val rigid = posture == "Cuerpo rígido" // Declaración manual
        val playing = context == "Jugando"    // Declaración manual
        val visitors = context == "Visitantes" // Declaración manual

        val phrases: Triple<String, String, String> = when (sound.label) {
            "Gruñidos" -> Triple(
                "¿Me dejás un poquito de espacio, por favor?",
                "¡Alto ahí, humano! Dame mi burbujita personal.",
                "Quizás necesite algo de espacio ahora."
            )
            "Gemidos" -> Triple(
                "¿Me acompañás un ratito? Quiero que me prestes atención.",
                "¡Ejem! Solicito asistencia de mi humano favorito.",
                "Puede que esté buscando contacto o manifestando alguna molestia."
            )
            "Aullidos" -> Triple(
                "¡Auuuu! ¿Me escuchás desde ahí?",
                "¡Atención, vecindario! Arranca mi concierto.",
                "Estoy haciendo una vocalización larga; observá cuándo ocurre."
            )
            "Ladridos agudos" -> Triple(
                "¡Ey, ey! ¡Tengo algo para contarte!",
                "¡Guau, guau! ¡Tengo una noticia urgente!",
                "Estoy emitiendo sonidos breves y agudos; mirá qué pasa alrededor."
            )
            else -> when {
                playing -> Triple(
                    "¡Qué lindo jugar! ¿Seguimos un ratito más?",
                    "¡Otra ronda, humano! ¡Dale que no me canso!",
                    "Estoy ladrando mientras jugamos; podría estar participando de la actividad."
                )
                visitors -> Triple(
                    "¡Hola! ¡Hay gente nueva por acá!",
                    "¡Timbre perruno activado! ¡Tenemos visitas!",
                    "Estoy ladrando durante una visita; observá mi lenguaje corporal."
                )
                rigid -> Triple(
                    "Ey, algo me llama la atención. ¿Me das un momento?",
                    "¡Momento, humano! Necesito mirar qué está pasando.",
                    "Hay ladridos y se informó una postura rígida; evitá sacar conclusiones apresuradas."
                )
                else -> Triple(
                    "¡Guau! ¡Tengo algo que decirte!",
                    "¡Atención, humano! ¡Comunicado oficial de cuatro patas!",
                    "Estoy ladrando; observá el contexto para saber qué podría significar."
                )
            }
        }
        val text = when (tone) {
            TranslationTone.TIERNO -> phrases.first
            TranslationTone.DIVERTIDO -> phrases.second
            TranslationTone.TRANQUILO -> phrases.third
        }
        val caution = when (sound.label) {
            "Gruñidos" -> "Si está gruñendo, respetá su espacio. No te acerques solo por esta interpretación."
            "Gemidos" -> "Si los gemidos persisten o hay dolor, consultá a un veterinario."
            else -> "Es un ejemplo simpático, no una traducción literal ni una confirmación de su intención."
        }
        val contextNote = when {
            context == "Sin contexto" && posture == "No observada" -> "Sin contexto aportado."
            else -> "Contexto y postura: indicados por vos, no detectados por IA."
        }
        return PlayfulTranslation(
            phrase = text,
            evidence = "Se detectaron ${sound.label.lowercase()}. $contextNote",
            caution = caution,
            kind = sound.label
        )
    }
}
