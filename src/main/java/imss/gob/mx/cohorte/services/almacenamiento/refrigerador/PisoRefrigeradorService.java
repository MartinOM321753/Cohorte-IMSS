package imss.gob.mx.cohorte.services.almacenamiento.refrigerador;

import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigeradorRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.RefrigeradorRepository;
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

    @Transactional
    public PisoRefrigerador createPiso(PisoRefrigerador piso) {
       Optional<PisoRefrigerador> findPiso = pisoRefrigeradorRepository.findByNumeroPiso(piso.getNumeroPiso());
       if (findPiso.isPresent()) {throw new ObjNotFoundException("El piso de refrigerador ya existe");}
        piso.setFechaRegistro(Timestamp.from(Instant.now()));
        piso.setActivo(true);
        return pisoRefrigeradorRepository.save(piso);
    }

    @Transactional
    public PisoRefrigerador updatePiso(PisoRefrigerador piso) {
        PisoRefrigerador pisoBD = pisoRefrigeradorRepository.findById(piso.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el piso de refrigerador con id: " + piso.getId()));

        if (!pisoBD.getNumeroPiso().equals(piso.getNumeroPiso())) {
            Optional<PisoRefrigerador> findPiso = pisoRefrigeradorRepository.findByNumeroPiso(piso.getNumeroPiso());
            if (findPiso.isPresent()) {throw new ObjNotFoundException("El piso de refrigerador ya existe");}
            pisoBD.setNumeroPiso(piso.getNumeroPiso());

        }

        pisoBD.setFilas(piso.getFilas());
        pisoBD.setColumnas(piso.getColumnas());
        pisoBD.setAltura(piso.getAltura());
        pisoBD.setActivo(piso.getActivo());

        return pisoRefrigeradorRepository.save(pisoBD);
    }

    public void deletePiso(Long id) {
        // Implementación básica para eliminar un piso
        pisoRefrigeradorRepository.deleteById(id);
    }
}