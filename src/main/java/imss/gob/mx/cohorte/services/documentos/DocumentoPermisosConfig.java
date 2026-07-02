package imss.gob.mx.cohorte.services.documentos;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DocumentoPermisosConfig {

    public boolean puedeVerMetadata() {
        return tieneAuthority("DOCUMENTOS_VER_METADATA");
    }

    public boolean puedeDescargar() {
        return tieneAuthority("DOCUMENTOS_DESCARGAR");
    }

    public boolean puedeSubir() {
        return tieneAuthority("DOCUMENTOS_SUBIR");
    }

    public boolean puedeEliminar() {
        return tieneAuthority("DOCUMENTOS_ELIMINAR");
    }

    private boolean tieneAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(authority));
    }
}
