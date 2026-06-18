package imss.gob.mx.cohorte.modules.paciente;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FolioSecuenciaId implements Serializable {
    private Integer anio;
    private Long idInstitucion;
}
