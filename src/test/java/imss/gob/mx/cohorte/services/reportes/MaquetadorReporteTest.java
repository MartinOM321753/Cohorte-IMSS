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
 * El maquetador: diseño de plantilla + datos → HTML.
 *
 * <p>Es el gemelo del lienzo del editor, y lo que se prueba aquí es sobre todo que
 * los datos entren donde deben y que un diseño imperfecto no tumbe la emisión: una
 * plantilla se hace una vez y se usa durante años, mientras el catálogo por debajo
 * cambia.</p>
 */
class MaquetadorReporteTest {

    private final ObjectMapper mapper = new ObjectMapper();
    // Las evidencias necesitan repositorio y almacenamiento, y ninguno de estos casos
    // las usa: se entrega el servicio sin dependencias en lugar de levantar el
    // contexto entero para probar maquetado, que es lógica sobre un JSON.
    private final MaquetadorReporte maquetador =
            new MaquetadorReporte(mapper, new BloqueResultados(), new EvidenciasReporte(null, null));

    // ── Escenario ────────────────────────────────────────────────────────────

    private EstudioMedico estudio() {
        Persona persona = new Persona();
        persona.setNombre("María");
        persona.setApellidoPaterno("Rodríguez");
        persona.setFechaNacimiento(LocalDate.of(1980, 1, 1));
        persona.setSexo(Persona.Sexo.F);

        Paciente paciente = new Paciente();
        paciente.setFolio("HWCS-000418");
        paciente.setPersona(persona);

        TipoEstudio tipo = new TipoEstudio();
        tipo.setNombre("Electrocardiograma");

        EstudioMedico e = new EstudioMedico();
        e.setPaciente(paciente);
        e.setTipoEstudio(tipo);
        e.setFechaEstudio(LocalDateTime.of(2026, 7, 16, 9, 40));
        e.setResultadoEstudio(new ArrayList<>());
        return e;
    }

    private ResultadoEstudio resultado(long idParam, String nombre, Double valor) {
        ParametroEstudio p = new ParametroEstudio();
        p.setId(idParam);
        p.setNombre(nombre);
        p.setUnidad("ms");
        p.setTipo(TipoParametro.NUMERICO);

        ResultadoEstudio r = new ResultadoEstudio();
        r.setParametro(p);
        r.setValorNumerico(valor);
        r.setGrupoCodigo("ROOT");
        r.setOrdenResultado(0);
        return r;
    }

    private ContextoEstudio contexto(EstudioMedico e) {
        return new ContextoEstudio(e, new ContextoEstudio.Totales(12, 40, 3));
    }

    private String diseno(String elementos) {
        return """
            {"version":1,"tamano":"CARTA","orientacion":"vertical",
             "margenes":{"superiorMm":18,"derechoMm":15,"inferiorMm":16,"izquierdoMm":15},
             "paginas":[{"id":"p1","elementos":[""" + elementos + "]}]}";
    }

    private String texto(String contenido) {
        return """
            {"id":"t1","tipo":"texto","xMm":20,"yMm":30,"anchoMm":100,"altoMm":10,"z":1,
             "contenido":"%s","tamanoPt":11,"color":"#111111","alineacion":"left"}
            """.formatted(contenido);
    }

    // ── Casos ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Los marcadores se sustituyen por el dato real")
    void losMarcadoresSeSustituyen() {
        String html = maquetador.maquetar(
                diseno(texto("Participante: {{participante.nombreCompleto}}, folio {{participante.folio}}")),
                contexto(estudio()));

        assertTrue(html.contains("María Rodríguez"), html);
        assertTrue(html.contains("HWCS-000418"), html);
        assertFalse(html.contains("{{"), "No debe quedar ningún marcador sin resolver");
    }

    @Test
    @DisplayName("Los conteos generales salen del contexto")
    void losConteosSalen() {
        String html = maquetador.maquetar(
                diseno(texto("Estudios: {{totales.estudios}} · Muestras: {{totales.muestras}}")),
                contexto(estudio()));

        assertTrue(html.contains("Estudios: 12"), html);
        assertTrue(html.contains("Muestras: 3"), html);
    }

    /**
     * Una plantilla se hace una vez y se usa años; el catálogo por debajo cambia.
     * Un campo que dejó de existir tiene que dejar un hueco, no imprimir su clave
     * en medio de la hoja ni tumbar la emisión entera.
     */
    @Test
    @DisplayName("Un campo desconocido deja hueco, no rompe ni imprime la clave")
    void campoDesconocidoDejaHueco() {
        String html = maquetador.maquetar(
                diseno(texto("Dato: {{campo.que.ya.no.existe}} fin")),
                contexto(estudio()));

        assertTrue(html.contains("Dato:  fin"), html);
        assertFalse(html.contains("campo.que.ya.no.existe"), "La clave no puede acabar impresa");
    }

