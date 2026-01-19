package imss.gob.mx.cohorte.modules.escalonPrueba.DTO;
import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;
import imss.gob.mx.cohorte.modules.usuarios.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Data
@Getter
@Setter
public class PruebaDTO {

    private Long id;
    private Long paciente;
    private Long usuarioRealiza;
    private LocalDate fechaEstudio;

    public PruebaEscalon toEntity() {
        PruebaEscalon pruebaEscalon = new PruebaEscalon();
        Paciente patient = new Paciente();
        BeanUser beanUser = new BeanUser();
        patient.setId(paciente);
        beanUser.setId(usuarioRealiza);

    pruebaEscalon.setPaciente(patient);
    pruebaEscalon.setUsuarioRealiza(beanUser);
    pruebaEscalon.setFechaEstudio(fechaEstudio);

        return pruebaEscalon;
    }

    public PruebaEscalon toEntityUpdate() {
        PruebaEscalon pruebaEscalon = new PruebaEscalon();
        Paciente patient = new Paciente();
        BeanUser beanUser = new BeanUser();
        patient.setId(paciente);
        beanUser.setId(usuarioRealiza);

        pruebaEscalon.setId(id);
        pruebaEscalon.setPaciente(patient);
        pruebaEscalon.setUsuarioRealiza(beanUser);
        pruebaEscalon.setFechaEstudio(fechaEstudio);

        return pruebaEscalon;
    }



}
