package imss.gob.mx.cohorte.services.solicitud.security;

import imss.gob.mx.cohorte.controllers.solicitud.dto.SolicitudAccesoDatosRequestDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SolicitudInputValidatorTest {
    private final Instant now=Instant.parse("2026-11-22T12:00:00Z");
    private final SolicitudInputValidator validator=new SolicitudInputValidator();
    private SolicitudAccesoDatosRequestDTO valid() {
        var d=new SolicitudAccesoDatosRequestDTO();
        d.setFullName("María-José O’Neill");d.setOrganization("UNAM / Facultad de Medicina");
        d.setEmail("Research+cohorte@EXAMPLE.ORG");d.setPhone("5512345678");d.setPurpose("academic");
        d.setProject("Estudiaremos IMC <25 y hábitos mediante análisis longitudinal.");
        d.setRequestedInformation("Variables clínicas y de seguimiento de distintas fases.");
        d.setProducts(List.of("article"));d.setEmailVerificationToken("a".repeat(43));
        d.setLocationConsent(true);d.setLatitude(19.4);d.setLongitude(-99.1);d.setLocationAccuracy(30.0);d.setLocationCapturedAt(now.toEpochMilli());return d;
    }
    @Test void acceptsResearchPunctuationAndNormalizes() {
        var d=valid();d.setFullName("  Jose\u0301 Pérez  ");validator.validate(d,now);
        assertEquals("José Pérez",d.getFullName());assertEquals("Research+cohorte@example.org",d.getEmail());
        assertEquals("ana@xn--bcher-kva.de",SolicitudInputValidator.email("ana@bücher.de"));
    }
    @ParameterizedTest @ValueSource(strings={"Li","李明","María-José O’Neill"}) void acceptsNames(String name) {
        var d=valid();d.setFullName(name);assertDoesNotThrow(()->validator.validate(d,now));
    }
    @ParameterizedTest @ValueSource(strings={"Ana123","Ana\u202Etxt","Ana\u200B","<script>alert(1)</script>","😀😀"}) void rejectsUnsafeNames(String name) {
        var d=valid();d.setFullName(name);assertThrows(SolicitudSecurityException.class,()->validator.validate(d,now));
    }
    @ParameterizedTest @ValueSource(strings={"551234567","55123456789","+525512345678","55 12345678","0000000000","５５１２３４５６７８"}) void rejectsPhones(String phone) {
        var d=valid();d.setPhone(phone);assertThrows(SolicitudSecurityException.class,()->validator.validate(d,now));
    }
    @ParameterizedTest @ValueSource(strings={"a@@example.org","a@localhost",".a@example.org","a..b@example.org","a@-example.org","a@example..org","a@example.org/path","a@example.org\nBcc:x@example.org"}) void rejectsEmails(String email) {
        assertThrows(SolicitudSecurityException.class,()->SolicitudInputValidator.email(email));
    }
    @Test void rejectsHtmlAndControlsWithoutRejectingMath() {
        assertEquals("IMC <25",SolicitudInputValidator.text("IMC <25",1,100,true,"Proyecto"));
        assertThrows(SolicitudSecurityException.class,()->SolicitudInputValidator.text("<img src=x onerror=alert(1)>",1,100,true,"Proyecto"));
        assertThrows(SolicitudSecurityException.class,()->SolicitudInputValidator.text("a\u0000b",1,100,true,"Proyecto"));
    }
    @Test void rejectsUntrustedSelectionAndIncompatibleProducts() {
        var d=valid();d.setPurpose("forged");assertThrows(SolicitudSecurityException.class,()->validator.validate(d,now));
        var invalid=valid();invalid.setProducts(List.of("undefined","article"));assertThrows(SolicitudSecurityException.class,()->validator.validate(invalid,now));
    }
    @Test void requiresConsentFreshFiniteLocationAndVerifiedEmailTicket() {
        var d=valid();d.setLocationConsent(false);assertThrows(SolicitudSecurityException.class,()->validator.validate(d,now));
        var stale=valid();stale.setLocationCapturedAt(now.minusSeconds(301).toEpochMilli());assertThrows(SolicitudSecurityException.class,()->validator.validate(stale,now));
        var future=valid();future.setLocationCapturedAt(now.plusSeconds(61).toEpochMilli());assertThrows(SolicitudSecurityException.class,()->validator.validate(future,now));
        var forged=valid();forged.setLatitude(Double.NaN);assertThrows(SolicitudSecurityException.class,()->validator.validate(forged,now));
        var missing=valid();missing.setEmailVerificationToken(null);assertThrows(SolicitudSecurityException.class,()->validator.validate(missing,now));
    }
}