    /**
     * El texto que escribe una persona entra al HTML. Un «&» suelto lo rompería, y
     * una etiqueta cruda sería algo peor.
     */
    @Test
    @DisplayName("El valor de un campo se escapa antes de entrar al HTML")
    void elValorSeEscapa() {
        EstudioMedico e = estudio();
        e.getPaciente().getPersona().setNombre("Ana <b>& Co</b>");

        String html = maquetador.maquetar(
                diseno(texto("{{participante.nombreCompleto}}")), contexto(e));

        assertTrue(html.contains("&lt;b&gt;"), "Se coló etiqueta cruda: " + html);
        assertTrue(html.contains("&amp;"), "El ampersand debe escaparse");
    }

    @Test
    @DisplayName("La tabla de resultados sale con todo si no se eligió nada")
    void tablaCompletaPorDefecto() {
        EstudioMedico e = estudio();
        e.getResultadoEstudio().add(resultado(1L, "Intervalo PR", 160.0));
        e.getResultadoEstudio().add(resultado(2L, "Duración QRS", 90.0));

        String bloque = """
            {"id":"b1","tipo":"datos","xMm":15,"yMm":60,"anchoMm":180,"altoMm":60,"z":1,
             "clave":"bloque.estudio.resultados","desbordamiento":"crecer"}
            """;
        String html = maquetador.maquetar(diseno(bloque), contexto(e));

        assertTrue(html.contains("Intervalo PR"), html);
        assertTrue(html.contains("Duración QRS"), html);
    }

    /**
     * Lo que pidió el usuario: poder mostrar solo algunos parámetros.
     */
    @Test
    @DisplayName("Si la plantilla eligió parámetros, solo salen esos")
    void soloLosElegidos() {
        EstudioMedico e = estudio();
        e.getResultadoEstudio().add(resultado(1L, "Intervalo PR", 160.0));
        e.getResultadoEstudio().add(resultado(2L, "Duración QRS", 90.0));

        String bloque = """
            {"id":"b1","tipo":"datos","xMm":15,"yMm":60,"anchoMm":180,"altoMm":60,"z":1,
             "clave":"bloque.estudio.resultados","desbordamiento":"crecer","seleccion":[1]}
            """;
        String html = maquetador.maquetar(diseno(bloque), contexto(e));

        assertTrue(html.contains("Intervalo PR"), "El elegido tiene que salir");
        assertFalse(html.contains("Duración QRS"), "El no elegido no debe salir");
    }

    /**
     * La misma plantilla sirve para estudios distintos del mismo tipo, y no todos
     * miden lo mismo. Que ninguno de los elegidos esté no es un fallo.
     */
    @Test
    @DisplayName("Si el estudio no midió ninguno de los elegidos, se avisa sin romper")
    void ningunoDeLosElegidos() {
        EstudioMedico e = estudio();
        e.getResultadoEstudio().add(resultado(1L, "Intervalo PR", 160.0));

        String bloque = """
            {"id":"b1","tipo":"datos","xMm":15,"yMm":60,"anchoMm":180,"altoMm":60,"z":1,
             "clave":"bloque.estudio.resultados","desbordamiento":"crecer","seleccion":[99]}
            """;
        String html = maquetador.maquetar(diseno(bloque), contexto(e));
        assertTrue(html.contains("Sin resultados para los parámetros seleccionados"), html);
    }

    @Test
    @DisplayName("Las medidas se emiten en milímetros, como se diseñaron")
    void lasMedidasSonMilimetros() {
        String html = maquetador.maquetar(diseno(texto("hola")), contexto(estudio()));
        assertTrue(html.contains("left:20.0mm"), html);
        assertTrue(html.contains("top:30.0mm"), html);
        assertTrue(html.contains("@page { size: 215.9mm 279.4mm"), html);
    }

    @Test
    @DisplayName("Un color que no tiene forma de color no entra en el HTML")
    void colorInvalidoNoEntra() {
        String elemento = """
            {"id":"t1","tipo":"texto","xMm":10,"yMm":10,"anchoMm":50,"altoMm":10,"z":1,
             "contenido":"x","tamanoPt":10,"color":"red; background:url(javascript:0)","alineacion":"left"}
            """;
        String html = maquetador.maquetar(diseno(elemento), contexto(estudio()));
        assertFalse(html.contains("javascript"), "Un valor así no puede llegar al atributo style");
        assertTrue(html.contains("color:#111111"), "Se cae al color por defecto");
    }

    @Test
    @DisplayName("Un diseño sin páginas se rechaza con un motivo legible")
    void disenoSinPaginas() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> maquetador.maquetar("{\"version\":1,\"paginas\":[]}", contexto(estudio())));
        assertTrue(e.getMessage().contains("página"), e.getMessage());
    }
}
