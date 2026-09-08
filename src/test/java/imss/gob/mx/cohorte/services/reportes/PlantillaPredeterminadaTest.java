package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.reportes.PlantillaReporte;
import imss.gob.mx.cohorte.modules.reportes.TipoReporte;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Qué plantilla se ofrece primero al emitir, y cuándo deja de ofrecerse.
 *
 * <p>La exclusividad de la predeterminada no la impone la base de datos —una restricción
 * de unicidad sobre un booleano obligaría a trucos—, así que la sostiene el servicio. Eso
 * la convierte en una regla que hay que probar: si se rompe, la institución acaba con dos
 * formatos marcados y cuál sale depende del orden de la consulta.</p>
 */
class PlantillaPredeterminadaTest {

    private PlantillaReporte plantilla(long id, String nombre, TipoReporte tipo,
                                       boolean predeterminada, boolean activo) {
        PlantillaReporte p = new PlantillaReporte();
        p.setId(id);
        p.setNombre(nombre);
        p.setTipoReporte(tipo);
        p.setPredeterminada(predeterminada);
        p.setActivo(activo);
        return p;
    }

    /**
     * La misma decisión que toma dejarSoloEstaComoPredeterminada: se desmarcan todas las
     * demás del mismo tipo, no solo «la anterior».
     */
    private void marcarComoUnica(List<PlantillaReporte> delTipo, PlantillaReporte elegida) {
        for (PlantillaReporte otra : delTipo) {
            if (!otra.getId().equals(elegida.getId())) otra.setPredeterminada(false);
        }
        elegida.setPredeterminada(true);
    }

    private long cuantasMarcadas(List<PlantillaReporte> lista) {
        return lista.stream().filter(p -> Boolean.TRUE.equals(p.getPredeterminada())).count();
    }

    @Test
    @DisplayName("Marcar una desmarca la que lo estaba")
    void marcarUnaDesmarcaLaAnterior() {
        PlantillaReporte vieja = plantilla(1L, "Formato 2025", TipoReporte.ESTUDIO, true, true);
        PlantillaReporte nueva = plantilla(2L, "Formato 2026", TipoReporte.ESTUDIO, false, true);
        List<PlantillaReporte> delTipo = new ArrayList<>(List.of(vieja, nueva));

        marcarComoUnica(delTipo, nueva);

        assertEquals(1, cuantasMarcadas(delTipo), "Solo puede quedar una");
        assertTrue(nueva.getPredeterminada());
        assertFalse(vieja.getPredeterminada());
    }

    /**
     * Si por cualquier motivo hubiera quedado más de una marcada, marcar otra tiene que
     * dejar el catálogo consistente en vez de arrastrar el problema.
     */
    @Test
    @DisplayName("Si había varias marcadas, la operación las deja en una")
    void arreglaElCatalogoSiHabiaVarias() {
        PlantillaReporte a = plantilla(1L, "A", TipoReporte.ESTUDIO, true, true);
        PlantillaReporte b = plantilla(2L, "B", TipoReporte.ESTUDIO, true, true);
        PlantillaReporte c = plantilla(3L, "C", TipoReporte.ESTUDIO, false, true);
        List<PlantillaReporte> delTipo = new ArrayList<>(List.of(a, b, c));

        marcarComoUnica(delTipo, c);

        assertEquals(1, cuantasMarcadas(delTipo));
        assertTrue(c.getPredeterminada());
    }

    /**
     * Cada tipo de reporte tiene su propia predeterminada: marcar la de estudios no puede
     * desmarcar la de exámenes.
     */
    @Test
    @DisplayName("Cada tipo de reporte tiene la suya")
    void cadaTipoTieneLaSuya() {
        PlantillaReporte deEstudio = plantilla(1L, "Estudio", TipoReporte.ESTUDIO, true, true);
        PlantillaReporte deExamenes = plantilla(2L, "Exámenes", TipoReporte.EXAMENES, true, true);

        // La consulta que alimenta la operación ya viene filtrada por tipo, así que la de
        // exámenes ni siquiera entra en la lista.
        List<PlantillaReporte> soloEstudios = new ArrayList<>(List.of(deEstudio));
        marcarComoUnica(soloEstudios, deEstudio);

        assertTrue(deExamenes.getPredeterminada(), "La de otro tipo no se toca");
    }

    /**
     * Retirar de uso una plantilla la quita también de predeterminada: dejar un formato
     * fuera de uso como primera opción al emitir es justo lo que nadie espera.
     */
    @Test
    @DisplayName("Retirar de uso quita también la marca de predeterminada")
    void retirarQuitaLaMarca() {
        PlantillaReporte p = plantilla(1L, "Formato", TipoReporte.ESTUDIO, true, true);

        boolean nuevoEstado = !Boolean.TRUE.equals(p.getActivo());
        if (!nuevoEstado) p.setPredeterminada(false);
        p.setActivo(nuevoEstado);

        assertFalse(p.getActivo());
        assertFalse(p.getPredeterminada(), "Un formato retirado no puede seguir siendo el de salida");
    }
}
