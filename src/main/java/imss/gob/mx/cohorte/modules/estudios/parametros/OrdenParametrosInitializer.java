package imss.gob.mx.cohorte.modules.estudios.parametros;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.TipoEstudioMuestraRepository;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Reparte el orden inicial de los parámetros cuando la columna {@code orden}
 * aparece por primera vez sobre catálogos que ya existían.
 *
 * <p>El DEFAULT de la columna no alcanza: deja a todos los parámetros de un tipo
 * empatados en cero, y con el empate el orden vuelve a ser el que decida la base
 * —justo lo que esta funcionalidad viene a quitar de en medio—. Hay que escribir
 * un número por fila.</p>
 *
 * <p>Qué número es lo importante. El administrador no pidió reordenar nada
 * todavía, así que el punto de partida tiene que ser <em>el orden que ya veía</em>,
 * o la primera consecuencia visible de agregar la funcionalidad sería que todos
 * los formularios cambiaron solos. Y ese orden previo no es el mismo en los dos
 * catálogos: los parámetros de estudio se leían sin ORDER BY y salían por id,
 * mientras que los de estudio de muestra tenían {@code @OrderBy("nombre ASC")}.
 * Por eso cada uno se numera con su propio criterio.</p>
 *
 * <p>Solo toca los tipos en los que <em>todos</em> los parámetros siguen en cero,
 * que es la huella de un catálogo que nunca se ha ordenado. En cuanto reparte
 * números, o en cuanto alguien reordena desde la pantalla, deja de reconocerlos y
 * no vuelve a tocarlos: un orden configurado a mano no se pisa en el siguiente
 * arranque.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(5)
public class OrdenParametrosInitializer {

    private final TipoEstudioRepository tipoEstudioRepository;
    private final ParametroEstudioRepository parametroEstudioRepository;
    private final TipoEstudioMuestraRepository tipoEstudioMuestraRepository;
    private final ParametroEstudioMuestraRepository parametroEstudioMuestraRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void repartirOrdenInicial() {
        int tiposEstudio = numerarEstudios();
        int tiposMuestra = numerarEstudiosDeMuestra();

        if (tiposEstudio + tiposMuestra > 0) {
            log.info("Orden inicial de parámetros repartido en {} tipo(s) de estudio y {} tipo(s) de estudio de muestra",
                    tiposEstudio, tiposMuestra);
        }
    }

    /** Numera por id: es el orden en que se venían leyendo, o sea el de creación. */
    private int numerarEstudios() {
        int tiposNumerados = 0;
        for (var tipo : tipoEstudioRepository.findAll()) {
            List<ParametroEstudio> parametros =
                    parametroEstudioRepository.findAllByTipoEstudio_Id(tipo.getId());
            List<ParametroEstudio> numerados = numerar(
                    parametros,
                    ParametroEstudio::getOrden,
                    ParametroEstudio::setOrden,
                    Comparator.comparing(ParametroEstudio::getId));
            if (!numerados.isEmpty()) {
                parametroEstudioRepository.saveAll(numerados);
                tiposNumerados++;
            }
        }
        return tiposNumerados;
    }

    /** Numera por nombre: es lo que hacía el {@code @OrderBy} que esta columna reemplaza. */
    private int numerarEstudiosDeMuestra() {
        int tiposNumerados = 0;
        for (var tipo : tipoEstudioMuestraRepository.findAll()) {
            List<ParametroEstudioMuestra> parametros =
                    parametroEstudioMuestraRepository.findAllByTipoEstudioMuestra_Id(tipo.getId());
            List<ParametroEstudioMuestra> numerados = numerar(
                    parametros,
                    ParametroEstudioMuestra::getOrden,
                    ParametroEstudioMuestra::setOrden,
                    Comparator.comparing(ParametroEstudioMuestra::getNombre, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(ParametroEstudioMuestra::getId));
            if (!numerados.isEmpty()) {
                parametroEstudioMuestraRepository.saveAll(numerados);
                tiposNumerados++;
            }
        }
        return tiposNumerados;
    }

    /**
     * Devuelve los parámetros ya numerados, o vacío si este tipo no hay que tocarlo.
     *
     * <p>Un tipo con un solo parámetro se deja en paz aunque esté en cero: no hay
     * dos filas que puedan quedar empatadas, así que no hay nada que decidir.</p>
     */
    private <T> List<T> numerar(
            List<T> parametros,
            Function<T, Integer> leerOrden,
            java.util.function.BiConsumer<T, Integer> escribirOrden,
            Comparator<T> criterioPrevio) {

        if (parametros.size() < 2) return List.of();

        boolean sinOrdenar = parametros.stream()
                .allMatch(p -> leerOrden.apply(p) == null || leerOrden.apply(p) == 0);
        if (!sinOrdenar) return List.of();

        List<T> ordenados = parametros.stream().sorted(criterioPrevio).toList();
        for (int i = 0; i < ordenados.size(); i++) {
            escribirOrden.accept(ordenados.get(i), i);
        }
        return ordenados;
    }
}
