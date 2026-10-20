package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.Cuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.CuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.EstadoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.OrigenCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacion;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.Severidad;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.TipoRegla;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacionRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OpcionVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.institucion.InstitucionModuloService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * El autoguardado del participante.
 *
 * <p>Son las cuatro propiedades de las que depende que nadie pierda respuestas, y ninguna
 * se ve a simple vista leyendo el código: que combinar no borre, que un cambio atrasado no
 * pise uno nuevo, que un valor malo no arrastre al lote, y que cambiar una pregunta llave
 * limpie lo que deja de aplicar. Las tres primeras fallan de forma intermitente —solo con
 * mala red— y por eso tienen que estar aquí y no en una prueba manual.</p>
 */
class AutoguardadoTest {

    private CuestionarioRepository cuestionarios;
    private RespuestaCuestionarioRepository respuestas;
    private VariableCuestionarioRepository variables;
    private SeccionCuestionarioRepository secciones;
    private PacienteRepository pacientes;
    private TipoCuestionarioRepository tipos;
    private InstitucionModuloService modulos;
    private InstitucionContextService contexto;
    private MisCuestionariosService servicio;

    private Cuestionario cuestionario;
    private TipoCuestionario tipo;
    private VariableCuestionario pFuma;      // llave: 1 = si
    private VariableCuestionario pCigarros;  // depende de pFuma = 1
    private VariableCuestionario pEdad;      // numero 0-120, independiente

    private ReglaValidacionRepository reglas;
    private final List<RespuestaCuestionario> enLaBase = new ArrayList<>();

