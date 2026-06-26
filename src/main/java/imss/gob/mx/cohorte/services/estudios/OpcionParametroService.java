package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.parametros.OpcionParametro;
import imss.gob.mx.cohorte.modules.estudios.parametros.OpcionParametroRepository;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
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
public class OpcionParametroService {

    private final OpcionParametroRepository opcionRepository;

    @Transactional(readOnly = true)
    public List<OpcionParametro> getByParametro(Long parametroId) {
        return opcionRepository.findAllByParametro_IdOrderByOrdenAsc(parametroId);
    }

    /**
     * Reemplaza todas las opciones de un parámetro con la lista proporcionada.
     * Trabaja a través de la colección del padre para que Hibernate gestione
     * el ciclo de vida (cascade + orphanRemoval) sin conflictos.
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceAll(ParametroEstudio parametro, List<String> valores) {
        Set<String> vistos = new LinkedHashSet<>();
        for (String valor : valores) {
            String val = valor.trim();
            if (val.isEmpty()) continue;
            if (!vistos.add(val.toLowerCase())) {
                throw new ValidationException("La opción \"" + val + "\" está repetida");
            }
        }

        parametro.getOpciones().clear();
        opcionRepository.flush();

        int orden = 1;
        for (String valor : valores) {
            String val = valor.trim();
            if (val.isEmpty()) continue;
            OpcionParametro op = new OpcionParametro();
            op.setParametro(parametro);
            op.setValor(val);
            op.setOrden(orden++);
            parametro.getOpciones().add(op);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public OpcionParametro addOpcion(ParametroEstudio parametro, String valor) {
        OpcionParametro op = new OpcionParametro();
        op.setParametro(parametro);
        op.setValor(valor.trim());
        op.setOrden(parametro.getOpciones().size() + 1);
        parametro.getOpciones().add(op);
        return op;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOpcion(Long opcionId) {
        OpcionParametro op = opcionRepository.findById(opcionId)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la opción"));
        ParametroEstudio parametro = op.getParametro();
        parametro.getOpciones().remove(op);
        int orden = 1;
        for (OpcionParametro restante : parametro.getOpciones()) {
            restante.setOrden(orden++);
        }
    }
}
