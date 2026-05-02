package imss.gob.mx.cohorte.controllers.users;

import imss.gob.mx.cohorte.application.UserApplicationService;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.ControllerValidationHandler;
import imss.gob.mx.cohorte.security.MainSecurity;
import imss.gob.mx.cohorte.security.filters.JWTFilter;
import imss.gob.mx.cohorte.security.jwt.JWTUtils;
import imss.gob.mx.cohorte.security.jwt.UDService;
import imss.gob.mx.cohorte.utils.Exceptions.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({MainSecurity.class, JWTFilter.class, ControllerValidationHandler.class, GlobalExceptionHandler.class})
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserApplicationService userApplicationService;
    @MockBean
    private UDService udService;
    @MockBean
    private JWTUtils jwtUtils;

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReturnsOk() throws Exception {
        when(userApplicationService.updateUser(any())).thenReturn(userResponse());

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("nuevo.usuario"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReturnsBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "abc",
                                  "password": "123",
                                  "idRol": 1,
                                  "persona": {}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.username").exists());
    }

    private String validPayload() {
        return """
                {
                  "username": "nuevo.usuario",
                  "password": "password123",
                  "idRol": 1,
                  "persona": {
                    "nombre": "Luis",
                    "apellidoPaterno": "Garcia",
                    "apellidoMaterno": "Diaz",
                    "fechaNacimiento": "1990-01-01",
                    "sexo": "M",
                    "telefono": "7772589476",
                    "email": "luis@example.com"
                  }
                }
                """;
    }

    private BeanUser userResponse() {
        Persona persona = new Persona();
        persona.setNombre("Luis");
        persona.setApellidoPaterno("Garcia");
        persona.setApellidoMaterno("Diaz");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        persona.setSexo(Persona.Sexo.M);
        persona.setTelefono("7772589476");
        persona.setEmail("luis@example.com");

        Role role = new Role();
        role.setRole("ADMIN");

        BeanUser user = new BeanUser();
        user.setId(1L);
        user.setUsername("nuevo.usuario");
        user.setUUID("uuid-1");
        user.setActivo(true);
        user.setRol(role);
        user.setPersona(persona);
        return user;
    }
}
