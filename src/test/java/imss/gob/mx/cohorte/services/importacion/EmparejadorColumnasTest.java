package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.services.importacion.EmparejadorColumnas.Destino;
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

    private static Destino parametro(long id, String nombre, String... alias) {
        List<String> normalizados = new ArrayList<>();
        for (String a : alias) {
            normalizados.add(imss.gob.mx.cohorte.utils.texto.NormalizadorAlias.normalizar(a));
        }
        return new Destino(id, nombre, normalizados, List.of(alias));
    }

    /** En estudios faltar una columna detiene la carga; el emparejador solo la reporta. */
    private static boolean utilizableComoEstudio(EmparejadorColumnas.Emparejado r) {
        return r.sinConflictos() && r.destinosSinColumna().isEmpty();
    }

    // ── El caso que funciona ─────────────────────────────────────────────────

    @Test
    void emparejaFolioFechaYParametrosPorSuAlias() {
        var peso = parametro(1, "Peso corporal", "Weight");
        var grasa = parametro(2, "Porcentaje de grasa", "Body Fat %");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Weight", "Body Fat %"), List.of(peso, grasa));

        assertTrue(utilizableComoEstudio(r), r.problemas().toString());
        assertEquals(0, r.indiceDe(Rol.FOLIO));
        assertEquals(1, r.indiceDe(Rol.FECHA));
        assertEquals(2, r.conRol(Rol.PARAMETRO).size());
        assertEquals("Peso corporal", r.conRol(Rol.PARAMETRO).get(0).destino().nombre());
    }

    @Test
    void elAliasEmparejaSinAcentosNiMayusculasNiEspaciosDeMas() {
        var p = parametro(1, "Índice de masa corporal", "Índice  de   Masa");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "INDICE DE MASA"), List.of(p));

        assertTrue(utilizableComoEstudio(r), r.problemas().toString());
        assertEquals("Índice  de   Masa", r.conRol(Rol.PARAMETRO).get(0).aliasUsado());
    }

    @Test
    void variosAliasParaElMismoParametroSonValidos() {
        // Un aparato puede cambiar el titulo entre versiones de firmware.
        var p = parametro(1, "Peso", "Weight", "PESO (KG)", "Peso corporal");

        for (String titulo : List.of("Weight", "peso (kg)", "PESO CORPORAL")) {
            var r = EmparejadorColumnas.emparejar(List.of("folio", "fecha", titulo), List.of(p));
            assertTrue(utilizableComoEstudio(r), titulo + " -> " + r.problemas());
        }
    }

    // ── Columnas de mas: se avisan, no detienen ──────────────────────────────

    @Test
    void unaColumnaDesconocidaSeIgnoraSinDetenerLaCarga() {
        var p = parametro(1, "Peso", "Weight");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Weight", "Serie del aparato", "Operador"), List.of(p));

        assertTrue(utilizableComoEstudio(r), "una columna de mas no debe impedir la carga");
        assertEquals(2, r.conRol(Rol.IGNORADA).size());
    }

    // ── Lo que si detiene la carga ───────────────────────────────────────────

    @Test
    void faltarDestinosNoEsUnConflicto() {
        // La diferencia entre estudios y examenes: en un estudio faltar un
        // parametro detiene la carga, pero un archivo de laboratorio que solo
        // trae glucosa es perfectamente valido. El emparejador se limita a
        // reportarlo y deja la decision a quien llama.
        var glucosa = parametro(1, "Glucosa", "GLU");
        var colesterol = parametro(2, "Colesterol", "COL");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "GLU"), List.of(glucosa, colesterol));

        assertTrue(r.sinConflictos(), "faltar un destino no es un conflicto");
        assertEquals(1, r.destinosSinColumna().size());
        assertFalse(utilizableComoEstudio(r), "como estudio si detendria la carga");
    }

    @Test
    void unParametroSinColumnaDetieneLaCarga() {
        // Todos los parametros son obligatorios: el estudio quedaria incompleto.
        var peso = parametro(1, "Peso", "Weight");
        var talla = parametro(2, "Talla", "Height");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Weight"), List.of(peso, talla));

        assertFalse(utilizableComoEstudio(r));
        assertEquals(1, r.destinosSinColumna().size());
        assertEquals("Talla", r.destinosSinColumna().get(0).nombre());
    }

    @Test
    void dosColumnasParaElMismoParametroSeRechazan() {
        // Elegir una en silencio guardaria una medicion y descartaria la otra
        // sin que nadie se entere.
        var p = parametro(1, "Peso", "Weight", "PESO");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Weight", "PESO"), List.of(p));

        assertFalse(utilizableComoEstudio(r));
        assertTrue(r.problemas().get(0).contains("apuntan a lo mismo"), r.problemas().toString());
    }

    @Test
    void unAliasCompartidoPorDosParametrosSeRechaza() {
        // Es el caso de un alias demasiado generico, como "Porcentaje".
        var grasa = parametro(1, "Grasa", "Porcentaje");
        var agua = parametro(2, "Agua", "Porcentaje");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Porcentaje"), List.of(grasa, agua));

        assertFalse(utilizableComoEstudio(r));
        assertTrue(r.problemas().get(0).contains("varios destinos"), r.problemas().toString());
        assertTrue(r.problemas().get(0).contains("Grasa"), r.problemas().toString());
    }

    @Test
    void faltarElFolioDetieneLaCargaYDiceComoTitularlo() {
        var p = parametro(1, "Peso", "Weight");

        var r = EmparejadorColumnas.emparejar(List.of("fecha", "Weight"), List.of(p));

        assertFalse(utilizableComoEstudio(r));
        assertTrue(r.problemas().stream().anyMatch(m -> m.contains("folio")), r.problemas().toString());
    }

    @Test
    void faltarLaFechaDetieneLaCarga() {
        var p = parametro(1, "Peso", "Weight");

        var r = EmparejadorColumnas.emparejar(List.of("folio", "Weight"), List.of(p));

        assertFalse(utilizableComoEstudio(r));
        assertTrue(r.problemas().stream().anyMatch(m -> m.contains("fecha")), r.problemas().toString());
    }

    @Test
    void dosColumnasDeFolioSeRechazan() {
        var p = parametro(1, "Peso", "Weight");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "No. Folio", "fecha", "Weight"), List.of(p));

        assertFalse(utilizableComoEstudio(r));
        assertTrue(r.problemas().stream().anyMatch(m -> m.contains("2 columnas de folio")),
                r.problemas().toString());
    }

    // ── El nombre como alias por omision ─────────────────────────────────────

    @Test
    void sinAliasConfigurados_elNombreEmpareja() {
        // Antes no emparejaba, y eso dejaba la carga masiva inservible hasta que
        // alguien diera de alta los alias uno por uno. El titulo que casi siempre
        // trae el archivo es justo el nombre del parametro.
        var p = parametro(1, "Peso corporal");   // sin alias configurados

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Peso corporal"), List.of(p));

        assertTrue(utilizableComoEstudio(r), r.problemas().toString());
        assertTrue(r.destinosSinColumna().isEmpty());
        assertEquals("Peso corporal", r.conRol(Rol.PARAMETRO).get(0).aliasUsado());
    }

    @Test
    void elNombrePorOmisionSeComparaNormalizado() {
        // Misma indulgencia que con los alias: acentos, mayusculas y espacios de mas
        // no deberian decidir si una carga funciona.
        var p = parametro(1, "Índice de Masa Corporal");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "indice  de masa corporal"), List.of(p));

        assertTrue(utilizableComoEstudio(r), r.problemas().toString());
    }

    @Test
    void conAliasConfigurados_elNombreYaNoCuenta() {
        // Configurar un alias significa que el aparato titula de otra forma. Aceptar
        // ademas el nombre reabriria las coincidencias por casualidad.
        var p = parametro(1, "Peso corporal", "Weight");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Peso corporal"), List.of(p));

        assertFalse(utilizableComoEstudio(r));
        assertEquals(1, r.destinosSinColumna().size());
        assertEquals(1, r.conRol(Rol.IGNORADA).size());
    }

    @Test
    void siElNombreDeUnoChocaConElAliasDeOtro_seDetiene() {
        // El nombre por omision entra al mismo juego que los alias, incluida la
        // deteccion de ambiguedad: elegir uno en silencio guardaria una medicion en
        // el sitio de otra.
        var sinAlias = parametro(1, "Masa grasa");
        var conAlias = parametro(2, "Grasa corporal", "Masa grasa");

        var r = EmparejadorColumnas.emparejar(
                List.of("folio", "fecha", "Masa grasa"), List.of(sinAlias, conAlias));

        assertFalse(r.sinConflictos());
        assertTrue(r.problemas().get(0).contains("coincide con varios destinos"),
                r.problemas().toString());
    }

    // ── Titulos alternativos de las columnas de control ──────────────────────

    @Test
    void reconoceLosTitulosHabitualesDeFolioYFecha() {
        var p = parametro(1, "Peso", "Weight");

        for (String folio : List.of("folio", "No. Folio", "FOLIO PARTICIPANTE", "id participante")) {
            for (String fecha : List.of("fecha", "Fecha del estudio", "FECHA DE MEDICION")) {
                var r = EmparejadorColumnas.emparejar(List.of(folio, fecha, "Weight"), List.of(p));
                assertTrue(utilizableComoEstudio(r), folio + " / " + fecha + " -> " + r.problemas());
            }
        }
    }
}
