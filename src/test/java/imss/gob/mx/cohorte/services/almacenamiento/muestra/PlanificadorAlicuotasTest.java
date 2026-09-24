package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El sitio donde el sistema inventaba volumen: un tubo de 5 × 50 mL creaba
 * siempre 5 alícuotas, se hubieran extraído 250 mL o 200.
 */
class PlanificadorAlicuotasTest {

    /** El tubo del ejemplo del cliente: 5 alícuotas de 50 mL de suero. */
    private static RecetaTubo suero5x50() {
        return new RecetaTubo("Suero", 5, 50.0, "mL", true);
    }

    private static RecetaTubo suero(int alicuotas, boolean permiteParcial) {
        return new RecetaTubo("Suero", alicuotas, 50.0, "mL", permiteParcial);
    }

    // ── Cuántas alcanzan ─────────────────────────────────────────────────────

    @Test
    void conVolumenExactoSeGeneraElLoteCompleto() {
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(suero5x50(), 250.0);

        assertEquals(5, plan.alicuotasCompletas());
        assertEquals(0.0, plan.remanente());
        assertTrue(plan.alcanzaLoteCompleto());
        assertEquals(List.of(50.0, 50.0, 50.0, 50.0, 50.0), plan.volumenesSugeridos());
    }

    @Test
    void con200mlSoloAlcanzanCuatroDeLasCincoConfiguradas() {
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(suero5x50(), 200.0);

        assertEquals(4, plan.alicuotasCompletas());
        assertEquals(0.0, plan.remanente());
        assertFalse(plan.alcanzaLoteCompleto());
        assertEquals(250.0, plan.totalRequerido());
        assertEquals(List.of(50.0, 50.0, 50.0, 50.0), plan.volumenesSugeridos());
        assertFalse(plan.puedeAlojarParcial(), "sin remanente no hay nada que alojar");
    }

    @Test
    void elSobranteNoCreaUnaSextaAlicuota() {
        // 300 mL dan para 6 de 50, pero el tubo define 5: las otras 50 se quedan
        // en la padre. Crear una sexta seria inventar un vial que no existe.
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(suero5x50(), 300.0);

        assertEquals(5, plan.alicuotasCompletas());
        assertEquals(50.0, plan.remanente());
        assertEquals(0, plan.lugaresRestantes());
        assertFalse(plan.puedeAlojarParcial(), "no quedan lugares en el tubo");
    }

    @Test
    void con220mlElPlanPorOmisionDejaElRemanenteEnLaPadre() {
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(suero5x50(), 220.0);

        assertEquals(4, plan.alicuotasCompletas());
        assertEquals(20.0, plan.remanente());
        assertEquals(1, plan.lugaresRestantes());
        assertTrue(plan.puedeAlojarParcial());
        assertEquals(4, plan.totalSugerido(), "por omision el remanente NO se alicuota");
    }

    @Test
    void siNoAlcanzaParaNingunaCompletaElPlanVieneVacio() {
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(suero5x50(), 30.0);

        assertEquals(0, plan.alicuotasCompletas());
        assertEquals(30.0, plan.remanente());
        assertTrue(plan.volumenesSugeridos().isEmpty());
        // Se puede pedir una parcial a proposito, pero nunca por omision.
        assertTrue(plan.puedeAlojarParcial());
    }

    @Test
    void unaMuestraSinVolumenNoPlanificaNada() {
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(suero5x50(), null);

        assertEquals(0, plan.alicuotasCompletas());
        assertEquals(0.0, plan.valorDisponible());
        assertTrue(plan.volumenesSugeridos().isEmpty());
    }

    // ── Reparto del remanente ────────────────────────────────────────────────

    @Test
    void elRemanentePuedeIrseEnUnaSolaAlicuotaParcial() {
        // Tubo de 6: se extrajeron 220, alcanzan 4 completas y sobran 20.
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(suero(6, true), 220.0);
        assertEquals(2, plan.lugaresRestantes());

        List<Double> volumenes = PlanificadorAlicuotas.distribuirRemanente(plan, 1);

        assertEquals(List.of(50.0, 50.0, 50.0, 50.0, 20.0), volumenes);
    }

    @Test
    void oRepartirseEntreLasQueFaltan() {
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(suero(6, true), 220.0);

        List<Double> volumenes = PlanificadorAlicuotas.distribuirRemanente(plan, 2);

        assertEquals(List.of(50.0, 50.0, 50.0, 50.0, 10.0, 10.0), volumenes);
    }

