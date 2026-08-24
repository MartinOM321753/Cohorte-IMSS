package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cuándo se puede eliminar un piso de un refrigerador.
 *
 * <p>La regla anterior rechazaba el borrado en cuanto el piso tuviera posiciones,
 * y un piso SIEMPRE las tiene: se generan solas al crearlo, con sus filas, columnas
 * y alturas. Es decir, la validación no protegía nada — simplemente hacía imposible
 * eliminar un piso, y el usuario recibía "tiene posiciones asociadas" sin manera de
 * dejarlo sin ellas.</p>
 *
 * <p>Lo que sí hay que proteger son las cajas, que las puso alguien. Las posiciones
 * son rejilla vacía y se van con el piso, porque la relación tiene orphanRemoval.</p>
 */
class EliminarPisoTest {

    @Test
    @DisplayName("Un piso lleno de posiciones vacías se puede eliminar")
    void pisoConPosicionesVaciasSeElimina() {
        // El caso de siempre: 128 huecos generados, ninguno usado.
        assertDoesNotThrow(() ->
                PisoRefrigeradorApplicationService.verificarPisoVaciable(0, 0));
    }

    @Test
    @DisplayName("Un piso con cajas dentro no se elimina, y el mensaje dice cuántas")
    void pisoConCajasNoSeElimina() {
        ObjConflictException e = assertThrows(ObjConflictException.class, () ->
                PisoRefrigeradorApplicationService.verificarPisoVaciable(3, 3));
        assertTrue(e.getMessage().contains("3 cajas dentro"),
                "El mensaje debe decir cuántas cajas estorban: " + e.getMessage());
    }

    @Test
    @DisplayName("Con una sola caja el mensaje va en singular")
    void mensajeEnSingular() {
        ObjConflictException e = assertThrows(ObjConflictException.class, () ->
                PisoRefrigeradorApplicationService.verificarPisoVaciable(1, 1));
        assertTrue(e.getMessage().contains("1 caja dentro"),
                "Singular, no '1 cajas': " + e.getMessage());
    }

    /**
     * Una posición marcada como ocupada cuya caja ya no está. El dato es
     * inconsistente y borrar el piso lo taparía, así que se para y se avisa.
     */
    @Test
    @DisplayName("Una marca de ocupado sin caja detiene el borrado en vez de taparlo")
    void marcaDeOcupadoSinCajaDetieneElBorrado() {
        ObjConflictException e = assertThrows(ObjConflictException.class, () ->
                PisoRefrigeradorApplicationService.verificarPisoVaciable(0, 2));
        assertTrue(e.getMessage().contains("sin caja"),
                "Debe distinguirse del caso de las cajas: " + e.getMessage());
    }

    /**
     * Las cajas se comprueban primero: si las hay, ese es el problema que el
     * usuario puede resolver, y el mensaje debe hablar de ellas.
     */
    @Test
    @DisplayName("Habiendo cajas, se reclaman las cajas y no la marca")
    void lasCajasMandanEnElMensaje() {
        ObjConflictException e = assertThrows(ObjConflictException.class, () ->
                PisoRefrigeradorApplicationService.verificarPisoVaciable(2, 5));
        assertTrue(e.getMessage().contains("2 cajas dentro"), e.getMessage());
        assertFalse(e.getMessage().contains("sin caja"), e.getMessage());
    }
}
