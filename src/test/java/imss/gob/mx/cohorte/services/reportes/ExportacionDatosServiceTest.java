package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * La descarga: mismos datos que el reporte, en un archivo.
 *
 * <p>Lo que se prueba aquí, más que el formato, es que el archivo <b>cuadre</b>. Un solo
 * campo con una coma dentro desplaza todas las columnas de esa fila, y un CSV
 * descuadrado no avisa: se abre igual y los datos quedan corridos.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExportacionDatosServiceTest {

    @Mock private CatalogoCamposReporte catalogo;

    private ExportacionDatosService exportacion;

    private final Map<String, ContextoReporte> participantes = Map.of(
            "uuid-1", contexto("Ana", "F-001"),
            "uuid-2", contexto("Luis", "F-002"));

    @BeforeEach
    void preparar() {
        exportacion = new ExportacionDatosService(new ResolvedorCampos(), catalogo);
        when(catalogo.todos()).thenReturn(List.of(
                new CatalogoCamposReporte.Campo(ResolvedorCampos.PARTICIPANTE_FOLIO, "Folio",
                        "Participante", null, CatalogoCamposReporte.Clase.CAMPO, null, null, false, Map.of()),
                new CatalogoCamposReporte.Campo(ResolvedorCampos.PARTICIPANTE_NOMBRE, "Nombre completo",
                        "Participante", null, CatalogoCamposReporte.Clase.CAMPO, null, null, false, Map.of())));
    }

    private ContextoReporte contexto(String nombre, String folio) {
        Persona persona = new Persona();
        persona.setNombre(nombre);
        Paciente paciente = new Paciente();
        paciente.setPersona(persona);
        paciente.setFolio(folio);
        return new ContextoReporte(paciente, List.of(), List.of(), null,
                ContextoReporte.Totales.sinCalcular());
    }

    private Function<String, ContextoReporte> datos() {
        return participantes::get;
    }

    private String csv(List<String> claves, String separador, List<String> uuids) {
        return new String(exportacion.aCsv(claves, separador, uuids, datos()), StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("la primera fila lleva los rótulos del catálogo, no las claves")
    void losTitulosSonLegibles() {
        String salida = csv(
                List.of(ResolvedorCampos.PARTICIPANTE_FOLIO, ResolvedorCampos.PARTICIPANTE_NOMBRE),
                ",", List.of("uuid-1"));

        assertThat(salida.lines().toList().get(0))
                .as("quien abre el archivo no tiene por qué saber qué es «participante.folio»")
                .endsWith("Folio,Nombre completo");
    }

    @Test
    @DisplayName("una fila por participante, en el orden pedido")
    void unaFilaPorParticipante() {
        String salida = csv(
                List.of(ResolvedorCampos.PARTICIPANTE_FOLIO, ResolvedorCampos.PARTICIPANTE_NOMBRE),
                ",", List.of("uuid-2", "uuid-1"));

        List<String> lineas = salida.lines().toList();
        assertThat(lineas).hasSize(3);
        assertThat(lineas.get(1)).isEqualTo("F-002,Luis");
        assertThat(lineas.get(2)).isEqualTo("F-001,Ana");
    }

    @Test
    @DisplayName("el archivo lleva la marca que hace que Excel respete los acentos")
    void llevaMarcaDeOrdenDeBytes() {
        byte[] bytes = exportacion.aCsv(
                List.of(ResolvedorCampos.PARTICIPANTE_FOLIO), ",", List.of("uuid-1"), datos());

        // Sin ella, «Institución» se abre como «InstituciÃ³n».
        assertThat(bytes[0] & 0xFF).isEqualTo(0xEF);
        assertThat(bytes[1] & 0xFF).isEqualTo(0xBB);
        assertThat(bytes[2] & 0xFF).isEqualTo(0xBF);
    }

    @Test
    @DisplayName("un valor con el separador dentro se entrecomilla y la fila sigue cuadrando")
    void loQueLlevaSeparadorSeEntrecomilla() {
        Persona persona = new Persona();
        persona.setNombre("Ana");
        persona.setApellidoPaterno("López, de la Cruz");
        Paciente paciente = new Paciente();
        paciente.setPersona(persona);
        paciente.setFolio("F-003");

        ContextoReporte conComa = new ContextoReporte(paciente, List.of(), List.of(), null,
                ContextoReporte.Totales.sinCalcular());

        String salida = new String(exportacion.aCsv(
                List.of(ResolvedorCampos.PARTICIPANTE_FOLIO, ResolvedorCampos.PARTICIPANTE_NOMBRE),
                ",", List.of("x"), uuid -> conComa), StandardCharsets.UTF_8);

        assertThat(salida.lines().toList().get(1))
                .isEqualTo("F-003,\"Ana López, de la Cruz\"");
    }

    @Test
    @DisplayName("con punto y coma la coma del nombre ya no estorba")
    void elSeparadorSeElige() {
        String salida = csv(
                List.of(ResolvedorCampos.PARTICIPANTE_FOLIO, ResolvedorCampos.PARTICIPANTE_NOMBRE),
                ";", List.of("uuid-1"));

        assertThat(salida.lines().toList().get(1)).isEqualTo("F-001;Ana");
    }

    @Test
    @DisplayName("solo se admiten la coma y el punto y coma")
    void separadorInvalido() {
        assertThatThrownBy(() -> csv(List.of(ResolvedorCampos.PARTICIPANTE_FOLIO), "|", List.of("uuid-1")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("coma");
    }

    @Test
    @DisplayName("una descarga sin columnas o sin participantes no tiene sentido")
    void seleccionVacia() {
        assertThatThrownBy(() -> csv(List.of(), ",", List.of("uuid-1")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("columna");

        assertThatThrownBy(() -> csv(List.of(ResolvedorCampos.PARTICIPANTE_FOLIO), ",", List.of()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("participante");
    }

    @Test
    @DisplayName("una cohorte entera de golpe se rechaza con una explicación")
    void elTopeSeAvisa() {
        List<String> demasiados = IntStream.range(0, ExportacionDatosService.MAXIMO_PARTICIPANTES + 1)
                .mapToObj(i -> "uuid-" + i).toList();

        // Es preferible a que la petición se quede colgada, que es lo que se ve desde el
        // navegador cuando no hay límite.
        assertThatThrownBy(() -> csv(List.of(ResolvedorCampos.PARTICIPANTE_FOLIO), ",", demasiados))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("hasta " + ExportacionDatosService.MAXIMO_PARTICIPANTES);
    }

    @Test
    @DisplayName("un dato que el participante no tiene deja la celda vacía")
    void loQueFaltaQuedaEnBlanco() {
        String salida = csv(
                List.of(ResolvedorCampos.PARTICIPANTE_FOLIO, "estudio.99.param.999"),
                ",", List.of("uuid-1"));

        assertThat(salida.lines().toList().get(1)).isEqualTo("F-001,");
    }
}
