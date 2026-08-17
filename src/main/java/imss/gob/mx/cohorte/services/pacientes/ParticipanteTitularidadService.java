package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.cita.CitaRepository;
import imss.gob.mx.cohorte.modules.documentos.PacienteDocumentoRepository;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamenRepository;
import imss.gob.mx.cohorte.modules.somatometria.SomatometriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Decide si un participante todavia puede cambiar de institucion.
 *
 * <p>La regla es que solo se mueve mientras nada lo ate a su institucion actual.
 * En cuanto tiene actividad clinica, cambiarlo partiria su historial: los
 * estudios, muestras, citas y somatometrias se quedarian con la sede que los
 * creo, y los resultados de examen —que no llevan institucion propia, sino que
 * heredan la del paciente— cambiarian de dueno en silencio, quitandoselos a
 * quien los capturo.</p>
 *
 * <p>Este servicio existe porque el caso real es la importacion masiva: entro
 * todo el padron a nombre de la institucion que subio el archivo, y esos
 * participantes estan limpios. Mientras lo esten, redistribuirlos es inofensivo.</p>
 */
@Service
@RequiredArgsConstructor
public class ParticipanteTitularidadService {

    private final EstudioMedicoRepository estudioMedicoRepository;
    private final MuestraRepository muestraRepository;
    private final SomatometriaRepository somatometriaRepository;
    private final ResultadoExamenRepository resultadoExamenRepository;
    private final CitaRepository citaRepository;
    private final PacienteDocumentoRepository pacienteDocumentoRepository;

    /** Un tipo de registro que ata al participante con su institucion actual. */
    public record Vinculo(String tipo, String etiqueta, long cantidad) {}

    /**
     * Vinculos que impiden mover al participante. Lista vacia significa que se
     * puede mover.
     *
     * <p>El reclutamiento queda fuera a proposito: registra quien contacto al
     * participante, un hecho historico que no cambia porque cambie su sede. La
     * cuenta de acceso tampoco aparece aqui — no bloquea, se actualiza junto con
     * el participante.</p>
     */
    @Transactional(readOnly = true)
    public List<Vinculo> vinculosQueImpidenCambio(String uuidPaciente) {
        List<Vinculo> vinculos = new ArrayList<>();

        agregarSiHay(vinculos, "ESTUDIOS", "estudio(s) médico(s)",
                estudioMedicoRepository.countByPaciente_Uuid(uuidPaciente));
        agregarSiHay(vinculos, "MUESTRAS", "muestra(s)",
                muestraRepository.countByPaciente_Uuid(uuidPaciente));
        agregarSiHay(vinculos, "SOMATOMETRIAS", "somatometría(s)",
                somatometriaRepository.countByPaciente_Uuid(uuidPaciente));
        agregarSiHay(vinculos, "EXAMENES", "resultado(s) de examen",
                resultadoExamenRepository.countByPaciente_Uuid(uuidPaciente));
        agregarSiHay(vinculos, "CITAS", "cita(s)",
                citaRepository.countByPaciente_Uuid(uuidPaciente));
        agregarSiHay(vinculos, "DOCUMENTOS", "documento(s)",
                pacienteDocumentoRepository.countByPaciente_Uuid(uuidPaciente));

        return vinculos;
    }

    @Transactional(readOnly = true)
    public boolean puedeCambiarInstitucion(String uuidPaciente) {
        return vinculosQueImpidenCambio(uuidPaciente).isEmpty();
    }

    /** Texto para el usuario: «2 estudio(s) médico(s), 1 muestra(s)». */
    public String describir(List<Vinculo> vinculos) {
        return vinculos.stream()
                .map(v -> v.cantidad() + " " + v.etiqueta())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private void agregarSiHay(List<Vinculo> acumulador, String tipo, String etiqueta, long cantidad) {
        if (cantidad > 0) {
            acumulador.add(new Vinculo(tipo, etiqueta, cantidad));
        }
    }
}
