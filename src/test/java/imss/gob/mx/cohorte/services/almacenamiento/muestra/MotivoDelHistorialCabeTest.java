package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.HistorialCambioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.HistorialCambioMuestraRepository;
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
 * El historial no puede tumbar la operación que está registrando.
 *
 * <p>El caso real: registrar una muestra de 12 dl con un tubo de 3 × 12 generaba
 * un motivo de más de 200 caracteres, `motivo` es un VARCHAR(200), y MySQL
 * respondía «Data too long for column 'motivo'». La excepción saltaba dentro de
 * la transacción de negocio y se llevaba por delante la muestra entera, sus
 * alícuotas y la reserva de volumen — por un renglón de bitácora.</p>
 */
class MotivoDelHistorialCabeTest {

    private HistorialCambioMuestraRepository repositorio;
    private HistorialCambioMuestraService servicio;
    private BeanUser usuario;

    @BeforeEach
    void preparar() {
        repositorio = mock(HistorialCambioMuestraRepository.class);
        servicio = new HistorialCambioMuestraService(repositorio);
        usuario = new BeanUser();
    }

    private HistorialCambioMuestra capturarGuardado() {
        ArgumentCaptor<HistorialCambioMuestra> captor =
                ArgumentCaptor.forClass(HistorialCambioMuestra.class);
        verify(repositorio).save(captor.capture());
        return captor.getValue();
    }

    // ── La red de seguridad ──────────────────────────────────────────────────

    @Test
    @DisplayName("Un motivo más largo que su columna se recorta en vez de reventar")
    void elMotivoLargoSeRecorta() {
        String largo = "x".repeat(500);

        assertDoesNotThrow(() -> servicio.registrarEvento(new Muestra(), usuario,
                TipoEventoMuestra.ALICUOTAS_COMPROMETIDAS, "0", "200", largo, null));

        String guardado = capturarGuardado().getMotivo();
        assertEquals(HistorialCambioMuestraService.MAX_MOTIVO, guardado.length());
        assertTrue(guardado.endsWith("…"), "debe verse que quedó cortado");
    }

    @Test
    @DisplayName("Un motivo que cabe se guarda intacto")
    void elMotivoCortoNoSeToca() {
        String motivo = "Lote de 1 alícuota (12 dl) del tubo «Tubo 01».";

        servicio.registrarEvento(new Muestra(), usuario,
                TipoEventoMuestra.ALICUOTAS_COMPROMETIDAS, "0", "12", motivo, null);

        assertEquals(motivo, capturarGuardado().getMotivo());
    }

    @Test
    @DisplayName("También se recortan campo y valores, que tienen sus propios anchos")
    void losDemasCamposTambienSeRecortan() {
        servicio.registrar(new Muestra(), usuario,
                "c".repeat(200), "a".repeat(900), "n".repeat(900), "m".repeat(400));

        HistorialCambioMuestra h = capturarGuardado();
        assertEquals(HistorialCambioMuestraService.MAX_CAMPO, h.getCampo().length());
        assertEquals(HistorialCambioMuestraService.MAX_VALOR, h.getValorAnterior().length());
        assertEquals(HistorialCambioMuestraService.MAX_VALOR, h.getValorNuevo().length());
        assertEquals(HistorialCambioMuestraService.MAX_MOTIVO, h.getMotivo().length());
    }

    @Test
    @DisplayName("Un motivo nulo sigue siendo nulo")
    void elMotivoNuloNoEstorba() {
        assertNull(HistorialCambioMuestraService.recortar(null, 200));
    }

    // ── Que los mensajes quepan de origen ────────────────────────────────────

    @Test
    @DisplayName("El motivo de materializar cabe aunque las etiquetas sean enormes")
    void elMotivoDeMaterializarCabeDeOrigen() {
        MuestraRepository muestras = mock(MuestraRepository.class);
        HistorialCambioMuestraService historial = mock(HistorialCambioMuestraService.class);
        MaterializacionAlicuotaService materializacion =
                new MaterializacionAlicuotaService(muestras, historial);

        Muestra padre = new Muestra();
        padre.setId(1L);
        padre.setEtiqueta("P".repeat(100)); // el máximo que admite la columna
        padre.setValor(10.0);
        padre.setUnidad("mL");
        padre.setValorComprometido(50.0);

        List<Muestra> lote = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Muestra a = new Muestra();
            a.setId((long) (10 + i));
            a.setEtiqueta("A".repeat(100));
            a.setValor(10.0);
            a.setUnidad("mL");
            a.setMuestraPadre(padre);
            lote.add(a);
        }

        // Ubicación larga y volumen insuficiente: el peor caso para el texto.
        materializacion.materializarLote(lote, usuario, "CRIO-DEMOSTRACION-LARGA A1 … E12");

        ArgumentCaptor<String> motivos = ArgumentCaptor.forClass(String.class);
        verify(historial, atLeastOnce()).registrarEvento(any(), any(), any(),
                any(), any(), motivos.capture(), any());

        for (String motivo : motivos.getAllValues()) {
            assertNotNull(motivo);
            assertTrue(motivo.length() <= HistorialCambioMuestraService.MAX_MOTIVO,
                    "un motivo de " + motivo.length() + " caracteres llegaría recortado: " + motivo);
        }
    }

    @Test
    @DisplayName("El motivo de agotamiento cabe")
    void elMotivoDeAgotamientoCabe() {
        MuestraRepository muestras = mock(MuestraRepository.class);
        HistorialCambioMuestraService historial = mock(HistorialCambioMuestraService.class);
        MaterializacionAlicuotaService materializacion =
                new MaterializacionAlicuotaService(muestras, historial);

        Muestra padre = new Muestra();
        padre.setId(1L);
        padre.setEtiqueta("P".repeat(100));
        padre.setValor(0.0);
        padre.setUnidad("mL");

        materializacion.sellarAgotamientoSiProcede(padre, usuario);

        ArgumentCaptor<String> motivo = ArgumentCaptor.forClass(String.class);
        verify(historial).registrarEvento(any(), any(), eq(TipoEventoMuestra.MUESTRA_AGOTADA),
                any(), any(), motivo.capture(), any());

        assertTrue(motivo.getValue().length() <= HistorialCambioMuestraService.MAX_MOTIVO);
    }
}
