package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.services.importacion.NormalizadorFecha.Orden;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Las fechas son el punto donde una carga masiva corrompe datos sin fallar: una
 * fecha leida al reves no lanza error, guarda el estudio en otro mes.
 */
class NormalizadorFechaTest {

    // ── Inferencia del orden ─────────────────────────────────────────────────

    @Test
    void unDiaMayorQue12FijaElOrdenParaTodoElArchivo() {
        // 25 no puede ser un mes, asi que el archivo es dia/mes.
        var i = NormalizadorFecha.inferirOrden(List.of("03/04/2026", "25/12/2026"));
        assertEquals(Orden.DIA_MES, i.orden());
        assertFalse(i.ambiguo());
    }

    @Test
    void unMesEnSegundaPosicionMayorQue12FijaElOrdenContrario() {
        var i = NormalizadorFecha.inferirOrden(List.of("03/04/2026", "12/25/2026"));
        assertEquals(Orden.MES_DIA, i.orden());
        assertFalse(i.ambiguo());
    }

    @Test
    void sinNingunDesempateElArchivoQuedaMarcadoComoAmbiguo() {
        // Todas las filas admiten las dos lecturas: hay que preguntar al usuario.
        var i = NormalizadorFecha.inferirOrden(List.of("03/04/2026", "05/06/2026"));
        assertTrue(i.ambiguo(), "sin desempate hay que preguntar, no adivinar");
    }

    @Test
    void unArchivoQueSeContradiceSeRechaza() {
        // 25/12 exige dia/mes y 12/25 exige mes/dia: elegir cualquiera corrompe la
        // mitad de las filas.
        assertThrows(ArchivoInvalidoException.class,
                () -> NormalizadorFecha.inferirOrden(List.of("25/12/2026", "12/25/2026")));
    }

    @Test
    void elFormatoIsoNoInterfiereEnLaInferencia() {
        var i = NormalizadorFecha.inferirOrden(List.of("2026-08-07", "2026-12-25"));
        assertTrue(i.ambiguo(), "las ISO no aportan desempate y no deben fingir que si");
    }

    // ── La ambiguedad importa de verdad ──────────────────────────────────────

    @Test
    void laMismaCadenaDaMesesDistintosSegunElOrden() {
        LocalDateTime comoDiaMes = NormalizadorFecha.parsear("03/04/2026", Orden.DIA_MES);
        LocalDateTime comoMesDia = NormalizadorFecha.parsear("03/04/2026", Orden.MES_DIA);

        assertEquals(4, comoDiaMes.getMonthValue(), "3 de abril");
        assertEquals(3, comoMesDia.getMonthValue(), "4 de marzo");
        // Justamente por esto no se puede adivinar.
    }

    // ── Formatos ─────────────────────────────────────────────────────────────

    @Test
    void iso() {
        assertEquals(LocalDateTime.of(2026, 8, 7, 0, 0),
                NormalizadorFecha.parsear("2026-08-07", Orden.DIA_MES));
        assertEquals(LocalDateTime.of(2026, 8, 7, 14, 30),
                NormalizadorFecha.parsear("2026-08-07T14:30", Orden.DIA_MES));
        assertEquals(LocalDateTime.of(2026, 8, 7, 14, 30, 15),
                NormalizadorFecha.parsear("2026-08-07T14:30:15", Orden.DIA_MES));
    }

    @Test
    void conBarrasYHora() {
        assertEquals(LocalDateTime.of(2026, 8, 7, 14, 30),
                NormalizadorFecha.parsear("07/08/2026 14:30", Orden.DIA_MES));
    }

    @Test
    void conGuiones() {
        assertEquals(LocalDateTime.of(2026, 8, 7, 0, 0),
                NormalizadorFecha.parsear("07-08-2026", Orden.DIA_MES));
    }

    @Test
    void conElAnoDelanteNoDependeDelOrden() {
        // 2026/08/07 no admite otra lectura, asi que ambos ordenes coinciden.
        assertEquals(NormalizadorFecha.parsear("2026/08/07", Orden.DIA_MES),
                     NormalizadorFecha.parsear("2026/08/07", Orden.MES_DIA));
    }

    @Test
    void conElMesEnLetra() {
        assertEquals(LocalDateTime.of(2026, 8, 7, 0, 0),
                NormalizadorFecha.parsear("7-ago-2026", Orden.DIA_MES));
        assertEquals(LocalDateTime.of(2026, 8, 7, 0, 0),
                NormalizadorFecha.parsear("Aug 7, 2026", Orden.DIA_MES));
    }

    // ── Zona horaria ─────────────────────────────────────────────────────────

    @Test
    void sinZonaSeConservaLaHoraTalComoVino() {
        // La captura manual guarda hora de pared sin convertir; si aqui se
        // convirtiera, las dos quedarian desplazadas entre si.
        assertEquals(LocalDateTime.of(2026, 8, 7, 14, 30),
                NormalizadorFecha.parsear("2026-08-07T14:30", Orden.DIA_MES));
    }

    @Test
    void conDesplazamientoExplicitoSiSeTraslada() {
        // El aparato dijo a que instante se refiere, asi que se lleva a la zona
        // del sistema en vez de tomar los digitos al pie de la letra.
        LocalDateTime r = NormalizadorFecha.parsear("2026-08-07T14:30:00Z", Orden.DIA_MES);
        LocalDateTime esperado = java.time.OffsetDateTime.parse("2026-08-07T14:30:00Z")
                .atZoneSameInstant(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
        assertEquals(esperado, r);
    }

    // ── Lo que se rechaza ────────────────────────────────────────────────────

    @Test
    void elAnoDeDosCifrasSeRechazaEnVezDeAdivinarElSiglo() {
        var e = assertThrows(NormalizadorFecha.FechaNoReconocidaException.class,
                () -> NormalizadorFecha.parsear("07/08/26", Orden.DIA_MES));
        assertTrue(e.getMessage().contains("dos cifras"), e.getMessage());
    }

    @Test
    void unaFechaQueNoExisteSeRechaza() {
        assertThrows(NormalizadorFecha.FechaNoReconocidaException.class,
                () -> NormalizadorFecha.parsear("31/02/2026", Orden.DIA_MES));
    }

    @Test
    void unTextoQueNoEsFechaSeRechazaDiciendoQueSeAdmite() {
        var e = assertThrows(NormalizadorFecha.FechaNoReconocidaException.class,
                () -> NormalizadorFecha.parsear("N/A", Orden.DIA_MES));
        assertTrue(e.getMessage().contains("2026-08-07"), "el mensaje debe enseñar formatos: " + e.getMessage());
    }

    @Test
    void vacioSeRechaza() {
        assertThrows(NormalizadorFecha.FechaNoReconocidaException.class,
                () -> NormalizadorFecha.parsear("  ", Orden.DIA_MES));
        assertThrows(NormalizadorFecha.FechaNoReconocidaException.class,
                () -> NormalizadorFecha.parsear(null, Orden.DIA_MES));
    }
}
