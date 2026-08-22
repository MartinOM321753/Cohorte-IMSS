package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.estudios.parametros.AliasParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.services.importacion.EmparejadorColumnas.Rol;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Emparejar mal una columna es el fallo mas caro de una carga masiva: el dato se
 * guarda, parece correcto y esta en el parametro equivocado. Por eso casi todas
 * estas pruebas comprueban que algo se detiene, no que algo pase.
 */
class EmparejadorColumnasTest {

    private static ParametroEstudio parametro(long id, String nombre, String... alias) {
        ParametroEstudio p = new ParametroEstudio();
        p.setId(id);
        p.setNombre(nombre);
        p.setTipo(TipoParametro.NUMERICO);
        List<AliasParametroEstudio> lista = new ArrayList<>();
        for (String a : alias) {
            AliasParametroEstudio ap = new AliasParametroEstudio();
            ap.setAlias(a);
            ap.setAliasNormalizado(imss.gob.mx.cohorte.utils.texto.NormalizadorAlias.normalizar(a));
            lista.add(ap);
        }
        p.setAlias(lista);
        return p;
    }

    // ── El caso que funciona ─────────────────────────────────────────────────

    @Test
    void emparejaFolioFechaYParametrosPorSuAlias() {
        var peso = parametro(1, "Peso corporal", "Weight");
        var grasa = parametro(2, "Porcentaje de grasa", "Body Fat %");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Weight", "Body Fat %"), List.of(peso, grasa));

        assertTrue(r.utilizable(), r.problemas().toString());
        assertEquals(0, r.indiceDe(Rol.FOLIO));
        assertEquals(1, r.indiceDe(Rol.FECHA));
        assertEquals(2, r.conRol(Rol.PARAMETRO).size());
        assertEquals("Peso corporal", r.conRol(Rol.PARAMETRO).get(0).parametro().getNombre());
    }

    @Test
    void elAliasEmparejaSinAcentosNiMayusculasNiEspaciosDeMas() {
        var p = parametro(1, "Índice de masa corporal", "Índice  de   Masa");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "INDICE DE MASA"), List.of(p));

        assertTrue(r.utilizable(), r.problemas().toString());
        assertEquals("Índice  de   Masa", r.conRol(Rol.PARAMETRO).get(0).aliasUsado());
    }

    @Test
    void variosAliasParaElMismoParametroSonValidos() {
        // Un aparato puede cambiar el titulo entre versiones de firmware.
        var p = parametro(1, "Peso", "Weight", "PESO (KG)", "Peso corporal");

        for (String titulo : List.of("Weight", "peso (kg)", "PESO CORPORAL")) {
            var r = EmparejadorColumnas.emparejar(List.of("folio", "fecha", titulo), List.of(p));
            assertTrue(r.utilizable(), titulo + " -> " + r.problemas());
        }
    }

    // ── Columnas de mas: se avisan, no detienen ──────────────────────────────

    @Test
    void unaColumnaDesconocidaSeIgnoraSinDetenerLaCarga() {
        var p = parametro(1, "Peso", "Weight");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Weight", "Serie del aparato", "Operador"), List.of(p));

        assertTrue(r.utilizable(), "una columna de mas no debe impedir la carga");
        assertEquals(2, r.conRol(Rol.IGNORADA).size());
    }

    // ── Lo que si detiene la carga ───────────────────────────────────────────

    @Test
    void unParametroSinColumnaDetieneLaCarga() {
        // Todos los parametros son obligatorios: el estudio quedaria incompleto.
        var peso = parametro(1, "Peso", "Weight");
        var talla = parametro(2, "Talla", "Height");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Weight"), List.of(peso, talla));

        assertFalse(r.utilizable());
        assertEquals(1, r.parametrosSinColumna().size());
        assertEquals("Talla", r.parametrosSinColumna().get(0).getNombre());
    }

    @Test
    void dosColumnasParaElMismoParametroSeRechazan() {
        // Elegir una en silencio guardaria una medicion y descartaria la otra
        // sin que nadie se entere.
        var p = parametro(1, "Peso", "Weight", "PESO");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Weight", "PESO"), List.of(p));

        assertFalse(r.utilizable());
        assertTrue(r.problemas().get(0).contains("mismo parametro"), r.problemas().toString());
    }

    @Test
    void unAliasCompartidoPorDosParametrosSeRechaza() {
        // Es el caso de un alias demasiado generico, como "Porcentaje".
        var grasa = parametro(1, "Grasa", "Porcentaje");
        var agua = parametro(2, "Agua", "Porcentaje");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Porcentaje"), List.of(grasa, agua));

        assertFalse(r.utilizable());
        assertTrue(r.problemas().get(0).contains("varios parametros"), r.problemas().toString());
        assertTrue(r.problemas().get(0).contains("Grasa"), r.problemas().toString());
    }

    @Test
    void faltarElFolioDetieneLaCargaYDiceComoTitularlo() {
        var p = parametro(1, "Peso", "Weight");

        var r = EmparejadorColumnas.emparejar(List.of("fecha", "Weight"), List.of(p));

        assertFalse(r.utilizable());
        assertTrue(r.problemas().stream().anyMatch(m -> m.contains("folio")), r.problemas().toString());
    }

    @Test
    void faltarLaFechaDetieneLaCarga() {
        var p = parametro(1, "Peso", "Weight");

        var r = EmparejadorColumnas.emparejar(List.of("folio", "Weight"), List.of(p));

        assertFalse(r.utilizable());
        assertTrue(r.problemas().stream().anyMatch(m -> m.contains("fecha")), r.problemas().toString());
    }

    @Test
    void dosColumnasDeFolioSeRechazan() {
        var p = parametro(1, "Peso", "Weight");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "No. Folio", "fecha", "Weight"), List.of(p));

        assertFalse(r.utilizable());
        assertTrue(r.problemas().stream().anyMatch(m -> m.contains("2 columnas de folio")),
                r.problemas().toString());
    }

    // ── El nombre del parametro no vale como alias ───────────────────────────

    @Test
    void elNombreDelParametroNoEmparejaSolo() {
        // Los nombres son clinicos y los titulos del aparato casi nunca
        // coinciden; aceptarlos produciria emparejados por casualidad.
        var p = parametro(1, "Peso corporal");   // sin alias configurados

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Peso corporal"), List.of(p));

        assertFalse(r.utilizable());
        assertEquals(1, r.parametrosSinColumna().size());
        assertEquals(1, r.conRol(Rol.IGNORADA).size());
    }

    // ── Titulos alternativos de las columnas de control ──────────────────────

    @Test
    void reconoceLosTitulosHabitualesDeFolioYFecha() {
        var p = parametro(1, "Peso", "Weight");

        for (String folio : List.of("folio", "No. Folio", "FOLIO PARTICIPANTE", "id participante")) {
            for (String fecha : List.of("fecha", "Fecha del estudio", "FECHA DE MEDICION")) {
                var r = EmparejadorColumnas.emparejar(List.of(folio, fecha, "Weight"), List.of(p));
                assertTrue(r.utilizable(), folio + " / " + fecha + " -> " + r.problemas());
            }
        }
    }
}
