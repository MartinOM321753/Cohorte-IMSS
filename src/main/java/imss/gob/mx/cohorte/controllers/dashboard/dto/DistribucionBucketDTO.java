package imss.gob.mx.cohorte.controllers.dashboard.dto;

public record DistribucionBucketDTO(
    int  cantidadTipos,      // k — cuántos tipos distintos tiene este grupo de pacientes
    long cantidadPacientes,  // pacientes con exactamente k tipos cubiertos
    int  totalTipos          // N total de tipos activos (denominador)
) {}
