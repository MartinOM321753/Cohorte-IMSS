package imss.gob.mx.cohorte.services.cuestionarios;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Que las rejillas se reconozcan, y que nada más se reconozca como rejilla.
 *
 * <p>Las dos mitades importan lo mismo. Si no detecta la 112, el participante recibe
 * ciento treinta y siete campos en fila india y deja de reconocer su propia tabla. Si
 * detecta una rejilla donde no la hay, reacomoda preguntas que no tienen relación y el
 * capturista deja de encontrarlas donde las buscaba — que es peor, porque nadie lo
 * reporta como error.</p>
 *
 * <p>Los casos están copiados del catálogo real, incluidos los que NO son rejilla.</p>
 */
class DetectorRejillaTest {

    /** La 112 tal como viene: 15 actividades x (7 dias + tiempo + forma), mas dos sueltas. */
    private static Map<String, String> bloque112() {
        String[] actividades = {"Caminar", "Correr", "Andar en bicicleta", "Softbol/beisbol",
                "Futbol soccer", "Voleibol", "Aerobics", "Bailar", "Boliche", "Nadar",
                "Tenis", "Fronton", "Basquetbol", "Squash", "Otro:"};
        String[] columnas = {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado",
                "Domingo", "Tiempo", "forma"};
        Map<String, String> m = new LinkedHashMap<>();
        m.put("P112_1", "Ninguna");
        int n = 2;
        for (String a : actividades) {
            for (String c : columnas) m.put("P112_" + n++, a + " " + c);
        }
        m.put("P112_137", "Otro especifique texto");
        return m;
    }

    @Test
    @DisplayName("la 112 se reconoce: 15 filas por 9 columnas")
    void reconoceLa112() {
        var reparto = DetectorRejilla.repartir(bloque112());

        assertEquals(135, reparto.size(), "las 135 celdas de la rejilla");
        assertEquals("Caminar", reparto.get("P112_2").fila());
        assertEquals("Lunes", reparto.get("P112_2").columna());
        assertEquals("Caminar", reparto.get("P112_9").fila());
        assertEquals("Tiempo", reparto.get("P112_9").columna());
        assertEquals("Andar en bicicleta", reparto.get("P112_20").fila(),
                "la fila puede tener varias palabras: solo la ultima es la columna");
    }

    /** La 57 tal como viene: 6 productos x 9 lugares, rotulada con guion. */
    private static Map<String, String> bloque57() {
        String[] productos = {"Cigarros", "Cigarros con filtros saborizados",
                "Cigarros con capsulas de sabor", "Cigarros electronicos",
                "Dispositivos de tabaco calentado", "Otro producto del tabaco"};
        String[] lugares = {"Yo no compre este producto", "Gasolinera",
                "Tienda de abarrotes", "Tiendas tipo \"Oxxo\"", "En internet",
                "Almacenes comerciales", "Tienda especializada en tabaco",
                "Con un conocido", "Otro lugar (Especificar)"};
        Map<String, String> m = new LinkedHashMap<>();
        int n = 1;
        for (String l : lugares) {
            for (String pr : productos) m.put("P57_" + n++, pr + "-" + l);
        }
        // Las dos de texto del final, que no son celdas.
        m.put("P57_55", "Otro producto de tabaco");
        m.put("P57_56", "Otro lugar");
        return m;
    }

    @Test
    @DisplayName("la 57 se reconoce aunque la columna no sea la ultima palabra")
    void reconoceLa57() {
        var reparto = DetectorRejilla.repartir(bloque57());

        assertEquals(54, reparto.size(), "6 productos por 9 lugares");
        assertEquals("Cigarros", reparto.get("P57_1").fila());
        assertEquals("Yo no compre este producto", reparto.get("P57_1").columna(),
                "la columna es todo lo que sigue al guion, no la ultima palabra");
        assertEquals("Otro producto del tabaco", reparto.get("P57_54").fila());
        assertEquals("Otro lugar (Especificar)", reparto.get("P57_54").columna());

        // Partida por el ultimo espacio, «Cigarros-Yo no compre este producto» daria
        // la fila «Cigarros-Yo no compre este» y la columna «producto»: seis filas
        // distintas con una sola columna cada una, que no es una rejilla. Por eso se
        // prueban las dos formas en vez de elegir una.
        assertFalse(reparto.containsKey("P57_55"), "las dos de texto no son celdas");
        assertFalse(reparto.containsKey("P57_56"));
    }

