package imss.gob.mx.cohorte.services.configuracion;

import imss.gob.mx.cohorte.modules.configuracion.ConfiguracionClave;
import imss.gob.mx.cohorte.modules.configuracion.ConfiguracionClaveRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfiguracionClaveService {

    private final ConfiguracionClaveRepository repository;
    private final InstitucionContextService institucionContext;

    public Optional<String> getValor(String clave) {
        return repository
                .findByInstitucion_IdAndClave(institucionContext.getIdInstitucionActual(), clave)
                .map(ConfiguracionClave::getValor);
    }

    @Transactional
    public void setValor(String clave, String valor) {
        Long instId = institucionContext.getIdInstitucionActual();
        ConfiguracionClave entity = repository
                .findByInstitucion_IdAndClave(instId, clave)
                .orElseGet(() -> {
                    ConfiguracionClave c = new ConfiguracionClave();
                    c.setInstitucion(institucionContext.getInstitucionActual());
                    c.setClave(clave);
                    return c;
                });
        entity.setValor(valor);
        repository.save(entity);
    }
}
