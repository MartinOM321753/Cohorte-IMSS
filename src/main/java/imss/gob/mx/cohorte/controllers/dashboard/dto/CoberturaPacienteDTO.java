package imss.gob.mx.cohorte.controllers.dashboard.dto;

import java.util.List;

public record CoberturaPacienteDTO(
    String folio,
    String uuid,              // para abrir el expediente sin exponer el id en la URL
    String nombre,            // apellidoPaterno + ", " + nombrePropio
    String sexo,
    int    total,             // tipos cubiertos
    int    totalTipos,        // denominador
    List<CeldaCoberturaDTO> celdas
) {}
