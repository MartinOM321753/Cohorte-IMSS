package imss.gob.mx.cohorte.application.almacenamiento;


import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.RefrigeradorService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;



@Service
@AllArgsConstructor
public class RefrieradorApplicationService {

    private final RefrigeradorService refrigeradorService;

    // ------------------- CRUD -------------------

    @Transactional(readOnly = true)
    public List<Refrigerador> getAllRefrigeradores() {
        return refrigeradorService.getAllRefrigeradores();
    }

    @Transactional(readOnly = true)
    public Refrigerador getRefrigerador(Long id) {
        return refrigeradorService.getRefrigerador(id);
    }

    @Transactional
    public Refrigerador createRefrigerador(Refrigerador refrigerador) {
        ensureCodigoUnicoOrThrow(refrigerador.getCodigo());
        if (refrigerador.getCodigo() == null || refrigerador.getCodigo().trim().isEmpty()) {
            throw new ObjConflictException("El código del refrigerador es obligatorio");
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
