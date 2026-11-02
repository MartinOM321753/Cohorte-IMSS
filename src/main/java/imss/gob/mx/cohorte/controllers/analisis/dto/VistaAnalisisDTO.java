package imss.gob.mx.cohorte.controllers.analisis.dto;

/** Vista guardada del analista, tal como se devuelve al cliente. */
public record VistaAnalisisDTO(
        String uuid,
        String nombre,
        String configuracion,
        String fechaActualizacion
) {}
