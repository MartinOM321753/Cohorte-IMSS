package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.modules.paciente.FolioSecuencia;
import imss.gob.mx.cohorte.modules.paciente.FolioSecuenciaRepository;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
@AllArgsConstructor
public class FolioGeneratorService {

    private final FolioSecuenciaRepository folioSecuenciaRepository;
    private final PacienteRepository pacienteRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generarFolio(Long idInstitucion) {
        int anioActual = Year.now().getValue();
        int anioCorto = anioActual % 100;

        FolioSecuencia secuencia = folioSecuenciaRepository
                .findByAnioAndIdInstitucionForUpdate(anioActual, idInstitucion)
                .orElseGet(() -> new FolioSecuencia(anioActual, idInstitucion));

        String folio;
        do {
            secuencia.setUltimoConsecutivo(secuencia.getUltimoConsecutivo() + 1);
            folio = String.format("C-%02d%02d%05d", idInstitucion, anioCorto, secuencia.getUltimoConsecutivo());
        } while (pacienteRepository.findByFolio(folio).isPresent());

        folioSecuenciaRepository.save(secuencia);
        return folio;
    }

    public String normalizar(String folioManual) {
        if (folioManual == null) return null;
        return folioManual.trim().toUpperCase().replaceAll("[^A-Z0-9-]", "");
    }
}
