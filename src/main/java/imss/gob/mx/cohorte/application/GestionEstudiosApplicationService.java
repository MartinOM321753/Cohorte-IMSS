package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.estudios.parametros.OpcionParametro;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.services.estudios.OpcionParametroService;
import imss.gob.mx.cohorte.services.estudios.ParametroEstudioService;
import imss.gob.mx.cohorte.services.estudios.ResultadoService;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;

@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.ESTUDIOS_MEDICOS)
public class GestionEstudiosApplicationService {

    private final TipoService tipoService;
    private final ParametroEstudioService parametroService;
    private final ResultadoService resultadoService;
    private final OpcionParametroService opcionService;

    @Transactional(readOnly = true)
    public List<TipoEstudio> getAllByEstatus() {
        return tipoService.getAllByStatus(true);
    }

    /** Todos los tipos de la institución actual (activos e inactivos) para gestión de catálogo. */
    @Transactional(readOnly = true)
    public List<TipoEstudio> getAllTipos() {
        return tipoService.getAllByInstitucion();
    }
    @Transactional
    public TipoEstudio getByName(String nombre) {
        return tipoService.getByName(nombre);
    }
    @Transactional
    public TipoEstudio getOne(Long id) {
        return tipoService.getOne(id);
    }

    @Transactional
    public TipoEstudio createTipoService(TipoEstudio tipoEstudio) {
        return tipoService.create(tipoEstudio);
    }

    @Transactional
    public TipoEstudio update(TipoEstudio tipoEstudio) {
        return tipoService.update(tipoEstudio);
    }
    @Transactional
    public Boolean Active(Long id){
        return tipoService.Active(id);
    }
    //PARAMETROS
    @Transactional(readOnly = true)
    public List<ParametroEstudio> getParametrosByTipo(Long tipoEstudioId) {
        // getOne verifica que el tipo pertenezca a la institución del usuario.
        getOne(tipoEstudioId);
        return parametroService.getByTipoEstudio(tipoEstudioId);
    }

    @Transactional
    public ParametroEstudio createParametro(ParametroEstudio parametroEstudio, List<String> opciones) {
        TipoEstudio tipoEstudio = tipoService.getOne(parametroEstudio.getTipoEstudio().getId());
        if (tipoEstudio == null) throw new ObjNotFoundException("No se encontro el tipo de estudio");
        parametroEstudio.setTipoEstudio(tipoEstudio);
        ParametroEstudio creado = parametroService.create(parametroEstudio);
        if (creado.getTipo() == TipoParametro.TEXTO_OPCIONES && opciones != null && !opciones.isEmpty()) {
            opcionService.replaceAll(creado, opciones);
        }
        return creado;
    }

    @Transactional
    public ParametroEstudio updateParametro(ParametroEstudio parametroEstudio, List<String> opciones) {
        TipoEstudio tipoEstudio = tipoService.getOne(parametroEstudio.getTipoEstudio().getId());
        if (tipoEstudio == null) throw new ObjNotFoundException("No se encontro el tipo de estudio");
        parametroEstudio.setTipoEstudio(tipoEstudio);
        ParametroEstudio actualizado = parametroService.update(parametroEstudio);
        if (actualizado.getTipo() == TipoParametro.TEXTO_OPCIONES) {
            opcionService.replaceAll(actualizado, opciones != null ? opciones : List.of());
        }
        return actualizado;
    }

    @Transactional
    public ParametroEstudio deleteParametro(Long id) {
        ResultadoEstudio resultadoEstudio = resultadoService.findResultadoByParametroId(id);
        if (resultadoEstudio != null) throw new RuntimeException("No se puede eliminar el parametro porque tiene resultados");
        return parametroService.delete(id);
    }

    // ─── Opciones individuales ───────────────────────────────────────────────

    @Transactional
    public OpcionParametro addOpcion(Long parametroId, String valor) {
        ParametroEstudio parametro = parametroService.getOne(parametroId);
        if (parametro.getTipo() != TipoParametro.TEXTO_OPCIONES) {
            throw new IllegalArgumentException("Solo se pueden agregar opciones a parámetros de tipo TEXTO_OPCIONES");
        }
        return opcionService.addOpcion(parametro, valor);
    }

    @Transactional
    public void deleteOpcion(Long opcionId) {
        opcionService.deleteOpcion(opcionId);
    }
}
