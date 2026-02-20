package imss.gob.mx.cohorte.services.almacenamiento.caja;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenicaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCajaRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PosicionCajaService {

    private final PosicionCajaRepository posicionCajaRepository;
    private final CajaCriogenicaRepository cajaCriogenicaRepository;

    @Transactional(readOnly = true)
    public List<PosicionCaja> getAll() {
        return posicionCajaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PosicionCaja getById(Long id) {
        return posicionCajaRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la posición de caja con id: " + id));
    }

    @Transactional
    public PosicionCaja create(PosicionCaja posicion) {
        if (posicion.getCaja() == null || posicion.getCaja().getId() == null) {
            throw new IllegalArgumentException("Debe especificar la caja criogénica para la posición");
        }
        CajaCriogenica caja = cajaCriogenicaRepository.findById(posicion.getCaja().getId())
                .orElseThrow(() -> new ObjNotFoundException("La caja criogénica especificada no existe"));
        if (posicion.getFila() == null || posicion.getFila() < 1) {
            throw new IllegalArgumentException("La fila debe ser mayor o igual a 1");
        }
        if (posicion.getColumna() == null || posicion.getColumna() < 1) {
            throw new IllegalArgumentException("La columna debe ser mayor o igual a 1");
        }
        // Validación: fila/columna no sobrepasen límites de la caja
        if (posicion.getFila() > caja.getFilas()) {
            throw new IllegalArgumentException("La fila no puede ser mayor a la cantidad de filas de la caja");
        }
        if (posicion.getColumna() > caja.getColumnas()) {
            throw new IllegalArgumentException("La columna no puede ser mayor a la cantidad de columnas de la caja");
        }
        posicion.setCaja(caja);
        if (posicion.getOcupada() == null) {
            posicion.setOcupada(false);
        }
        return posicionCajaRepository.save(posicion);
    }

    @Transactional
    public PosicionCaja update(PosicionCaja posicion) {
        PosicionCaja posicionBD = posicionCajaRepository.findById(posicion.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la posición de caja con id: " + posicion.getId()));
        if (posicion.getCaja() != null && posicion.getCaja().getId() != null) {
            CajaCriogenica caja = cajaCriogenicaRepository.findById(posicion.getCaja().getId())
                    .orElseThrow(() -> new ObjNotFoundException("La caja criogénica no existe"));
            posicionBD.setCaja(caja);
        }
        if (posicion.getFila() != null && posicion.getFila() > 0) {
            posicionBD.setFila(posicion.getFila());
        }
        if (posicion.getColumna() != null && posicion.getColumna() > 0) {
            posicionBD.setColumna(posicion.getColumna());
        }
        posicionBD.setOcupada(posicion.getOcupada() != null ? posicion.getOcupada() : posicionBD.getOcupada());
        return posicionCajaRepository.save(posicionBD);
    }
}