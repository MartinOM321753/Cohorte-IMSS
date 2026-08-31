package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El maquetador: diseño de plantilla + datos del participante → HTML.
 *
 * <p>Lo que de verdad se prueba aquí es el modelo de claves. Un reporte no habla de
 * un estudio, habla de una persona: puede pedir cinco parámetros de su DEXA, tres de
 * sus signos vitales y las evidencias solo del primero. Por eso el tipo de estudio va
 * <b>dentro de la clave</b> y no en la plantilla — atarla a un tipo, como estuvo al
 * principio, hacía imposible justamente eso.</p>
 */
class MaquetadorReporteTest {

    private static final long DEXA = 10L;
    private static final long SIGNOS = 20L;
    private static final long TANITA = 30L;

    private final ResolvedorCampos resolvedor = new ResolvedorCampos();
    private final MaquetadorReporte maquetador = new MaquetadorReporte(
            new ObjectMapper(), resolvedor, new BloqueResultados(resolvedor),
            new BloqueEstudios(), new EvidenciasReporte(null, null));

    // ── Escenario: un participante con tres estudios distintos ───────────────

    private Paciente participante() {
        Persona persona = new Persona();
        persona.setNombre("María");
        persona.setApellidoPaterno("Rodríguez");
        persona.setFechaNacimiento(LocalDate.of(1980, 1, 1));
        persona.setSexo(Persona.Sexo.F);

        Paciente p = new Paciente();
        p.setFolio("HWCS-000418");
        p.setPersona(persona);
        return p;
    }

    private EstudioMedico estudio(long idTipo, String nombre, LocalDateTime fecha) {
        TipoEstudio tipo = new TipoEstudio();
        tipo.setId(idTipo);
        tipo.setNombre(nombre);

        EstudioMedico e = new EstudioMedico();
        e.setId(idTipo * 100);
        e.setTipoEstudio(tipo);
        e.setFechaEstudio(fecha);
        e.setResultadoEstudio(new ArrayList<>());
        return e;
    }

    private void conResultado(EstudioMedico e, long idParam, String nombre, Double valor, String unidad) {
        ParametroEstudio p = new ParametroEstudio();
        p.setId(idParam);
        p.setNombre(nombre);
        p.setUnidad(unidad);
        p.setTipo(TipoParametro.NUMERICO);

        ResultadoEstudio r = new ResultadoEstudio();
        r.setParametro(p);
        r.setValorNumerico(valor);
        r.setGrupoCodigo("ROOT");
        r.setOrdenResultado(e.getResultadoEstudio().size());
        e.getResultadoEstudio().add(r);
    }

    private ContextoReporte contexto() {
        EstudioMedico dexa = estudio(DEXA, "DEXA", LocalDateTime.of(2026, 5, 10, 9, 0));
        conResultado(dexa, 101L, "Densidad mineral ósea", 1.15, "g/cm²");
        conResultado(dexa, 102L, "Masa magra", 42.3, "kg");

        EstudioMedico signos = estudio(SIGNOS, "Signos vitales", LocalDateTime.of(2026, 6, 1, 8, 30));
        conResultado(signos, 201L, "Presión sistólica", 128.0, "mmHg");
        conResultado(signos, 202L, "Frecuencia cardiaca", 72.0, "lpm");

        EstudioMedico tanita = estudio(TANITA, "TANITA", LocalDateTime.of(2026, 6, 2, 10, 0));
        conResultado(tanita, 301L, "Porcentaje de grasa", 28.4, "%");

        return new ContextoReporte(participante(), List.of(dexa, signos, tanita), null,
                new ContextoReporte.Totales(3, 40, 2));
    }

    private String diseno(String elementos) {
        return """
            {"version":1,"tamano":"CARTA","orientacion":"vertical",
             "margenes":{"superiorMm":18,"derechoMm":15,"inferiorMm":16,"izquierdoMm":15},
             "paginas":[{"id":"p1","elementos":[""" + elementos + "]}]}";
    }

    private String texto(String contenido) {
        return """
            {"id":"t1","tipo":"texto","xMm":20,"yMm":30,"anchoMm":150,"altoMm":20,"z":1,
             "contenido":"%s","tamanoPt":11,"color":"#111111","alineacion":"left"}
            """.formatted(contenido);
    }

    private String bloque(String clave, String extra) {
        return """
            {"id":"b1","tipo":"datos","xMm":15,"yMm":80,"anchoMm":180,"altoMm":60,"z":1,
             "clave":"%s","desbordamiento":"crecer"%s}
            """.formatted(clave, extra.isEmpty() ? "" : "," + extra);
    }

    // ── El caso que motivó el rediseño ───────────────────────────────────────

    /**
     * Lo que pidió el usuario, literal: parámetros de dos estudios distintos
     * incrustados en el mismo párrafo.
     */
    @Test
    @DisplayName("Un mismo texto mezcla parámetros de estudios distintos")
    void mezclaParametrosDeEstudiosDistintos() {
        String contenido = "Densidad {{estudio.10.param.101}} g/cm2, "
                + "presion {{estudio.20.param.201}} mmHg";
        String html = maquetador.maquetar(diseno(texto(contenido)), contexto());

        assertTrue(html.contains("1.15"), "Falta el dato del DEXA: " + html);
        assertTrue(html.contains("128"), "Falta el dato de signos vitales: " + html);
        assertFalse(html.contains("{{"), "No debe quedar ningún marcador sin resolver");
    }

