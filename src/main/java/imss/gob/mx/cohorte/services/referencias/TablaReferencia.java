package imss.gob.mx.cohorte.services.referencias;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import imss.gob.mx.cohorte.services.formulas.Percentiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Una tabla de percentiles por sexo y edad, tal como vive en su archivo JSON.
 *
 * <p>Los datos son fijos: vienen de una publicación y no los edita nadie desde la
 * aplicación. Por eso van en un archivo del proyecto y no en la base de datos.</p>
 *
 * @param version sube si alguna vez se corrige un dato; un reporte emitido debe poder
 *                decir con qué versión salió
 * @param fuente  de dónde salen los valores
 * @param cortes  los percentiles que marcan las columnas; hoy siempre el P5 al P95
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TablaReferencia(String id, String nombre, int version, String unidad,
                              String fuente, int edadMinima, List<Integer> cortes,
                              List<Fila> filas) {

    /**
     * @param sexo       {@code H} u {@code M}, como en la hoja de origen
     * @param edadHasta  inclusiva; el último tramo abierto («100+») lleva un tope alto
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fila(String sexo, int edadDesde, int edadHasta, List<BigDecimal> valores) {}

    /** La fila que corresponde, o vacío si la edad queda fuera de la tabla. */
    public Optional<Fila> fila(boolean mujer, int edad) {
        String sexo = mujer ? "M" : "H";
        return filas.stream()
                .filter(f -> f.sexo().equals(sexo) && edad >= f.edadDesde() && edad <= f.edadHasta())
                .findFirst();
    }

    /**
     * Los problemas del archivo, uno por línea; vacío si está bien.
     *
     * <p>Se revisa al arrancar y en una prueba: un dato mal copiado cambia lo que se
     * le dice a un participante sobre su fuerza, y es preferible que la aplicación no
     * levante a que emita un reporte con una tabla rota.</p>
     */
    public List<String> problemas() {
        List<String> p = new ArrayList<>();
        if (id == null || id.isBlank()) p.add("falta el id");
        if (fuente == null || fuente.isBlank()) p.add("falta la fuente");
        if (version < 1) p.add("la versión debe ser 1 o mayor");

        List<Integer> esperados = java.util.Arrays.stream(Percentiles.PERCENTILES).boxed().toList();
        if (!esperados.equals(cortes)) {
            p.add("los cortes deben ser " + esperados + " y son " + cortes);
        }
        if (filas == null || filas.isEmpty()) {
            p.add("no trae filas");
            return p;
        }

        for (String sexo : List.of("H", "M")) {
            List<Fila> deSexo = filas.stream().filter(f -> sexo.equals(f.sexo()))
                    .sorted(java.util.Comparator.comparingInt(Fila::edadDesde)).toList();
            if (deSexo.isEmpty()) {
                p.add("faltan las filas del sexo " + sexo);
                continue;
            }
            if (deSexo.get(0).edadDesde() != edadMinima) {
                p.add(sexo + ": el primer tramo empieza en " + deSexo.get(0).edadDesde()
                        + " y la tabla declara " + edadMinima);
            }
            for (int i = 0; i < deSexo.size(); i++) {
                Fila f = deSexo.get(i);
                String donde = sexo + " " + f.edadDesde() + "–" + f.edadHasta();
                if (f.edadHasta() < f.edadDesde()) p.add(donde + ": el tramo está al revés");
                if (i > 0 && f.edadDesde() != deSexo.get(i - 1).edadHasta() + 1) {
                    p.add(donde + ": hueco o traslape con el tramo anterior");
                }
                if (f.valores() == null || f.valores().size() != esperados.size()) {
                    p.add(donde + ": debe traer " + esperados.size() + " valores");
                    continue;
                }
                for (int j = 1; j < f.valores().size(); j++) {
                    if (f.valores().get(j).compareTo(f.valores().get(j - 1)) < 0) {
                        p.add(donde + ": los valores no crecen del P"
                                + esperados.get(j - 1) + " al P" + esperados.get(j));
                    }
                }
            }
        }
        return p;
    }
}
