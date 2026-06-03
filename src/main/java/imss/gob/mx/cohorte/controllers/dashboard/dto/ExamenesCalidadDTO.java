package imss.gob.mx.cohorte.controllers.dashboard.dto;

public record ExamenesCalidadDTO(
    long enRango,        // valorObtenido dentro del rango normal del sexo del paciente
    long fueraDeRango,   // valorObtenido fuera del rango
    long sinReferencia   // examen sin valorMin/Max definido para ese sexo
) {}
