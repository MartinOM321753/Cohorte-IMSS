package imss.gob.mx.cohorte.controllers.pacientes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultDTO {

    private int totalFilas;
    private int exitosos;
    private int errores;
    private int duplicados;
    private int advertencias;
    private List<FilaError> detalleErrores;
    private List<FilaError> detalleAdvertencias;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilaError {
        private int fila;
        private String folio;
        private String motivo;
    }
}
