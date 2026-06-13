package imss.gob.mx.cohorte.modules.paciente;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contador del último consecutivo de folio generado automáticamente, llevado
 * por año (la numeración reinicia cada año calendario). Usado por
 * {@code FolioGeneratorService} bajo bloqueo pesimista para garantizar
 * consecutivos únicos incluso con altas concurrentes.
 */
@Entity
@Table(name = "folio_secuencia")
@Getter
@Setter
@NoArgsConstructor
public class FolioSecuencia {

    /** Año calendario (p. ej. 2026) al que corresponde este contador. */
    @Id
    @Column(name = "anio")
    private Integer anio;

    @Column(name = "ultimo_consecutivo", nullable = false)
    private Integer ultimoConsecutivo = 0;

    public FolioSecuencia(Integer anio) {
        this.anio = anio;
        this.ultimoConsecutivo = 0;
    }
}