    @Test
    void unRepartoNoDivisibleSumaExactamenteElRemanente() {
        // 20 entre 3 da 6.666..., que redondeado hacia arriba tres veces daria
        // 20.0001 y lo rechazaria la validacion del propio plan que generamos.
        RecetaTubo tubo = new RecetaTubo("Suero", 8, 50.0, "mL", true);
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(tubo, 220.0);

        List<Double> volumenes = PlanificadorAlicuotas.distribuirRemanente(plan, 3);

        assertEquals(7, volumenes.size());
        assertEquals(220.0, PlanificadorAlicuotas.sumar(volumenes));
        // Y el plan resultante se valida contra si mismo sin quejas.
        assertDoesNotThrow(() -> PlanificadorAlicuotas.validarPlan(volumenes, tubo, 220.0));
    }

    @Test
    void noSePuedeRepartirEntreMasLugaresDeLosQueQuedan() {
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(suero5x50(), 220.0);

        ValidationException e = assertThrows(ValidationException.class,
                () -> PlanificadorAlicuotas.distribuirRemanente(plan, 2));
        assertTrue(e.getMessage().contains("entre 1 y 1"));
    }

    @Test
    void siElTuboNoAdmiteParcialesNoSeRepartaNada() {
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(suero(6, false), 220.0);

        assertFalse(plan.puedeAlojarParcial());
        assertThrows(ValidationException.class, () -> PlanificadorAlicuotas.distribuirRemanente(plan, 1));
    }

    // ── Validación del plan que llega del cliente ────────────────────────────

    @Test
    void unPlanQueCabeSeAcepta() {
        List<Double> ok = PlanificadorAlicuotas.validarPlan(
                List.of(50.0, 50.0, 50.0, 50.0, 20.0), suero5x50(), 220.0);

        assertEquals(List.of(50.0, 50.0, 50.0, 50.0, 20.0), ok);
    }

