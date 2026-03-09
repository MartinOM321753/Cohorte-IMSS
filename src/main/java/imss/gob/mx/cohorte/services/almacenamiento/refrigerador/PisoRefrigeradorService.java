package imss.gob.mx.cohorte.services.almacenamiento.refrigerador;

import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigeradorRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.RefrigeradorRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PisoRefrigeradorService {

    private final PisoRefrigeradorRepository pisoRefrigeradorRepository;
    private final RefrigeradorRepository refrigeradorRepository;

    @Transactional(readOnly = true)
    public List<PisoRefrigerador> getAllPisos() {
        return pisoRefrigeradorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PisoRefrigerador getPiso(Long id) {
        return pisoRefrigeradorRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el piso de refrigerador con id: " + id));
    }

    @Transactional
    public PisoRefrigerador createPiso(PisoRefrigerador piso) {
        // Validar existencia del refrigerador
        Long idRefri = piso.getRefrigerador().getId();
        Refrigerador refri = refrigeradorRepository.findById(idRefri)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el refrigerador con id: " + idRefri));
        piso.setRefrigerador(refri);
        piso.setFechaRegistro(Timestamp.from(Instant.now()));
        piso.setActivo(true);
        return pisoRefrigeradorRepository.save(piso);
    }

    @Transactional
    public PisoRefrigerador updatePiso(PisoRefrigerador piso) {
        PisoRefrigerador pisoBD = pisoRefrigeradorRepository.findById(piso.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el piso de refrigerador con id: " + piso.getId()));

        // Validar si cambia el refrigerador asociado
        if (piso.getRefrigerador() != null) {
            Long idRefri = piso.getRefrigerador().getId();
            Refrigerador refri = refrigeradorRepository.findById(idRefri)
                    .orElseThrow(() -> new ObjNotFoundException("No se encontró el refrigerador con id: " + idRefri));
            pisoBD.setRefrigerador(refri);
        }

        pisoBD.setNumeroPiso(piso.getNumeroPiso());
        pisoBD.setFilas(piso.getFilas());
        pisoBD.setColumnas(piso.getColumnas());
        pisoBD.setAltura(piso.getAltura());
        pisoBD.setActivo(piso.getActivo());
        // fechaRegistro no se actualiza

        return pisoRefrigeradorRepository.save(pisoBD);
    }
}