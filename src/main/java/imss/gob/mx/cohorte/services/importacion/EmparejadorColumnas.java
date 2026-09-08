package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.estudios.parametros.AliasParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.examenes.AliasExamen;
import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.utils.texto.NormalizadorAlias;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decide que representa cada columna del archivo.
 *
 * <p>Hay tres clases de columna: las dos que identifican la fila —el folio del
 * participante y la fecha—, las que corresponden a algo del catalogo por su
 * alias, y las que no son ninguna de las anteriores.</p>
 *
 * <h3>El nombre como alias por omision</h3>
 *
 * <p>Un destino sin ningun alias configurado se empareja por su <b>nombre</b>. Antes no
 * se hacia, y la consecuencia era que la carga masiva no funcionaba en absoluto hasta
 * que alguien diera de alta los alias uno a uno — cuando el titulo que casi siempre
 * lleva el archivo es justo el nombre del parametro.</p>
 *
 * <p>En cuanto el destino tiene aunque sea un alias, mandan los alias y el nombre deja
 * de contar. Configurarlos significa que el aparato titula sus columnas de otra forma,
 * y aceptar tambien el nombre reabriria las coincidencias por casualidad que los alias
 * existen para evitar.</p>
 *
 * <h3>Lo que se ignora y lo que detiene la carga</h3>
 *
 * <p>Una columna sobrante se avisa y se ignora: los aparatos exportan cosas que
 * al registro no le interesan y obligar a limpiar el archivo a mano no aporta
 * nada.</p>
 *
 * <p>Lo que si detiene la carga es que dos columnas apunten a lo mismo, o que una
 * columna coincida con varios destinos. Elegir uno en silencio guardaria una
 * medicion en el sitio de otra, que es el peor resultado posible: el dato existe,
 * parece correcto y esta mal.</p>
 *
 * <h3>Por que no conoce ni parametros ni examenes</h3>
 *
 * <p>Trabaja sobre {@link Destino}, que es solo un id, un nombre y unos alias. Un
 * parametro de estudio y un examen de laboratorio son cosas distintas, pero se
 * emparejan igual, y la parte delicada —detectar el alias ambiguo, reconocer los
 * titulos de control— no deberia existir dos veces: la segunda copia es la que se
 * queda sin arreglar cuando aparece un caso nuevo.</p>
 */
public final class EmparejadorColumnas {

    private EmparejadorColumnas() {}

    /** Nombres con los que un archivo puede titular la columna del participante. */
    private static final Set<String> TITULOS_FOLIO = Set.of(
            "FOLIO", "NO FOLIO", "NUM FOLIO", "NUMERO DE FOLIO",
            "FOLIO PARTICIPANTE", "FOLIO DEL PARTICIPANTE", "ID PARTICIPANTE");

    /** Nombres con los que un archivo puede titular la columna de la fecha. */
    private static final Set<String> TITULOS_FECHA = Set.of(
            "FECHA", "FECHA ESTUDIO", "FECHA DEL ESTUDIO", "FECHA DE ESTUDIO",
            "FECHA MEDICION", "FECHA DE MEDICION", "FECHA Y HORA",
            "FECHA RESULTADO", "FECHA DEL RESULTADO", "FECHA DE RESULTADO",
            "FECHA MUESTRA", "FECHA DE MUESTRA", "FECHA DE TOMA");

    /** A que se resolvio una columna del archivo. */
    public enum Rol { FOLIO, FECHA, PARAMETRO, IGNORADA }

    /**
     * Algo del catalogo a lo que una columna puede corresponder: un parametro de
     * estudio o un examen de laboratorio.
     *
     * @param aliasNormalizados en la forma en que estan guardados en la base
     * @param alias             tal como los escribio el usuario, para poder citarlos
     */
    public record Destino(Long id, String nombre, List<String> aliasNormalizados, List<String> alias) {}

    /**
     * Una columna del archivo ya interpretada.
     *
     * @param indice     posicion en el archivo, base 0
     * @param encabezado el titulo tal como venia
     * @param rol        a que se resolvio
     * @param destino    a que apunta; solo viene con rol PARAMETRO
     * @param aliasUsado el alias que hizo la coincidencia, para poder explicarla
     */
    public record Columna(int indice, String encabezado, Rol rol,
                          Destino destino, String aliasUsado) {}