    /**
     * La 199 y la 203, que usan guion LARGO: «Coche – Usted posee».
     *
     * <p>Son las dos tablas de la pagina 30. Con solo el guion corto se pintaban como
     * una lista de 28 y 45 campos sueltos, que es justo lo que la rejilla evita.</p>
     */
    @Test
    @DisplayName("tambien se reconocen las tablas rotuladas con guion largo")
    void reconoceElGuionLargo() {
        String[] bienes = {"Coche", "Motocicleta", "Casa / departamento", "Terreno(s)",
                "Locales comerciales", "Cuenta bancaria", "Otro (especifique)"};
        String[] deQuien = {"Usted posee", "Su pareja posee", "Poseen juntos",
                "Son rentados o de la empresa"};
        Map<String, String> m = new LinkedHashMap<>();
        int n = 1;
        for (String d : deQuien) {
            for (String b : bienes) m.put("P199_" + n++, b + " – " + d);
        }

        var reparto = DetectorRejilla.repartir(m);

        assertEquals(28, reparto.size(), "7 bienes por 4 formas de posesion");
        assertEquals("Coche", reparto.get("P199_1").fila());
        assertEquals("Usted posee", reparto.get("P199_1").columna());
        assertEquals("Casa / departamento", reparto.get("P199_3").fila(),
                "la diagonal del renglon no es un guion: la fila se queda entera");
    }

    @Test
    @DisplayName("se parte por el primer guion: lo de despues es el nombre de la columna")
    void parteporElPrimerGuion() {
        // El enunciado real de la 203 trae signos de interrogacion y comas despues del
        // guion; si se partiera por el ultimo, la columna seria un trozo de frase.
        Map<String, String> m = new LinkedHashMap<>();
        String[] eventos = {"Se enfermo gravemente", "Se lesiono gravemente",
                "Perdio la vista o la audicion"};
        String[] columnas = {"¿Suspendio las actividades que realizaba?",
                "¿Por que periodo las suspendio?",
                "Desde entonces, ¿que tan a menudo piensa en lo que ocurrio?"};
        int n = 1;
        for (String c : columnas) {
            for (String e : eventos) m.put("P203_" + n++, e + " – " + c);
        }

        var reparto = DetectorRejilla.repartir(m);

        assertEquals(9, reparto.size());
        assertEquals("Desde entonces, ¿que tan a menudo piensa en lo que ocurrio?",
                reparto.get("P203_9").columna());
    }

    /**
     * Las siete situaciones de la 183, ya sin sus tres tablas.
     *
     * <p>Partidas por el ultimo espacio arman por casualidad una rejilla de 4 x 2
     * —«agredido»/«intentado agredir» por «fisicamente»/«sexualmente»— llena del todo.
     * Es el caso que obligo a mirar tambien que la rejilla sea el bloque y no una
     * coincidencia de las ultimas palabras de ocho de catorce enunciados.</p>
     */
    @Test
    @DisplayName("ocho enunciados que riman no son una tabla")
    void laRimaNoEsRejilla() {
        String[] situaciones = {"le han ofendido verbalmente", "le han agredido fisicamente",
                "le han agredido sexualmente", "le han amenazado con palabras",
                "le han hecho sentir inferior", "le han intentado agredir fisicamente",
                "le han intentado agredir sexualmente"};
        Map<String, String> m = new LinkedHashMap<>();
        int n = 1;
        for (String s : situaciones) {
            m.put("P183_" + n++, "Le han " + s.substring("le han ".length()));
            m.put("P183_" + n++, "Con que frecuencia " + s);
        }

        assertTrue(DetectorRejilla.repartir(m).isEmpty(),
                "en el papel son siete preguntas con su frecuencia, no una tabla");
    }

