package imss.gob.mx.cohorte.controllers.dashboard.dto;

import java.util.List;

public record CoberturaPacienteDTO(
    String folio,
    String nombre,            // apellidoPaterno + ", " + nombrePropio
    String sexo,
    int    total,             // tipos cubiertos
    int    totalTipos,        // denominador
    List<CeldaCoberturaDTO> celdas
) {}