    /**
     * El resultado del emparejado.
     *
     * @param columnas           todas las del archivo, en su orden
     * @param destinosSinColumna los que no encontraron columna. Que eso impida o no
     *                           continuar lo decide quien llama: en un estudio todos
     *                           los parametros son obligatorios, pero un archivo de
     *                           laboratorio puede traer solo algunos examenes y ser
     *                           perfectamente valido
     * @param problemas          conflictos que impiden continuar, ya redactados
     */
    public record Emparejado(List<Columna> columnas,
                             List<Destino> destinosSinColumna,
                             List<String> problemas) {

        /** Sin conflictos. No dice nada sobre los destinos que se quedaron sin columna. */
        public boolean sinConflictos() {
            return problemas.isEmpty();
        }

        public List<Columna> conRol(Rol rol) {
            return columnas.stream().filter(c -> c.rol() == rol).toList();
        }

        /** El indice de la columna con ese rol, o -1 si no esta. */
        public int indiceDe(Rol rol) {
            return columnas.stream().filter(c -> c.rol() == rol)
                    .mapToInt(Columna::indice).findFirst().orElse(-1);
        }
    }

    public static Emparejado emparejar(List<String> encabezados, List<Destino> destinos) {
        List<Columna> columnas = new ArrayList<>();
        List<String> problemas = new ArrayList<>();

        // Que destino se lleva cada columna, para poder detectar los dos sentidos
        // del conflicto: dos columnas al mismo destino, y una columna a varios.
        Map<Long, Integer> columnaPorDestino = new LinkedHashMap<>();

        int vistasFolio = 0;
        int vistasFecha = 0;

        for (int i = 0; i < encabezados.size(); i++) {
            String encabezado = encabezados.get(i);
            String normalizado = NormalizadorAlias.normalizar(encabezado);
            String comoControl = sinPuntuacion(normalizado);

            if (TITULOS_FOLIO.contains(comoControl)) {
                vistasFolio++;
                columnas.add(new Columna(i, encabezado, Rol.FOLIO, null, null));
                continue;
            }
            if (TITULOS_FECHA.contains(comoControl)) {
                vistasFecha++;
                columnas.add(new Columna(i, encabezado, Rol.FECHA, null, null));
                continue;
            }

            List<Destino> candidatos = candidatosPara(normalizado, destinos);

            if (candidatos.isEmpty()) {
                columnas.add(new Columna(i, encabezado, Rol.IGNORADA, null, null));
                continue;
            }
            if (candidatos.size() > 1) {
                problemas.add("La columna \"" + encabezado + "\" coincide con varios destinos ("
                        + nombres(candidatos) + "). Revisa los alias en el catalogo: "
                        + "un alias no puede repetirse.");
                columnas.add(new Columna(i, encabezado, Rol.IGNORADA, null, null));
                continue;
            }

            Destino d = candidatos.get(0);
            Integer yaAsignada = columnaPorDestino.get(d.id());
            if (yaAsignada != null) {
                problemas.add("Las columnas \"" + encabezados.get(yaAsignada) + "\" y \"" + encabezado
                        + "\" apuntan a lo mismo (" + d.nombre() + "). Deja solo una en el archivo.");
                columnas.add(new Columna(i, encabezado, Rol.IGNORADA, null, null));
                continue;
            }

            columnaPorDestino.put(d.id(), i);
            columnas.add(new Columna(i, encabezado, Rol.PARAMETRO, d, aliasQueCoincide(normalizado, d)));
        }

        if (vistasFolio == 0) {
            problemas.add("Falta la columna del participante. Titulala \"folio\" "
                    + "y pon en ella el folio de cada participante.");
        } else if (vistasFolio > 1) {
            problemas.add("Hay " + vistasFolio + " columnas de folio. Deja solo una.");
        }
        if (vistasFecha == 0) {
            problemas.add("Falta la columna de la fecha. Titulala \"fecha\".");
        } else if (vistasFecha > 1) {
            problemas.add("Hay " + vistasFecha + " columnas de fecha. Deja solo una.");
        }

        List<Destino> sinColumna = destinos.stream()
                .filter(d -> !columnaPorDestino.containsKey(d.id()))
                .toList();

        return new Emparejado(columnas, sinColumna, problemas);
    }

