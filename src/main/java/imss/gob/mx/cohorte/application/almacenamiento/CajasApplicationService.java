package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPiso;
import imss.gob.mx.cohorte.services.almacenamiento.caja.CajaCriojenicaService;
import imss.gob.mx.cohorte.services.almacenamiento.caja.PosicionCajaService;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.PosicionPisoService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CajasApplicationService {

    private final CajaCriojenicaService cajaCriojenicaService;
    private final PosicionCajaService posicionCajaService;
    private final PosicionPisoService posicionPisoService;

    @Autowired
    public CajasApplicationService(CajaCriojenicaService cajaCriojenicaService, 
                                 PosicionCajaService posicionCajaService, 
                                 PosicionPisoService posicionPisoService) {
        this.cajaCriojenicaService = cajaCriojenicaService;
        this.posicionCajaService = posicionCajaService;
        this.posicionPisoService = posicionPisoService;
    }

    @Transactional(readOnly = true)
    public List<CajaCriogenica> getAllCajas() {
        return cajaCriojenicaService.getAll();
    }

    @Transactional(readOnly = true)
    public CajaCriogenica getCaja(Long id) {
        return cajaCriojenicaService.getById(id);
    }

    @Transactional
    public CajaCriogenica createCaja(CajaCriogenica caja, Long idPosicionPiso) {
        if (idPosicionPiso != null) {
            PosicionPiso posicion = posicionPisoService.getPosicion(idPosicionPiso);
            if (posicion.getOcupada()) {
                throw new ObjConflictException("La posición de piso ya está ocupada");
            }
            caja.setPosicionPiso(posicion);
        }
        CajaCriogenica saved = cajaCriojenicaService.create(caja);
        
        // Generar posiciones automáticamente
        posicionCajaService.generarPosicionesParaCaja(saved.getId());

        if (idPosicionPiso != null) {
            marcarPosicionPisoOcupada(idPosicionPiso, true);
        }
        return saved;
    }

    @Transactional
    public CajaCriogenica updateCaja(Long id, CajaCriogenica caja, Long idPosicionNueva) {
        CajaCriogenica cajaBD = cajaCriojenicaService.getById(id);

        Long idPosicionActual = cajaBD.getPosicionPiso() != null ? cajaBD.getPosicionPiso().getId() : null;

        // ── Gestión de posición de piso ──────────────────────────────────
        if (idPosicionNueva != null) {
            if (!idPosicionNueva.equals(idPosicionActual)) {
                // Mover a una posición diferente
                PosicionPiso nuevaPos = posicionPisoService.getPosicion(idPosicionNueva);
                if (nuevaPos.getOcupada()) {
                    throw new ObjConflictException("La posición de piso destino ya está ocupada");
                }
                if (idPosicionActual != null) {
                    marcarPosicionPisoOcupada(idPosicionActual, false); // liberar anterior
                }
                marcarPosicionPisoOcupada(idPosicionNueva, true);
                caja.setPosicionPiso(nuevaPos);
            } else {
                // Misma posición — mantener referencia para que el service la persista
                caja.setPosicionPiso(cajaBD.getPosicionPiso());
            }
        } else {
            // null → quitar la posición asignada
            if (idPosicionActual != null) {
                marcarPosicionPisoOcupada(idPosicionActual, false);
            }
            caja.setPosicionPiso(null);
        }

        int nuevasFilas = caja.getFilas() != null ? caja.getFilas() : cajaBD.getFilas();
        int nuevasColumnas = caja.getColumnas() != null ? caja.getColumnas() : cajaBD.getColumnas();
        boolean cambiaronDimensiones = (nuevasFilas != cajaBD.getFilas() || nuevasColumnas != cajaBD.getColumnas());

        if (cambiaronDimensiones) {
            List<PosicionCaja> todasPosiciones = posicionCajaService.getPositionsByCaja(id);

            List<PosicionCaja> afectadas = todasPosiciones.stream()
                .filter(p -> p.getOcupada() && (p.getFila() > nuevasFilas || p.getColumna() > nuevasColumnas))
                .toList();

            if (!afectadas.isEmpty()) {
                String detalles = afectadas.stream()
                    .map(p -> "Fila " + p.getFila() + " Col " + p.getColumna())
                    .collect(Collectors.joining(", "));
                throw new ObjConflictException(
                    "No se puede actualizar la caja: posiciones ocupadas fuera del nuevo rango " +
                    nuevasFilas + "x" + nuevasColumnas + " → " + detalles);
            }

            List<PosicionCaja> aEliminar = todasPosiciones.stream()
                .filter(p -> p.getFila() > nuevasFilas || p.getColumna() > nuevasColumnas)
                .toList();
            posicionCajaService.deletePositions(aEliminar);

            caja.setId(id);
            CajaCriogenica cajaActualizada = cajaCriojenicaService.update(caja);

            Set<String> existentes = todasPosiciones.stream()
                .filter(p -> p.getFila() <= nuevasFilas && p.getColumna() <= nuevasColumnas)
                .map(p -> p.getFila() + "-" + p.getColumna())
                .collect(Collectors.toSet());

            for (int f = 1; f <= nuevasFilas; f++) {
                for (int c = 1; c <= nuevasColumnas; c++) {
                    if (!existentes.contains(f + "-" + c)) {
                        posicionCajaService.crearPosicionSiNoExiste(cajaActualizada, f, c);
                    }
                }
            }

            return cajaActualizada;
        }

        caja.setId(id);
        return cajaCriojenicaService.update(caja);
    }

    @Transactional(readOnly = true)
    public List<PosicionCaja> getPosicionesByCaja(Long idCaja) {
        cajaCriojenicaService.getById(idCaja);
        return posicionCajaService.getAll().stream()
            .filter(p -> p.getCaja().getId().equals(idCaja))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PosicionCaja> getPosicionesLibresByCaja(Long idCaja) {
        return getPosicionesByCaja(idCaja).stream()
            .filter(p -> !p.getOcupada())
            .toList();
    }

    private void marcarPosicionPisoOcupada(Long idPosicion, Boolean ocupada) {
        PosicionPiso pos = posicionPisoService.getPosicion(idPosicion);
        pos.setOcupada(ocupada);
        posicionPisoService.updatePosicion(pos);
    }
}
