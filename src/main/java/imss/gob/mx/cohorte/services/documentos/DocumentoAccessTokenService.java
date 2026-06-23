package imss.gob.mx.cohorte.services.documentos;

import imss.gob.mx.cohorte.modules.documentos.Documento;
import imss.gob.mx.cohorte.modules.documentos.DocumentoAccessToken;
import imss.gob.mx.cohorte.modules.documentos.DocumentoAccessTokenRepository;
import imss.gob.mx.cohorte.modules.documentos.DocumentoRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class DocumentoAccessTokenService {

    private static final int TOKEN_EXPIRY_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DocumentoAccessTokenRepository tokenRepository;
    private final DocumentoRepository documentoRepository;
    private final InstitucionContextService institucionCtx;

    @Transactional
    public DocumentoAccessToken generarToken(String etiqueta) {
        Documento doc = documentoRepository.findByEtiqueta(etiqueta)
                .orElseThrow(() -> new ObjNotFoundException("Documento no encontrado con etiqueta: " + etiqueta));

        if (doc.getIdInstitucion() != null) {
            institucionCtx.verificarPerteneceOAncestra(doc.getIdInstitucion());
        }

        String tokenStr = generarTokenSeguro();
        LocalDateTime ahora = LocalDateTime.now();

        DocumentoAccessToken token = new DocumentoAccessToken();
        token.setToken(tokenStr);
        token.setIdDocumento(doc.getId());
        token.setIdInstitucion(institucionCtx.getIdInstitucionActual());
        token.setUsuarioUuid(institucionCtx.getUsuarioActual().getUUID());
        token.setFechaCreacion(ahora);
        token.setFechaExpiracion(ahora.plusMinutes(TOKEN_EXPIRY_MINUTES));

        return tokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public Documento validarTokenYObtenerDocumento(String tokenStr) {
        DocumentoAccessToken token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new ObjNotFoundException("Token de acceso inválido o no encontrado"));

        if (token.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new AccessDeniedException("El enlace de visualización ha expirado (válido por "
                    + TOKEN_EXPIRY_MINUTES + " minutos)");
        }

        return documentoRepository.findById(token.getIdDocumento())
                .orElseThrow(() -> new ObjNotFoundException("El documento asociado al token ya no existe"));
    }

    @Scheduled(fixedRate = 600_000)
    @Transactional
    public void limpiarTokensExpirados() {
        tokenRepository.eliminarExpirados(LocalDateTime.now());
    }

    private String generarTokenSeguro() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
