package imss.gob.mx.cohorte.services.almacenamiento.refrigerador;

import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPiso;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPisoRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigeradorRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PosicionPisoService {

    private final PosicionPisoRepository posicionPisoRepository;
    private final PisoRefrigeradorRepository pisoRefrigeradorRepository;

    @Transactional(readOnly = true)
    public List<PosicionPiso> getAllPosiciones() {
        return posicionPisoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PosicionPiso getPosicion(Long id) {
        return posicionPisoRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la posición en piso con id: " + id));
    }

    @Transactional
    public PosicionPiso createPosicion(PosicionPiso posicion) {
        // Validar existencia del PisoRefrigerador
        Long idPiso = posicion.getPiso().getId();
        PisoRefrigerador piso = pisoRefrigeradorRepository.findById(idPiso)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el piso de refrigerador con id: " + idPiso));
        posicion.setPiso(piso);
        return posicionPisoRepository.save(posicion);
    }

    @Transactional
    public PosicionPiso updatePosicion(PosicionPiso posicion) {
        PosicionPiso posBD = posicionPisoRepository.findById(posicion.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la posición en piso con id: " + posicion.getId()));

        // Validar si cambia el piso asociado
        if (posicion.getPiso() != null) {
            Long idPiso = posicion.getPiso().getId();
            PisoRefrigerador piso = pisoRefrigeradorRepository.findById(idPiso)
                    .orElseThrow(() -> new ObjNotFoundException("No se encontró el piso de refrigerador con id: " + idPiso));
            posBD.setPiso(piso);
        }

        // Asumiendo estos atributos comunes
        posBD.setFila(posicion.getFila());
        posBD.setColumna(posicion.getColumna());
        posBD.setAltura(posicion.getAltura());
        posBD.setOcupada(posicion.getOcupada());
        return posicionPisoRepository.save(posBD);
    }
}