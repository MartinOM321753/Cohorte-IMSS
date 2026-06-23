package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.parametros.OpcionParametro;
import imss.gob.mx.cohorte.modules.estudios.parametros.OpcionParametroRepository;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
     * Se borran las existentes y se insertan las nuevas respetando el orden.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<OpcionParametro> replaceAll(ParametroEstudio parametro, List<String> valores) {
        opcionRepository.deleteAllByParametro_Id(parametro.getId());
        opcionRepository.flush();

        for (int i = 0; i < valores.size(); i++) {
            String val = valores.get(i).trim();
            if (val.isEmpty()) continue;
            OpcionParametro op = new OpcionParametro();
            op.setParametro(parametro);
            op.setValor(val);
            op.setOrden(i + 1);
            opcionRepository.save(op);
        }
        return opcionRepository.findAllByParametro_IdOrderByOrdenAsc(parametro.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public OpcionParametro addOpcion(ParametroEstudio parametro, String valor) {
        List<OpcionParametro> actuales = opcionRepository.findAllByParametro_IdOrderByOrdenAsc(parametro.getId());
        OpcionParametro op = new OpcionParametro();
        op.setParametro(parametro);
        op.setValor(valor.trim());
        op.setOrden(actuales.size() + 1);
        return opcionRepository.save(op);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOpcion(Long opcionId) {
        OpcionParametro op = opcionRepository.findById(opcionId)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la opción"));
        opcionRepository.delete(op);
        // Re-numerar opciones restantes del mismo parámetro
        List<OpcionParametro> restantes = opcionRepository.findAllByParametro_IdOrderByOrdenAsc(
                op.getParametro().getId());
        for (int i = 0; i < restantes.size(); i++) {
            restantes.get(i).setOrden(i + 1);
        }
        opcionRepository.saveAll(restantes);
    }

    @Transactional(rollbackFor = Exception.class)
    public void clearByParametro(Long parametroId) {
        opcionRepository.deleteAllByParametro_Id(parametroId);
    }
}
