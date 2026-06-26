package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.OpcionParametroEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.OpcionParametroEstudioMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestra;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class OpcionParametroEstudioMuestraService {

    private final OpcionParametroEstudioMuestraRepository repository;

    @Transactional(readOnly = true)
    public List<OpcionParametroEstudioMuestra> getByParametro(Long parametroId) {
        return repository.findAllByParametro_IdOrderByOrdenAsc(parametroId);
    }

    /**
     * Reemplaza completamente las opciones del parámetro con los valores dados.
     * Trabaja a través de la colección del padre para que Hibernate gestione
     * el ciclo de vida (cascade + orphanRemoval) sin conflictos.
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceAll(ParametroEstudioMuestra parametro, List<String> valores) {
        Set<String> vistos = new LinkedHashSet<>();
        for (String valor : valores) {
            String val = valor.trim();
            if (val.isEmpty()) continue;
            if (!vistos.add(val.toLowerCase())) {
                throw new ValidationException("La opción \"" + val + "\" está repetida");
            }
        }

        parametro.getOpciones().clear();
        repository.flush();

        int orden = 1;
        for (String valor : valores) {
            String val = valor.trim();
            if (val.isEmpty()) continue;
            OpcionParametroEstudioMuestra op = new OpcionParametroEstudioMuestra();
            op.setParametro(parametro);
            op.setValor(val);
            op.setOrden(orden++);
            parametro.getOpciones().add(op);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public OpcionParametroEstudioMuestra addOpcion(ParametroEstudioMuestra parametro, String valor) {
        OpcionParametroEstudioMuestra op = new OpcionParametroEstudioMuestra();
        op.setParametro(parametro);
        op.setValor(valor.trim());
        op.setOrden(parametro.getOpciones().size() + 1);
        parametro.getOpciones().add(op);
        return op;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOpcion(Long opcionId) {
        OpcionParametroEstudioMuestra op = repository.findById(opcionId)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la opción"));
        ParametroEstudioMuestra parametro = op.getParametro();
        parametro.getOpciones().remove(op);
        int orden = 1;
        for (OpcionParametroEstudioMuestra restante : parametro.getOpciones()) {
            restante.setOrden(orden++);
        }
    }
}
