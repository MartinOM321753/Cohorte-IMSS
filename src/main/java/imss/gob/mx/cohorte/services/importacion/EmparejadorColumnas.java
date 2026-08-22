package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.estudios.parametros.AliasParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
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
 * participante y la fecha del estudio—, las que corresponden a un parametro por
 * su alias, y las que no son ninguna de las anteriores.</p>
 *
 * <h3>Lo que se ignora y lo que detiene la carga</h3>
 *
 * <p>Una columna sobrante se avisa y se ignora: los aparatos exportan cosas que
 * al estudio no le interesan y obligar a limpiar el archivo a mano no aporta
 * nada. Un parametro sin columna, en cambio, detiene la carga, porque todos los
 * parametros son obligatorios y el estudio quedaria incompleto.</p>
 *
 * <p>Tambien detiene la carga que dos columnas reclamen el mismo parametro o que
 * dos parametros reclamen la misma columna. Elegir una en silencio guardaria una
 * medicion en el sitio de otra, que es el peor resultado posible: el dato existe,
 * parece correcto y esta mal.</p>
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
            "FECHA MEDICION", "FECHA DE MEDICION", "FECHA Y HORA");

    /** A que se resolvio una columna del archivo. */
    public enum Rol { FOLIO, FECHA, PARAMETRO, IGNORADA }

    /**
     * Una columna del archivo ya interpretada.
     *
     * @param indice     posicion en el archivo, base 0
     * @param encabezado el titulo tal como venia
     * @param rol        a que se resolvio
     * @param parametro  el parametro al que corresponde; solo con rol PARAMETRO
     * @param aliasUsado el alias que hizo la coincidencia, para poder explicarla
     */
    public record Columna(int indice, String encabezado, Rol rol,
                          ParametroEstudio parametro, String aliasUsado) {}

    /**
     * El resultado del emparejado.
     *
     * @param columnas            todas las del archivo, en su orden
     * @param parametrosSinColumna los que no encontraron columna; si hay alguno,
     *                            la carga no puede continuar
     * @param problemas           conflictos que impiden continuar, ya redactados
     */
    public record Emparejado(List<Columna> columnas,
                             List<ParametroEstudio> parametrosSinColumna,
                             List<String> problemas) {

        public boolean utilizable() {
            return problemas.isEmpty() && parametrosSinColumna.isEmpty();
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

    public static Emparejado emparejar(List<String> encabezados, List<ParametroEstudio> parametros) {
        List<Columna> columnas = new ArrayList<>();
        List<String> problemas = new ArrayList<>();

        // Que parametro se lleva cada columna, y a la inversa, para poder
        // detectar los dos sentidos del conflicto.
        Map<Long, Integer> columnaPorParametro = new LinkedHashMap<>();

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

            List<ParametroEstudio> candidatos = candidatosPara(normalizado, parametros);

            if (candidatos.isEmpty()) {
                columnas.add(new Columna(i, encabezado, Rol.IGNORADA, null, null));
                continue;
            }
            if (candidatos.size() > 1) {
                problemas.add("La columna \"" + encabezado + "\" coincide con varios parametros ("
                        + nombres(candidatos) + "). Revisa los alias en el catalogo: "
                        + "un alias no puede repetirse entre parametros del mismo tipo de estudio.");
                columnas.add(new Columna(i, encabezado, Rol.IGNORADA, null, null));
                continue;
            }

            ParametroEstudio p = candidatos.get(0);
            Integer yaAsignada = columnaPorParametro.get(p.getId());
            if (yaAsignada != null) {
                problemas.add("Las columnas \"" + encabezados.get(yaAsignada) + "\" y \"" + encabezado
                        + "\" apuntan al mismo parametro (" + p.getNombre() + "). "
                        + "Deja solo una en el archivo.");
                columnas.add(new Columna(i, encabezado, Rol.IGNORADA, null, null));
                continue;
            }

            columnaPorParametro.put(p.getId(), i);
            columnas.add(new Columna(i, encabezado, Rol.PARAMETRO, p, aliasQueCoincide(normalizado, p)));
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

        List<ParametroEstudio> sinColumna = parametros.stream()
                .filter(p -> !columnaPorParametro.containsKey(p.getId()))
                .toList();

        return new Emparejado(columnas, sinColumna, problemas);
    }

    /**
     * Un parametro es candidato si alguno de sus alias coincide con el
     * encabezado. El nombre del parametro NO se usa como alias implicito: son
     * nombres clinicos que casi nunca coinciden con lo que titula el aparato, y
     * aceptarlos produciria coincidencias por casualidad.
     */
    private static List<ParametroEstudio> candidatosPara(String encabezadoNormalizado,
                                                         List<ParametroEstudio> parametros) {
        return parametros.stream()
                .filter(p -> aliasQueCoincide(encabezadoNormalizado, p) != null)
                .toList();
    }

    /**
     * Limpieza extra para los titulos de control.
     *
     * <p>Solo se aplica a folio y fecha, que son un conjunto cerrado definido
     * aqui: asi "No. Folio", "Nº folio" y "no folio" caen todos en el mismo
     * sitio sin tener que enumerar cada variante de puntuacion.</p>
     *
     * <p>A proposito NO se hace lo mismo con los alias de parametro. Su forma
     * normalizada esta guardada en la base, en alias_normalizado, y cambiar como
     * se calcula dejaria de emparejar con lo ya escrito. Ademas, un alias es
     * texto que escribe el usuario: cuanto mas se manipule, mas facil es que dos
     * alias distintos acaben colisionando.</p>
     */
    private static String sinPuntuacion(String normalizado) {
        return normalizado.replaceAll("[^\\p{L}\\p{N} ]", "").replaceAll("\\s+", " ").trim();
    }

    private static String aliasQueCoincide(String encabezadoNormalizado, ParametroEstudio p) {
        if (p.getAlias() == null) return null;
        for (AliasParametroEstudio a : p.getAlias()) {
            if (encabezadoNormalizado.equals(a.getAliasNormalizado())) {
                return a.getAlias();
            }
        }
        return null;
    }

    private static String nombres(List<ParametroEstudio> ps) {
        return ps.stream().map(ParametroEstudio::getNombre).reduce((a, b) -> a + ", " + b).orElse("");
    }
}
