package imss.gob.mx.cohorte.modules.escalonPrueba.DTO;



import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;
import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapa;
import imss.gob.mx.cohorte.modules.escalonPrueba.medicion.PruebaEscalonMedicion;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ValorDTO {

    private Long Id;
    private Long etapa;
    private String parametro;
    private Double valor;
    private String unidad;


    public PruebaEscalonMedicion toEntity(){
        PruebaEscalonMedicion valor = new PruebaEscalonMedicion();

        PruebaEscalonEtapa etapa = new PruebaEscalonEtapa();
        etapa.setId(this.etapa);

        valor.setEtapa(etapa);
        valor.setUnidad(this.unidad);
        valor.setParametro(PruebaEscalonMedicion.Parametro.valueOf(this.parametro));
        valor.setValor(this.valor);

        return valor;

    }

    public PruebaEscalonMedicion toEntityUpdate(){
        PruebaEscalonMedicion valor = new PruebaEscalonMedicion();

        PruebaEscalonEtapa etapa = new PruebaEscalonEtapa();
        etapa.setId(this.etapa);

        valor.setId(this.Id);
        valor.setEtapa(etapa);
        valor.setUnidad(this.unidad);
        valor.setParametro(PruebaEscalonMedicion.Parametro.valueOf(this.parametro));
        valor.setValor(this.valor);

        return valor;

    }
}
