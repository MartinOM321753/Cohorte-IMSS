package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OpcionVariableRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OperadorDependencia;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Que al editar una pregunta no se pierda ninguno de sus campos.
 *
 * <h3>El mismo guardia que el de los instrumentos, y por el mismo motivo</h3>
 *
 * <p>{@code actualizar} copia campo por campo con una lista escrita a mano, y esa
 * lista ya se quedó corta una vez: {@code permiteAutollenado} no se copiaba y la
 * pantalla decía «guardado» mientras el valor se perdía. Aquí hay el triple de campos
 * y cada tanto se añade uno —el operador de la dependencia y la celda de la rejilla
 * son de esta misma tanda—, así que el riesgo es mayor, no menor.</p>
 *
 * <p>Por eso se recorre la entidad por reflexión en vez de enumerar: una prueba
 * escrita a mano tampoco conocería el campo nuevo.</p>
 */
class ActualizarPreguntaConservaTodoTest {

    /**
     * Campos que a propósito no se copian de los datos recibidos.
     *
     * <p>Ninguno es un olvido. La identidad y el instrumento no se tocan nunca; la
     * columna normalizada la deriva la propia entidad y aceptarla de fuera permitiría
     * desalinearla de la columna real; la sección viaja por su id en un parámetro
     * aparte, y las opciones tienen su propio reemplazo porque hay que conservar la
     * fila de los códigos que siguen existiendo.</p>
     */
    private static final List<String> EXENTOS =
            List.of("id", "tipoCuestionario", "columnaNormalizada", "seccion", "opciones");

    private VariableCuestionarioRepository variables;
    private VariableCuestionarioService servicio;
    private VariableCuestionario guardada;

    @BeforeEach
    void preparar() {
        variables = mock(VariableCuestionarioRepository.class);
        var opciones = mock(OpcionVariableRepository.class);
        var respuestas = mock(RespuestaCuestionarioRepository.class);
        var tipoService = mock(TipoCuestionarioService.class);
        var contexto = mock(InstitucionContextService.class);

        servicio = new VariableCuestionarioService(variables, opciones, respuestas,
                tipoService, contexto);

        Institucion institucion = new Institucion();
        institucion.setId(1L);
        TipoCuestionario tipo = new TipoCuestionario();
        tipo.setId(3L);
        tipo.setInstitucion(institucion);

        guardada = new VariableCuestionario();
        guardada.setId(11L);
        guardada.setTipoCuestionario(tipo);
        guardada.setCodigo("P112_2");
        guardada.setTipo(TipoVariable.CASILLA);

        when(variables.findById(11L)).thenReturn(Optional.of(guardada));
        when(variables.save(any())).thenAnswer(i -> i.getArgument(0));
        // Sin respuestas guardadas, para que tambien se pueda cambiar el codigo.
        when(respuestas.existsByVariable_Id(anyLong())).thenReturn(false);
    }

    @Test
    @DisplayName("todo campo configurable sobrevive a la edicion")
    void nadaSePierde() throws Exception {
        VariableCuestionario datos = new VariableCuestionario();
        datos.setCodigo("P112_3");
        datos.setColumnaVolcado("P112_3");
        datos.setPregunta("Caminar Martes");
        datos.setTipo(TipoVariable.OPCION_UNICA);
        datos.setOrden(4);
        datos.setPagina(15);
        datos.setGrupoMultiple("P112");
        datos.setDependeDe("P112_1");
        datos.setValorHabilitante("1");
        datos.setOperadorDependencia(OperadorDependencia.DISTINTO);
        datos.setObligatoria(true);
        datos.setValorMin(1.0);
        datos.setValorMax(7.0);
        datos.setLongitudMax(3);
        datos.setUnidad("dias");
        datos.setDocumentada(false);
        datos.setActiva(false);
        datos.setObservaciones("revisada contra el papel");
        datos.setInstruccion("Considerando horario de 24 hrs.");
        datos.setFigura("figuras/trago_estandar.png");
        datos.setFilaMatriz("Caminar");
        datos.setColumnaMatriz("Martes");

        VariableCuestionario resultado = servicio.actualizar(11L, datos, null, null);

        for (Field campo : VariableCuestionario.class.getDeclaredFields()) {
            if (campo.isSynthetic() || Modifier.isStatic(campo.getModifiers())) continue;
            if (EXENTOS.contains(campo.getName())) continue;

            campo.setAccessible(true);
            assertNotNull(campo.get(datos),
                    "La prueba tiene que dar un valor al campo \"" + campo.getName()
                            + "\" para que comprobarlo signifique algo.");
            assertEquals(campo.get(datos), campo.get(resultado),
                    "El campo \"" + campo.getName() + "\" no se copio al editar la pregunta. "
                            + "Anadelo a VariableCuestionarioService.actualizar(), o a la lista "
                            + "EXENTOS de esta prueba si de verdad no debe copiarse.");
        }
    }

    @Test
    @DisplayName("la columna normalizada no se acepta de fuera")
    void normalizadaDerivada() {
        // Es uno de los exentos y conviene comprobarlo en vez de dejarlo anotado:
        // aceptarla de fuera la desalinearia de la columna real y el emparejamiento del
        // volcado dejaria de encontrar la pregunta, sin error visible. Quien la calcula
        // es la propia entidad, en @PreUpdate; aqui solo se comprueba que el servicio no
        // la copie, porque con el repositorio simulado ese gancho no corre.
        VariableCuestionario datos = new VariableCuestionario();
        datos.setCodigo("P112_2");
        datos.setColumnaVolcado("P112_2");
        datos.setTipo(TipoVariable.CASILLA);
        datos.setColumnaNormalizada("LO QUE SEA");

        assertNotEquals("LO QUE SEA",
                servicio.actualizar(11L, datos, null, null).getColumnaNormalizada());
    }
}
