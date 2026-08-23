package imss.gob.mx.cohorte.application.almacenamiento;


import imss.gob.mx.cohorte.controllers.almacenamiento.dto.PisoResumenDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.RefrigeradorMapper;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.RefrigeradorResponseDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d.Ubicacion3DPisoDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d.Ubicacion3DRefrigeradorDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.PisoRefrigeradorService;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.RefrigeradorService;
import imss.gob.mx.cohorte.services.almacenamiento.ubicacion3d.Ubicacion3DService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;



@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.BIOBANCO)
public class RefrigeradorApplicationService {

    private final RefrigeradorService refrigeradorService;
    private final PisoRefrigeradorService pisoRefrigeradorService;
    private final Ubicacion3DService ubicacion3DService;
    private final InstitucionContextService institucionContextService;

    // ------------------- Vista 3D (exploracion libre) -------------------

    /** Escena 3D de un refrigerador completo, sin muestra objetivo. */
    @Transactional(readOnly = true)
    public Ubicacion3DRefrigeradorDTO getVista3D(Long idRefrigerador) {
        return ubicacion3DService.explorarRefrigerador(refrigeradorService.getRefrigerador(idRefrigerador));
    }

    /**
     * Escena 3D de un piso concreto, sin muestra objetivo.
     *
     * <p>El aislamiento se comprueba aqui contra el refrigerador que lo aloja:
     * {@code PisoRefrigeradorService.getPiso} no lo hace, porque el piso no
     * guarda institucion propia.
     */
    @Transactional(readOnly = true)
    public Ubicacion3DPisoDTO getVista3DPiso(Long idPiso) {
        PisoRefrigerador piso = pisoRefrigeradorService.getPiso(idPiso);
        institucionContextService.verificarPertenece(piso.getRefrigerador().getInstitucion());
        return ubicacion3DService.explorarPiso(piso);
    }

    // ------------------- CRUD -------------------

    @Transactional(readOnly = true)
    public List<Refrigerador> getAllRefrigeradores() {
        return refrigeradorService.getAllRefrigeradores();
    }

    @Transactional(readOnly = true)
    public List<RefrigeradorResponseDTO> getAllRefrigeradoresConEstadisticas() {
        List<Refrigerador> refs = refrigeradorService.getAllRefrigeradores();
        Map<Long, List<PisoResumenDTO>> pisosMap = refrigeradorService.getPisosConOcupacion();
        return RefrigeradorMapper.toResponseDTOList(refs, pisosMap);
    }

    @Transactional(readOnly = true)
    public Refrigerador getRefrigerador(Long id) {
        return refrigeradorService.getRefrigerador(id);
    }

    @Transactional
    public Refrigerador createRefrigerador(Refrigerador refrigerador) {
        if (refrigerador.getCodigo() != null && !refrigerador.getCodigo().isBlank()) {
            ensureCodigoUnicoOrThrow(refrigerador.getCodigo());
        }
        return refrigeradorService.createRefrigerador(refrigerador);
    }

    @Transactional
    public Refrigerador updateRefrigerador(Long id, Refrigerador refrigerador) {
        Refrigerador refBD = refrigeradorService.getRefrigerador(id);

        // Validación: El código no debe repetirse en otro registro
        if (!refBD.getCodigo().equals(refrigerador.getCodigo())) {
            ensureCodigoUnicoOrThrow(refrigerador.getCodigo());
        }
        // Actualiza datos básicos
        refBD.setCodigo(refrigerador.getCodigo());
        refBD.setNombre(refrigerador.getNombre());
        refBD.setMarca(refrigerador.getMarca());
        refBD.setModelo(refrigerador.getModelo());
        refBD.setActivo(refrigerador.getActivo());

        return refrigeradorService.updateRefrigerador(refBD);
    }

    @Transactional
    public void deleteRefrigerador(Long id) {
        refrigeradorService.deleteRefrigerador(id);
    }

    // ----------- HELPERS DE VALIDACIÓN (private) ------------

    private void ensureCodigoUnicoOrThrow(String codigo) {
        try {
            refrigeradorService.getRefrigeradorByCode(codigo);
            throw new ObjConflictException("Ya existe un refrigerador con el código: " + codigo);
        } catch (ObjNotFoundException ignored) {
            // Ok: no existe, todo bien
        }
    }
}
