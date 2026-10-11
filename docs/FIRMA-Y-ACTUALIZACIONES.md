# Firma, actualizaciones y publicación segura

LadrIA usa `applicationId=com.softwareparatodos.ladria`. No modificar después del primer APK release. Android solo permite reemplazar una instalación si firma y package coinciden y `versionCode` aumenta. Si una versión anterior estaba firmada con una clave de debug distinta, **no existe un truco para actualizar encima**: se necesita desinstalar (previo respaldo si aplica) o empezar con la clave definitiva.

## Crear una clave de publicación, una sola vez

En una máquina bajo control del propietario y con JDK instalado:

```bash
keytool -genkeypair -v -keystore ladria-release.jks -alias ladria -keyalg RSA -keysize 3072 -validity 10000
```

Resguardar `.jks` offline en dos ubicaciones cifradas; nunca subirlo al repo, enviarlo por chat ni compartir contraseñas. Verificar huella con `keytool -list -v -keystore ladria-release.jks`. Al perder la clave puede perderse la capacidad de actualizar todas las instalaciones fuera de Play.

## GitHub Actions Secrets (configurar manualmente)

`LADRIA_KEYSTORE_BASE64` (base64 del JKS de publicación), `LADRIA_STORE_PASSWORD`, `LADRIA_KEY_ALIAS`, `LADRIA_KEY_PASSWORD`. Secretos en Settings > Secrets and variables > Actions. No incluir los secretos en artefactos ni logs. Antes de generar release firmada, el workflow debe fallar si cualquiera de los cuatro falta.

## Ciclo release

1. Elegir `versionName` semántica (por ejemplo `0.2.0`) y un `versionCode` entero superior al anterior (por ejemplo 2). NO reutilizar.
2. Ejecutar manualmente workflow en GitHub, canal `release`, verificando keystore y modelo.
3. `assembleRelease`, `apksigner verify --print-certs`, conservar certificado y registrar SHA-256. Publicar un único APK firmado como asset en GitHub Releases.
4. En la app: Ajustes > Buscar actualizaciones; consultar GitHub Releases por HTTPS; mostrar nueva versión y enlace oficial. El usuario decide descargar/instalar mediante Android.
5. Probar en teléfono con versión anterior, instalar encima, comprobar datos y firma y luego distribuir.

Para APK de prueba, el canal `test` compila `assembleDebug` con clave debug efímera. **No tratar los APK debug de CI como una vía de actualizaciones**: pueden tener firmas diferentes por cada ejecución. La experiencia de actualización estable empieza en el primer release firmado con clave permanente.

En caso de distribuir por Google Play en el futuro, deberá ajustarse el canal a Play In-App Updates y políticas de distribución correspondientes; no usar el APK externo para eludirlas.
