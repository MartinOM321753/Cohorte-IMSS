package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.controllers.somatometria.dto.SomatometriaRequestDTO;
import imss.gob.mx.cohorte.modules.cita.ConfiguracionHorario;
import imss.gob.mx.cohorte.modules.cita.ConfiguracionHorarioRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.somatometria.Somatometria;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.somatometria.SomatometriaService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.SOMATOMETRIA)
public class SomatometriaApplicationService {

    private final SomatometriaService somatometriaService;
    private final PacienteService pacienteService;
    private final UserService userService;
    private final InstitucionContextService institucionContextService;
    private final ConfiguracionHorarioRepository configuracionHorarioRepository;

    @Transactional(readOnly = true)
    public List<Somatometria> getHistorialByPaciente(String pacienteUUID) {
        return somatometriaService.findByPacienteUuid(pacienteUUID);
    }

    @Transactional(readOnly = true)
    public Optional<Somatometria> getLatest(String pacienteUUID) {
        return somatometriaService.findLatest(pacienteUUID);
    }

    @Transactional(readOnly = true)
    public Somatometria getById(Long id) {
        Somatometria somatometria = somatometriaService.findById(id);
        institucionContextService.verificarPertenece(somatometria.getInstitucion());
        return somatometria;
    }

    @Transactional
    public Somatometria create(SomatometriaRequestDTO dto) {
        validarFechaMedicion(dto.getFechaMedicion());
        Paciente paciente = pacienteService.getByUUID(dto.getPacienteUUID(), institucionContextService.getIdInstitucionActual());
        BeanUser usuario = userService.getByUUID(dto.getUsuarioRegistraUUID());
        Institucion institucion = institucionContextService.getInstitucionActual();

        Somatometria somatometria = Somatometria.builder()
                .paciente(paciente)
                .usuarioRegistra(usuario)
                .institucion(institucion)
                .fechaMedicion(dto.getFechaMedicion())
                .pesoKg(dto.getPesoKg())
                .tallaM(dto.getTallaM())
                .presionSistolica(dto.getPresionSistolica())
                .presionDiastolica(dto.getPresionDiastolica())
                .circunferenciaAbdominalCm(dto.getCircunferenciaAbdominalCm())
                .frecuenciaCardiacaReposo(dto.getFrecuenciaCardiacaReposo())
                .observaciones(dto.getObservaciones())
                .build();

        return somatometriaService.save(somatometria);
    }

    @Transactional
    public Somatometria update(Long id, SomatometriaRequestDTO dto) {
        validarFechaMedicion(dto.getFechaMedicion());
        Somatometria existente = somatometriaService.findById(id);
        institucionContextService.verificarPertenece(existente.getInstitucion());
        Somatometria incoming = Somatometria.builder()
                .fechaMedicion(dto.getFechaMedicion())
                .pesoKg(dto.getPesoKg())
                .tallaM(dto.getTallaM())
                .presionSistolica(dto.getPresionSistolica())
                .presionDiastolica(dto.getPresionDiastolica())
                .circunferenciaAbdominalCm(dto.getCircunferenciaAbdominalCm())
                .frecuenciaCardiacaReposo(dto.getFrecuenciaCardiacaReposo())
                .observaciones(dto.getObservaciones())
                .build();

        return somatometriaService.update(id, incoming);
    }

    @Transactional
    public void delete(Long id) {
        Somatometria existente = somatometriaService.findById(id);
        institucionContextService.verificarPertenece(existente.getInstitucion());
        somatometriaService.delete(id);
    }

    private void validarFechaMedicion(LocalDateTime fechaMedicion) {
        if (fechaMedicion.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de medición no puede ser futura");
        }

        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        ConfiguracionHorario horario = configuracionHorarioRepository
                .findByInstitucionIdAndActivaTrue(idInstitucion)
                .orElse(null);

        if (horario == null) return;

        int hora = fechaMedicion.getHour();
        if (hora < horario.getHoraInicio() || hora >= horario.getHoraFin()) {
            throw new IllegalArgumentException(
                    "La hora de medición debe estar entre las "
                            + horario.getHoraInicio() + ":00 y las "
                            + (horario.getHoraFin() - 1) + ":59");
        }

        DayOfWeek day = fechaMedicion.getDayOfWeek();
        boolean diaPermitido = switch (day) {
            case MONDAY -> Boolean.TRUE.equals(horario.getLunes());
            case TUESDAY -> Boolean.TRUE.equals(horario.getMartes());
            case WEDNESDAY -> Boolean.TRUE.equals(horario.getMiercoles());
            case THURSDAY -> Boolean.TRUE.equals(horario.getJueves());
            case FRIDAY -> Boolean.TRUE.equals(horario.getViernes());
            case SATURDAY -> Boolean.TRUE.equals(horario.getSabado());
            case SUNDAY -> Boolean.TRUE.equals(horario.getDomingo());
        };

        if (!diaPermitido) {
            throw new IllegalArgumentException(
                    "No se permiten registros en el día seleccionado según el horario activo");
        }
    }
}
