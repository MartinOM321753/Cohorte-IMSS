package imss.gob.mx.cohorte.infrastructure.whatsapp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Cliente HTTP para WAHA — WhatsApp HTTP API (self-hosted, sin Redis ni DB).
 * https://waha.devlike.pro
 *
 * Endpoint de envío: POST /api/sendText
 * Body: { "session": "<nombre>", "chatId": "521234567890@c.us", "text": "..." }
 *
 * Cuando notificaciones.whatsapp.enabled=false solo loguea.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvolutionApiClient {

    private final RestTemplate restTemplate;

    @Value("${waha.api.url:http://localhost:8082}")
    private String apiUrl;

    @Value("${waha.api.key:}")
    private String apiKey;

    @Value("${waha.session:cohorte-clinica}")
    private String sessionName;

    @Value("${notificaciones.whatsapp.enabled:false}")
    private boolean enabled;

    public void enviarMensaje(String telefono, String mensaje) {
        if (!enabled) {
            log.info("[WHATSAPP DESACTIVADO] Para: {} | Msg: {}...",
                    telefono, mensaje.substring(0, Math.min(60, mensaje.length())));
            return;
        }

        String numero = normalizarTelefono(telefono);
        if (numero == null) {
            log.warn("WhatsApp omitido: teléfono inválido '{}'", telefono);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("X-Api-Key", apiKey);
        }

        // WAHA espera el número con sufijo @c.us
        Map<String, Object> body = Map.of(
                "session", sessionName,
                "chatId",  numero + "@c.us",
                "text",    mensaje
        );

        try {
            restTemplate.postForObject(
                    apiUrl + "/api/sendText",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            log.info("WhatsApp enviado a {} via WAHA", numero);
        } catch (Exception e) {
            log.error("Error enviando WhatsApp a {} via WAHA: {}", numero, e.getMessage());
            throw new RuntimeException("Error enviando WhatsApp: " + e.getMessage(), e);
        }
    }

    /**
     * Normaliza número mexicano a formato internacional (52 + 10 dígitos).
     * "5512345678" → "525512345678"
     */
    private String normalizarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) return null;
        String d = telefono.replaceAll("[^0-9]", "");
        if (d.length() == 10)                           return "52" + d;
        if (d.length() == 12 && d.startsWith("52"))     return d;
        if (d.length() == 13 && d.startsWith("521"))    return d;
        log.warn("Formato de teléfono no reconocido: '{}' (dígitos: '{}')", telefono, d);
        return null;
    }
}
