package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Qué se exige al guardar un estudio, ahora que un parámetro puede estar fuera de uso.
 *
 * <p>Antes no se exigía nada: la obligatoriedad vivía en los formularios, y una
 * petición que no pasara por ellos podía guardar un estudio a medias. Tenerla en el
 * servidor es lo que permite leer la ausencia de un resultado como «ese parámetro no
 * aplicaba» en vez de «alguien no lo llenó», que es de lo que depende mostrar bien
 * las capturas antiguas.</p>
 *
 * <p>La regla no es la misma al registrar que al editar, y la diferencia importa:
 * exigir la lista completa al editar dejaría atrapados los estudios a los que
 * legítimamente les falta un parámetro añadido después de su captura.</p>
 */
class ObligatoriedadPorParametroActivoTest {

    private ParametroEstudio parametro(long id, String nombre, boolean activo) {
        ParametroEstudio p = new ParametroEstudio();
        p.setId(id);
        p.setNombre(nombre);
        p.setActivo(activo);
        return p;
    }

    /** La misma decisión que toma exigirParametrosActivos. */
    private List<String> faltantesAlRegistrar(List<ParametroEstudio> delTipo, Set<Long> capturados) {
        return delTipo.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .filter(p -> !capturados.contains(p.getId()))
                .map(ParametroEstudio::getNombre)
                .collect(Collectors.toList());
    }

    /** La misma decisión que toma exigirNoPerderResultados. */
    private List<String> perdidosAlEditar(List<ParametroEstudio> conResultadoPrevio, Set<Long> entran) {
        return conResultadoPrevio.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .filter(p -> !entran.contains(p.getId()))
                .map(ParametroEstudio::getNombre)
                .collect(Collectors.toList());
    }

    // ── Al registrar ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Un parámetro fuera de uso no se echa en falta al registrar")
    void elRetiradoNoSeExige() {
        List<ParametroEstudio> delTipo = List.of(
                parametro(121L, "PR", true),
                parametro(229L, "P", false));

        assertTrue(faltantesAlRegistrar(delTipo, Set.of(121L)).isEmpty(),
                "Solo se capturó el que sigue en uso, y eso basta");
    }

    @Test
    @DisplayName("Un parámetro en uso sin capturar sí se echa en falta")
    void elActivoSeExige() {
        List<ParametroEstudio> delTipo = List.of(
                parametro(121L, "PR", true),
                parametro(122L, "QRS", true));

        assertEquals(List.of("QRS"), faltantesAlRegistrar(delTipo, Set.of(121L)),
                "El mensaje tiene que decir cuál falta, no solo que falta algo");
    }

    @Test
    @DisplayName("Un estudio sin ningún resultado no pasa")
    void estudioVacioNoPasa() {
        List<ParametroEstudio> delTipo = List.of(parametro(121L, "PR", true));
        assertEquals(List.of("PR"), faltantesAlRegistrar(delTipo, Set.of()),
                "Antes se aceptaba y quedaba un estudio sin un solo resultado");
    }

    // ── Al editar ───────────────────────────────────────────────────────────

    /**
     * El caso real: los electrocardiogramas capturados antes de que existiera el
     * parámetro «P». Entrar a corregirles una observación no puede obligar a
     * inventar un dato que nadie midió.
     */
    @Test
    @DisplayName("Editar un estudio antiguo no obliga a llenar un parámetro que nunca tuvo")
    void editarNoExigeLoQueNuncaEstuvo() {
        List<ParametroEstudio> conResultadoPrevio = List.of(parametro(121L, "PR", true));

        assertTrue(perdidosAlEditar(conResultadoPrevio, Set.of(121L)).isEmpty(),
                "«P» nunca estuvo en este estudio, así que su ausencia no es una pérdida");
    }

    @Test
    @DisplayName("Una edición no puede dejar sin resultado un parámetro que lo tenía")
    void editarNoPuedeBorrarLoQueHabia() {
        List<ParametroEstudio> conResultadoPrevio = List.of(
                parametro(121L, "PR", true),
                parametro(122L, "QRS", true));

        assertEquals(List.of("QRS"), perdidosAlEditar(conResultadoPrevio, Set.of(121L)));
    }

    /**
     * La contrapartida de la regla anterior: si el parámetro quedó fuera de uso, su
     * valor sí se puede vaciar. Se conserva por defecto, pero no queda congelado.
     */
    @Test
    @DisplayName("El valor de un parámetro retirado sí se puede vaciar")
    void elRetiradoSePuedeVaciar() {
        List<ParametroEstudio> conResultadoPrevio = List.of(
                parametro(121L, "PR", true),
                parametro(229L, "P", false));

        assertTrue(perdidosAlEditar(conResultadoPrevio, Set.of(121L)).isEmpty(),
                "Conservarlo es lo que pasa si no se toca, no una condena");
    }
}
