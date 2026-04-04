package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPiso;
import imss.gob.mx.cohorte.services.almacenamiento.caja.CajaCriojenicaService;
import imss.gob.mx.cohorte.services.almacenamiento.caja.PosicionCajaService;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.PosicionPisoService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class CajasApplicationService {

    private final CajaCriojenicaService cajaCriojenicaService;
    private final PosicionCajaService posicionCajaService;
    private final PosicionPisoService posicionPisoService;

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
        if (idPosicionPiso != null) {
            marcarPosicionPisoOcupada(idPosicionPiso, true);
        }
        return saved;
    }

    @Transactional
    public CajaCriogenica updateCaja(Long id, CajaCriogenica caja) {
        CajaCriogenica cajaBD = cajaCriojenicaService.getById(id);

        Long idPosicionNueva = caja.getPosicionPiso() != null ? caja.getPosicionPiso().getId() : null;
        Long idPosicionActual = cajaBD.getPosicionPiso() != null ? cajaBD.getPosicionPiso().getId() : null;

        if (idPosicionNueva != null && !idPosicionNueva.equals(idPosicionActual)) {
            PosicionPiso nuevaPos = posicionPisoService.getPosicion(idPosicionNueva);
            if (nuevaPos.getOcupada()) {
                throw new ObjConflictException("La posición de piso destino ya está ocupada");
            }
            if (idPosicionActual != null) {
                marcarPosicionPisoOcupada(idPosicionActual, false);
            }
            marcarPosicionPisoOcupada(idPosicionNueva, true);
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
