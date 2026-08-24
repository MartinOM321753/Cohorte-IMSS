package imss.gob.mx.cohorte.services.importacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Parte una linea de CSV respetando las comillas.
 *
 * <p>Existe porque partir por el separador a secas —{@code linea.split(",")}— es
 * incorrecto en cuanto un campo contiene el separador: {@code "Gomez, Juan"} se
 * convierte en dos campos y <b>todo lo que viene despues se corre una
 * posicion</b>. No falla ni avisa; simplemente lee el folio de la columna del
 * sexo. Es un fallo silencioso y por eso merece codigo propio y pruebas.</p>
 *
 * <p>Sigue la convencion habitual (RFC 4180): los campos pueden ir entrecomillados,
 * y dentro de un campo entrecomillado unas comillas dobles seguidas representan
 * una comilla literal.</p>
 */
final class ParserCsv {

    private ParserCsv() {}

    /**
     * Deduce el separador mirando la linea de encabezados.
     *
     * <p>Hace falta porque un Excel en configuracion regional espanola exporta
     * con punto y coma, y el mismo archivo abierto en otra maquina sale con coma.
     * Se elige el candidato que produzca mas campos: el separador de verdad
     * aparece una vez por columna, los demas caracteres a lo sumo sueltos.</p>
     */
    static char detectarSeparador(String encabezado) {
        char mejor = ',';
        int maximo = -1;
        for (char candidato : new char[]{',', ';', (char) 9, '|'}) {
            int campos = partir(encabezado, candidato).size();
            if (campos > maximo) {
                maximo = campos;
                mejor = candidato;
            }
        }
        return mejor;
    }

    /**
     * Parte una linea en campos.
     *
     * <p>Recorre caracter a caracter llevando la cuenta de si esta dentro de un
     * campo entrecomillado; dentro, el separador es texto y no corta.</p>
     */
    static List<String> partir(String linea, char separador) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);

            if (entreComillas) {
                if (c == '"') {
                    // Dos comillas seguidas dentro del campo son una comilla literal;
                    // una sola cierra el campo.
                    if (i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                        actual.append('"');
                        i++;
                    } else {
                        entreComillas = false;
                    }
                } else {
                    actual.append(c);
                }
                continue;
            }

            if (c == '"' && actual.isEmpty()) {
                // Solo abre campo si la comilla esta al principio; una comilla en
                // mitad de un valor sin entrecomillar es texto.
                entreComillas = true;
            } else if (c == separador) {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }
}
