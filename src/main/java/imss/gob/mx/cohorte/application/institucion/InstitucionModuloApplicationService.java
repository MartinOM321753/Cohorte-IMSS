package imss.gob.mx.cohorte.application.institucion;

import imss.gob.mx.cohorte.modules.institucion.InstitucionModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.services.institucion.InstitucionModuloService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstitucionModuloApplicationService {

    private final InstitucionModuloService institucionModuloService;

    @Transactional(readOnly = true)
    public List<InstitucionModulo> getByInstitucion(Long idInstitucion) {
        return institucionModuloService.getByInstitucion(idInstitucion);
    }

    /** Sólo registros habilitados — usar para construir el menú/sidebar del frontend. */
    @Transactional(readOnly = true)
    public List<InstitucionModulo> getHabilitadosByInstitucion(Long idInstitucion) {
        return institucionModuloService.getHabilitadosByInstitucion(idInstitucion);
    }

    @Transactional(readOnly = true)
    public boolean tieneAccesoHabilitado(Long idInstitucion, ModuloSistema modulo) {
        return institucionModuloService.tieneAccesoHabilitado(idInstitucion, modulo);
    }

    @Transactional
    public InstitucionModulo otorgar(Long idInstitucion, ModuloSistema modulo, boolean habilitado, Long idOtorgante) {
        return institucionModuloService.otorgar(idInstitucion, modulo, habilitado, idOtorgante);
    }

    @Transactional
    public InstitucionModulo revocar(Long idInstitucion, ModuloSistema modulo, Long idOtorgante) {
        return institucionModuloService.revocar(idInstitucion, modulo, idOtorgante);
    }
}
