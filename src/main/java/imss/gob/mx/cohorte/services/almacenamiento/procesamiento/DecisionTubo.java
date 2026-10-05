package imss.gob.mx.cohorte.services.almacenamiento.procesamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.AccionTubo;

import java.util.List;

/**
 * Lo que se decide, al procesar un participante, para un tubo primario del
 * protocolo.
 *
 * <p>El protocolo da los valores por omisión; aquí se concretan para esta tanda:
 * qué se le hace de verdad al tubo (la acción puede cambiar: un tubo que el
 * protocolo guarda hoy puede alicuotarse), cuánto se extrajo y, opcionalmente, el
 * reparto exacto de las alícuotas.</p>
 *
 * @param idTuboProtocolo tubo del protocolo al que aplica
 * @param incluir         si este tubo se procesa en esta tanda
 * @param accion          acción a ejecutar; null = la del protocolo
 * @param volumen         volumen extraído que entra a la padre; null = sin dato
 * @param unidad          unidad del volumen para GUARDAR/ESTUDIO; en ALICUOTAR manda el tubo
 * @param planVolumenes   reparto explícito de las alícuotas; vacío = automático
 */
public record DecisionTubo(
        Long idTuboProtocolo,
        boolean incluir,
        AccionTubo accion,
        Double volumen,
        String unidad,
        List<Double> planVolumenes
) {
    public DecisionTubo {
        planVolumenes = planVolumenes == null ? List.of() : List.copyOf(planVolumenes);
    }
}
