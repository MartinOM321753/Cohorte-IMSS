package imss.gob.mx.cohorte.utils.texto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * De este normalizador depende que una columna del archivo se resuelva al
 * parametro correcto. Un fallo aqui no da error: guarda el resultado en el sitio
 * equivocado y nadie se entera.
 */
class NormalizadorAliasTest {

    @Test
    void mismaColumnaEscritaDeVariasFormas() {
        String esperado = "SISTOLICA";
        assertEquals(esperado, NormalizadorAlias.normalizar("Sistolica"));
        assertEquals(esperado, NormalizadorAlias.normalizar("Sistolica"));
        assertEquals(esperado, NormalizadorAlias.normalizar("SISTOLICA"));
        assertEquals(esperado, NormalizadorAlias.normalizar("  sistolica  "));
    }

    @Test
    void quitaAcentos() {
        assertEquals("SISTOLICA", NormalizadorAlias.normalizar("Sistólica"));
        assertEquals("PRESION ARTERIAL", NormalizadorAlias.normalizar("Presión Arterial"));
        assertEquals("NUMERO", NormalizadorAlias.normalizar("Número"));
    }

    @Test
    void colapsaEspaciosInternosYElEspacioDuro() {
        assertEquals("PRESION ARTERIAL", NormalizadorAlias.normalizar("Presion    Arterial"));
        // El espacio duro llega pegado en los CSV que pasaron por una hoja de calculo.
        assertEquals("PRESION ARTERIAL", NormalizadorAlias.normalizar("Presion Arterial"));
        assertEquals("PRESION ARTERIAL", NormalizadorAlias.normalizar("Presion	Arterial"));
    }

    @Test
    void vacioYNuloNoSonAlias() {
        assertNull(NormalizadorAlias.normalizar(null));
        assertNull(NormalizadorAlias.normalizar(""));
        assertNull(NormalizadorAlias.normalizar("   "));
    }

    @Test
    void columnasDistintasNoSeConfunden() {
        assertNotEquals(NormalizadorAlias.normalizar("SYS"), NormalizadorAlias.normalizar("DIA"));
        assertNotEquals(NormalizadorAlias.normalizar("Glucosa"), NormalizadorAlias.normalizar("Glucosa 2"));
    }

    @Test
    void coincidenComparaPorLaFormaCanonica() {
        assertTrue(NormalizadorAlias.coinciden("Sistólica", "  SISTOLICA "));
        assertFalse(NormalizadorAlias.coinciden("Sistolica", "Diastolica"));
        assertFalse(NormalizadorAlias.coinciden(null, null), "dos ausencias no son una coincidencia");
    }
}
