package imss.gob.mx.cohorte.controllers.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResumenDTO {
    private Long id;
    private String username;
    @JsonProperty("UUID")
    private String UUID;
    private String nombreCompleto;


    public UsuarioResumenDTO toUserEntity(BeanUser user){

        UsuarioResumenDTO userResponse = new UsuarioResumenDTO();
        userResponse.setId(0L);
        userResponse.setUsername(user.getUsername());
        userResponse.setUUID(user.getUUID());
        userResponse.setNombreCompleto(
                user.getPersona().getNombre()
                + (user.getPersona().getSegundoNombre() != null ? " " + user.getPersona().getSegundoNombre() : "")
                + " " + user.getPersona().getApellidoPaterno()
                + (user.getPersona().getApellidoMaterno() != null ? " " + user.getPersona().getApellidoMaterno() : ""));

        return userResponse;

    }


}
