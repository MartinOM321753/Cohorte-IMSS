package imss.gob.mx.cohorte.services.almacenamiento.refrigerador;

import imss.gob.mx.cohorte.controllers.almacenamiento.dto.PisoResumenDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigeradorRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.RefrigeradorRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefrigeradorService {
    private final RefrigeradorRepository refrigeradorRepository;
    private final PisoRefrigeradorRepository pisoRefrigeradorRepository;
    private final InstitucionContextService institucionContextService;

    public List<Refrigerador> getAllRefrigeradores() {
        return refrigeradorRepository.findAllWithPisosByInstitucion(institucionContextService.getIdInstitucionActual());
    }

    public Refrigerador getRefrigerador(Long id) {
        Refrigerador refrigerador = refrigeradorRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el refrigerador con id: " + id));
        institucionContextService.verificarPertenece(refrigerador.getInstitucion());
        return refrigerador;
    }
    public Refrigerador getRefrigeradorByCode(String code) {
        Refrigerador refrigerador = refrigeradorRepository.findByCodigo(code)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el refrigerador con codigo: " + code));
        institucionContextService.verificarPertenece(refrigerador.getInstitucion());
        return refrigerador;
    }

    public String generarCodigoAutomatico() {
        String prefix = "REF-";
        Long idInst = institucionContextService.getIdInstitucionActual();
        Optional<String> maxCodigo = refrigeradorRepository.findMaxCodigoByPrefixAndInstitucion(prefix, idInst);
        int siguiente = 1;
        if (maxCodigo.isPresent()) {
            String numPart = maxCodigo.get().substring(prefix.length());
            try { siguiente = Integer.parseInt(numPart) + 1; } catch (NumberFormatException ignored) { }
        }
        return String.format("%s%04d", prefix, siguiente);
    }

    public Refrigerador createRefrigerador(Refrigerador refrigerador) {
        if (refrigerador.getCodigo() == null || refrigerador.getCodigo().isBlank()) {
            refrigerador.setCodigo(generarCodigoAutomatico());
        }
        refrigerador.setFechaRegistro(Timestamp.from(Instant.now()));
        refrigerador.setActivo(true);
        refrigerador.setInstitucion(institucionContextService.getInstitucionActual());
        return refrigeradorRepository.save(refrigerador);
    }

    public Refrigerador updateRefrigerador(Refrigerador refrigerador) {
        Refrigerador refBD = refrigeradorRepository.findById(refrigerador.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el refrigerador con id: " + refrigerador.getId()));
        institucionContextService.verificarPertenece(refBD.getInstitucion());

        refBD.setCodigo(refrigerador.getCodigo());
        refBD.setNombre(refrigerador.getNombre());
        refBD.setMarca(refrigerador.getMarca());
        refBD.setModelo(refrigerador.getModelo());
        refBD.setActivo(refrigerador.getActivo());
        // fechaRegistro no se actualiza (updatable = false)

        return refrigeradorRepository.save(refBD);
    }

    public void deleteRefrigerador(Long id) {
        Refrigerador refBD = refrigeradorRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el refrigerador con id: " + id));
        institucionContextService.verificarPertenece(refBD.getInstitucion());
        if(!refBD.getPisos().isEmpty()){
            throw new ObjConflictException("No se puede eliminar un refrigerador que tenga pisos asociados." );
        }
        refrigeradorRepository.delete(refBD);
    }

    /**
     * Devuelve un mapa refId → List&lt;PisoResumenDTO&gt; con ocupación real de posiciones,
     * obtenido en una sola consulta para toda la institución.
     */
    public Map<Long, List<PisoResumenDTO>> getPisosConOcupacion() {
        Long idInst = institucionContextService.getIdInstitucionActual();
        List<Object[]> rows = pisoRefrigeradorRepository.findAllPisosConOcupacionByInstitucion(idInst);
        Map<Long, List<PisoResumenDTO>> map = new HashMap<>();
        for (Object[] row : rows) {
            Long refId      = (Long)   row[0];
            Long pisoId     = (Long)   row[1];
            String numPiso  = (String) row[2];
            int total       = ((Number) row[3]).intValue();
            int ocupadas    = ((Number) row[4]).intValue();
            int libres      = total - ocupadas;
            double pct      = total == 0 ? 0.0 : ocupadas * 100.0 / total;

            PisoResumenDTO dto = PisoResumenDTO.builder()
                .id(pisoId)
                .numeroPiso(numPiso)
                .totalPosiciones(total)
                .posicionesOcupadas(ocupadas)
                .posicionesLibres(libres)
                .porcentajeUso(Math.round(pct * 10.0) / 10.0)
                .build();

            map.computeIfAbsent(refId, k -> new ArrayList<>()).add(dto);
        }
        return map;
    }
}