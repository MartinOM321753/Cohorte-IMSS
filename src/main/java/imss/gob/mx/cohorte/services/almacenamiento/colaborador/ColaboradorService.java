package imss.gob.mx.cohorte.services.almacenamiento.colaborador;

import imss.gob.mx.cohorte.modules.almacenamiento.colaborador.Colaborador;
import imss.gob.mx.cohorte.modules.almacenamiento.colaborador.ColaboradorRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CRUD de colaboradores, siempre acotado a una institución (aislamiento de datos).
 * La institución del usuario la resuelve el ApplicationService y la pasa aquí.
 */
@Service
@RequiredArgsConstructor
public class ColaboradorService {

    private final ColaboradorRepository colaboradorRepository;

    @Transactional(readOnly = true)
    public List<Colaborador> getByInstitucion(Long idInstitucion) {
        return colaboradorRepository.findAllByInstitucion_IdOrderByFechaRegistroDesc(idInstitucion);
    }

    @Transactional(readOnly = true)
    public List<Colaborador> getActivosByInstitucion(Long idInstitucion) {
        return colaboradorRepository.findAllByInstitucion_IdAndActivoOrderByPersona_ApellidoPaternoAsc(idInstitucion, true);
    }

    @Transactional(readOnly = true)
    public Page<Colaborador> buscarPaginado(Long idInstitucion, String buscar, Pageable pageable) {
        return colaboradorRepository.buscarPaginado(idInstitucion, buscar, pageable);
    }

    /** Obtiene un colaborador verificando que pertenece a la institución indicada. */
    @Transactional(readOnly = true)
    public Colaborador getByIdYInstitucion(Long id, Long idInstitucion) {
        return colaboradorRepository.findByIdAndInstitucion_Id(id, idInstitucion)
                .orElseThrow(() -> new ObjNotFoundException(
                        "No se encontró el colaborador con id " + id + " en su institución."));
    }

    @Transactional(rollbackFor = Exception.class)
    public Colaborador crear(Persona persona, Institucion institucion, String cargo,
                             String adscripcion, Boolean esExterno) {
        validarPersona(persona);

        Colaborador colaborador = new Colaborador();
        colaborador.setPersona(persona);
        colaborador.setInstitucion(institucion);
        colaborador.setCargo(trimOrNull(cargo));
        colaborador.setAdscripcion(trimOrNull(adscripcion));
        colaborador.setEsExterno(Boolean.TRUE.equals(esExterno));
        colaborador.setActivo(true);
        LocalDateTime ahora = LocalDateTime.now();
        colaborador.setFechaRegistro(ahora);
        colaborador.setFechaActualizacion(ahora);
        if (persona.getFechaRegistro() == null) {
            persona.setFechaRegistro(ahora);
        }
        persona.setFechaActualizacion(ahora);
        return colaboradorRepository.save(colaborador);
    }

    @Transactional(rollbackFor = Exception.class)
    public Colaborador actualizar(Colaborador existente, Persona datosPersona, String cargo,
                                  String adscripcion, Boolean esExterno) {
        validarPersona(datosPersona);

        Persona persona = existente.getPersona();
        persona.setNombre(datosPersona.getNombre());
        persona.setSegundoNombre(datosPersona.getSegundoNombre());
        persona.setApellidoPaterno(datosPersona.getApellidoPaterno());
        persona.setApellidoMaterno(datosPersona.getApellidoMaterno());
        persona.setFechaNacimiento(datosPersona.getFechaNacimiento());
        persona.setSexo(datosPersona.getSexo());
        persona.setTelefono(datosPersona.getTelefono());
        persona.setCurp(datosPersona.getCurp());
        persona.setEmail(datosPersona.getEmail());
        persona.setFechaActualizacion(LocalDateTime.now());

        existente.setCargo(trimOrNull(cargo));
        existente.setAdscripcion(trimOrNull(adscripcion));
        existente.setEsExterno(Boolean.TRUE.equals(esExterno));
        existente.setFechaActualizacion(LocalDateTime.now());
        return colaboradorRepository.save(existente);
    }

    @Transactional(rollbackFor = Exception.class)
    public Colaborador setActivo(Colaborador colaborador, Boolean activo) {
        colaborador.setActivo(Boolean.TRUE.equals(activo));
        colaborador.setFechaActualizacion(LocalDateTime.now());
        return colaboradorRepository.save(colaborador);
    }

    private void validarPersona(Persona persona) {
        if (persona == null) {
            throw new ValidationException("Faltan los datos de la persona del colaborador.");
        }
        if (persona.getNombre() == null || persona.getNombre().isBlank()) {
            throw new ValidationException("El nombre del colaborador es obligatorio.");
        }
        if (persona.getApellidoPaterno() == null || persona.getApellidoPaterno().isBlank()) {
            throw new ValidationException("El apellido paterno del colaborador es obligatorio.");
        }
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
