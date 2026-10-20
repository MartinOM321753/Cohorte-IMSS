package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class FolioGeneratorService {

    private final PacienteRepository pacienteRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generarFolio(Institucion institucion) {
        if (institucion != null
                && institucion.getFolioInicio() != null
                && institucion.getFolioFin() != null) {
            return generarFolioEnRango(institucion.getFolioInicio(), institucion.getFolioFin());
        }
        return generarFolioGlobal();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generarFolio() {
        return generarFolioGlobal();
    }

    private String generarFolioGlobal() {
        List<Integer> existentes = pacienteRepository.findAllFoliosNumericos();

        int siguiente = 1;
        for (int folio : existentes) {
            if (folio != siguiente) {
                break;
            }
            siguiente++;
        }

        String folioStr = String.format("%06d", siguiente);

        if (pacienteRepository.existsByFolio(folioStr)) {
            return generarFolioGlobal();
        }

        return folioStr;
    }

    private String generarFolioEnRango(int inicio, int fin) {
        List<Integer> existentes = pacienteRepository.findFoliosNumericosEnRango(inicio, fin);

        int siguiente = inicio;
        for (int folio : existentes) {
            if (folio != siguiente) {
                break;
            }
            siguiente++;
        }

        if (siguiente > fin) {
            throw new ObjConflictException(
                    "El rango de folios asignado a esta institución está agotado ("
                    + String.format("%06d", inicio) + " – " + String.format("%06d", fin) + ")");
        }

        String folioStr = String.format("%06d", siguiente);

        if (pacienteRepository.existsByFolio(folioStr)) {
            return generarFolioEnRango(inicio, fin);
        }

        return folioStr;
    }

    public String normalizar(String folioManual) {
        if (folioManual == null) return null;
        String limpio = folioManual.trim().toUpperCase().replaceAll("[^A-Z0-9-]", "");
        if (limpio.matches("^\\d+$")) {
            int num = Integer.parseInt(limpio);
            return String.format("%06d", num);
        }
        return limpio;
    }
}
