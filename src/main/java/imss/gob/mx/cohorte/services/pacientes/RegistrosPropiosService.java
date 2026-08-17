package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.controllers.pacientes.dto.MisRegistrosParticipanteDTO;
import imss.gob.mx.cohorte.controllers.pacientes.dto.MisRegistrosParticipanteDTO.Registro;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.cita.CitaRepository;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamenRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.somatometria.SomatometriaRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Reune lo que UNA institucion le registro a un participante que ya no gestiona.
 *
 * <p>Cada consulta lleva el filtro por institucion incorporado, asi que no puede
 * devolver nada ajeno aunque se invoque con un participante cualquiera: si la
 * sede no capturo nada, todas las listas vienen vacias. Esa es la garantia — no
 * hay una puerta que se pueda olvidar, la consulta misma acota.</p>
 */
@Service
@RequiredArgsConstructor
public class RegistrosPropiosService {

    private final EstudioMedicoRepository estudioMedicoRepository;
    private final MuestraRepository muestraRepository;
    private final CitaRepository citaRepository;
    private final SomatometriaRepository somatometriaRepository;
    private final ResultadoExamenRepository resultadoExamenRepository;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public MisRegistrosParticipanteDTO recopilar(Paciente paciente) {
        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        String uuid = paciente.getUuid();

        List<Registro> estudios = estudioMedicoRepository
                .findAllByPaciente_UuidAndInstitucion_IdOrderByFechaEstudioDesc(uuid, idInstitucion)
                .stream()
                .map(e -> Registro.builder()
                        .id(e.getId())
                        .fecha(e.getFechaEstudio())
                        .descripcion(e.getTipoEstudio() != null ? e.getTipoEstudio().getNombre() : "Estudio")
                        .build())
                .toList();

        List<Registro> muestras = muestraRepository
                .findAllByPaciente_UuidAndInstitucion_Id(uuid, idInstitucion)
                .stream()
                .map(m -> Registro.builder()
                        .id(m.getId())
                        .fecha(m.getFechaRecoleccion())
                        .descripcion(m.getEtiqueta())
                        .build())
                .toList();

        List<Registro> citas = citaRepository
                .findAllByPaciente_UuidAndInstitucion_IdOrderByStartAtUtcDesc(uuid, idInstitucion)
                .stream()
                .map(c -> Registro.builder()
                        .id(c.getId())
                        .fecha(c.getStartAtUtc() != null
                                ? LocalDateTime.ofInstant(c.getStartAtUtc(), ZoneId.systemDefault())
                                : null)
                        .descripcion(c.getEstadoCita() != null ? c.getEstadoCita().name() : "Cita")
                        .build())
                .toList();

        List<Registro> somatometrias = somatometriaRepository
                .findAllByPaciente_UuidAndInstitucion_IdOrderByFechaMedicionDesc(uuid, idInstitucion)
                .stream()
                .map(s -> Registro.builder()
                        .id(s.getId())
                        .fecha(s.getFechaMedicion())
                        .descripcion(s.getPesoKg() != null ? s.getPesoKg() + " kg" : "Somatometría")
                        .build())
                .toList();

        List<Registro> examenes = resultadoExamenRepository
                .findAllByPaciente_UuidAndInstitucion_IdOrderByFechaResultadoDesc(uuid, idInstitucion)
                .stream()
                .map(r -> Registro.builder()
                        .id(r.getId())
                        .fecha(r.getFechaResultado())
                        .descripcion(r.getExamen() != null ? r.getExamen().getParametro() : "Resultado")
                        .build())
                .toList();

        return MisRegistrosParticipanteDTO.builder()
                .pacienteUuid(uuid)
                .folio(paciente.getFolio())
                .nombreCompleto(nombreCompleto(paciente.getPersona()))
                .institucionActualNombre(paciente.getInstitucion() != null
                        ? paciente.getInstitucion().getNombre() : null)
                .estudios(estudios)
                .muestras(muestras)
                .citas(citas)
                .somatometrias(somatometrias)
                .examenes(examenes)
                .build();
    }

    private String nombreCompleto(Persona p) {
        if (p == null) return "";
        return (p.getNombre() + " " + p.getApellidoPaterno()
                + (p.getApellidoMaterno() != null ? " " + p.getApellidoMaterno() : "")).trim();
    }
}
