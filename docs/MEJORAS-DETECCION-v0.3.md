# LadrIA v0.3 — Detección más sensible y resultados temporales

## Problema verificado en código v0.2
El clasificador limitaba resultados a 12 de las 521 categorías generales de YAMNet **antes** de buscar las categorías de sonidos caninos. Así podía omitir sonidos caninos presentes en el resto de resultados.

## Correcciones
- `setMaxResults(-1)` consulta todas las categorías del modelo.
- Mapeo estricto de etiquetas YAMNet: Bark, Bow-wow, Yip, Howl, Growling, Whimper (dog) y Dog. Se evita confundir "Whimper" genérico con "Whimper (dog)".
- Selector de sensibilidad Normal (>= 0.16) y Sensible (>= 0.09). Para "Dog" genérico, mínimo 0.20. Los umbrales son heurísticos de prueba, **no calibración científica**.
- Micrófono: lista temporal de señales recientes, con deduplicación por tipo y un intervalo de 3 segundos.
- Video: hasta 12 cuadros, hasta 30 segundos de audio; conserva el momento (segundos) de los sonidos detectados y muestra el rango realmente examinado.
- Patrón explícito: ladridos observados en varios segundos; **no inferir emociones, causa, agresividad ni identidad del animal a partir de ello**.
- Análisis totalmente en dispositivo; publicidad real y gastos en créditos siguen desactivados.

## Limitaciones y pruebas pendientes
- No incluye aún un modelo de postura canina ni una clasificación fiable de emociones. Etiquetar un cuadro como "Dog" **no analiza cola, orejas ni expresiones**.
- El modo sensible puede aumentar falsos positivos; necesita validar con grabaciones etiquetadas de perros y otros ruidos.
- YAMNet no fue diseñado específicamente para comportamiento canino; hace falta benchmark por perro distinto y licencia de datasets especializada.
- No extrapolar a "video completo": la inspección está limitada a los primeros 30 segundos.
- Probar audio real suave, ladrido corto, aullido, silencio, humanos llorando, televisores, video sin audio y códecs distintos.
- La salida con scores de YAMNet no son porcentajes de intención, estrés o miedo.
