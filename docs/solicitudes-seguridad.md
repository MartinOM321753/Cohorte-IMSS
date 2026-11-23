# Activación del formulario público

Implementación integrada en `develop` para pruebas locales. No se ha desplegado en producción ni configurado el proveedor VPN/proxy. El entorno de desarrollo local gestiona los campos nuevos mediante su configuración habitual de Hibernate; la migración SQL queda preparada para los otros entornos.

## Cambios que requiere el servidor

1. Aplicar, tras revisar el esquema y hacer respaldo, `src/main/resources/db/manual/2026-11-22_solicitudes_seguridad.sql`. Es una migración manual de una sola ejecución. Si el entorno de desarrollo ya creó estas columnas con Hibernate, no ejecutar el ALTER de nuevo. En producción usar `ddl-auto=validate`, no `update`.
2. Desplegar este backend junto con el frontend actualizado. Mantener `/api` en el mismo origen detrás del proxy; Vite usa el origen de `VITE_API_URL` únicamente en desarrollo. El backend anterior no implementa la verificación por código.
3. Activar HTTPS en el sitio público. La geolocalización funciona en contextos seguros y exige permiso explícito del visitante. No se necesita una API de mapas para obtener coordenadas.
4. Configurar SMTP existente (`MAIL_ENABLED=true`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`) y una clave aleatoria independiente de al menos 32 caracteres en `SOLICITUDES_VERIFICATION_SECRET`. No poner claves en variables `VITE_*` ni en el navegador. Configurar SPF/DKIM/DMARC del dominio remitente y comprobar la entrega antes de habilitar solicitudes.
5. Configurar el adaptador de VPN/proxy si se decide utilizar MaxMind **GeoIP Insights**: `SOLICITUDES_MAXMIND_ACCOUNT_ID` y `SOLICITUDES_MAXMIND_LICENSE_KEY`. Requiere cuenta/servicio compatible y salida HTTPS del backend hacia `geoip.maxmind.com:443`. Una licencia de GeoLite local no reemplaza este servicio. El adaptador `SolicitudNetworkCheck` permite sustituir el proveedor. Como se solicitó, esta configuración queda pendiente.
6. Definir `SOLICITUDES_TRUSTED_PROXIES` con las IP exactas de los proxies controlados, separadas por comas. El proxy de entrada debe sobrescribir el encabezado de origen; los saltos internos deben anexar correctamente la IP. Bloquear el acceso público directo al puerto del backend. No confiar en cualquier `X-Forwarded-For`. `server.forward-headers-strategy=none` evita una segunda interpretación global. Revisar redirecciones/OAuth si la infraestructura dependía de esa propiedad. Ajustar `CORS_ORIGINS` al dominio autorizado, sin comodines.

Fuera del modo de prueba local explícito, sin credenciales del proveedor, SMTP o clave de verificación, el formulario falla cerrado: responde 503 y no acepta solicitudes. Las IP privadas/locales tampoco se envían al proveedor.

## Prueba manual local en 5173

El frontend de `develop` se sirve en `http://localhost:5173`, con `/api` reenviado al backend de desarrollo en 8081. En el archivo local ignorado `application-dev.properties` se configura una clave independiente de verificación y `solicitudes.security.local-testing=true`. Este permiso solo funciona con perfil `dev` o `test`, nunca con `prod`, y únicamente para IP de origen loopback. En ese caso la revisión de VPN se registra como **Prueba local; VPN sin verificar** (país `ZZ`) y Turnstile usa sus claves oficiales de prueba. El correo sigue siendo real: no se devuelve ni se muestra el código en la API. El campo de código siempre está visible y se habilita después del envío. Se comprobó conexión TLS y autenticación SMTP sin enviar mensajes.

Desactivar el modo local o retirar esa configuración al probar la detección VPN real. No habilitarlo detrás de un proxy que convierta tráfico público en origen loopback; producción siempre debe usar perfil `prod` y claves reales.

## CAPTCHA final

Turnstile se carga solo al llegar al producto/último paso. El POST final exige `captchaToken` (máximo 2048 caracteres). El servidor valida el token con Siteverify y comprueba `success`, hostname autorizado y acción `data_request` antes de registrar. El token no reemplaza la verificación de correo ni los límites de intentos. Se reinicia después de cada POST final y la interfaz controla su caducidad.

Producción: configurar `SOLICITUDES_TURNSTILE_SITE_KEY`, `SOLICITUDES_TURNSTILE_SECRET_KEY` y `SOLICITUDES_TURNSTILE_HOSTNAMES` (hostnames exactos separados por comas). Las claves de prueba se rechazan fuera de loopback/dev y la ausencia de configuración bloquea el envío. Autorizar salida HTTPS hacia `challenges.cloudflare.com:443` y carga del script/iframe de ese dominio en la CSP del sitio. El proveedor recibe la IP para validar la conexión; no se envían los textos, correo ni coordenadas GPS a Turnstile.

## Qué comprueba cada control

- Validación en cliente y servidor: texto NFC, límites, nombres Unicode con acentos/apóstrofos/guiones, exclusión de controles invisibles y etiquetas HTML, teléfono de diez dígitos ASCII y no repetidos, listas de propósitos/productos permitidos. Mantiene la salida escapada y la persistencia JPA; las validaciones no sustituyen esos controles.
- Correo: sintaxis y dominio DNS, lista inicial de dominios desechables ampliable con `SOLICITUDES_BLOCKED_EMAIL_DOMAINS`, código de seis dígitos por SMTP. El código prueba acceso al buzón en ese momento; no certifica identidad ni que el remitente nunca enviará spam. DNS por sí solo no demuestra que un buzón exista.
- Código: hash HMAC en base de datos, diez minutos, cinco errores como máximo, tres emisiones por dirección/hora y cinco por IP/hora, intervalo mínimo de 60 segundos. Confirmación limitada a 30 intentos/IP/hora. El ticket confirmado dura 30 minutos, se vincula a correo e IP y se consume una sola vez bajo bloqueo de fila dentro de la transacción de registro. Cambiar de red puede exigir repetir la verificación.
- Ubicación: consentimiento y permiso de navegador obligatorios; se solicita nuevamente al enviar. Guarda coordenadas, precisión, fecha de captura y fecha de consentimiento. El servidor comprueba rangos, números finitos y antigüedad (máximo cinco minutos; tolerancia futura de un minuto). Una ubicación aproximada es válida; se registra su precisión. Estas coordenadas las proporciona el cliente y pueden falsificarse: no son prueba de identidad o residencia. Se muestran solo en el detalle administrativo autorizado y no se incluyen en los correos de notificación.
- Red: revisa IP de origen en servidor al pedir el código y al registrar. Consulta país y señales de VPN, proxy público/residencial, Tor y alojamiento; conserva proveedor y fecha de revisión. No inspecciona extensiones del navegador. La clasificación es probabilística y puede generar falsos positivos y negativos, especialmente en redes corporativas o móviles. No se afirma «sin VPN» como certeza. El proveedor recibe la IP, no las coordenadas GPS ni los textos de la solicitud.
- Abuso: honeypot, máximo tres solicitudes aceptadas/IP/hora, filtro JSON de 16 KiB, hasta 60 POST/IP/hora y cachés acotadas. Los límites de memoria son por instancia y se reinician al reiniciar el servicio. Para varias réplicas o ataques de volumen, añadir límites compartidos en gateway/Redis y límites de conexiones/cuerpo/tiempo en el proxy; la capa del formulario no evita DDoS por sí sola. Evitar registrar códigos, claves o cuerpos completos en logs/APM.

## Operación y verificación pendiente

Revisar el aviso de privacidad existente antes de activar la recogida de ubicación: finalidad, responsable, acceso y conservación acordes al flujo. Definir conservación de solicitudes y coordenadas en la operación del sistema. Los tickets caducados se purgan al solicitar nuevos códigos cuando llevan más de 24 horas vencidos; añadir una tarea programada si se necesita una purga independiente de uso.

Pruebas locales sin bases de datos ni envíos reales:

```powershell
./mvnw.cmd '-Dtest=SolicitudInputValidatorTest,SolicitudEmailVerificationTest,SolicitudNetworkSecurityTest,SolicitudCaptchaTest' test
```

Antes de desplegar: validar SQL contra una copia de la base, comprobar una entrega SMTP real autorizada, probar bloqueo/clasificación con el proveedor contratado y verificar la IP correcta a través de los proxies de producción. Probar desde móvil real permiso concedido/denegado con HTTPS. No se han ejecutado estas comprobaciones externas en esta tarea.

Referencias: [OWASP validación](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html), [OWASP correo](https://cheatsheetseries.owasp.org/cheatsheets/Email_Validation_and_Verification_Cheat_Sheet.html), [MDN geolocalización](https://developer.mozilla.org/en-US/docs/Web/API/Geolocation/getCurrentPosition), [MaxMind respuestas y señales de anonimización](https://dev.maxmind.com/geoip/docs/web-services/responses/).
