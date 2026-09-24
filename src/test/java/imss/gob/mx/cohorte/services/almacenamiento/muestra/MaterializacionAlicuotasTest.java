package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.TipoEventoMuestra;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * El descuento del volumen de la muestra padre, que tiene que ocurrir
 * exactamente una vez por alícuota.
 *
 * <p>Hay cuatro caminos que asignan posición —el endpoint de posición, la
 * edición de la muestra, la confirmación de un traslado y la cancelación de
 * uno—. Si el descuento colgara de «tiene posición» en lugar de una marca
 * propia, ubicar y liberar dos veces descontaría dos veces, y confirmar la
 * recepción de una alícuota ya ubicada descontaría otra vez más.</p>
 */
class MaterializacionAlicuotasTest {

    private MuestraRepository repositorio;
    private HistorialCambioMuestraService historial;
    private MaterializacionAlicuotaService servicio;
    private BeanUser usuario;

    @BeforeEach
    void preparar() {
        repositorio = mock(MuestraRepository.class);
        historial = mock(HistorialCambioMuestraService.class);
        servicio = new MaterializacionAlicuotaService(repositorio, historial);
        usuario = new BeanUser();
    }

    /** Muestra padre de 200 mL con 200 mL ya comprometidos en 4 alícuotas de 50. */
    private Muestra padreCon(double valor, double comprometido) {
        Muestra padre = new Muestra();
        padre.setId(1L);
        padre.setEtiqueta("S/00123/I1F4-L1");
        padre.setValor(valor);
        padre.setUnidad("mL");
        padre.setValorComprometido(comprometido);
        return padre;
    }

    private Muestra alicuotaDe(Muestra padre, long id, double volumen) {
        Muestra a = new Muestra();
        a.setId(id);
        a.setEtiqueta(padre.getEtiqueta() + "/" + id + "-4");
        a.setValor(volumen);
        a.setUnidad("mL");
        a.setMuestraPadre(padre);
        return a;
    }

    // ── Descontar una sola vez ───────────────────────────────────────────────

    @Test
    @DisplayName("Ubicar una alícuota descuenta su volumen de la muestra padre")
    void ubicarDescuentaDeLaPadre() {
        Muestra padre = padreCon(200.0, 200.0);
        Muestra alicuota = alicuotaDe(padre, 11L, 50.0);

        servicio.materializar(alicuota, usuario, "CRIO-A3 B2");

        assertEquals(150.0, padre.getValor());
        assertEquals(150.0, padre.getValorComprometido());
        assertTrue(alicuota.isMaterializada());
        assertEquals(50.0, alicuota.getCantidadDescontadaPadre());
    }

    @Test
    @DisplayName("Ubicar, liberar y volver a ubicar descuenta una sola vez")
    void ubicarLiberarYVolverAUbicarNoDescuentaDosVeces() {
        Muestra padre = padreCon(200.0, 200.0);
        Muestra alicuota = alicuotaDe(padre, 11L, 50.0);

        servicio.materializar(alicuota, usuario, "CRIO-A3 B2");
        // Liberar la posición no devuelve líquido a la padre: no se puede
        // despipetear. La marca de materialización sobrevive.
        alicuota.setPosicionCaja(null);
        servicio.materializar(alicuota, usuario, "CRIO-A3 B5");

        assertEquals(150.0, padre.getValor(), "el segundo intento no debe descontar nada");
        assertEquals(50.0, alicuota.getCantidadDescontadaPadre());
    }

    @Test
    @DisplayName("Confirmar la recepción de una alícuota ya ubicada no vuelve a descontar")
    void recibirUnaAlicuotaYaMaterializadaNoDescuenta() {
        Muestra padre = padreCon(200.0, 200.0);
        Muestra alicuota = alicuotaDe(padre, 11L, 50.0);
        servicio.materializar(alicuota, usuario, "CRIO-A3 B2");

        // La alícuota viaja a otra institución y allí le asignan hueco.
        servicio.materializar(alicuota, usuario, "Recepción de traslado en INSP");

        assertEquals(150.0, padre.getValor());
    }