    @Test
    @DisplayName("lo que no es una celda se queda fuera, no se fuerza")
    void loQueNoEsCelda() {
        var reparto = DetectorRejilla.repartir(bloque112());

        // «Ninguna» no tiene por donde partirse y «Otro especifique texto» partiria en
        // una fila que no existe en la tabla. Las dos son preguntas del bloque, pero no
        // son celdas: la pantalla las pinta aparte.
        assertFalse(reparto.containsKey("P112_1"));
        assertFalse(reparto.containsKey("P112_137"));
    }

    @Test
    @DisplayName("un bloque normal no se convierte en rejilla")
    void bloqueNormal() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("P158_1", "Se ha sentido amenazado en la comunidad alrededor de su trabajo");
        m.put("P158_2", "Se ha sentido amenazado en la comunidad alrededor de donde vive");
        m.put("P158_3", "Se ha sentido amenazado en su casa");
        m.put("P159_1", "Que tan seguro se ha sentido en el lugar donde trabaja");
        m.put("P159_2", "Que tan seguro se ha sentido en el lugar donde vive");

        assertTrue(DetectorRejilla.repartir(m).isEmpty());
    }

    @Test
    @DisplayName("una rejilla a medio llenar no es una rejilla")
    void rejillaDispersa() {
        // Caso real del bloque P183: hay palabras finales que se repiten —«verbalmente»,
        // «casa», «Trabajo»— y el reparto sale con la mayoria de los huecos vacios. Eso
        // no es una tabla, es un bloque normal donde coinciden las terminaciones.
        Map<String, String> m = new LinkedHashMap<>();
        m.put("P183_1", "Le han ofendido verbalmente");
        m.put("P183_2", "Con que frecuencia le han ofendido verbalmente");
        m.put("P183_3", "Donde le han ofendido verbalmente - casa");
        m.put("P183_4", "Donde le han ofendido verbalmente - escuela");
        m.put("P183_5", "Donde le han ofendido verbalmente - Trabajo");
        m.put("P183_6", "Quienes le han ofendido verbalmente - Padres");
        m.put("P183_7", "Quienes le han ofendido verbalmente - Hermanos");
        m.put("P183_8", "Le han golpeado");
        m.put("P183_9", "Con que frecuencia le han golpeado");
        m.put("P183_10", "Donde le han golpeado - casa");

        assertTrue(DetectorRejilla.repartir(m).isEmpty(),
                "con los huecos vacios que deja, esto no es una tabla");
    }

    @Test
    @DisplayName("hace falta mas de una columna y mas de dos filas")
    void demasiadoChica() {
        Map<String, String> dosFilas = new LinkedHashMap<>();
        dosFilas.put("A", "Primera Lunes");
        dosFilas.put("B", "Primera Martes");
        dosFilas.put("C", "Segunda Lunes");
        dosFilas.put("D", "Segunda Martes");

        assertTrue(DetectorRejilla.repartir(dosFilas).isEmpty(),
                "dos filas no alcanzan para hablar de una tabla");
    }

    @Test
    @DisplayName("los espacios de sobra del libro no estorban")
    void espaciosDeSobra() {
        Map<String, String> m = new LinkedHashMap<>();
        String[] filas = {"Caminar", "Correr", "Nadar"};
        for (String f : filas) {
            m.put(f + "1", "  " + f + "   Lunes ");
            m.put(f + "2", f + "\tMartes");
            m.put(f + "3", f + " Tiempo");
        }
        var reparto = DetectorRejilla.repartir(m);

        assertEquals(9, reparto.size());
        assertEquals("Caminar", reparto.get("Caminar1").fila());
        assertEquals("Lunes", reparto.get("Caminar1").columna());
    }

    @Test
    @DisplayName("un bloque vacio o de una sola pregunta no truena")
    void bordes() {
        assertTrue(DetectorRejilla.repartir(Map.<String, String>of()).isEmpty());
        assertTrue(DetectorRejilla.repartir(Map.of("P1", "Edad")).isEmpty());
        Map<String, String> conNulo = new LinkedHashMap<>();
        conNulo.put("P1", null);
        assertTrue(DetectorRejilla.repartir(conNulo).isEmpty());
    }
}
