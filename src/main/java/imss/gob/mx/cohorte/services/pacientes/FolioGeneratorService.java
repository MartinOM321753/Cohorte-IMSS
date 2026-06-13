package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.modules.paciente.FolioSecuencia;
import imss.gob.mx.cohorte.modules.paciente.FolioSecuenciaRepository;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Genera folios automáticos para participantes nuevos y normaliza folios
 * capturados manualmente (cuando el participante ya contaba con uno previo).
 *
 * Formato de folio autogenerado: {@code COH-AA-NNNNN}
 *   - "COH"  → prefijo fijo que distingue los folios generados por el sistema
 *              de folios legados capturados manualmente (que normalmente no lo usan).
 *   - "AA"   → año calendario en 2 dígitos (la numeración reinicia cada año).
 *   - "NNNNN"→ consecutivo de 5 dígitos, único dentro del año.
 *
 * Ejemplos: COH-26-00001, COH-26-00002 … COH-27-00001 (reinicia en el nuevo año).
 *
 * Tanto los folios autogenerados como los capturados manualmente se normalizan
 * a MAYÚSCULAS y se validan contra la tabla completa de pacientes para evitar
 * colisiones entre ambos orígenes.
 */
@Service
@AllArgsConstructor
public class FolioGeneratorService {

    private static final String PREFIJO = "COH";

    private final FolioSecuenciaRepository folioSecuenciaRepository;
    private final PacienteRepository pacienteRepository;

    /**
     * Genera el siguiente folio disponible para el año en curso.
     * Usa una transacción independiente con bloqueo pesimista sobre el contador
     * del año para evitar consecutivos duplicados ante altas concurrentes, y
     * además verifica contra la tabla de pacientes (salvaguarda ante folios
     * legados que pudieran coincidir por azar con el patrón generado).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generarFolio() {
        int anioActual = Year.now().getValue();
        int anioCorto = anioActual % 100;

        FolioSecuencia secuencia = folioSecuenciaRepository.findByAnioForUpdate(anioActual)
                .orElseGet(() -> new FolioSecuencia(anioActual));

        String folio;
        do {
            secuencia.setUltimoConsecutivo(secuencia.getUltimoConsecutivo() + 1);
            folio = String.format("%s-%02d-%05d", PREFIJO, anioCorto, secuencia.getUltimoConsecutivo());
        } while (pacienteRepository.findByFolio(folio).isPresent());

        folioSecuenciaRepository.save(secuencia);
        return folio;
    }

    /**
     * Normaliza un folio capturado manualmente por el usuario (participantes que
     * ya contaban con un folio de seguimiento previo): lo recorta, convierte a
     * MAYÚSCULAS y conserva solo letras, números y guiones — para evitar choques
     * de formato con los folios autogenerados y mantener consistencia visual.
     */
    public String normalizar(String folioManual) {
        if (folioManual == null) return null;
        return folioManual.trim().toUpperCase().replaceAll("[^A-Z0-9-]", "");
    }
}
