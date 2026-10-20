package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.CuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.DirigidoA;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Que al editar un instrumento no se pierda ninguno de sus campos.
 *
 * <h3>Por qué esta prueba usa reflexión</h3>
 *
 * <p>{@code actualizar} copia campo por campo con una lista escrita a mano. Esa lista es
 * una trampa: al añadir un campo nuevo hay que acordarse de tres sitios —el DTO, esta
 * copia y el mapeador— y <b>olvidar justo este no produce ningún error</b>. La pantalla
 * guarda, el servidor responde que todo bien, y el valor simplemente no está cuando se
 * vuelve a abrir. Ya pasó una vez con {@code permiteAutollenado}.</p>
 *
 * <p>Una prueba que enumerara los campos a mano no serviría de nada: tampoco conocería el
 * campo nuevo. Recorriendo la entidad por reflexión, cualquier campo que se agregue y no
 * se copie hace fallar esto con su nombre, y quien lo añadió decide: copiarlo, o ponerlo
 * en la lista de exentos de abajo con su motivo.</p>
 */
class ActualizarTipoConservaTodoTest {

    /**
     * Campos que a propósito no se copian al editar.
     *
     * <p>Cada uno tiene su razón y ninguna es «se nos olvidó»: la identidad y la fecha de
     * creación no se tocan nunca, y la institución la pone el servicio desde la sesión, no
     * quien manda el formulario — admitirla de fuera permitiría mover un instrumento a otra
     * sede con una peticion a mano.</p>
     */
    private static final List<String> EXENTOS = List.of("id", "fechaCreacion", "institucion");

    private TipoCuestionarioRepository tipos;
    private CuestionarioRepository cuestionarios;
    private TipoCuestionarioService servicio;
    private TipoCuestionario guardado;

    @BeforeEach
    void preparar() {
        tipos = mock(TipoCuestionarioRepository.class);
        cuestionarios = mock(CuestionarioRepository.class);
        var secciones = mock(SeccionCuestionarioRepository.class);
        var variables = mock(VariableCuestionarioRepository.class);
        var contexto = mock(InstitucionContextService.class);

        servicio = new TipoCuestionarioService(tipos, secciones, variables, cuestionarios, contexto);

        Institucion institucion = new Institucion();
        institucion.setId(1L);

        guardado = new TipoCuestionario();
        guardado.setId(5L);
        guardado.setClave("C1");
        guardado.setVersion("1");
        guardado.setNombre("Cuestionario General");
        guardado.setDescripcion("antes");
        guardado.setColumnaEtiqueta("Folio");
        guardado.setDirigidoA(DirigidoA.AMBOS);
        guardado.setEdadMinima(null);
        guardado.setEdadMaxima(null);
        guardado.setPermiteAutollenado(false);
        guardado.setActivo(true);
        guardado.setInstitucion(institucion);

        when(contexto.getIdInstitucionActual()).thenReturn(1L);
        when(tipos.findByIdAndInstitucion_Id(5L, 1L)).thenReturn(Optional.of(guardado));
        when(tipos.save(any())).thenAnswer(i -> i.getArgument(0));
        // Sin cuestionarios contestados, para que tambien se puedan cambiar clave y version.
        when(cuestionarios.countByTipoCuestionario_Id(anyLong())).thenReturn(0L);
    }

    @Test
    @DisplayName("todo campo configurable sobrevive a la edicion")
    void nadaSePierde() throws Exception {
        TipoCuestionario datos = new TipoCuestionario();
        datos.setClave("C9");
        datos.setVersion("19-08-2024");
        datos.setNombre("Otro nombre");
        datos.setDescripcion("despues");
        datos.setColumnaEtiqueta("Id-Participante");
        datos.setDirigidoA(DirigidoA.MUJERES);
        datos.setEdadMinima(18);
        datos.setEdadMaxima(49);
        datos.setPermiteAutollenado(true);
        datos.setActivo(false);

        TipoCuestionario resultado = servicio.actualizar(5L, datos);

        for (Field campo : TipoCuestionario.class.getDeclaredFields()) {
            if (campo.isSynthetic() || java.lang.reflect.Modifier.isStatic(campo.getModifiers())) continue;
            if (EXENTOS.contains(campo.getName())) continue;

            campo.setAccessible(true);
            Object esperado = campo.get(datos);
            Object real = campo.get(resultado);

            assertEquals(esperado, real,
                    "El campo \"" + campo.getName() + "\" no se copio al editar el instrumento. "
                            + "Anadelo a TipoCuestionarioService.actualizar(), o a la lista EXENTOS "
                            + "de esta prueba si de verdad no debe copiarse.");

            assertNotNull(esperado,
                    "La prueba tiene que dar un valor distinto del previo al campo \""
                            + campo.getName() + "\" para que comprobarlo signifique algo.");
        }
    }

    @Test
    @DisplayName("el caso que se escapo: activar el autollenado se guarda")
    void elAutollenadoSeGuarda() {
        // La prueba de arriba ya lo cubre, pero este caso concreto merece su nombre: el
        // sintoma era que la casilla volvia a aparecer apagada al reabrir el instrumento,
        // y por tanto el participante seguia sin ver el cuestionario.
        TipoCuestionario datos = new TipoCuestionario();
        datos.setClave("C1");
        datos.setVersion("1");
        datos.setNombre("Cuestionario General");
        datos.setColumnaEtiqueta("Folio");
        datos.setDirigidoA(DirigidoA.AMBOS);
        datos.setPermiteAutollenado(true);

        assertTrue(servicio.actualizar(5L, datos).getPermiteAutollenado());
    }

    @Test
    @DisplayName("la institucion no se puede cambiar desde el formulario")
    void laInstitucionNoViajaDesdeFuera() {
        // Es uno de los exentos, y conviene que se compruebe en lugar de quedar solo
        // anotado: aceptarla de fuera moveria un instrumento a otra sede.
        Institucion otra = new Institucion();
        otra.setId(99L);

        TipoCuestionario datos = new TipoCuestionario();
        datos.setClave("C1");
        datos.setVersion("1");
        datos.setNombre("Cuestionario General");
        datos.setColumnaEtiqueta("Folio");
        datos.setDirigidoA(DirigidoA.AMBOS);
        datos.setInstitucion(otra);

        assertEquals(1L, servicio.actualizar(5L, datos).getInstitucion().getId());
    }
}
