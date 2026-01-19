package imss.gob.mx.cohorte.modules.escalonPrueba.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;
import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapa;
import imss.gob.mx.cohorte.modules.escalonPrueba.medicion.PruebaEscalonMedicion;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class EtapaDTO {


    private Long Id;
    private Long pruebaEscalon;
    private String etapa;
    private String observaciones;


    public PruebaEscalonEtapa toEntity() {
        PruebaEscalonEtapa etapaEntity = new PruebaEscalonEtapa();
        PruebaEscalon pruebaEscalon = new PruebaEscalon();

        pruebaEscalon.setId(this.pruebaEscalon);

        etapaEntity.setPruebaEscalon(pruebaEscalon);
        etapaEntity.setEtapa(PruebaEscalonEtapa.Etapa.valueOf(this.etapa));
        etapaEntity.setObservaciones(this.observaciones);
        return etapaEntity;
    }

    public PruebaEscalonEtapa toEntityUpdate() {
        PruebaEscalonEtapa etapaEntity = new PruebaEscalonEtapa();
        PruebaEscalon pruebaEscalon = new PruebaEscalon();

        pruebaEscalon.setId(this.pruebaEscalon);

        etapaEntity.setPruebaEscalon(pruebaEscalon);
        etapaEntity.setId(this.Id);
        etapaEntity.setEtapa(PruebaEscalonEtapa.Etapa.valueOf(this.etapa));
        etapaEntity.setObservaciones(this.observaciones);
        return etapaEntity;
    }



}