    @Test
    @DisplayName("La tabla de un estudio no arrastra los resultados de otro")
    void laTablaEsDeSuEstudio() {
        String html = maquetador.maquetar(
                diseno(bloque(ClaveCampo.deBloqueResultados(DEXA), "")), contexto());

        assertTrue(html.contains("Densidad mineral ósea"), "Debe salir lo del DEXA");
        assertTrue(html.contains("Masa magra"));
        assertFalse(html.contains("Presión sistólica"), "No debe colarse lo de signos vitales");
        assertFalse(html.contains("Porcentaje de grasa"), "Ni lo de la TANITA");
    }

    /**
     * El otro caso que pidió: evidencias de un estudio y no de otro. Aquí se
     * comprueba que el bloque va dirigido al estudio correcto; el contenido de las
     * evidencias depende del almacenamiento y se prueba aparte.
     */
    @Test
    @DisplayName("Las evidencias se piden por estudio, no en bloque")
    void lasEvidenciasSonPorEstudio() {
        // Con un estudio que el participante no tiene, el bloque lo dice en vez de
        // fallar: la misma plantilla se usa con gente que no tiene todos los estudios.
        String html = maquetador.maquetar(
                diseno(bloque("bloque.estudio.999.evidencias", "")), contexto());

        assertTrue(html.contains("no tiene este estudio"), html);
    }

    @Test
    @DisplayName("Si la plantilla eligió parámetros, solo salen esos")
    void soloLosElegidos() {
        String html = maquetador.maquetar(
                diseno(bloque(ClaveCampo.deBloqueResultados(DEXA), "\"seleccion\":[101]")), contexto());

        assertTrue(html.contains("Densidad mineral ósea"), "El elegido tiene que salir");
        assertFalse(html.contains("Masa magra"), "El no elegido no debe salir");
    }

    // ── Robustez ─────────────────────────────────────────────────────────────

    /**
     * La misma plantilla se usa con participantes distintos, y no todos tienen hechos
     * los mismos estudios. Que falte uno deja hueco, no rompe el documento.
     */
    @Test
    @DisplayName("Un estudio que el participante no tiene deja hueco")
    void estudioAusenteDejaHueco() {
        String html = maquetador.maquetar(
                diseno(texto("Valor: {{estudio.999.param.1}} fin")), contexto());

        assertTrue(html.contains("Valor:  fin"), html);
        assertFalse(html.contains("999"), "La clave no puede acabar impresa");
    }

    @Test
    @DisplayName("Un parámetro que ese estudio no midió deja hueco")
    void parametroAusenteDejaHueco() {
        String html = maquetador.maquetar(
                diseno(texto("X{{estudio.10.param.777}}X")), contexto());
        assertTrue(html.contains("XX"), html);
    }

    @Test
    @DisplayName("Los datos del participante y los conteos salen igual")
    void datosDelParticipante() {
        String html = maquetador.maquetar(
                diseno(texto("{{participante.nombreCompleto}} · {{participante.folio}} · "
                        + "{{totales.muestras}} muestras")), contexto());

        assertTrue(html.contains("María Rodríguez"), html);
        assertTrue(html.contains("HWCS-000418"), html);
        assertTrue(html.contains("2 muestras"), html);
    }

    @Test
    @DisplayName("El listado de estudios trae los tres, del más reciente al más antiguo")
    void listadoDeEstudios() {
        String html = maquetador.maquetar(
                diseno(bloque(ClaveCampo.BLOQUE_LISTADO_ESTUDIOS, "")), contexto());

        assertTrue(html.contains("DEXA"));
        assertTrue(html.contains("Signos vitales"));
        assertTrue(html.contains("TANITA"));
        assertTrue(html.indexOf("TANITA") < html.indexOf("DEXA"),
                "La TANITA es de junio y el DEXA de mayo: va antes");
    }

    @Test
    @DisplayName("El valor de un campo se escapa antes de entrar al HTML")
    void elValorSeEscapa() {
        ContextoReporte ctx = contexto();
        ctx.persona().setNombre("Ana <b>& Co</b>");

        String html = maquetador.maquetar(diseno(texto("{{participante.nombreCompleto}}")), ctx);

        assertTrue(html.contains("&lt;b&gt;"), "Se coló etiqueta cruda: " + html);
        assertTrue(html.contains("&amp;"), "El ampersand debe escaparse");
    }

    @Test
    @DisplayName("Un color que no tiene forma de color no entra en el HTML")
    void colorInvalidoNoEntra() {
        String elemento = """
            {"id":"t1","tipo":"texto","xMm":10,"yMm":10,"anchoMm":50,"altoMm":10,"z":1,
             "contenido":"x","tamanoPt":10,"color":"red; background:url(javascript:0)","alineacion":"left"}
            """;
        String html = maquetador.maquetar(diseno(elemento), contexto());

        assertFalse(html.contains("javascript"), "Un valor así no puede llegar al atributo style");
        assertTrue(html.contains("color:#111111"), "Se cae al color por defecto");
    }

    @Test
    @DisplayName("Las medidas se emiten en milímetros, como se diseñaron")
    void lasMedidasSonMilimetros() {
        String html = maquetador.maquetar(diseno(texto("hola")), contexto());
        assertTrue(html.contains("left:20.0mm"), html);
        assertTrue(html.contains("@page { size: 215.9mm 279.4mm"), html);
    }

    @Test
    @DisplayName("Un diseño sin páginas se rechaza con un motivo legible")
    void disenoSinPaginas() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> maquetador.maquetar("{\"version\":1,\"paginas\":[]}", contexto()));
        assertTrue(e.getMessage().contains("página"), e.getMessage());
    }
}
