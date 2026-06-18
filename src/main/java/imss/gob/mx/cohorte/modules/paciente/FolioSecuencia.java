package imss.gob.mx.cohorte.modules.paciente;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "folio_secuencia")
@IdClass(FolioSecuenciaId.class)
@Getter
@Setter
@NoArgsConstructor
public class FolioSecuencia {

    @Id
    @Column(name = "anio")
    private Integer anio;

    @Id
    @Column(name = "id_institucion")
    private Long idInstitucion;

    @Column(name = "ultimo_consecutivo", nullable = false)
    private Integer ultimoConsecutivo = 0;

    public FolioSecuencia(Integer anio, Long idInstitucion) {
        this.anio = anio;
        this.idInstitucion = idInstitucion;
        this.ultimoConsecutivo = 0;
    }
}
