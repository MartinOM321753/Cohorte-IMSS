package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.almacenamiento.caja.PosicionCajaService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.HistorialCambioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.TipoMuestraService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class MuestraApplicationService {

    private final MuestraService muestraService;
    private final PacienteService pacienteService;
    private final UserService userService;
    private final PosicionCajaService posicionCajaService;
    private final TipoMuestraService tipoMuestraService;
    private final HistorialCambioMuestraService historialService;

    @Transactional(readOnly = true)
    public List<Muestra> getAllMuestras() {
        return muestraService.getAll();
    }

    @Transactional(readOnly = true)
    public Muestra getMuestra(Long id) {
        return muestraService.getById(id);
    }

    @Transactional(readOnly = true)
    public List<Muestra> getMuestrasByPacienteUUID(String uuid) {
        return muestraService.getAll().stream()
            .filter(m -> m.getPaciente().getUuid().equals(uuid))
            .toList();
    }

    @Transactional(readOnly = true)
    public long countMuestrasByPacienteUuid(String uuid) {
        return muestraService.countByPacienteUuid(uuid);
    }

    @Transactional
    public Muestra createMuestra(Muestra muestra) {
        Paciente paciente = pacienteService.getByUUID(muestra.getPaciente().getUuid());
        muestra.setPaciente(paciente);

        BeanUser usuario = userService.getByUUID(muestra.getUsuarioRecolecta().getUUID());
        muestra.setUsuarioRecolecta(usuario);

        if (muestra.getPosicionCaja() != null && muestra.getPosicionCaja().getId() != null) {
            PosicionCaja posicion = posicionCajaService.getById(muestra.getPosicionCaja().getId());
            if (posicion.getOcupada()) {
                throw new ObjConflictException("La posición de caja ya está ocupada");
            }
            muestra.setPosicionCaja(posicion);
        }

        // Stream C — resolver TipoMuestra / TuboMuestra si vienen en el request
        if (muestra.getTipoMuestra() != null && muestra.getTipoMuestra().getId() != null) {
            TipoMuestra tipo = tipoMuestraService.getById(muestra.getTipoMuestra().getId());
            muestra.setTipoMuestra(tipo);
        }
        if (muestra.getTuboMuestra() != null && muestra.getTuboMuestra().getId() != null) {
            TuboMuestra tubo = tipoMuestraService.getTuboById(muestra.getTuboMuestra().getId());
            muestra.setTuboMuestra(tubo);
        }

        Muestra saved = muestraService.create(muestra);

        if (saved.getPosicionCaja() != null) {
            marcarPosicionCajaOcupada(saved.getPosicionCaja().getId(), true);
        }

        // Stream C — auto-generar alícuotas si el tubo lo requiere
        if (saved.getTuboMuestra() != null) {
            int numAlicuotas = saved.getTuboMuestra().getNumeroAlicuotas() != null
                    ? saved.getTuboMuestra().getNumeroAlicuotas() : 0;
            if (numAlicuotas > 0) {
                generarAlicuotas(saved, numAlicuotas);
            }
        }

        return saved;
    }

    @Transactional
    public Muestra updateMuestra(Long id, Muestra muestra) {
        // Capturar estado anterior para historial ANTES de aplicar cambios
        Muestra muestraAnterior = muestraService.getById(id);
        Muestra muestraBD = muestraService.getById(id);

        // Resolver entidades por UUID antes de pasar al servicio de dominio
        // (el controller solo pasa uuid/uuid, sin id numérico)
        if (muestra.getPaciente() != null && muestra.getPaciente().getUuid() != null) {
            muestra.setPaciente(pacienteService.getByUUID(muestra.getPaciente().getUuid()));
        } else {
            muestra.setPaciente(muestraBD.getPaciente());
        }

        if (muestra.getUsuarioRecolecta() != null && muestra.getUsuarioRecolecta().getUUID() != null) {
            muestra.setUsuarioRecolecta(userService.getByUUID(muestra.getUsuarioRecolecta().getUUID()));
        } else {
            muestra.setUsuarioRecolecta(muestraBD.getUsuarioRecolecta());
        }

        Long idPosActual = muestraBD.getPosicionCaja() != null ? muestraBD.getPosicionCaja().getId() : null;
        Long idPosNueva = muestra.getPosicionCaja() != null ? muestra.getPosicionCaja().getId() : null;

        if (idPosNueva != null && !idPosNueva.equals(idPosActual)) {
            PosicionCaja nuevaPos = posicionCajaService.getById(idPosNueva);
            if (nuevaPos.getOcupada()) {
                throw new ObjConflictException("La posición de caja destino ya está ocupada");
            }
            if (idPosActual != null) {
                marcarPosicionCajaOcupada(idPosActual, false);
            }
            marcarPosicionCajaOcupada(idPosNueva, true);
        } else if (idPosNueva == null && idPosActual != null) {
            marcarPosicionCajaOcupada(idPosActual, false);
        }

        // Stream C — resolver TipoMuestra / TuboMuestra si vienen en el request
        if (muestra.getTipoMuestra() != null && muestra.getTipoMuestra().getId() != null) {
            TipoMuestra tipo = tipoMuestraService.getById(muestra.getTipoMuestra().getId());
            muestra.setTipoMuestra(tipo);
        }
        if (muestra.getTuboMuestra() != null && muestra.getTuboMuestra().getId() != null) {
            TuboMuestra tubo = tipoMuestraService.getTuboById(muestra.getTuboMuestra().getId());
            muestra.setTuboMuestra(tubo);
        }

        muestra.setId(id);
        Muestra actualizada = muestraService.update(muestra);

        // Registrar historial de cambios (comparar estado anterior vs nuevo)
        BeanUser usuarioEditor = actualizada.getUsuarioRecolecta(); // quien edita = quien recolecta (por ahora)
        historialService.registrarCambios(muestraAnterior, actualizada, usuarioEditor);

        return actualizada;
    }

    private void marcarPosicionCajaOcupada(Long idPosicion, Boolean ocupada) {
        PosicionCaja pos = posicionCajaService.getById(idPosicion);
        pos.setOcupada(ocupada);
        posicionCajaService.update(pos);
    }

    /**
     * Genera N alícuotas hija para la muestra primaria dada.
     * Cada alícuota hereda paciente, usuario, tipo, tubo, fecha de recolección de la primaria.
     * No tienen posición en caja (asignación diferida).
     * Etiqueta: {etiquetaPrimaria}-{prefijoCodigo}{i}  ej: "M001-EDTA-S1"
     */
    private void generarAlicuotas(Muestra primaria, int cantidad) {
        TuboMuestra tubo = primaria.getTuboMuestra();
        String prefijo = (tubo.getPrefijoCodigo() != null && !tubo.getPrefijoCodigo().isBlank())
                ? tubo.getPrefijoCodigo() : "A";
        String unidad = (tubo.getUnidadVolumen() != null && !tubo.getUnidadVolumen().isBlank())
                ? tubo.getUnidadVolumen() : primaria.getUnidad();

        for (int i = 1; i <= cantidad; i++) {
            Muestra alicuota = new Muestra();
            alicuota.setEtiqueta(primaria.getEtiqueta() + "-" + prefijo + i);
            alicuota.setValor(tubo.getVolumenAlicuota());
            alicuota.setUnidad(unidad);
            alicuota.setFechaRecoleccion(primaria.getFechaRecoleccion());
            alicuota.setPaciente(primaria.getPaciente());
            alicuota.setUsuarioRecolecta(primaria.getUsuarioRecolecta());
            alicuota.setTipoMuestra(primaria.getTipoMuestra());
            alicuota.setTuboMuestra(tubo);
            alicuota.setMuestraPadre(primaria);
            alicuota.setNumeroAlicuota(i);
            alicuota.setTotalAlicuotas(cantidad);
            alicuota.setFechaRegistro(Timestamp.valueOf(LocalDateTime.now()));
            // posicionCaja = null → asignación diferida
            muestraService.createAlicuota(alicuota);
        }
    }
}
