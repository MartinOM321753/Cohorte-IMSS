package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.colaborador.Colaborador;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.almacenamiento.colaborador.ColaboradorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@RequireModulo(ModuloSistema.BIOBANCO)
public class ColaboradorApplicationService {

    private final ColaboradorService colaboradorService;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public List<Colaborador> getDeMiInstitucion() {
        return colaboradorService.getByInstitucion(institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public List<Colaborador> getActivosDeMiInstitucion() {
        return colaboradorService.getActivosByInstitucion(institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public Page<Colaborador> buscarPaginado(String buscar, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return colaboradorService.buscarPaginado(
                institucionContextService.getIdInstitucionActual(), buscar, pageable);
    }

    @Transactional(readOnly = true)
    public Colaborador getById(Long id) {
        return colaboradorService.getByIdYInstitucion(id, institucionContextService.getIdInstitucionActual());
    }

    @Transactional
    public Colaborador crear(Persona persona, String cargo, String adscripcion, Boolean esExterno) {
        Institucion institucion = institucionContextService.getInstitucionActual();
        return colaboradorService.crear(persona, institucion, cargo, adscripcion, esExterno);
    }

    @Transactional
    public Colaborador actualizar(Long id, Persona datosPersona, String cargo,
                                  String adscripcion, Boolean esExterno) {
        Colaborador existente = getById(id);
        return colaboradorService.actualizar(existente, datosPersona, cargo, adscripcion, esExterno);
    }

    @Transactional
    public Colaborador setActivo(Long id, Boolean activo) {
        Colaborador colaborador = getById(id);
        return colaboradorService.setActivo(colaborador, activo);
    }
}