    /** Un destino es candidato si su nombre o alguno de sus alias coincide. */
    private static List<Destino> candidatosPara(String encabezadoNormalizado, List<Destino> destinos) {
        return destinos.stream()
                .filter(d -> aliasQueCoincide(encabezadoNormalizado, d) != null)
                .toList();
    }

    /**
     * Con que coincidio el encabezado, o null si no coincidio con nada.
     *
     * <p><b>Cuando el destino no tiene ningun alias configurado, su propio nombre hace
     * de alias.</b> Un parametro sin alias no se emparejaba nunca, asi que la carga
     * masiva no servia hasta que alguien se sentara a dar de alta los alias uno por
     * uno; y el titulo que casi siempre se escribe en el archivo es, precisamente, el
     * nombre del parametro.</p>
     *
     * <p>Solo cuando no hay ninguno. Si el destino ya tiene alias, mandan ellos y el
     * nombre se queda fuera: quien se tomo el trabajo de configurarlos lo hizo porque
     * el aparato titula sus columnas de otra forma, y colar el nombre ademas volveria a
     * abrir la puerta a las coincidencias por casualidad que los alias vinieron a
     * cerrar.</p>
     */
    private static String aliasQueCoincide(String encabezadoNormalizado, Destino d) {
        List<String> normalizados = d.aliasNormalizados() == null ? List.of() : d.aliasNormalizados();

        for (int i = 0; i < normalizados.size(); i++) {
            if (encabezadoNormalizado.equals(normalizados.get(i))) {
                return i < d.alias().size() ? d.alias().get(i) : normalizados.get(i);
            }
        }

        if (normalizados.isEmpty() && d.nombre() != null
                && encabezadoNormalizado.equals(NormalizadorAlias.normalizar(d.nombre()))) {
            return d.nombre();
        }
        return null;
    }

    private static String nombres(List<Destino> ds) {
        return ds.stream().map(Destino::nombre).reduce((a, b) -> a + ", " + b).orElse("");
    }

    /**
     * Limpieza extra para los titulos de control.
     *
     * <p>Solo se aplica a folio y fecha, que son un conjunto cerrado definido
     * aqui: asi "No. Folio", "Nº folio" y "no folio" caen todos en el mismo
     * sitio sin tener que enumerar cada variante de puntuacion.</p>
     *
     * <p>A proposito NO se hace lo mismo con los alias. Su forma normalizada esta
     * guardada en la base, en alias_normalizado, y cambiar como se calcula dejaria
     * de emparejar con lo ya escrito. Ademas, un alias es texto que escribe el
     * usuario: cuanto mas se manipule, mas facil es que dos alias distintos acaben
     * colisionando.</p>
     */
    private static String sinPuntuacion(String normalizado) {
        return normalizado.replaceAll("[^\\p{L}\\p{N} ]", "").replaceAll("\\s+", " ").trim();
    }

    // ── Adaptadores desde el catalogo ────────────────────────────────────────

    /** Un parametro de estudio visto como destino. */
    public static Destino desdeParametro(ParametroEstudio p) {
        List<AliasParametroEstudio> as = p.getAlias() == null ? List.of() : p.getAlias();
        return new Destino(p.getId(), p.getNombre(),
                as.stream().map(AliasParametroEstudio::getAliasNormalizado).toList(),
                as.stream().map(AliasParametroEstudio::getAlias).toList());
    }

    /** Un examen de laboratorio visto como destino. */
    public static Destino desdeExamen(Examen e, List<AliasExamen> alias) {
        List<AliasExamen> as = alias == null ? List.of() : alias;
        return new Destino(e.getId(), e.getParametro(),
                as.stream().map(AliasExamen::getAliasNormalizado).toList(),
                as.stream().map(AliasExamen::getAlias).toList());
    }
}
