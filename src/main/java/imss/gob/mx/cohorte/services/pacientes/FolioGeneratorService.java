package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
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
    public String generarFolio() {
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
            return generarFolio();
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
