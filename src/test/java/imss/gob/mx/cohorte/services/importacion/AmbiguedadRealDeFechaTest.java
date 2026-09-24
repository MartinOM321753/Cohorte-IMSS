package imss.gob.mx.cohorte.services.importacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La diferencia entre «no encontre desempate» y «hay algo que elegir».
 *
 * <p>{@code inferirOrden} responde lo primero, y eso tambien ocurre cuando no hay
 * ninguna fecha que desempatar. Un archivo entero en ISO no desempata nada, pero
 * tampoco admite dos lecturas: {@code parsear} resuelve el ISO antes de mirar el
 * orden. Decirle al usuario «sus fechas admiten dos lecturas» en ese caso es una
 * falsa alarma, y las falsas alarmas ensenan a ignorar los avisos.</p>
 */
class AmbiguedadRealDeFechaTest {

    @Test
    @DisplayName("Dos numericas que no desempatan: la eleccion importa")
    void numericasSinDesempate() {
        assertTrue(NormalizadorFecha.admiteDosLecturas(List.of("03/04/2026", "05/06/2026")));
        assertTrue(NormalizadorFecha.admiteDosLecturas(List.of("1-2-2026")));
        assertTrue(NormalizadorFecha.admiteDosLecturas(List.of("03/04/2026 08:30")));
    }

    @Test
    @DisplayName("Una numerica ya desempatada no deja nada que elegir")
    void numericaYaDesempatada() {
        assertFalse(NormalizadorFecha.admiteDosLecturas(List.of("25/12/2026")), "25 solo es dia");
        assertFalse(NormalizadorFecha.admiteDosLecturas(List.of("12/25/2026")), "25 solo es dia");
    }

    @Test
    @DisplayName("Con el ano delante no hay dilema posible")
    void isoNoEsAmbigua() {
        assertFalse(NormalizadorFecha.admiteDosLecturas(List.of("2026-08-07", "2026-12-25")));
        assertFalse(NormalizadorFecha.admiteDosLecturas(List.of("2026-08-07T09:30")));
    }

    @Test
    @DisplayName("El mes en letra tampoco")
    void mesEnLetraNoEsAmbiguo() {
        assertFalse(NormalizadorFecha.admiteDosLecturas(List.of("7-ago-2026", "25 dic 2026")));
    }

    @Test
    @DisplayName("Sin fechas, o con basura, no hay ambiguedad que avisar")
    void sinFechas() {
        assertFalse(NormalizadorFecha.admiteDosLecturas(null));
        assertFalse(NormalizadorFecha.admiteDosLecturas(List.of()));
        assertFalse(NormalizadorFecha.admiteDosLecturas(Arrays.asList("", "   ", null)));
        assertFalse(NormalizadorFecha.admiteDosLecturas(List.of("ayer", "s/f")));
    }

    /**
     * Una sola fila ambigua basta: el orden se aplica al archivo entero, asi que
     * si una fila puede leerse de dos formas, la eleccion la afecta.
     */
    @Test
    @DisplayName("Basta una fila ambigua entre muchas que no lo son")
    void unaBasta() {
        assertTrue(NormalizadorFecha.admiteDosLecturas(
                List.of("2026-08-07", "25/12/2026", "03/04/2026")));
    }
}
