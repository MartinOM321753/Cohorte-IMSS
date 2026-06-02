package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.controllers.somatometria.dto.SomatometriaRequestDTO;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.somatometria.Somatometria;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.somatometria.SomatometriaService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SomatometriaApplicationService {

    private final SomatometriaService somatometriaService;
    private final PacienteService pacienteService;
    private final UserService userService;

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
        return somatometriaService.findById(id);
    }

    @Transactional
    public Somatometria create(SomatometriaRequestDTO dto) {
        Paciente paciente = pacienteService.getByUUID(dto.getPacienteUUID());
        BeanUser usuario = userService.getByUUID(dto.getUsuarioRegistraUUID());

        Somatometria somatometria = Somatometria.builder()
                .paciente(paciente)
                .usuarioRegistra(usuario)
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
        somatometriaService.delete(id);
    }
}