    @Test
    @DisplayName("Una muestra padre nunca se materializa a sí misma")
    void unaPadreNoSeMaterializa() {
        Muestra padre = padreCon(200.0, 0.0);

        servicio.materializar(padre, usuario, "CRIO-A3 B2");

        assertEquals(200.0, padre.getValor());
        assertFalse(padre.isMaterializada());
    }

    // ── El invariante ────────────────────────────────────────────────────────

    @Test
    @DisplayName("El volumen disponible no cambia al materializar")
    void elDisponibleEsInvarianteALaMaterializacion() {
        Muestra padre = padreCon(220.0, 200.0);
        double disponibleAntes = padre.getValorDisponible();

        servicio.materializar(alicuotaDe(padre, 11L, 50.0), usuario, "CRIO-A3 B2");

        assertEquals(disponibleAntes, padre.getValorDisponible(),
                "si el disponible se moviera, el mismo mililitro se contaría dos veces");
        assertEquals(20.0, padre.getValorDisponible());
    }

    @Test
    @DisplayName("Reservar un lote sube el comprometido sin tocar el valor")
    void reservarNoDescuenta() {
        Muestra padre = padreCon(220.0, 0.0);

        servicio.reservar(padre, 200.0, usuario, "Lote de 4 alícuotas");

        assertEquals(220.0, padre.getValor(), "crear el lote no saca líquido del tubo");
        assertEquals(200.0, padre.getValorComprometido());
        assertEquals(20.0, padre.getValorDisponible());
    }

    // ── Lote completo ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ubicar el lote completo deja la padre en cero de una sola vez")
    void ubicarElLoteCompleto() {
        Muestra padre = padreCon(200.0, 200.0);
        List<Muestra> lote = List.of(
                alicuotaDe(padre, 11L, 50.0),
                alicuotaDe(padre, 12L, 50.0),
                alicuotaDe(padre, 13L, 50.0),
                alicuotaDe(padre, 14L, 50.0));

        servicio.materializarLote(lote, usuario, "CRIO-A3 B2 … B5");

        assertEquals(0.0, padre.getValor());
        assertEquals(0.0, padre.getValorComprometido());
        assertTrue(lote.stream().allMatch(Muestra::isMaterializada));
    }

    @Test
    @DisplayName("Un lote deja un solo renglón de historial en la padre, no uno por vial")
    void elLoteNoInundaLaLineaDeTiempoDeLaPadre() {
        Muestra padre = padreCon(200.0, 200.0);
        List<Muestra> lote = List.of(
                alicuotaDe(padre, 11L, 50.0),
                alicuotaDe(padre, 12L, 50.0),
                alicuotaDe(padre, 13L, 50.0),
                alicuotaDe(padre, 14L, 50.0));

        servicio.materializarLote(lote, usuario, "CRIO-A3 B2 … B5");

        ArgumentCaptor<Muestra> sujeto = ArgumentCaptor.forClass(Muestra.class);
        verify(historial, atLeastOnce()).registrarEvento(sujeto.capture(), any(),
                eq(TipoEventoMuestra.ALICUOTA_MATERIALIZADA), any(), any(), any(), any());

        long renglonesEnLaPadre = sujeto.getAllValues().stream()
                .filter(m -> m.getMuestraPadre() == null)
                .count();
        assertEquals(1, renglonesEnLaPadre,
                "cuatro renglones sepultarían el evento que de verdad importa");
    }

    // ── Datos heredados sin contabilidad ─────────────────────────────────────

