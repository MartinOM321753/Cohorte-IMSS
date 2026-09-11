package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El encabezado, el pie y lo que se marca como oculto.
 *
 * <p>Las bandas son opcionales en los dos sentidos, y eso es lo que más importa
 * probar: un diseño anterior no las trae y tiene que salir exactamente igual que
 * antes, y apagar una no puede borrar lo que lleva dentro.</p>
 */
class BandasYOcultosTest {

    private final ResolvedorCampos resolvedor = new ResolvedorCampos();
    private final MaquetadorReporte maquetador = new MaquetadorReporte(
            new ObjectMapper(), resolvedor, new BloqueResultados(resolvedor),
            new BloqueEstudios(), new BloqueExamenes(resolvedor),
            new BloqueLista(resolvedor),
            new EvidenciasReporte(null, null), new ImagenesReporte(null));

    private ContextoReporte contexto() {
        Persona persona = new Persona();
        persona.setNombre("Ana");
        persona.setApellidoPaterno("López");
        Paciente paciente = new Paciente();
        paciente.setPersona(persona);
        return new ContextoReporte(paciente, List.of(), List.of(), null,
                new ContextoReporte.Totales(0, 0, 0));
    }

    private String texto(String id, String contenido) {
        return """
            {"id":"%s","tipo":"texto","xMm":10,"yMm":5,"anchoMm":100,"altoMm":10,"z":1,
             "contenido":"%s","tamanoPt":10,"color":"#111111","alineacion":"left"}
            """.formatted(id, contenido);
    }

    private String textoOculto(String id, String contenido) {
        return """
            {"id":"%s","tipo":"texto","xMm":10,"yMm":5,"anchoMm":100,"altoMm":10,"z":1,
             "oculto":true,
             "contenido":"%s","tamanoPt":10,"color":"#111111","alineacion":"left"}
            """.formatted(id, contenido);
    }

    /** Un diseño con dos páginas, para comprobar que las bandas salen en las dos. */
    private String diseno(String cuerpo, String bandas) {
        return """
            {"version":1,"tamano":"CARTA","orientacion":"vertical",
             "margenes":{"superiorMm":18,"derechoMm":15,"inferiorMm":16,"izquierdoMm":15},
             %s
             "paginas":[{"id":"p1","elementos":[%s]},{"id":"p2","elementos":[]}]}
            """.formatted(bandas.isEmpty() ? "" : bandas + ",", cuerpo);
    }

    @Test
    @DisplayName("un diseño sin bandas sale como siempre")
    void sinBandas() {
        String html = maquetador.maquetar(diseno(texto("t1", "Cuerpo"), ""), contexto());

        assertThat(html).contains("Cuerpo");
        assertThat(html).doesNotContain("Membrete");
    }

    @Test
    @DisplayName("el encabezado y el pie se dibujan en todas las páginas")
    void bandasEnTodasLasPaginas() {
        String bandas = """
            "encabezado":{"activo":true,"altoMm":25,"elementos":[%s]},
            "pie":{"activo":true,"altoMm":18,"elementos":[%s]}
            """.formatted(texto("e1", "Membrete"), texto("f1", "Pie legal"));

        String html = maquetador.maquetar(diseno(texto("t1", "Cuerpo"), bandas), contexto());

        // Dos páginas, así que cada banda tiene que aparecer dos veces.
        assertThat(contar(html, "Membrete")).isEqualTo(2);
        assertThat(contar(html, "Pie legal")).isEqualTo(2);
        assertThat(contar(html, "Cuerpo")).isEqualTo(1);
    }

    @Test
    @DisplayName("el pie se ancla abajo, no arriba")
    void elPieVaAbajo() {
        String bandas = """
            "pie":{"activo":true,"altoMm":18,"elementos":[%s]}
            """.formatted(texto("f1", "Pie legal"));

        String html = maquetador.maquetar(diseno("", bandas), contexto());

        // Carta vertical mide 279.4 mm de alto; el pie de 18 empieza en 261.4.
        assertThat(html).contains("top:261.4");
    }

    @Test
    @DisplayName("una banda apagada no se dibuja pero conserva su contenido")
    void bandaApagada() {
        String bandas = """
            "encabezado":{"activo":false,"altoMm":25,"elementos":[%s]}
            """.formatted(texto("e1", "Membrete"));

        String html = maquetador.maquetar(diseno(texto("t1", "Cuerpo"), bandas), contexto());

        assertThat(html).doesNotContain("Membrete");
        assertThat(html).contains("Cuerpo");
    }

    @Test
    @DisplayName("lo oculto tampoco se imprime")
    void ocultoNoSeImprime() {
        // Si solo desapareciera del editor, saldría en el papel algo que quien lo
        // diseñó creía haber quitado.
        String html = maquetador.maquetar(
                diseno(texto("t1", "Visible") + "," + textoOculto("t2", "Apartado"), ""),
                contexto());

        assertThat(html).contains("Visible");
        assertThat(html).doesNotContain("Apartado");
    }

    @Test
    @DisplayName("lo oculto dentro de una banda tampoco")
    void ocultoEnBanda() {
        String bandas = """
            "encabezado":{"activo":true,"altoMm":25,"elementos":[%s,%s]}
            """.formatted(texto("e1", "Membrete"), textoOculto("e2", "Borrador"));

        String html = maquetador.maquetar(diseno("", bandas), contexto());

        assertThat(html).contains("Membrete");
        assertThat(html).doesNotContain("Borrador");
    }

    private int contar(String texto, String aguja) {
        int total = 0, desde = 0, i;
        while ((i = texto.indexOf(aguja, desde)) >= 0) { total++; desde = i + aguja.length(); }
        return total;
    }
}
