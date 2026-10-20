package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.tipos.DirigidoA;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Decide si un instrumento le toca a un participante.
 *
 * <h3>La edad se calcula al día de la aplicación, no al de hoy</h3>
 *
 * <p>Es la parte que se equivoca sola si no se piensa. El minimental se aplica de los 45
 * años en adelante; un participante que lo contestó a los 45 tiene 48 cuando alguien
 * revisa el expediente dos años después. Evaluando con la fecha de hoy, su cuestionario
 * seguiría estando bien — pero el de alguien que lo contestó a los 44 por error pasaría a
 * parecer correcto en cuanto cumpliera 45, y el error desaparecería del registro. Y al
 * revés: un instrumento con tope de edad empezaría a marcar como indebidos cuestionarios
 * que fueron correctos cuando se aplicaron.</p>
 *
 * <h3>Tres respuestas, no dos</h3>
 *
 * <p>«Sí aplica», «no aplica» y <b>«no se puede saber»</b>. La tercera existe porque el
 * sexo y la fecha de nacimiento admiten nulos en el expediente: si faltan, el hueco es
 * nuestro, no del participante. Tratarlo como «no aplica» dejaría a esa persona sin poder
 * contestar nada por un dato que nadie capturó; tratarlo como «sí aplica» escondería el
 * hueco. Se devuelve como lo que es, y cada pantalla decide: ofrecerlo con aviso, o
 * exigir que primero se complete el expediente.</p>
 */
public final class CriterioAplicabilidad {

    private CriterioAplicabilidad() {}

    /**
     * @param aplica        si el participante entra en el público del instrumento
     * @param indeterminado si faltan datos del expediente para poder decidirlo
     * @param motivos       en qué no coincide, o qué dato falta; vacío cuando aplica limpio
     * @param edad          años cumplidos usados en la evaluación, o null si no se pudo calcular
     */
    public record Resultado(boolean aplica, boolean indeterminado, List<String> motivos, Integer edad) {

        /** Un texto corto para la pantalla y para los mensajes de error. */
        public String motivo() {
            return motivos.isEmpty() ? null : String.join("; ", motivos);
        }
    }

    /**
     * @param fechaAplicacion día en que se contestó. Si viene nulo se usa hoy, que es lo
     *                        correcto para un cuestionario que se está por abrir.
     */
    public static Resultado evaluar(TipoCuestionario tipo, Paciente paciente, LocalDate fechaAplicacion) {
        List<String> motivos = new ArrayList<>();
        boolean indeterminado = false;

        Persona persona = paciente == null ? null : paciente.getPersona();
        LocalDate referencia = fechaAplicacion == null ? LocalDate.now() : fechaAplicacion;

        // ── Sexo ────────────────────────────────────────────────────────────────
        DirigidoA dirigido = tipo.getDirigidoA() == null ? DirigidoA.AMBOS : tipo.getDirigidoA();
        if (dirigido != DirigidoA.AMBOS) {
            Persona.Sexo sexo = persona == null ? null : persona.getSexo();
            if (sexo == null) {
                indeterminado = true;
                motivos.add("el participante no tiene sexo registrado y el cuestionario es solo para "
                        + dirigido.etiqueta());
            } else if (!dirigido.admite(sexo)) {
                motivos.add("el cuestionario es solo para " + dirigido.etiqueta());
            }
        }

        // ── Edad ────────────────────────────────────────────────────────────────
        Integer edad = null;
        boolean pideEdad = tipo.getEdadMinima() != null || tipo.getEdadMaxima() != null;
        if (pideEdad) {
            LocalDate nacimiento = persona == null ? null : persona.getFechaNacimiento();
            if (nacimiento == null) {
                indeterminado = true;
                motivos.add("el participante no tiene fecha de nacimiento registrada y el cuestionario "
                        + "tiene restriccion de edad (" + rangoComoTexto(tipo) + ")");
            } else if (nacimiento.isAfter(referencia)) {
                // Una fecha de nacimiento posterior a la aplicacion no es una edad
                // negativa: es un dato mal capturado, y hay que decirlo asi.
                indeterminado = true;
                motivos.add("la fecha de nacimiento (" + nacimiento + ") es posterior a la fecha de "
                        + "aplicacion (" + referencia + ")");
            } else {
                edad = Period.between(nacimiento, referencia).getYears();
                if (tipo.getEdadMinima() != null && edad < tipo.getEdadMinima()) {
                    motivos.add("tenia " + edad + " anos y el cuestionario es " + rangoComoTexto(tipo));
                }
                if (tipo.getEdadMaxima() != null && edad > tipo.getEdadMaxima()) {
                    motivos.add("tenia " + edad + " anos y el cuestionario es " + rangoComoTexto(tipo));
                }
            }
        } else if (persona != null && persona.getFechaNacimiento() != null
                && !persona.getFechaNacimiento().isAfter(referencia)) {
            // Se calcula aunque no haya restriccion: la pantalla la muestra junto al
            // participante y pedirla aparte seria otra consulta para el mismo dato.
            edad = Period.between(persona.getFechaNacimiento(), referencia).getYears();
        }

        boolean aplica = motivos.isEmpty();
        return new Resultado(aplica, indeterminado, motivos, edad);
    }

    /** «de 45 anos en adelante», «hasta 44 anos», «de 18 a 65 anos», «de exactamente 45 anos». */
    public static String rangoComoTexto(TipoCuestionario tipo) {
        Integer min = tipo.getEdadMinima();
        Integer max = tipo.getEdadMaxima();
        if (min == null && max == null) return "para cualquier edad";
        if (min != null && max != null) {
            return min.equals(max)
                    ? "de exactamente " + min + " anos"
                    : "de " + min + " a " + max + " anos";
        }
        return min != null ? "de " + min + " anos en adelante" : "hasta " + max + " anos";
    }

    /** Resumen del publico del instrumento, para mostrarlo en el catalogo. */
    public static String publicoComoTexto(TipoCuestionario tipo) {
        DirigidoA dirigido = tipo.getDirigidoA() == null ? DirigidoA.AMBOS : tipo.getDirigidoA();
        boolean pideEdad = tipo.getEdadMinima() != null || tipo.getEdadMaxima() != null;

        if (dirigido == DirigidoA.AMBOS && !pideEdad) return "Todos los participantes";
        if (!pideEdad) return "Solo " + dirigido.etiqueta();
        if (dirigido == DirigidoA.AMBOS) {
            return capitalizar(rangoComoTexto(tipo));
        }
        return capitalizar(dirigido.etiqueta()) + " " + rangoComoTexto(tipo);
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
