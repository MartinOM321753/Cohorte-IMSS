package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Maqueta un diseño hecho en el editor y lo convierte a PDF, para poder mirarlo.
 *
 * <p>Sirve para cerrar el círculo de la verificación: comprobar que lo que alguien
 * dibujó en pantalla sale igual en papel. No se ejecuta en la batería normal —
 * necesita un archivo de diseño que solo existe cuando se está revisando algo—, así
 * que se activa a propósito:</p>
 *
 * <pre>mvnw test -Dtest=PlantillaRealAPdfTest -Ddiseno=ruta/al/diseno.json</pre>
 */
@EnabledIfSystemProperty(named = "diseno", matches = ".+")
class PlantillaRealAPdfTest {

    private final MaquetadorReporte maquetador = new MaquetadorReporte(
            new ObjectMapper(), new BloqueResultados(), new EvidenciasReporte(null, null));
    private final ReportePdfService pdfService = new ReportePdfService();

    @Test
    @DisplayName("Un diseño hecho en el editor se convierte en PDF")
    void disenoRealAPdf() throws Exception {
        String diseno = Files.readString(Path.of(System.getProperty("diseno")));

        byte[] pdf = pdfService.aPdf(maquetador.maquetar(diseno, contextoDePrueba()));
        assertTrue(pdf.length > 0);
        assertEquals('%', (char) pdf[0], "Debe ser un PDF");

        Path destino = Path.of("target", "reporte-plantilla-real.pdf");
        Files.createDirectories(destino.getParent());
        Files.write(destino, pdf);
        System.out.println("PDF escrito en " + destino.toAbsolutePath());
    }

    /** Un estudio con datos verosímiles, del tamaño de uno real. */
    private ContextoEstudio contextoDePrueba() {
        Persona persona = new Persona();
        persona.setNombre("María"); persona.setSegundoNombre("Fernanda");
        persona.setApellidoPaterno("Rodríguez"); persona.setApellidoMaterno("Núñez");
        persona.setFechaNacimiento(LocalDate.of(1976, 4, 12));
        persona.setSexo(Persona.Sexo.F);

        Paciente paciente = new Paciente();
        paciente.setFolio("HWCS-000418");
        paciente.setPersona(persona);

        Institucion institucion = new Institucion();
        institucion.setNombre("IMSS Cuernavaca — Sede Central");

        TipoEstudio tipo = new TipoEstudio();
        tipo.setNombre("Prueba de caminata de 6 minutos PC6M");

        EstudioMedico e = new EstudioMedico();
        e.setId(7L);
        e.setPaciente(paciente);
        e.setInstitucion(institucion);
        e.setTipoEstudio(tipo);
        e.setFechaEstudio(LocalDateTime.of(2026, 7, 16, 9, 40));
        e.setObservaciones("Prueba completada sin incidencias.");
        e.setResultadoEstudio(new ArrayList<>());

        String[] nombres = {"Distancia recorrida", "Frecuencia cardiaca inicial",
                            "Frecuencia cardiaca final", "Saturación inicial", "Saturación final",
                            "Presión sistólica", "Presión diastólica", "Escala de Borg",
                            "Vueltas completadas", "Tiempo de recuperación"};
        String[] unidades = {"m", "lpm", "lpm", "%", "%", "mmHg", "mmHg", "pts", "n", "s"};
        double[] valores  = {420, 72, 118, 98, 94, 128, 82, 3, 14, 95};
        double[] minimos  = {350, 60, 60, 95, 95, 90, 60, 0, 10, 0};
        double[] maximos  = {700, 100, 140, 100, 100, 130, 85, 10, 30, 120};

        for (int i = 0; i < nombres.length; i++) {
            ParametroEstudio p = new ParametroEstudio();
            p.setId(100L + i);
            p.setNombre(nombres[i]);
            p.setUnidad(unidades[i]);
            p.setTipo(TipoParametro.NUMERICO);
            p.setValorMinMujeres(minimos[i]); p.setValorMaxMujeres(maximos[i]);
            p.setValorMinHombres(minimos[i]); p.setValorMaxHombres(maximos[i]);

            ResultadoEstudio r = new ResultadoEstudio();
            r.setParametro(p);
            r.setValorNumerico(valores[i]);
            r.setGrupoCodigo("ROOT");
            r.setOrdenResultado(i);
            e.getResultadoEstudio().add(r);
        }

        return new ContextoEstudio(e, new ContextoEstudio.Totales(12, 40, 3));
    }
}
