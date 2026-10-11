package com.softwareparatodos.ladria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DogTalkTest {
    @Test fun noSoundMeansNoInventedTranslation() {
        assertNull(DogTalk.imagine(emptyList(),"Jugando","Cola en movimiento",TranslationTone.TIERNO))
    }
    @Test fun genericDogLabelCannotBeTranslated() {
        assertNull(DogTalk.imagine(listOf(SoundFinding("Sonido de perro",0.9f)),
            "Jugando","No observada",TranslationTone.DIVERTIDO))
    }
    @Test fun playfulSpeechRemainsContextual() {
        val sounds = listOf(SoundFinding("Ladridos",0.35f))
        val play = DogTalk.imagine(sounds,"Jugando","No observada",TranslationTone.TIERNO)!!
        val strangers = DogTalk.imagine(sounds,"Visitantes","No observada",TranslationTone.TIERNO)!!
        assertNotEquals(play.phrase,strangers.phrase)
        assertTrue(play.evidence.contains("indicados por vos"))
        assertTrue(play.caution.contains("no una traducción literal"))
    }
    @Test fun growlingIsNotPresentedAsSafeToApproach() {
        val result = DogTalk.imagine(listOf(SoundFinding("Gruñidos",0.5f)),
            "Sin contexto","Cuerpo rígido",TranslationTone.DIVERTIDO)!!
        assertTrue(result.caution.contains("respetá su espacio"))
    }
    @Test fun whimperHasHealthDisclaimer() {
        val result = DogTalk.imagine(listOf(SoundFinding("Gemidos",0.4f)),
            "Descansando","No observada",TranslationTone.TIERNO)!!
        assertTrue(result.caution.contains("veterinario"))
    }
    @Test fun noEmotionPercentagesInGeneratedPhrase() {
        val result = DogTalk.imagine(listOf(SoundFinding("Aullidos",0.4f)),
            "Sin contexto","No observada",TranslationTone.TRANQUILO)!!
        assertFalse(result.phrase.contains("%"))
        assertEquals("Aullidos",result.kind)
    }
}
