package imss.gob.mx.cohorte.services.importacion;

import java.util.List;

/**
 * Resultado de la carga masiva por protocolo: lo que se creó (o se crearía en la
 * previsualización) y lo que no cuadró.
 *
 * @param procesamientos participantes × protocolo creados
 * @param padres         tubos primarios generados (sintéticos, T1…TN)
 * @param alicuotas      alícuotas creadas
 * @param lotes          lotes creados
 * @param ubicadas       alícuotas que llegaron con posición y quedaron ubicadas
 * @param errores        filas rechazadas (no se escribe nada si hay errores)
 * @param avisos         advertencias que no bloquean
 */
public record ResultadoCargaProtocolo(
        int procesamientos,
        int padres,
        int alicuotas,
        int lotes,
        int ubicadas,
        List<String> errores,
        List<String> avisos
) {
    public ResultadoCargaProtocolo {
        errores = errores == null ? List.of() : List.copyOf(errores);
        avisos = avisos == null ? List.of() : List.copyOf(avisos);
    }

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }
}
