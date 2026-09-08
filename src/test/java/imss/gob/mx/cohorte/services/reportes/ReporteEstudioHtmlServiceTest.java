package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El reporte de un estudio: qué sale impreso y qué no.
 *
 * <p>Además de comprobar el contenido, el último caso deja un PDF en {@code target/}
 * para poder mirarlo. Es la única forma de ver si un documento está bien maquetado —eso
 * no lo dice ninguna aserción—.</p>
 */
class ReporteEstudioHtmlServiceTest {

    private final ReporteEstudioHtmlService html = new ReporteEstudioHtmlService();
    private final ReportePdfService pdf = new ReportePdfService();

    // ── Armado del escenario ─────────────────────────────────────────────────

    private ParametroEstudio parametro(long id, String nombre, String unidad,
                                       Double minM, Double maxM, Double minH, Double maxH) {
        ParametroEstudio p = new ParametroEstudio();
        p.setId(id);
        p.setNombre(nombre);
        p.setUnidad(unidad);
        p.setTipo(TipoParametro.NUMERICO);
        p.setValorMinMujeres(minM); p.setValorMaxMujeres(maxM);
        p.setValorMinHombres(minH); p.setValorMaxHombres(maxH);
        return p;
    }

    private ResultadoEstudio resultado(ParametroEstudio p, Double valor, String grupo, String etiqueta, int orden) {
        ResultadoEstudio r = new ResultadoEstudio();
        r.setParametro(p);
        r.setValorNumerico(valor);
        r.setGrupoCodigo(grupo);
        r.setGrupoEtiqueta(etiqueta);
        r.setOrdenResultado(orden);
        return r;
    }

    private EstudioMedico estudioBase(Persona.Sexo sexo) {
        Persona persona = new Persona();
        persona.setNombre("María"); persona.setSegundoNombre("Fernanda");
        persona.setApellidoPaterno("Rodríguez"); persona.setApellidoMaterno("Núñez");
        persona.setFechaNacimiento(LocalDate.of(1976, 4, 12));
        persona.setSexo(sexo);

        Paciente paciente = new Paciente();
        paciente.setFolio("HWCS-000418");
        paciente.setPersona(persona);

        Institucion institucion = new Institucion();
        institucion.setNombre("IMSS Cuernavaca — Sede Central");

        TipoEstudio tipo = new TipoEstudio();
        tipo.setNombre("Electrocardiograma EKG");

        EstudioMedico e = new EstudioMedico();
        e.setId(1L);
        e.setPaciente(paciente);
        e.setInstitucion(institucion);
        e.setTipoEstudio(tipo);
        e.setFechaEstudio(LocalDateTime.of(2026, 7, 16, 9, 40));
        e.setResultadoEstudio(new ArrayList<>());
        return e;
    }

    // ── Casos ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sale el participante, su folio y el estudio")
    void saleLaFicha() {
        EstudioMedico e = estudioBase(Persona.Sexo.F);
        String salida = html.generar(e);

        assertTrue(salida.contains("María Fernanda Rodríguez Núñez"), "Falta el nombre completo");
        assertTrue(salida.contains("HWCS-000418"), "Falta el folio");
        assertTrue(salida.contains("Electrocardiograma EKG"), "Falta el tipo de estudio");
        assertTrue(salida.contains("16/07/2026 09:40"), "Falta la fecha del estudio");
        assertTrue(salida.contains("Mujer"), "Falta el sexo legible");
    }

    /**
     * El rango que aplica depende del sexo. Con el mismo valor, un participante puede
     * estar dentro y otro fuera; marcarlo por el rango equivocado en un documento
     * clínico es peor que no marcarlo.
     */
    @Test
    @DisplayName("El rango de referencia es el del sexo del participante")
    void elRangoDependeDelSexo() {
        ParametroEstudio hb = parametro(1L, "Hemoglobina", "g/dL", 12.0, 16.0, 13.5, 17.5);

        EstudioMedico mujer = estudioBase(Persona.Sexo.F);
        mujer.getResultadoEstudio().add(resultado(hb, 13.0, "ROOT", null, 0));
        String salidaMujer = html.generar(mujer);

        EstudioMedico hombre = estudioBase(Persona.Sexo.M);
        hombre.getResultadoEstudio().add(resultado(hb, 13.0, "ROOT", null, 0));
        String salidaHombre = html.generar(hombre);

        assertTrue(salidaMujer.contains("12 – 16"), "A una mujer le toca el rango femenino");
        assertFalse(salidaMujer.contains("class=\"valor fuera\""), "13 está dentro de 12–16");

        assertTrue(salidaHombre.contains("13.5 – 17.5"), "A un hombre le toca el masculino");
        assertTrue(salidaHombre.contains("class=\"valor fuera\""), "13 está por debajo de 13.5");
    }

