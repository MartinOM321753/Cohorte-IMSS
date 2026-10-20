package imss.gob.mx.cohorte.services.almacenamiento.toma;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.toma.Toma;
import imss.gob.mx.cohorte.modules.almacenamiento.toma.TomaRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TomaService {

    private final TomaRepository tomaRepository;

    /**
     * Busca una toma existente por (participante, tipo, fecha, institución) o la crea.
     * Si no existe para esta institución pero sí para otra (muestra transferida),
     * reutiliza la toma original: la recolección fue una sola.
     */
    @Transactional
    public Toma findOrCreate(Paciente paciente, TipoMuestra tipoMuestra,
                             LocalDateTime fechaRecoleccion, Institucion institucionOrigen,
                             BeanUser usuarioRecolecta, String observaciones) {
        return tomaRepository.findByPaciente_IdAndTipoMuestra_IdAndFechaRecoleccionAndInstitucionOrigen_Id(
                        paciente.getId(), tipoMuestra.getId(), fechaRecoleccion, institucionOrigen.getId())
                .or(() -> tomaRepository.findFirstByPaciente_IdAndTipoMuestra_IdAndFechaRecoleccion(
                        paciente.getId(), tipoMuestra.getId(), fechaRecoleccion))
                .orElseGet(() -> {
                    Toma toma = new Toma();
                    toma.setPaciente(paciente);
                    toma.setTipoMuestra(tipoMuestra);
                    toma.setInstitucionOrigen(institucionOrigen);
                    toma.setUsuarioRecolecta(usuarioRecolecta);
                    toma.setFechaRecoleccion(fechaRecoleccion);
                    toma.setObservaciones(observaciones);
                    toma.setFechaRegistro(Timestamp.valueOf(LocalDateTime.now()));
                    return tomaRepository.save(toma);
                });
    }

    @Transactional(readOnly = true)
    public Toma getById(Long id) {
        return tomaRepository.findById(id)
                .orElseThrow(() -> new imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException(
                        "No se encontró la toma"));
    }

    @Transactional(readOnly = true)
    public List<Toma> listarPorPaciente(Long idPaciente) {
        return tomaRepository.findAllByPaciente_IdOrderByFechaRecoleccionDesc(idPaciente);
    }
}
