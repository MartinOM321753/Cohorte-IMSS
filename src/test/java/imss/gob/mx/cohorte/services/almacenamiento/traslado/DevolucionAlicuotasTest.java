package imss.gob.mx.cohorte.services.almacenamiento.traslado;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Una fila de traslado en EN_DEVOLUCION puede tener dos formas, y confundirlas
 * bloqueaba la devolución de las alícuotas.
 *
 * <p>El caso que lo destapó: una muestra padre prestada del Laboratorio (2) al
 * IMSS (1), devuelta con atajo hacia el INSP (3), llevándose las alícuotas que el
 * IMSS había generado. Al confirmar, el sistema decía que la alícuota
 * "ya no se encuentra en INSP" — cuando estaba en el IMSS, que es justo donde
 * debía estar hasta que la devolución se confirmara.</p>
 *
 * <p>Se prueba la regla de lectura de la fila, que es donde estaba el error, sin
 * levantar el contexto: es lógica pura sobre el registro.</p>
 */
class DevolucionAlicuotasTest {

    private Institucion inst(long id, String nombre) {
        Institucion i = new Institucion();
        i.setId(id);
        i.setNombre(nombre);
        return i;
    }

    private final Institucion laboratorio = inst(2, "Laboratorio de Demostración");
    private final Institucion imss = inst(1, "IMSS Cuernavaca - Sede Central");
    private final Institucion insp = inst(3, "INSP");

    /**
     * Quién está autorizado a confirmar la devolución. Misma regla que aplica
     * TrasladoMuestraApplicationService: el que RECIBE, nunca el que manda.
     */
    private Long quienConfirma(TrasladoMuestra t) {
        return Boolean.TRUE.equals(t.getEsMovimientoDevolucion())
                ? t.getInstitucionDestino().getId()
                : t.getIdInstitucionDestinoDevolucion() != null
                        ? t.getIdInstitucionDestinoDevolucion()
                        : t.getInstitucionOrigen().getId();
    }

    @Test
    @DisplayName("La confirmación de un movimiento de devolución corresponde a quien recibe, no a quien manda")
    void confirmaQuienRecibeElMovimiento() {
        // La fila que la devolución crea para una alícuota: la tiene el IMSS y
        // viaja al INSP.
        TrasladoMuestra fila = new TrasladoMuestra();
        fila.setEstado(EstadoTraslado.EN_DEVOLUCION);
        fila.setEsMovimientoDevolucion(true);
        fila.setInstitucionOrigen(imss);
        fila.setInstitucionDestino(insp);

        assertEquals(insp.getId(), quienConfirma(fila),
                "debe confirmar el INSP, que es quien la recibe");
        assertNotEquals(imss.getId(), quienConfirma(fila),
                "el IMSS la envía: dejarle confirmar sería firmar el acuse en nombre ajeno, "
                + "y DEVUELTA no tiene vuelta atrás");
    }

    @Test
    @DisplayName("En un préstamo de ida confirma el origen, o el atajo si lo hay")
    void confirmaElOrigenEnUnPrestamoDeIda() {
        TrasladoMuestra ida = new TrasladoMuestra();
        ida.setEstado(EstadoTraslado.EN_DEVOLUCION);
        ida.setEsMovimientoDevolucion(false);
        ida.setInstitucionOrigen(laboratorio);
        ida.setInstitucionDestino(imss);

        assertEquals(laboratorio.getId(), quienConfirma(ida),
                "vuelve a quien la prestó");

        // Con atajo, la recibe un tercero en vez del prestador original.
        ida.setIdInstitucionDestinoDevolucion(insp.getId());
        assertEquals(insp.getId(), quienConfirma(ida),
                "el atajo manda sobre el origen");
    }

    /** Quién debe tener la muestra para que la devolución sea confirmable. */
    private Institucion tenedorEsperado(TrasladoMuestra t) {
        return Boolean.TRUE.equals(t.getEsMovimientoDevolucion())
                ? t.getInstitucionOrigen()
                : t.getInstitucionDestino();
    }

    private Muestra muestraEn(Institucion donde, String etiqueta) {
        Muestra m = new Muestra();
        m.setEtiqueta(etiqueta);
        m.setInstitucionActual(donde);
        m.setEstadoMuestra(EstadoMuestra.PRESTADA);
        return m;
    }

    @Test
    @DisplayName("préstamo de ida: el tenedor es el destino de la fila")
    void prestamoDeIda() {
        TrasladoMuestra padre = new TrasladoMuestra();
        padre.setInstitucionOrigen(laboratorio);
        padre.setInstitucionDestino(imss);
        padre.setIdInstitucionDestinoDevolucion(insp.getId());
        padre.setEstado(EstadoTraslado.EN_DEVOLUCION);

        assertEquals(imss.getId(), tenedorEsperado(padre).getId(),
                "en un prestamo de ida quien tiene la muestra es el destino de la fila");
    }

    @Test
    @DisplayName("movimiento de devolución: el tenedor es el origen de la fila")
    void movimientoDeDevolucion() {
        TrasladoMuestra alicuota = new TrasladoMuestra();
        alicuota.setInstitucionOrigen(imss);   // quien la tiene
        alicuota.setInstitucionDestino(insp);  // a donde va
        alicuota.setEsMovimientoDevolucion(true);
        alicuota.setEstado(EstadoTraslado.EN_DEVOLUCION);

        assertEquals(imss.getId(), tenedorEsperado(alicuota).getId(),
                "la fila creada por la devolucion guarda al tenedor en el origen");
    }

    @Test
    @DisplayName("el caso real: la alícuota en el IMSS pasa la comprobación")
    void elCasoQueFallaba() {
        Muestra alic = muestraEn(imss, "PL/000001/I1F4-L1/4-4");

        TrasladoMuestra t = new TrasladoMuestra();
        t.setMuestra(alic);
        t.setInstitucionOrigen(imss);
        t.setInstitucionDestino(insp);
        t.setEsMovimientoDevolucion(true);
        t.setEstado(EstadoTraslado.EN_DEVOLUCION);

        assertEquals(alic.getInstitucionActual().getId(), tenedorEsperado(t).getId(),
                "la alicuota esta en el IMSS y ahi debe estar: la devolucion tiene que poder confirmarse");
    }

    @Test
    @DisplayName("sin la marca, la misma fila se leería mal (regresión)")
    void sinLaMarcaSeLeeMal() {
        Muestra alic = muestraEn(imss, "PL/000001/I1F4-L1/4-4");

        TrasladoMuestra t = new TrasladoMuestra();
        t.setMuestra(alic);
        t.setInstitucionOrigen(imss);
        t.setInstitucionDestino(insp);
        t.setEsMovimientoDevolucion(false);   // como estaban las filas viejas
        t.setEstado(EstadoTraslado.EN_DEVOLUCION);

        assertNotEquals(alic.getInstitucionActual().getId(), tenedorEsperado(t).getId(),
                "asi es como se producia el error: se esperaba a la muestra en el INSP");
    }

    @Test
    @DisplayName("una alícuota nueva nace como préstamo de ida, no como movimiento")
    void porOmisionNoEsMovimiento() {
        assertFalse(Boolean.TRUE.equals(new TrasladoMuestra().getEsMovimientoDevolucion()),
                "el valor por omision debe dejar intactos los prestamos de ida existentes");
    }
}
