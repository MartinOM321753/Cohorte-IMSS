package imss.gob.mx.cohorte.services.almacenamiento.refrigerador;

import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.RefrigeradorRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefrigeradorService {
    private final RefrigeradorRepository refrigeradorRepository;

    public List<Refrigerador> getAllRefrigeradores() {
        return refrigeradorRepository.findAll();
    }

    public Refrigerador getRefrigerador(Long id) {
        return refrigeradorRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el refrigerador con id: " + id));
    }
    public Refrigerador getRefrigeradorByCode(String code) {
        return refrigeradorRepository.findByCodigo(code)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el refrigerador con codigo: " + code));
    }

    public Refrigerador createRefrigerador(Refrigerador refrigerador) {
        refrigerador.setFechaRegistro(Timestamp.from(Instant.now()));
        refrigerador.setActivo(true);
        return refrigeradorRepository.save(refrigerador);
    }

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

    public void deleteRefrigerador(Long id) {
        Refrigerador refBD = refrigeradorRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el refrigerador con id: " + id));
        if(!refBD.getPisos().isEmpty()){
            throw new ObjConflictException("No se puede eliminar un refrigerador que tenga pisos asociados." );
        }
        refrigeradorRepository.delete(refBD);
    }
}