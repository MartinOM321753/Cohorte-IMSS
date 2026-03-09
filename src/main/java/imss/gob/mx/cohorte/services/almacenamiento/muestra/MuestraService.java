package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;

import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCajaRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MuestraService {

    private final MuestraRepository muestraRepository;
    private final PacienteRepository pacienteRepository;
    private final UserRepository userRepository;
    private final PosicionCajaRepository posicionCajaRepository;

    @Transactional(readOnly = true)
    public List<Muestra> getAll() {
        return muestraRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Muestra getById(Long id) {
        return muestraRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la muestra"));
    }

    @Transactional
    public Muestra create(Muestra muestra) {

        // Validar etiqueta única
        Optional<Muestra> etiquetaExistente = muestraRepository.findAll().stream()
                .filter(m -> m.getEtiqueta().equalsIgnoreCase(muestra.getEtiqueta()))
                .findFirst();
        if (etiquetaExistente.isPresent()) {
            throw new ObjConflictException("La etiqueta de la muestra ya existe");
        }

        // Validar existencia de paciente
        Paciente paciente = pacienteRepository.findById(muestra.getPaciente().getId())
                .orElseThrow(() -> new ObjNotFoundException("El paciente no existe"));
        muestra.setPaciente(paciente);

        // Validar existencia de usuario recolecta
        BeanUser usuarioRecolecta = userRepository.findById(muestra.getUsuarioRecolecta().getId())
                .orElseThrow(() -> new ObjNotFoundException("El usuario que recolecta no existe"));
        muestra.setUsuarioRecolecta(usuarioRecolecta);

        // Validar existencia de posición de caja solo si se asignó
        if (muestra.getPosicionCaja() != null && muestra.getPosicionCaja().getId() != null) {
            PosicionCaja posicionCaja = posicionCajaRepository.findById(muestra.getPosicionCaja().getId())
                    .orElseThrow(() -> new ObjNotFoundException("La posición de caja especificada no existe"));
            muestra.setPosicionCaja(posicionCaja);
        } else {
            muestra.setPosicionCaja(null);
        }

        muestra.setFechaRegistro(Timestamp.valueOf(LocalDateTime.now()));
        muestra.setFechaActualizacion(null);

        return muestraRepository.save(muestra);
    }

    @Transactional
    public Muestra update(Muestra muestra) {

        Muestra muestraBD = muestraRepository.findById(muestra.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la muestra"));

        // Validar si se cambia etiqueta y que sea única
        if (!muestraBD.getEtiqueta().equalsIgnoreCase(muestra.getEtiqueta())) {
            Optional<Muestra> etiquetaExistente = muestraRepository.findAll().stream()
                    .filter(m -> m.getEtiqueta().equalsIgnoreCase(muestra.getEtiqueta()))
                    .findFirst();
            if (etiquetaExistente.isPresent()) {
                throw new ObjConflictException("La etiqueta de la muestra ya existe");
            }
            muestraBD.setEtiqueta(muestra.getEtiqueta());
        }

        // Validar paciente si se intenta cambiar
        if (muestra.getPaciente() != null && !muestraBD.getPaciente().getId().equals(muestra.getPaciente().getId())) {
            Paciente paciente = pacienteRepository.findById(muestra.getPaciente().getId())
                    .orElseThrow(() -> new ObjNotFoundException("El paciente no existe"));
            muestraBD.setPaciente(paciente);
        }

        // Validar usuario recolecta si se intenta cambiar
        if (muestra.getUsuarioRecolecta() != null &&
                !muestraBD.getUsuarioRecolecta().getId().equals(muestra.getUsuarioRecolecta().getId())) {
            BeanUser usuarioRecolecta = userRepository.findById(muestra.getUsuarioRecolecta().getId())
                    .orElseThrow(() -> new ObjNotFoundException("El usuario que recolecta no existe"));
            muestraBD.setUsuarioRecolecta(usuarioRecolecta);
        }

        // Validar posición caja si se intenta cambiar
        if (muestra.getPosicionCaja() != null) {
            Long idCajaNueva = muestra.getPosicionCaja().getId();
            if (muestraBD.getPosicionCaja() == null ||
                    !muestraBD.getPosicionCaja().getId().equals(idCajaNueva)) {
                PosicionCaja posicionCaja = posicionCajaRepository.findById(idCajaNueva)
                        .orElseThrow(() -> new ObjNotFoundException("La posición de caja especificada no existe"));
                muestraBD.setPosicionCaja(posicionCaja);
            }
        } else {
            muestraBD.setPosicionCaja(null);
        }

        muestraBD.setValor(muestra.getValor());
        muestraBD.setUnidad(muestra.getUnidad());
        muestraBD.setFechaRecoleccion(muestra.getFechaRecoleccion());
        muestraBD.setObservaciones(muestra.getObservaciones());
        muestraBD.setFechaActualizacion(Timestamp.valueOf(LocalDateTime.now()));

        return muestraRepository.save(muestraBD);
    }
}