package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.OpcionParametroEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.OpcionParametroEstudioMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestra;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
     * El orden se asigna secuencialmente (1-indexed).
     */
    @Transactional(rollbackFor = Exception.class)
    public List<OpcionParametroEstudioMuestra> replaceAll(ParametroEstudioMuestra parametro, List<String> valores) {
        repository.deleteAllByParametro_Id(parametro.getId());
        repository.flush();
        // El borrado anterior va directo por repositorio y no actualiza la coleccion
        // en memoria de `parametro` (EAGER + cascade=ALL). Si no se limpia aqui, al
        // hacer flush del padre al final de la transaccion Hibernate intenta volver
        // a guardar esas opciones ya eliminadas -> "deleted object would be re-saved
        // by cascade" (500).
        parametro.getOpciones().clear();

        for (int i = 0; i < valores.size(); i++) {
            String val = valores.get(i).trim();
            if (val.isEmpty()) continue;
            OpcionParametroEstudioMuestra op = new OpcionParametroEstudioMuestra();
            op.setParametro(parametro);
            op.setValor(val);
            op.setOrden(i + 1);
            repository.save(op);
        }
        return repository.findAllByParametro_IdOrderByOrdenAsc(parametro.getId());
    }

    /**
     * Agrega una nueva opción al final de la lista.
     */
    @Transactional(rollbackFor = Exception.class)
    public OpcionParametroEstudioMuestra addOpcion(ParametroEstudioMuestra parametro, String valor) {
        List<OpcionParametroEstudioMuestra> actuales =
                repository.findAllByParametro_IdOrderByOrdenAsc(parametro.getId());
        OpcionParametroEstudioMuestra op = new OpcionParametroEstudioMuestra();
        op.setParametro(parametro);
        op.setValor(valor.trim());
        op.setOrden(actuales.size() + 1);
        return repository.save(op);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOpcion(Long opcionId) {
        OpcionParametroEstudioMuestra op = repository.findById(opcionId)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la opción"));
        Long parametroId = op.getParametro().getId();
        repository.delete(op);
        // Renumerar restantes
        List<OpcionParametroEstudioMuestra> restantes =
                repository.findAllByParametro_IdOrderByOrdenAsc(parametroId);
        for (int i = 0; i < restantes.size(); i++) {
            restantes.get(i).setOrden(i + 1);
            repository.save(restantes.get(i));
        }
    }
}
