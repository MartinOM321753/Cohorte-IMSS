package imss.gob.mx.cohorte.services.almacenamiento.refrigerador;

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
public class RefrigeradorService {
    private final RefrigeradorRepository refrigeradorRepository;

    @Transactional(readOnly = true)
    public List<Refrigerador> getAllRefrigeradores() {
        return refrigeradorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Refrigerador getRefrigerador(Long id) {
        return refrigeradorRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el refrigerador con id: " + id));
    }

    @Transactional
    public Refrigerador createRefrigerador(Refrigerador refrigerador) {
        refrigerador.setFechaRegistro(Timestamp.from(Instant.now()));
        refrigerador.setActivo(true);
        return refrigeradorRepository.save(refrigerador);
    }

    @Transactional
    public Refrigerador updateRefrigerador(Refrigerador refrigerador) {
        Refrigerador refBD = refrigeradorRepository.findById(refrigerador.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el refrigerador con id: " + refrigerador.getId()));

        refBD.setCodigo(refrigerador.getCodigo());
        refBD.setNombre(refrigerador.getNombre());
        refBD.setMarca(refrigerador.getMarca());
        refBD.setModelo(refrigerador.getModelo());
        refBD.setActivo(refrigerador.getActivo());
        // fechaRegistro no se actualiza (updatable = false)

        return refrigeradorRepository.save(refBD);
    }
}