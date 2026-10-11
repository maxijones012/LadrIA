# LadrIA v0.5 — Voz imaginaria del perro

## Nueva pantalla: «Si pudiera hablar…»

Se agrega una **representación creativa y agradable** de las categorías acústicas reconocidas por YAMNet, usando el contexto que la persona selecciona manualmente. La frase se escribe como si hablara un perro; **no se afirma traducción real, emoción real ni lectura de pensamientos**. Funciona 100% sin conexión ni nuevas credenciales.

Tonos disponibles:
- Tierno, por defecto: frases amables.
- Divertido: humor liviano.
- Tranquilo: tono descriptivo cercano.

Según YAMNet y contexto, una categoría Bark/Ladridos podría representarse como «¡Guau! ¡Tengo algo que decirte!» o, si la persona indicó que estaban jugando, «¡Qué lindo jugar! ¿Seguimos un ratito más?». Es **una posibilidad narrativa**, no una inferencia veterinaria.

Una etiqueta genérica «Dog» sin vocalización específica o el silencio **no producen una frase inventada**. En caso de gruñidos, se aconseja respetar el espacio. En gemidos persistentes o sospecha de dolor se recomienda consulta veterinaria.

## Seguridad, privacidad y monetización

- El generador usa reglas y plantillas locales, no un LLM remoto. Conectar un LLM sin datos caninos validados solo agrega fluidez, **no verdad**, e introduciría costo y privacidad adicional.
- Audio, video y texto de análisis permanecen en el dispositivo. No se cobran créditos por la interpretación creativa.
- La frase figura en pantalla durante escucha y junto al reporte de video y se incorpora como texto al historial en el momento de guardar.
- Contexto y postura SIEMPRE declarados manualmente. No se deduce intención ni emoción específica.
- Test unitarios obligatorios: sin datos => sin frase; etiqueta genérica => sin frase; gruñido/quejido con advertencia; estilos y contextos diferenciados.

Pendiente: validar textos mediante usuarios, probar UX con tamaños de letra accesibles y mejorar comprensión sin confundir entretenimento con interpretación veterinaria.
