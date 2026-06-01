package imss.gob.mx.cohorte.services.almacenamiento.caja;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenicaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPiso;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPisoRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CajaCriojenicaService {

    private final CajaCriogenicaRepository cajaCriogenicaRepository;
    private final PosicionPisoRepository posicionPisoRepository;

    @Autowired
    public CajaCriojenicaService(CajaCriogenicaRepository cajaCriogenicaRepository, 
                                 PosicionPisoRepository posicionPisoRepository) {
        this.cajaCriogenicaRepository = cajaCriogenicaRepository;
        this.posicionPisoRepository = posicionPisoRepository;
    }

    @Transactional(readOnly = true)
    public List<CajaCriogenica> getAll() {
        return cajaCriogenicaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CajaCriogenica getById(Long id) {
        return cajaCriogenicaRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la caja criogénica con id: " + id));
    }

    @Transactional
    public CajaCriogenica create(CajaCriogenica caja) {
        Optional<CajaCriogenica> cajaBD = cajaCriogenicaRepository.findByCodigoCaja(caja.getCodigoCaja());
        if (cajaBD.isPresent()) {
            throw new ObjConflictException("El codgo de la caja ya existe");
        }
        if (caja.getCodigoCaja() == null || caja.getCodigoCaja().trim().isEmpty()) {
            throw new IllegalArgumentException("El código de la caja es obligatorio");
        }
        if (caja.getFilas() == null || caja.getFilas() <= 0) {
            throw new IllegalArgumentException("El número de filas debe ser mayor a 0");
        }
        if (caja.getColumnas() == null || caja.getColumnas() <= 0) {
            throw new IllegalArgumentException("El número de columnas debe ser mayor a 0");
        }
        if (caja.getPosicionPiso() != null && caja.getPosicionPiso().getId() != null) {
            PosicionPiso posicion = posicionPisoRepository.findById(caja.getPosicionPiso().getId())
                    .orElseThrow(() -> new ObjNotFoundException("La posición de piso asignada no existe"));
            caja.setPosicionPiso(posicion);
        }
        caja.setFechaRegistro(Timestamp.from(Instant.now()));
        caja.setActivo(true);

        return cajaCriogenicaRepository.save(caja);
    }

    @Transactional
    public CajaCriogenica update(CajaCriogenica caja) {
        CajaCriogenica cajaBD = cajaCriogenicaRepository.findById(caja.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la caja criogénica con id: " + caja.getId()));

        if (caja.getCodigoCaja() != null && !caja.getCodigoCaja().trim().isEmpty()) {
            cajaBD.setCodigoCaja(caja.getCodigoCaja());
        }
        if (caja.getFilas() != null && caja.getFilas() > 0) {
            cajaBD.setFilas(caja.getFilas());
        }
        if (caja.getColumnas() != null && caja.getColumnas() > 0) {
            cajaBD.setColumnas(caja.getColumnas());
        }
        cajaBD.setTipoCaja(caja.getTipoCaja());
        cajaBD.setColor(caja.getColor());
        cajaBD.setObservaciones(caja.getObservaciones());
        // Aplicar siempre: null = quitar posición, non-null = asignar/mantener
        if (caja.getPosicionPiso() != null && caja.getPosicionPiso().getId() != null) {
            PosicionPiso posicion = posicionPisoRepository.findById(caja.getPosicionPiso().getId())
                    .orElseThrow(() -> new ObjNotFoundException("La posición de piso no existe"));
            cajaBD.setPosicionPiso(posicion);
        } else {
            cajaBD.setPosicionPiso(null);
        }
        cajaBD.setActivo(caja.getActivo() != null ? caja.getActivo() : cajaBD.getActivo());
        // La fechaRegistro no se actualiza (updatable = false)
        return cajaCriogenicaRepository.save(cajaBD);
    }
}