    @Test
    @DisplayName("Si la padre no alcanza, se trunca el descuento en lugar de bloquear")
    void volumenInsuficienteNoBloqueaLaUbicacion() {
        // Registro heredado: la padre nunca se descontó y ahora no da de sí.
        Muestra padre = padreCon(30.0, 0.0);
        Muestra alicuota = alicuotaDe(padre, 11L, 50.0);

        assertDoesNotThrow(() -> servicio.materializar(alicuota, usuario, "CRIO-A3 B2"),
                "guardar un tubo en una caja es un acto físico: no puede quedar "
                + "bloqueado por un descuadre contable heredado");

        assertEquals(0.0, padre.getValor());
        assertEquals(30.0, alicuota.getCantidadDescontadaPadre(),
                "queda constancia de cuánto se pudo descontar de verdad");
    }

    @Test
    @DisplayName("El descuadre deja rastro en el historial de la padre")
    void elDescuadreSeRegistra() {
        Muestra padre = padreCon(30.0, 0.0);

        servicio.materializar(alicuotaDe(padre, 11L, 50.0), usuario, "CRIO-A3 B2");

        ArgumentCaptor<String> motivo = ArgumentCaptor.forClass(String.class);
        verify(historial, atLeastOnce()).registrarEvento(any(), any(),
                eq(TipoEventoMuestra.ALICUOTA_MATERIALIZADA), any(), any(), motivo.capture(), any());

        assertTrue(motivo.getAllValues().stream().anyMatch(m -> m != null && m.contains("AVISO")),
                "un descuadre silencioso es peor que uno ruidoso");
    }

    // ── Agotamiento ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("La padre se sella como agotada al quedarse en cero")
    void llegarACeroSellaElAgotamiento() {
        Muestra padre = padreCon(50.0, 50.0);

        servicio.materializar(alicuotaDe(padre, 11L, 50.0), usuario, "CRIO-A3 B2");

        assertTrue(padre.isAgotada());
        assertNotNull(padre.getFechaAgotamiento());
        verify(historial).registrarEvento(eq(padre), any(), eq(TipoEventoMuestra.MUESTRA_AGOTADA),
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("El agotamiento no se sella dos veces")
    void elAgotamientoSeSellaUnaSolaVez() {
        Muestra padre = padreCon(0.0, 0.0);

        servicio.sellarAgotamientoSiProcede(padre, usuario);
        var primeraFecha = padre.getFechaAgotamiento();
        servicio.sellarAgotamientoSiProcede(padre, usuario);

        assertEquals(primeraFecha, padre.getFechaAgotamiento());
        verify(historial, times(1)).registrarEvento(any(), any(),
                eq(TipoEventoMuestra.MUESTRA_AGOTADA), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Una padre con volumen no se marca agotada")
    void conVolumenNoHayAgotamiento() {
        Muestra padre = padreCon(20.0, 0.0);

        servicio.sellarAgotamientoSiProcede(padre, usuario);

        assertFalse(padre.isAgotada());
    }

    @Test
    @DisplayName("Agotarse no libera la posición: el tubo vacío puede seguir en la caja")
    void agotarseNoVaciaElHueco() {
        Muestra padre = padreCon(50.0, 50.0);
        var posicion = new imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja();
        posicion.setId(77L);
        padre.setPosicionCaja(posicion);

        servicio.materializar(alicuotaDe(padre, 11L, 50.0), usuario, "CRIO-A3 B2");

        assertTrue(padre.isAgotada());
        assertNotNull(padre.getPosicionCaja(),
                "vaciar el hueco solo le mentiría al visor 3D mientras el tubo siga dentro");
    }

    // ── Coma flotante ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Tres descuentos de 0.1 dejan la padre exactamente en cero")
    void elDescuentoNoArrastraErrorDeComaFlotante() {
        Muestra padre = padreCon(0.3, 0.3);

        servicio.materializarLote(List.of(
                alicuotaDe(padre, 11L, 0.1),
                alicuotaDe(padre, 12L, 0.1),
                alicuotaDe(padre, 13L, 0.1)), usuario, "CRIO-A3 B2 … B4");

        assertEquals(0.0, padre.getValor(),
                "en double crudo quedaría en 2.7e-17 y la muestra nunca se daría por agotada");
        assertTrue(padre.isAgotada());
    }
}
