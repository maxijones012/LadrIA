
package com.softwareparatodos.ladria

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservationGuideTest {
    @Test fun doesNotInventEmotion() {
        val result = ObservationGuide.build(true, listOf("Ladridos"), "Jugando", "Cuerpo rígido")
        assertTrue(result.contains("Contexto informado por la persona"))
        assertTrue(result.contains("Postura informada manualmente"))
        assertTrue(result.contains("No se infieren emociones"))
        assertFalse(result.contains("está agresivo"))
    }
    @Test fun abstainsWithoutEvidence() {
        val result = ObservationGuide.build(false, emptyList(), "Sin contexto", "No observada")
        assertTrue(result.contains("insuficiente"))
    }
    @Test fun avoidsSameAnimalAssumption() {
        val result = ObservationGuide.build(true, listOf("Gemidos"), "Durante paseo", "No observada")
        assertTrue(result.contains("distintos animales"))
    }
}
