package imss.gob.mx.cohorte.controllers.pacientes;

import imss.gob.mx.cohorte.application.PacienteApplicationService;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
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

@WebMvcTest(controllers = PacienteController.class)
@Import({MainSecurity.class, JWTFilter.class, ControllerValidationHandler.class, GlobalExceptionHandler.class})
class PacienteControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PacienteApplicationService pacienteApplicationService;
    @MockBean
    private UDService udService;
    @MockBean
    private JWTUtils jwtUtils;

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReturnsOk() throws Exception {
        when(pacienteApplicationService.updateUser(any())).thenReturn(pacienteResponse());

        mockMvc.perform(put("/api/pacientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.folio").value("F-2026"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReturnsBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(put("/api/pacientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "folio": "",
                                  "persona": {}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.folio").exists());
    }

    private String validPayload() {
        return """
                {
                  "folio": "F-2026",
                  "persona": {
                    "nombre": "Ana",
                    "apellidoPaterno": "Perez",
                    "apellidoMaterno": "Lopez",
                    "fechaNacimiento": "1988-05-10",
                    "sexo": "F",
                    "telefono": "7772589476",
                    "email": "ana@example.com"
                  }
                }
                """;
    }

    private Paciente pacienteResponse() {
        Persona persona = new Persona();
        persona.setId(10L);
        persona.setNombre("Ana");
        persona.setApellidoPaterno("Perez");
        persona.setApellidoMaterno("Lopez");
        persona.setFechaNacimiento(LocalDate.of(1988, 5, 10));
        persona.setSexo(Persona.Sexo.F);
        persona.setTelefono("7772589476");
        persona.setEmail("ana@example.com");

        Paciente paciente = new Paciente();
        paciente.setId(1L);
        paciente.setUuid("paciente-1");
        paciente.setFolio("F-2026");
        paciente.setActivo(true);
        paciente.setPersona(persona);
        return paciente;
    }
}