    @Test
    @DisplayName("Sin sexo registrado no se marca nada como fuera de rango")
    void sinSexoNoSeMarca() {
        ParametroEstudio hb = parametro(1L, "Hemoglobina", "g/dL", 12.0, 16.0, 13.5, 17.5);
        EstudioMedico e = estudioBase(null);
        e.getResultadoEstudio().add(resultado(hb, 2.0, "ROOT", null, 0));

        String salida = html.generar(e);
        // Se busca la clase aplicada a la celda, no la palabra suelta: «fuera» aparece
        // tambien en el CSS y en la nota al pie, y daria un falso positivo.
        assertFalse(salida.contains("class=\"valor fuera\""),
                "Un «fuera de rango» inventado se lee como un hallazgo clínico");
    }

    /**
     * Un parámetro retirado del catálogo conserva sus resultados. El reporte recorre
     * lo capturado, no el catálogo vigente, así que debe seguir apareciendo.
     */
    @Test
    @DisplayName("Un parámetro retirado sigue apareciendo si el estudio lo midió")
    void elRetiradoSigueSaliendo() {
        ParametroEstudio retirado = parametro(229L, "P", "ms", null, null, null, null);
        retirado.setActivo(false);

        EstudioMedico e = estudioBase(Persona.Sexo.F);
        e.getResultadoEstudio().add(resultado(retirado, 88.0, "ROOT", null, 0));

        assertTrue(html.generar(e).contains(">P<"),
                "Se capturó con ese parámetro; el documento tiene que reflejarlo");
    }

    @Test
    @DisplayName("Los estudios por grupos salen separados y con su etiqueta")
    void losGruposSalenSeparados() {
        ParametroEstudio fc = parametro(1L, "Frecuencia cardiaca", "lpm", 60.0, 100.0, 60.0, 100.0);

        EstudioMedico e = estudioBase(Persona.Sexo.F);
        e.getResultadoEstudio().add(resultado(fc, 72.0, "G1", "Antes del esfuerzo", 1));
        e.getResultadoEstudio().add(resultado(fc, 118.0, "G2", "Después del esfuerzo", 2));

        String salida = html.generar(e);
        assertTrue(salida.contains("Antes del esfuerzo"));
        assertTrue(salida.contains("Después del esfuerzo"));
        assertTrue(salida.contains("class=\"valor fuera\""), "118 se sale de 60–100");
    }

    @Test
    @DisplayName("Un estudio sin resultados lo dice, no sale una tabla vacía")
    void sinResultadosLoDice() {
        assertTrue(html.generar(estudioBase(Persona.Sexo.F)).contains("no tiene resultados"));
    }

    /**
     * Los nombres y las observaciones los escribe una persona. Un «&» suelto rompería
     * el documento sin que nadie entendiera por qué.
     */
    @Test
    @DisplayName("El texto que escribe el usuario se escapa")
    void seEscapaElTextoDelUsuario() {
        EstudioMedico e = estudioBase(Persona.Sexo.F);
        e.setObservaciones("Control <urgente> & seguimiento");

        String salida = html.generar(e);
        assertTrue(salida.contains("&lt;urgente&gt; &amp; seguimiento"));
        assertFalse(salida.contains("<urgente>"), "Se coló etiqueta cruda en el HTML");
    }

    /**
     * Reporte completo a PDF, con un estudio largo para que parta página. Deja el
     * archivo en target/ para mirarlo: la maquetación no la juzga una aserción.
     */
    @Test
    @DisplayName("El reporte completo se convierte en un PDF de varias páginas")
    void reporteCompletoAPdf() throws Exception {
        EstudioMedico e = estudioBase(Persona.Sexo.F);
        e.setObservaciones("Trazo con ritmo sinusal. Se sugiere control en seis meses.");

        String[] nombres = {"Frecuencia cardiaca", "Intervalo PR", "Duración QRS", "Intervalo QTc",
                            "Eje P", "Eje QRS", "Eje T", "Onda P", "Segmento ST", "Onda T"};
        String[] unidades = {"lpm", "ms", "ms", "ms", "°", "°", "°", "ms", "mm", "mm"};
        for (int i = 0; i < 34; i++) {
            ParametroEstudio p = parametro(100L + i,
                    nombres[i % nombres.length] + (i >= nombres.length ? " " + (i / nombres.length + 1) : ""),
                    unidades[i % unidades.length], 60.0, 100.0, 60.0, 100.0);
            e.getResultadoEstudio().add(resultado(p, 55.0 + i * 2.0, "ROOT", null, i));
        }

        byte[] bytes = pdf.aPdf(html.generar(e));
        assertTrue(bytes.length > 0);

        Path destino = Path.of("target", "reporte-estudio-muestra.pdf");
        Files.createDirectories(destino.getParent());
        Files.write(destino, bytes);
        System.out.println("Reporte de muestra escrito en " + destino.toAbsolutePath());
    }
}