    @Test
    void ningunaAlicuotaPuedeExcederLaCapacidadDelVial() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> PlanificadorAlicuotas.validarPlan(List.of(50.0, 60.0), suero5x50(), 500.0));

        assertTrue(e.getMessage().contains("excede la capacidad"));
    }

    @Test
    void noSePuedenPedirMasAlicuotasDeLasQueElTuboDefine() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> PlanificadorAlicuotas.validarPlan(
                        List.of(50.0, 50.0, 50.0, 50.0, 50.0, 50.0), suero5x50(), 1000.0));

        assertTrue(e.getMessage().contains("define 5"));
    }

    @Test
    void elLoteNoPuedeSumarMasDeLoDisponible() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> PlanificadorAlicuotas.validarPlan(List.of(50.0, 50.0, 50.0), suero5x50(), 120.0));

        assertTrue(e.getMessage().contains("disponibles"));
    }

    @Test
    void unaAlicuotaDeCeroNoEsUnaAlicuota() {
        assertThrows(ValidationException.class,
                () -> PlanificadorAlicuotas.validarPlan(List.of(50.0, 0.0), suero5x50(), 200.0));
    }

    @Test
    void unTuboQueNoAdmiteParcialesRechazaElPlanConParcial() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> PlanificadorAlicuotas.validarPlan(List.of(50.0, 20.0), suero(5, false), 200.0));

        assertTrue(e.getMessage().contains("no admite alícuotas incompletas"));
    }

    @Test
    void unTuboDirectoNoPlanificaAlicuotas() {
        RecetaTubo directo = new RecetaTubo("Tubo directo", 0, null, "mL", false);

        assertThrows(ValidationException.class, () -> PlanificadorAlicuotas.planificar(directo, 100.0));
    }

    @Test
    void unTuboSinVolumenConfiguradoNoPuedeAlicuotar() {
        RecetaTubo sinVolumen = new RecetaTubo("Suero", 5, null, "mL", true);

        ValidationException e = assertThrows(ValidationException.class,
                () -> PlanificadorAlicuotas.planificar(sinVolumen, 200.0));
        assertTrue(e.getMessage().contains("volumen por alícuota"));
    }

    // ── Coma flotante ────────────────────────────────────────────────────────

    @Test
    void elErrorDelComaFlotanteNoRechazaUnPlanCorrecto() {
        // 0.1 + 0.2 = 0.30000000000000004 en double. Comparando en crudo, este
        // plan "no cabe" en 0.3 por una diezmilmillonesima de mililitro.
        RecetaTubo micro = new RecetaTubo("Micro", 2, 0.2, "mL", true);

        assertDoesNotThrow(() -> PlanificadorAlicuotas.validarPlan(List.of(0.1, 0.2), micro, 0.3));
    }

    @Test
    void laAritmeticaDeVolumenesNoArrastraError() {
        assertEquals(0.3, PlanificadorAlicuotas.sumar(0.1, 0.2));
        assertEquals(0.1, PlanificadorAlicuotas.restar(0.3, 0.2));
        assertTrue(PlanificadorAlicuotas.agotado(PlanificadorAlicuotas.restar(0.3, 0.3)));
    }

    // ── Unidades ─────────────────────────────────────────────────────────────

    @Test
    void unTuboEnOtraUnidadNoPuedeAlicuotarLaPadre() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> PlanificadorAlicuotas.validarUnidad("µL", "mL"));

        assertTrue(e.getMessage().contains("no convierte unidades"));
    }

    @Test
    void laMismaUnidadPasaAunqueCambieLaCaja() {
        assertDoesNotThrow(() -> PlanificadorAlicuotas.validarUnidad("ML", " mL "));
    }

    @Test
    void unaPadreHeredadaSinUnidadAdoptaLaDelTubo() {
        assertDoesNotThrow(() -> PlanificadorAlicuotas.validarUnidad("mL", null));
    }

    @Test
    void unTuboSinUnidadNoPuedeAlicuotar() {
        assertThrows(ValidationException.class, () -> PlanificadorAlicuotas.validarUnidad(null, "mL"));
    }

    // ── Completar un lote empezado ───────────────────────────────────────────

    @Test
    void unLoteEmpezadoSePuedeCompletarConElVolumenQueAparezcaDespues() {
        // El caso real: tubo de 3 x 12, se extraen 12 y solo alcanza para 1.
        // Mas tarde la padre sube a 36, se ubica la primera (descuenta 12) y
        // quedan 24 disponibles. Tienen que poder generarse las 2 que faltan.
        RecetaTubo tubo = new RecetaTubo("Heces", 3, 12.0, "dl", true);

        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(tubo, 24.0, 1);

        assertEquals(1, plan.slotsOcupados());
        assertEquals(2, plan.slotsLibres());
        assertEquals(2, plan.alicuotasCompletas());
        assertTrue(plan.alcanzaLoteCompleto(), "24 dl llenan los 2 huecos que quedan");
        assertEquals(List.of(12.0, 12.0), plan.volumenesSugeridos());
    }

    @Test
    void enUnaContinuacionElRequeridoEsLoQueFaltaNoElTuboEntero() {
        RecetaTubo tubo = new RecetaTubo("Heces", 3, 12.0, "dl", true);

        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(tubo, 6.0, 1);

        // Pedir 36 dl a quien ya gasto 12 le reclama volumen que ya no existe.
        assertEquals(24.0, plan.totalRequerido());
        assertTrue(plan.mensaje().contains("completar el lote (faltan 2 de 3)"), plan.mensaje());
    }

    @Test
    void loQueQuedaSePuedeRepartirAunqueNoLleneUnVial() {
        // Tubo de 3 x 12: ya hay 1 hecha y quedan 10 dl. No da para una completa,
        // pero el usuario puede meterlos en una parcial o repartirlos entre las
        // dos que faltan. Antes esto se quedaba sin ninguna opcion.
        RecetaTubo tubo = new RecetaTubo("Heces", 3, 12.0, "dl", true);

        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(tubo, 10.0, 1);

        assertEquals(0, plan.alicuotasCompletas(), "10 no llena un vial de 12");
        assertEquals(10.0, plan.remanente());
        assertEquals(2, plan.slotsLibres());
        assertTrue(plan.puedeAlojarParcial());

        assertEquals(List.of(10.0), PlanificadorAlicuotas.distribuirRemanente(plan, 1),
                "una sola parcial con los 10");
        assertEquals(List.of(5.0, 5.0), PlanificadorAlicuotas.distribuirRemanente(plan, 2),
                "o repartidos entre las dos que faltan");
    }

    @Test
    void elRepartoDeLoQueQuedaSeValidaContraLosHuecosLibres() {
        RecetaTubo tubo = new RecetaTubo("Heces", 3, 12.0, "dl", true);

        // Dos parciales de 5 caben en los dos huecos que quedan.
        assertDoesNotThrow(() -> PlanificadorAlicuotas.validarPlan(
                List.of(5.0, 5.0), tubo, 10.0, 1));
        // Tres no, aunque el volumen diera.
        assertThrows(ValidationException.class, () -> PlanificadorAlicuotas.validarPlan(
                List.of(4.0, 3.0, 3.0), tubo, 10.0, 1));
    }

    @Test
    void noSePuedePasarDeLosHuecosQueQuedanLibres() {
        RecetaTubo tubo = new RecetaTubo("Heces", 3, 12.0, "dl", true);

        ValidationException e = assertThrows(ValidationException.class,
                () -> PlanificadorAlicuotas.validarPlan(List.of(12.0, 12.0, 12.0), tubo, 100.0, 1));
        assertTrue(e.getMessage().contains("quedan 2 hueco"), e.getMessage());
    }

    @Test
    void unLoteYaCompletoNoAdmiteMas() {
        RecetaTubo tubo = new RecetaTubo("Heces", 3, 12.0, "dl", true);

        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(tubo, 100.0, 3);
        assertEquals(0, plan.slotsLibres());
        assertEquals(0, plan.alicuotasCompletas());
        assertTrue(plan.mensaje().contains("ya está completo"), plan.mensaje());

        assertThrows(ValidationException.class,
                () -> PlanificadorAlicuotas.validarPlan(List.of(12.0), tubo, 100.0, 3));
    }

    // ── Mensajes ─────────────────────────────────────────────────────────────

    @Test
    void elMensajeDiceCuantoFaltaYCuantoAlcanza() {
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(suero5x50(), 200.0);

        assertEquals(
                "Para generar el lote completo se requieren 250 mL. Con 200 mL alcanzan 4 alícuotas de 50 mL.",
                plan.mensaje());
    }

    @Test
    void elMensajeAvisaDelRemanenteQueSeQuedaEnLaPadre() {
        PlanAlicuotas plan = PlanificadorAlicuotas.planificar(suero5x50(), 220.0);

        assertTrue(plan.mensaje().endsWith("Restan 20 mL en la muestra padre."), plan.mensaje());
    }

    @Test
    void losVolumenesNoSeImprimenConCerosDeRelleno() {
        assertEquals("250", PlanificadorAlicuotas.fmt(250.0));
        assertEquals("2.5", PlanificadorAlicuotas.fmt(2.5));
    }

    // ── La misma unidad escrita de dos formas ────────────────────────────────

    /**
     * No es una conversion: el sistema sigue sin convertir magnitudes. Es que
     * una hoja de calculo escribe el micro como U+00B5, como mu griega o como
     * una u suelta segun quien la teclee, y las tres son el mismo microlitro.
     * Distinguirlas solo rechazaba archivos correctos.
     */
    @Test
    @DisplayName("El micro escrito de cualquier forma es la misma unidad")
    void elMicroSeEscribeDeVariasFormas() {
        assertTrue(PlanificadorAlicuotas.mismaUnidad("µL", "uL"));
        assertTrue(PlanificadorAlicuotas.mismaUnidad("μL", "uL"));
        assertTrue(PlanificadorAlicuotas.mismaUnidad("µL", "μL"));
        assertTrue(PlanificadorAlicuotas.mismaUnidad("  µl  ", "UL"));
    }

    @Test
    @DisplayName("Mayusculas y espacios sobrantes tampoco cambian la unidad")
    void toleraComoSeEscriba() {
        assertTrue(PlanificadorAlicuotas.mismaUnidad("mL", "ml"));
        assertTrue(PlanificadorAlicuotas.mismaUnidad(" MG ", "mg"));
    }

    /** Lo que sigue prohibido: dos magnitudes distintas nunca son la misma. */
    @Test
    @DisplayName("Magnitudes distintas siguen sin coincidir")
    void lasMagnitudesDistintasNoCoinciden() {
        assertFalse(PlanificadorAlicuotas.mismaUnidad("mL", "L"));
        assertFalse(PlanificadorAlicuotas.mismaUnidad("µL", "mL"));
        assertFalse(PlanificadorAlicuotas.mismaUnidad("mg", "g"));
        assertFalse(PlanificadorAlicuotas.mismaUnidad("mL", null));
    }

    @Test
    @DisplayName("validarUnidad acepta el micro escrito distinto y rechaza otra magnitud")
    void validarUnidadUsaLaMismaRegla() {
        assertDoesNotThrow(() -> PlanificadorAlicuotas.validarUnidad("µL", "uL"));
        assertThrows(ValidationException.class,
                () -> PlanificadorAlicuotas.validarUnidad("mL", "L"));
    }
}
