package imss.gob.mx.cohorte.services.almacenamiento.caja;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenicaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCajaRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PosicionCajaService {

    private final PosicionCajaRepository posicionCajaRepository;
    private final CajaCriogenicaRepository cajaCriogenicaRepository;

    @Autowired
    public PosicionCajaService(PosicionCajaRepository posicionCajaRepository, 
                               CajaCriogenicaRepository cajaCriogenicaRepository) {
        this.posicionCajaRepository = posicionCajaRepository;
        this.cajaCriogenicaRepository = cajaCriogenicaRepository;
    }

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

    @Transactional(readOnly = true)
    public List<PosicionCaja> getPositionsByCaja(Long cajaId) {
        return posicionCajaRepository.findAllByCaja_Id(cajaId);
    }

    @Transactional
    public void deletePositions(List<PosicionCaja> positions) {
        posicionCajaRepository.deleteAll(positions);
    }

    @Transactional
    public void crearPosicionSiNoExiste(CajaCriogenica caja, int fila, int columna) {
        if (posicionCajaRepository.findByCaja_IdAndFilaAndColumna(caja.getId(), fila, columna).isEmpty()) {
            PosicionCaja nueva = new PosicionCaja();
            nueva.setCaja(caja);
            nueva.setFila(fila);
            nueva.setColumna(columna);
            nueva.setOcupada(false);
            posicionCajaRepository.save(nueva);
        }
    }

    /** Marca una posición como libre (ocupada = false). */
    @Transactional
    public void liberarPosicion(Long idPosicion) {
        PosicionCaja pos = posicionCajaRepository.findById(idPosicion)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la posición de caja con id: " + idPosicion));
        pos.setOcupada(false);
        posicionCajaRepository.save(pos);
    }

    /** Marca una posición como ocupada (ocupada = true). */
    @Transactional
    public void ocuparPosicion(Long idPosicion) {
        PosicionCaja pos = posicionCajaRepository.findById(idPosicion)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la posición de caja con id: " + idPosicion));
        if (pos.getOcupada()) {
            throw new imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException(
                    "La posición de caja ya está ocupada.");
        }
        pos.setOcupada(true);
        posicionCajaRepository.save(pos);
    }

    /**
     * Genera automáticamente todas las posiciones de una caja criogénica una vez creada,
     * según sus dimensiones (filas y columnas).
     *
     * @param idCaja ID de la CajaCriogenica a la que se le generarán posiciones.
     */
    @Transactional
    public void generarPosicionesParaCaja(Long idCaja) {
        CajaCriogenica caja = cajaCriogenicaRepository.findById(idCaja)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la caja criogénica con id: " + idCaja));

        int totalFilas = caja.getFilas();
        int totalColumnas = caja.getColumnas();

        for (int f = 1; f <= totalFilas; f++) {
            for (int c = 1; c <= totalColumnas; c++) {
                PosicionCaja nueva = new PosicionCaja();
                nueva.setCaja(caja);
                nueva.setFila(f);
                nueva.setColumna(c);
                nueva.setOcupada(false);
                posicionCajaRepository.save(nueva);
            }
        }
    }
}