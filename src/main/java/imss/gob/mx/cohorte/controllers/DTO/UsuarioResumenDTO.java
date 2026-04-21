package imss.gob.mx.cohorte.controllers.DTO;

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
    private String UUID;
    private String nombreCompleto;


    public UsuarioResumenDTO toUserEntity(BeanUser user){

        UsuarioResumenDTO userResponse = new UsuarioResumenDTO();
        userResponse.setId(0L);
        userResponse.setUsername(user.getUsername());
        userResponse.setUUID(user.getUUID());
        userResponse.setNombreCompleto(user.getPersona().getNombre() + " " + user.getPersona().getApellidoPaterno() + " " + user.getPersona().getApellidoMaterno());

        return userResponse;

    }


}
