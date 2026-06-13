package imss.gob.mx.cohorte.services.almacenamiento.refrigerador;

import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigeradorRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PisoRefrigeradorService {

    private final PisoRefrigeradorRepository pisoRefrigeradorRepository;


    @Transactional(readOnly = true)
    public List<PisoRefrigerador> getAllPisos(Long idRefrigerador) {
        return pisoRefrigeradorRepository.findAllByRefrigerador_Id(idRefrigerador);
    }

    @Transactional(readOnly = true)
    public PisoRefrigerador getPiso(Long id) {
        return pisoRefrigeradorRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el piso de refrigerador con id: " + id));
    }
    @Transactional(readOnly = true)
    public PisoRefrigerador getPisoByNumber(String numero) {
        return pisoRefrigeradorRepository.findByNumeroPiso(numero)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el piso de refrigerador con el numero: " + numero));
    }

    @Transactional(readOnly = true)
    public Optional<PisoRefrigerador> findByNumber(String numero) {
        return pisoRefrigeradorRepository.findByNumeroPiso(numero);
    }

    public String generarCodigoAutomatico(Long idRefrigerador) {
        String prefix = "P-";
        Optional<String> maxCodigo = pisoRefrigeradorRepository.findMaxNumeroPisoByPrefixAndRefrigerador(prefix, idRefrigerador);
        int siguiente = 1;
        if (maxCodigo.isPresent()) {
            String numPart = maxCodigo.get().substring(prefix.length());
            try { siguiente = Integer.parseInt(numPart) + 1; } catch (NumberFormatException ignored) { }
        }
        return String.format("%s%04d", prefix, siguiente);
    }

    @Transactional
    public PisoRefrigerador createPiso(PisoRefrigerador piso) {
        Long idRef = piso.getRefrigerador().getId();
        if (piso.getNumeroPiso() == null || piso.getNumeroPiso().isBlank()) {
            piso.setNumeroPiso(generarCodigoAutomatico(idRef));
        } else {
            Optional<PisoRefrigerador> findPiso = pisoRefrigeradorRepository.findByNumeroPisoAndRefrigerador_Id(piso.getNumeroPiso(), idRef);
            if (findPiso.isPresent()) { throw new ObjNotFoundException("El piso de refrigerador ya existe"); }
        }
        piso.setFechaRegistro(Timestamp.from(Instant.now()));
        piso.setActivo(true);
        return pisoRefrigeradorRepository.save(piso);
    }

    @Transactional
    public PisoRefrigerador updatePiso(PisoRefrigerador piso) {
        PisoRefrigerador pisoBD = pisoRefrigeradorRepository.findById(piso.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el piso de refrigerador con id: " + piso.getId()));

        if (!pisoBD.getNumeroPiso().equals(piso.getNumeroPiso())) {
            Long idRef = pisoBD.getRefrigerador().getId();
            Optional<PisoRefrigerador> findPiso = pisoRefrigeradorRepository.findByNumeroPisoAndRefrigerador_Id(piso.getNumeroPiso(), idRef);
            if (findPiso.isPresent()) {throw new ObjNotFoundException("El piso de refrigerador ya existe");}
            pisoBD.setNumeroPiso(piso.getNumeroPiso());
        }

        pisoBD.setFilas(piso.getFilas());
        pisoBD.setColumnas(piso.getColumnas());
        pisoBD.setAltura(piso.getAltura());
        pisoBD.setActivo(piso.getActivo());

        return pisoRefrigeradorRepository.save(pisoBD);
    }

    @Transactional
    public void deletePiso(Long id) {
        if (!pisoRefrigeradorRepository.existsById(id)) {
            throw new ObjNotFoundException("No se encontró el piso con id: " + id);
        }
        pisoRefrigeradorRepository.deleteById(id);
    }
}