    @BeforeEach
    void preparar() {
        cuestionarios = mock(CuestionarioRepository.class);
        respuestas = mock(RespuestaCuestionarioRepository.class);
        variables = mock(VariableCuestionarioRepository.class);
        secciones = mock(SeccionCuestionarioRepository.class);
        pacientes = mock(PacienteRepository.class);
        tipos = mock(TipoCuestionarioRepository.class);
        modulos = mock(InstitucionModuloService.class);
        contexto = mock(InstitucionContextService.class);
        reglas = mock(ReglaValidacionRepository.class);
        // Sin reglas configuradas: estas pruebas son del autoguardado, no del motor
        // de validaciones, que tiene las suyas en MotorValidacionesTest.
        when(reglas.findAllByTipoCuestionario_IdAndActivaTrue(anyLong()))
                .thenReturn(List.of());
        servicio = new MisCuestionariosService(cuestionarios, respuestas, variables,
                secciones, pacientes, tipos, reglas, modulos, contexto);

        Institucion institucion = new Institucion();
        institucion.setId(1L);

        Persona persona = new Persona();
        persona.setId(10L);

        Paciente paciente = new Paciente();
        paciente.setId(100L);
        paciente.setFolio("000502");
        paciente.setPersona(persona);
        paciente.setInstitucion(institucion);

        BeanUser usuario = new BeanUser();
        usuario.setPersona(persona);

        TipoCuestionario tipo = new TipoCuestionario();
        tipo.setId(5L);
        tipo.setClave("C1");
        tipo.setNombre("Cuestionario General");
        tipo.setActivo(true);
        tipo.setInstitucion(institucion);
        this.tipo = tipo;

        cuestionario = new Cuestionario();
        cuestionario.setId(50L);
        cuestionario.setPaciente(paciente);
        cuestionario.setTipoCuestionario(tipo);
        cuestionario.setOrigen(OrigenCuestionario.APLICACION);
        cuestionario.setEstado(EstadoCuestionario.BORRADOR);

        pFuma = variable(1L, "P10", TipoVariable.OPCION_UNICA, tipo, null, null,
                new String[]{"1", "2"});
        pCigarros = variable(2L, "P11", TipoVariable.NUMERO, tipo, "P10", "1", null);
        pCigarros.setValorMax(80d);
        pEdad = variable(3L, "P4", TipoVariable.NUMERO, tipo, null, null, null);
        pEdad.setValorMax(120d);

        when(contexto.getUsuarioActual()).thenReturn(usuario);
        when(pacientes.findByPersona_Id(10L)).thenReturn(Optional.of(paciente));
        when(modulos.tieneAccesoHabilitado(1L, ModuloSistema.CUESTIONARIOS)).thenReturn(true);
        when(cuestionarios.findById(50L)).thenReturn(Optional.of(cuestionario));
        when(cuestionarios.save(any())).thenAnswer(i -> i.getArgument(0));

        when(respuestas.findAllByCuestionario_Id(50L)).thenAnswer(i -> new ArrayList<>(enLaBase));
        when(respuestas.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(variables.findAllById(any())).thenAnswer(i -> {
            Iterable<Long> ids = i.getArgument(0);
            List<VariableCuestionario> salida = new ArrayList<>();
            for (Long id : ids) {
                if (id.equals(1L)) salida.add(pFuma);
                else if (id.equals(2L)) salida.add(pCigarros);
                else if (id.equals(3L)) salida.add(pEdad);
            }
            return salida;
        });
        when(variables.findAllByTipoCuestionario_IdAndDependeDeIn(anyLong(), any()))
                .thenAnswer(i -> {
                    Collection<String> llaves = i.getArgument(1);
                    return llaves.contains("P10") ? List.of(pCigarros) : List.of();
                });
        when(respuestas.findAllByCuestionario_IdAndVariable_Id(eq(50L), anyLong()))
                .thenAnswer(i -> {
                    Long idVar = i.getArgument(1);
                    return enLaBase.stream()
                            .filter(r -> r.getVariable().getId().equals(idVar))
                            .toList();
                });
    }

    private VariableCuestionario variable(Long id, String codigo, TipoVariable tipoVar,
                                          TipoCuestionario tipo, String dependeDe,
                                          String habilitante, String[] codigosOpcion) {
        VariableCuestionario v = new VariableCuestionario();
        v.setId(id);
        v.setCodigo(codigo);
        v.setTipo(tipoVar);
        v.setTipoCuestionario(tipo);
        v.setActiva(true);
        v.setDependeDe(dependeDe);
        v.setValorHabilitante(habilitante);
        List<OpcionVariable> opciones = new ArrayList<>();
        if (codigosOpcion != null) {
            for (String c : codigosOpcion) {
                OpcionVariable o = new OpcionVariable();
                o.setCodigo(c);
                o.setEtiqueta("Opcion " + c);
                opciones.add(o);
            }
        }
        v.setOpciones(opciones);
        return v;
    }

    /** Mete en la base una respuesta ya guardada, con su secuencia. */
    private RespuestaCuestionario yaGuardada(VariableCuestionario v, String codigo,
                                             Double numero, Long secuencia) {
        RespuestaCuestionario r = new RespuestaCuestionario();
        r.setCuestionario(cuestionario);
        r.setVariable(v);
        r.setOrdenRespuesta(0);
        r.setValorCodigo(codigo);
        r.setValorNumerico(numero);
        r.setMarcaAusencia(MarcaAusencia.NINGUNA);
        r.setSecuenciaCliente(secuencia);
        enLaBase.add(r);
        return r;
    }

    private List<RespuestaCuestionario> loGuardado() {
        ArgumentCaptor<List<RespuestaCuestionario>> captor = ArgumentCaptor.forClass(List.class);
        verify(respuestas).saveAll(captor.capture());
        return captor.getValue();
    }

    // ── 1. Combinar, no reemplazar ───────────────────────────────────────────

    @Nested
    @DisplayName("Combina en lugar de reemplazar")
    class Combina {

        @Test
        @DisplayName("lo que no viene en el lote no se borra")
        void noBorraLoAusente() {
            // Es LA diferencia con el guardado por bloque del personal. Si esto fallara,
            // el primer autoguardado borraria el resto del bloque: el cliente solo manda
            // lo que cambio.
            yaGuardada(pEdad, null, 52d, 1L);

            servicio.autoguardar(50L, List.of(
                    new MisCuestionariosService.Cambio(1L, "1", 2L)));

            verify(respuestas, never()).deleteAll(argThat(
                    (Iterable<RespuestaCuestionario> borradas) -> borradas.iterator().hasNext()));
            assertEquals(1, loGuardado().size(), "solo se escribe el cambio recibido");
            assertEquals("P10", loGuardado().get(0).getVariable().getCodigo());
        }

        @Test
        @DisplayName("un valor vacio no se ignora: borra la respuesta")
        void elVacioEsUnaRespuesta() {
            // Si se ignorara, el participante no podria deshacer lo que contesto por error.
            yaGuardada(pEdad, null, 52d, 1L);

            var r = servicio.autoguardar(50L, List.of(
                    new MisCuestionariosService.Cambio(3L, "", 2L)));

            assertTrue(r.aceptados().containsKey(3L));
            RespuestaCuestionario fila = loGuardado().get(0);
            assertEquals(MarcaAusencia.SIN_RESPUESTA, fila.getMarcaAusencia());
            assertNull(fila.getValorNumerico());
        }
    }

    // ── 2. El orden de llegada no manda ──────────────────────────────────────

    @Nested
    @DisplayName("Un cambio atrasado no pisa uno nuevo")
    class Secuencia {

        @Test
        @DisplayName("una secuencia menor que la guardada se rechaza sin tocar el valor")
        void rechazaElAtrasado() {
            // El caso real: el participante escribe «5», lo borra; el borrado llega
            // primero y el «5» despues. Sin esto, el 5 reaparece solo.
            yaGuardada(pEdad, null, null, 7L);   // el borrado ya aterrizo, secuencia 7

            var r = servicio.autoguardar(50L, List.of(
                    new MisCuestionariosService.Cambio(3L, "5", 6L)));   // el «5», atrasado

            assertTrue(r.rechazados().contains(3L));
            assertTrue(r.aceptados().isEmpty());
            assertTrue(loGuardado().isEmpty(), "no se escribe nada");
        }

        @Test
        @DisplayName("la misma secuencia tambien se rechaza: hace el reenvio inofensivo")
        void reenviarNoHaceNada() {
            yaGuardada(pEdad, null, 52d, 4L);

            var r = servicio.autoguardar(50L, List.of(
                    new MisCuestionariosService.Cambio(3L, "52", 4L)));

            assertTrue(r.rechazados().contains(3L));
        }

        @Test
        @DisplayName("una secuencia mayor si se aplica")
        void aceptaElNuevo() {
            yaGuardada(pEdad, null, 52d, 4L);

            var r = servicio.autoguardar(50L, List.of(
                    new MisCuestionariosService.Cambio(3L, "53", 5L)));

            assertEquals(5L, r.aceptados().get(3L));
            assertEquals(53d, loGuardado().get(0).getValorNumerico());
        }
    }

    // ── 3. Un cambio malo no arrastra al lote ────────────────────────────────

    @Nested
    @DisplayName("Un cambio invalido no tumba los demas")
    class Aislamiento {

        @Test
        @DisplayName("se guardan los buenos y se reporta el malo")
        void losBuenosPasan() {
            // Si el lote se rechazara entero, el participante perderia lo que si estaba
            // bien y el autoguardado reintentaria el lote para siempre.
            var r = servicio.autoguardar(50L, List.of(
                    new MisCuestionariosService.Cambio(3L, "200", 1L),   // pasa de 120
                    new MisCuestionariosService.Cambio(1L, "1", 2L)));   // correcto

            assertTrue(r.invalidos().containsKey(3L));
            assertTrue(r.aceptados().containsKey(1L));
            assertEquals(1, loGuardado().size());
        }

        @Test
        @DisplayName("una pregunta de otro cuestionario se rechaza, no se guarda")
        void preguntaAjena() {
            TipoCuestionario otro = new TipoCuestionario();
            otro.setId(99L);
            pEdad.setTipoCuestionario(otro);

            var r = servicio.autoguardar(50L, List.of(
                    new MisCuestionariosService.Cambio(3L, "40", 1L)));

            assertTrue(r.invalidos().containsKey(3L));
            assertTrue(r.aceptados().isEmpty());
        }

        @Test
        @DisplayName("no se contesta lo que la pregunta llave dejo fuera")
        void dependienteDeshabilitada() {
            yaGuardada(pFuma, "2", null, 1L);   // dijo que no fuma

            var r = servicio.autoguardar(50L, List.of(
                    new MisCuestionariosService.Cambio(2L, "20", 2L)));

            assertTrue(r.invalidos().containsKey(2L));
        }
    }

    // ── 4. Cambiar la llave limpia lo que deja de aplicar ────────────────────

    @Nested
    @DisplayName("Al cambiar una pregunta llave")
    class Llaves {

        @Test
        @DisplayName("se borra lo contestado en las que dejan de aplicar")
        void limpiaDependientes() {
            // Contesto 20 cigarros y despues cambio a «no fumo». Dejar el 20 guardaria que
            // fuma veinte al dia alguien que declaro no fumar, y la pantalla ya no lo
            // muestra: nadie volveria a verlo.
            yaGuardada(pFuma, "1", null, 1L);
            RespuestaCuestionario cigarros = yaGuardada(pCigarros, null, 20d, 2L);

            servicio.autoguardar(50L, List.of(
                    new MisCuestionariosService.Cambio(1L, "2", 3L)));

            ArgumentCaptor<List<RespuestaCuestionario>> captor =
                    ArgumentCaptor.forClass(List.class);
            verify(respuestas).deleteAll(captor.capture());
            assertEquals(List.of(cigarros), captor.getValue());
        }

        @Test
        @DisplayName("si la llave sigue habilitando, no se borra nada")
        void noLimpiaSiSigueAplicando() {
            yaGuardada(pFuma, "1", null, 1L);
            yaGuardada(pCigarros, null, 20d, 2L);

            servicio.autoguardar(50L, List.of(
                    new MisCuestionariosService.Cambio(1L, "1", 3L)));

            verify(respuestas, never()).deleteAll(any());
        }
    }

    // ── 5. Lo que no se puede contestar en linea ─────────────────────────────

    @Nested
    @DisplayName("Lo que el participante no puede tocar")
    class Limites {

        @Test
        @DisplayName("un cuestionario que vino de su hoja escaneada no se contesta en linea")
        void elDePapelNo() {
            // Esos valores son la lectura del instrumento. Permitir sobrescribirlos
            // dejaria al participante corrigiendo lo que el escaner leyo de su propia hoja.
            cuestionario.setOrigen(OrigenCuestionario.PAPEL_REMARK);

            var e = assertThrows(ObjConflictException.class, () ->
                    servicio.autoguardar(50L, List.of(
                            new MisCuestionariosService.Cambio(3L, "40", 1L))));
            assertTrue(e.getMessage().contains("papel"));
        }

        @Test
        @DisplayName("uno ya enviado no admite cambios")
        void elEnviadoNo() {
            cuestionario.setEstado(EstadoCuestionario.COMPLETO);

            assertThrows(ObjConflictException.class, () ->
                    servicio.autoguardar(50L, List.of(
                            new MisCuestionariosService.Cambio(3L, "40", 1L))));
        }

        @Test
        @DisplayName("el de otra persona se trata como inexistente")
        void elDeOtroNo() {
            // Mismo mensaje que si no existiera: decir «es de otra persona» confirmaria
            // que ese id existe.
            Paciente otro = new Paciente();
            otro.setId(999L);
            cuestionario.setPaciente(otro);

            assertThrows(imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException.class,
                    () -> servicio.autoguardar(50L, List.of(
                            new MisCuestionariosService.Cambio(3L, "40", 1L))));
        }

        @Test
        @DisplayName("enviar deja COMPLETO, nunca VALIDADO")
        void enviarNoValida() {
            when(variables.findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(5L))
                    .thenReturn(List.of(pFuma, pCigarros, pEdad));

            var resultado = servicio.enviar(50L);

            assertEquals(EstadoCuestionario.COMPLETO, resultado.getEstado(),
                    "validar es hacerse responsable de lo capturado; le toca al personal");
        }

        @Test
        @DisplayName("no se puede enviar con algo que la plantilla marca como error")
        void noEnviaConErrorPendiente() {
            // Despues de enviar ya no puede corregir nada, asi que este es el ultimo
            // momento en que pararlo sirve de algo.
            when(variables.findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(5L))
                    .thenReturn(List.of(pFuma, pCigarros, pEdad));

            ReglaValidacion regla = new ReglaValidacion();
            regla.setRegla(TipoRegla.REQUIERE_TEXTO);
            regla.setPregunta(pFuma.getCodigo());
            regla.setValor("1");
            regla.setComparaCon(pEdad.getCodigo());
            regla.setSeveridad(Severidad.ERROR);
            regla.setMensaje("Dijo que si y no escribio cual.");
            regla.setActiva(true);
            when(reglas.findAllByTipoCuestionario_IdAndActivaTrue(5L))
                    .thenReturn(List.of(regla));

            RespuestaCuestionario contestada = new RespuestaCuestionario();
            contestada.setCuestionario(cuestionario);
            contestada.setVariable(pFuma);
            contestada.setValorCodigo("1");
            contestada.setMarcaAusencia(MarcaAusencia.NINGUNA);
            enLaBase.add(contestada);

            var e = assertThrows(ObjConflictException.class, () -> servicio.enviar(50L));
            assertTrue(e.getMessage().contains("no escribio cual"),
                    "el mensaje que ve el participante sale de la plantilla, no del codigo");
            assertEquals(EstadoCuestionario.BORRADOR, cuestionario.getEstado(),
                    "no debe quedar enviado a medias");
        }

        @Test
        @DisplayName("lo que solo es para revisar no le impide enviar")
        void loDeRevisionNoPara() {
            // Es la diferencia que hace util el aviso: si todo bloqueara, el
            // participante no podria terminar por cosas que le toca mirar al personal.
            when(variables.findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(5L))
                    .thenReturn(List.of(pFuma, pCigarros, pEdad));

            ReglaValidacion regla = new ReglaValidacion();
            regla.setRegla(TipoRegla.REQUIERE_TEXTO);
            regla.setPregunta(pFuma.getCodigo());
            regla.setValor("1");
            regla.setComparaCon(pEdad.getCodigo());
            regla.setSeveridad(Severidad.REVISION);
            regla.setMensaje("Conviene revisarlo.");
            regla.setActiva(true);
            when(reglas.findAllByTipoCuestionario_IdAndActivaTrue(5L))
                    .thenReturn(List.of(regla));

            RespuestaCuestionario contestada = new RespuestaCuestionario();
            contestada.setCuestionario(cuestionario);
            contestada.setVariable(pFuma);
            contestada.setValorCodigo("1");
            contestada.setMarcaAusencia(MarcaAusencia.NINGUNA);
            enLaBase.add(contestada);

            assertEquals(EstadoCuestionario.COMPLETO, servicio.enviar(50L).getEstado());
        }
    }

    // ── 6. Empezarlo por cuenta propia ───────────────────────────────────────

    @Nested
    @DisplayName("Empezar un cuestionario por cuenta propia")
    class Autollenado {

        @BeforeEach
        void prepararCatalogo() {
            when(tipos.findById(5L)).thenReturn(Optional.of(tipo));
            when(tipos.findAllByActivoAndInstitucion_IdOrderByClaveAsc(true, 1L))
                    .thenReturn(List.of(tipo));
            when(cuestionarios.existsByPaciente_IdAndTipoCuestionario_Id(100L, 5L))
                    .thenReturn(false);
        }

        @Test
        @DisplayName("un instrumento sin autollenado no se ofrece ni se puede iniciar")
        void apagadoPorOmision() {
            // El defecto es apagado a proposito: una prueba cognitiva autoaplicada parece
            // valida y no lo es. Es el caso que hay que proteger.
            tipo.setPermiteAutollenado(false);

            assertTrue(servicio.disponibles().isEmpty());

            var e = assertThrows(ObjConflictException.class, () -> servicio.iniciar(5L));
            assertTrue(e.getMessage().contains("personal"));
        }

        @Test
        @DisplayName("con autollenado y criterio cumplido, se ofrece y se crea en APLICACION")
        void seCreaParaContestarEnLinea() {
            tipo.setPermiteAutollenado(true);

            assertEquals(1, servicio.disponibles().size());

            Cuestionario creado = servicio.iniciar(5L);
            assertEquals(OrigenCuestionario.APLICACION, creado.getOrigen(),
                    "tiene que ser APLICACION o el propio participante no podria contestarlo");
            assertEquals(EstadoCuestionario.BORRADOR, creado.getEstado());
            assertEquals("C1/000502/F4", creado.getEtiqueta());
        }

        @Test
        @DisplayName("no se ofrece el que ya tiene")
        void noDuplica() {
            tipo.setPermiteAutollenado(true);
            when(cuestionarios.existsByPaciente_IdAndTipoCuestionario_Id(100L, 5L))
                    .thenReturn(true);

            assertTrue(servicio.disponibles().isEmpty());
            assertThrows(ObjConflictException.class, () -> servicio.iniciar(5L));
        }

        @Test
        @DisplayName("no se puede iniciar uno que no le corresponde por edad")
        void respetaElCriterio() {
            // La pantalla solo sugiere; la regla vive en el servidor. Sin esta
            // comprobacion, una peticion a mano saltaria el criterio del protocolo.
            tipo.setPermiteAutollenado(true);
            tipo.setEdadMinima(45);
            cuestionario.getPaciente().getPersona()
                    .setFechaNacimiento(java.time.LocalDate.now().minusYears(30));

            assertTrue(servicio.disponibles().isEmpty());
            var e = assertThrows(ObjConflictException.class, () -> servicio.iniciar(5L));
            assertTrue(e.getMessage().contains("no le corresponde"));
        }

        @Test
        @DisplayName("si falta la fecha de nacimiento se permite, pero queda senalado")
        void indeterminadoNoBloquea() {
            // El hueco es del expediente, no del participante: bloquearlo lo dejaria sin
            // poder contestar por un dato que nadie capturo.
            tipo.setPermiteAutollenado(true);
            tipo.setEdadMinima(45);
            cuestionario.getPaciente().getPersona().setFechaNacimiento(null);

            assertEquals(1, servicio.disponibles().size());
            assertTrue(servicio.disponibles().get(0).indeterminado());

            Cuestionario creado = servicio.iniciar(5L);
            assertTrue(creado.getFueraDeCriterio());
            assertNotNull(creado.getMotivoFueraDeCriterio());
        }
    }
}
