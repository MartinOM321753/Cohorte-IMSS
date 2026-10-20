package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.services.importacion.LectorArchivoTabular;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Que al recargar el catálogo se note que una pregunta cambió de bloque.
 *
 * <h3>Por qué necesita prueba</h3>
 *
 * <p>La carga solo toca las preguntas que cambiaron, y para saberlo compara campo por
 * campo con una lista escrita a mano. Esa lista no incluía el bloque, así que una
 * pregunta que <b>solo</b> cambia de bloque se daba por igual y se quedaba donde
 * estaba. No fallaba nada: el archivo decía una cosa, la base otra, y la pantalla
 * seguía agrupando como antes.</p>
 *
 * <p>Se descubrió al partir la 183 en sus tres tablas —98 preguntas que no cambian
 * nada más que de bloque—, y es el mismo caso de cada reacomodo que venga después.</p>
 */
class CambioDeBloqueEnLaCargaTest {

    private CargaCatalogoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CargaCatalogoService(
                mock(LectorArchivoTabular.class),
                mock(CargaCatalogoService.TipoCuestionarioRepositorios.class));
    }

    /** La pregunta como está guardada: en el bloque P183. */
    private VariableCuestionario guardada() {
        SeccionCuestionario bloque = new SeccionCuestionario();
        bloque.setNombre("P183");

        VariableCuestionario v = new VariableCuestionario();
        v.setCodigo("P183_3");
        v.setPregunta("Donde le han ofendido verbalmente - casa");
        v.setColumnaVolcado("P183_3");
        v.setTipo(TipoVariable.CASILLA);
        v.setSeccion(bloque);
        v.setObligatoria(false);
        return v;
    }

    /** Lo que trae el archivo: lo mismo, pero en otro bloque. */
    private CargaCatalogoService.EntradaPlan entrante(String bloque) {
        CargaCatalogoService.EntradaPlan e = new CargaCatalogoService.EntradaPlan();
        e.codigo = "P183_3";
        e.pregunta = "Donde le han ofendido verbalmente - casa";
        e.columna = "P183_3";
        e.tipo = TipoVariable.CASILLA;
        e.bloque = bloque;
        e.obligatoria = false;
        return e;
    }

    @Test
    @DisplayName("cambiar de bloque cuenta como cambio")
    void elBloqueCuenta() {
        assertTrue(servicio.cambia(entrante("P183 donde"), guardada()),
                "si esto da falso, el reacomodo de bloques no se aplica al recargar");
    }

    @Test
    @DisplayName("sin cambios no se toca la pregunta")
    void loIgualNoCambia() {
        assertFalse(servicio.cambia(entrante("P183"), guardada()),
                "tocar lo que no cambió ensucia la bitácora de cada carga");
    }

    @Test
    @DisplayName("una pregunta sin bloque que pasa a tener uno tambien cuenta")
    void deSinBloqueABloque() {
        VariableCuestionario suelta = guardada();
        suelta.setSeccion(null);
        assertTrue(servicio.cambia(entrante("P183"), suelta));
        assertFalse(servicio.cambia(entrante(""), suelta),
                "vacio y sin bloque son lo mismo");
    }
}
