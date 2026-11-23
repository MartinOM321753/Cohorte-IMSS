package imss.gob.mx.cohorte.services.solicitud.security;

import imss.gob.mx.cohorte.controllers.solicitud.dto.SolicitudAccesoDatosRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.net.IDN;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class SolicitudInputValidator {
    private static final Pattern NAME = Pattern.compile("[\\p{L}\\p{M}][\\p{L}\\p{M} .'’\\-]*");
    private static final Pattern ORGANIZATION = Pattern.compile("[\\p{L}\\p{M}\\p{N} .,'’&()/+:#ºª%\\-]+");
    private static final Pattern LOCAL = Pattern.compile("[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+");
    private static final Pattern TAG = Pattern.compile("<\\s*/?\\s*[A-Za-z!][^>]*>");
    private static final Set<String> PURPOSES = Set.of("academic", "thesis", "teaching", "policy", "journalism", "other");
    private static final Set<String> PRODUCTS = Set.of("article", "thesis", "report", "conference", "other", "undefined");
    public static SolicitudSecurityException invalid(String message) {
        return new SolicitudSecurityException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message);
    }
    public static String text(String value, int min, int max, boolean multiline, String label) {
        String s = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC).strip();
        if (s.length() < min || s.length() > max) throw invalid(label + ": revisa la longitud del texto.");
        for (int c : s.codePoints().toArray()) {
            int type = Character.getType(c);
            if ((Character.isISOControl(c) && !(multiline && (c == '\n' || c == '\r' || c == '\t')))
                    || type == Character.FORMAT || type == Character.SURROGATE
                    || type == Character.PRIVATE_USE || type == Character.UNASSIGNED) {
                throw invalid(label + ": contiene caracteres no permitidos.");
            }
        }
        if (TAG.matcher(s).find()) throw invalid(label + ": utiliza texto sin etiquetas HTML.");
        return s;
    }
    public static String email(String raw) {
        String value = text(raw, 5, 180, false, "Correo");
        int at = value.lastIndexOf('@');
        if (at < 1 || at != value.indexOf('@')) throw invalid("Escribe un correo electrónico válido.");
        String local = value.substring(0, at);
        String domain;
        try { domain = IDN.toASCII(value.substring(at + 1), IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT); }
        catch (IllegalArgumentException e) { throw invalid("Escribe un correo electrónico válido."); }
        if (local.length() > 64 || !LOCAL.matcher(local).matches() || local.startsWith(".")
                || local.endsWith(".") || local.contains("..") || domain.length() > 253 || !domain.contains(".")) {
            throw invalid("Escribe un correo electrónico válido.");
        }
        String[] labels = domain.split("\\.", -1);
        for (String label : labels) if (!label.matches("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")) throw invalid("Escribe un correo electrónico válido.");
        if (labels[labels.length - 1].matches("[0-9]+")) throw invalid("Escribe un correo electrónico válido.");
        String normalized = local + "@" + domain;
        if (normalized.length() > 180) throw invalid("El correo es demasiado largo.");
        return normalized;
    }
    public void validate(SolicitudAccesoDatosRequestDTO dto, Instant now) {
        String name = text(dto.getFullName(), 2, 140, false, "Nombre");
        if (!NAME.matcher(name).matches() || name.codePoints().filter(Character::isLetter).count() < 2) throw invalid("El nombre admite letras, espacios, acentos, puntos, guiones y apóstrofos.");
        dto.setFullName(name);
        String org = text(dto.getOrganization(), 3, 180, false, "Institución");
        if (!ORGANIZATION.matcher(org).matches()) throw invalid("La institución contiene caracteres no permitidos.");
        dto.setOrganization(org);
        dto.setEmail(email(dto.getEmail()));
        if (dto.getPhone() == null || !dto.getPhone().matches("[0-9]{10}") || dto.getPhone().matches("([0-9])\\1{9}")) throw invalid("El teléfono debe tener diez dígitos válidos.");
        if (!PURPOSES.contains(dto.getPurpose())) throw invalid("Selecciona un propósito válido.");
        dto.setPurposeOther("other".equals(dto.getPurpose()) ? text(dto.getPurposeOther(), 5, 180, false, "Otro propósito") : null);
        dto.setProject(text(dto.getProject(), 30, 2400, true, "Proyecto"));
        dto.setRequestedInformation(text(dto.getRequestedInformation(), 20, 1800, true, "Información solicitada"));
        List<String> products = dto.getProducts();
        if (products == null || products.isEmpty() || products.size() > 6 || new HashSet<>(products).size() != products.size()
                || !PRODUCTS.containsAll(products) || (products.contains("undefined") && products.size() > 1)) throw invalid("Selecciona productos válidos.");
        dto.setProductOther(products.contains("other") ? text(dto.getProductOther(), 3, 180, false, "Otro producto") : null);
        if (!Boolean.TRUE.equals(dto.getLocationConsent()) || dto.getLatitude() == null || dto.getLongitude() == null
                || dto.getLocationAccuracy() == null || dto.getLocationCapturedAt() == null) throw invalid("Comparte tu ubicación y autoriza su uso antes de enviar.");
        if (!Double.isFinite(dto.getLatitude()) || !Double.isFinite(dto.getLongitude()) || !Double.isFinite(dto.getLocationAccuracy())
                || Math.abs(dto.getLatitude()) > 90 || Math.abs(dto.getLongitude()) > 180
                || dto.getLocationAccuracy() <= 0 || dto.getLocationAccuracy() > 100000) throw invalid("La ubicación recibida no es válida.");
        long age = now.toEpochMilli() - dto.getLocationCapturedAt();
        if (age < -60000 || age > 300000) throw invalid("Actualiza tu ubicación antes de enviar.");
        if (dto.getEmailVerificationToken() == null || !dto.getEmailVerificationToken().matches("[A-Za-z0-9_-]{43}")) throw invalid("Verifica tu correo antes de enviar.");
    }
}
