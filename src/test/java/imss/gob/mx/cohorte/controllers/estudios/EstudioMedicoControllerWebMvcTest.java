package imss.gob.mx.cohorte.controllers.estudios;

import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.application.EstudiosApplicationService;
import imss.gob.mx.cohorte.application.GestionEstudiosApplicationService;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.ControllerValidationHandler;
import imss.gob.mx.cohorte.security.MainSecurity;
import imss.gob.mx.cohorte.security.filters.JWTFilter;
import imss.gob.mx.cohorte.security.jwt.JWTUtils;
import imss.gob.mx.cohorte.security.jwt.UDService;
import imss.gob.mx.cohorte.utils.Exceptions.GlobalExceptionHandler;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EstudioMedicoController.class)
@Import({MainSecurity.class, JWTFilter.class, ControllerValidationHandler.class, GlobalExceptionHandler.class})
class EstudioMedicoControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EstudiosApplicationService estudiosApplicationService;
    @MockBean
    private GestionEstudiosApplicationService gestionEstudiosApplicationService;
    @MockBean
    private UDService udService;
    @MockBean
    private JWTUtils jwtUtils;

    @Test
    @WithMockUser(roles = "USER")
    void createReturnsCreatedForAllowedRole() throws Exception {
        when(estudiosApplicationService.createEstudio(any())).thenReturn(estudioResponse());

        mockMvc.perform(post("/api/estudios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Estudio creado correctamente"))
                .andExpect(jsonPath("$.data.tipoEstudio.nombre").value("Antropometria"));
    }

    @Test
    @WithMockUser(roles = "OTHER")
    void createReturnsForbiddenForWrongRole() throws Exception {
        mockMvc.perform(post("/api/estudios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createReturnsBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/estudios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuarioRealizaUUID": "usuario-1",
                                  "idTipoEstudio": 10,
                                  "fechaEstudio": "2026-04-20"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Favor de corregir los siguientes errores"))
                .andExpect(jsonPath("$.data.pacienteUUID").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createReturnsConflictWhenApplicationRejectsRequest() throws Exception {
        when(estudiosApplicationService.createEstudio(any()))
                .thenThrow(new ObjConflictException("El tipo de estudio del parametro no coincide con el del estudio"));

        mockMvc.perform(post("/api/estudios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El tipo de estudio del parametro no coincide con el del estudio"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getByIdReturnsNotFoundWhenStudyDoesNotExist() throws Exception {
        when(estudiosApplicationService.getEstudio(9L))
                .thenThrow(new ObjNotFoundException("No se encontro el estudio medico"));

        mockMvc.perform(get("/api/estudios/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontro el estudio medico"));
    }

    private String validPayload() {
        return """
                {
                  "pacienteUUID": "paciente-1",
                  "usuarioRealizaUUID": "usuario-1",
                  "idTipoEstudio": 10,
                  "fechaEstudio": "2026-04-20",
                  "observaciones": "ok",
                  "resultados": [
                    {
                      "idParametro": 100,
                      "valorNumerico": 23.5
                    }
                  ],
                  "adjuntos": [
                    {
                      "tipo": "PDF",
                      "nombreOriginal": "reporte.pdf",
                      "mimeType": "application/pdf",
                      "rutaUrl": "/tmp/reporte.pdf",
                      "orden": 0
                    }
                  ]
                }
                """;
    }

    private EstudioMedico estudioResponse() {
        Persona personaPaciente = new Persona();
        personaPaciente.setNombre("Ana");
        personaPaciente.setApellidoPaterno("Perez");
        personaPaciente.setApellidoMaterno("Lopez");
        personaPaciente.setSexo(Persona.Sexo.F);

        Paciente paciente = new Paciente();
        paciente.setId(1L);
        paciente.setUuid("paciente-1");
        paciente.setFolio("F001");
        paciente.setPersona(personaPaciente);

        Persona personaUsuario = new Persona();
        personaUsuario.setNombre("Luis");
        personaUsuario.setApellidoPaterno("Garcia");
        personaUsuario.setApellidoMaterno("Diaz");

        BeanUser usuario = new BeanUser();
        usuario.setUUID("usuario-1");
        usuario.setUsername("lgarcia");
        usuario.setPersona(personaUsuario);

        TipoEstudio tipo = new TipoEstudio();
        tipo.setId(10L);
        tipo.setNombre("Antropometria");
        tipo.setActivo(true);
        tipo.setParametros(new ArrayList<>());

        ResultadoEstudio resultado = new ResultadoEstudio();
        resultado.setId(5L);
        resultado.setValorNumerico(23.5);

        EstudioMedico estudio = new EstudioMedico();
        estudio.setId(1L);
        estudio.setFechaEstudio(LocalDate.of(2026, 4, 20));
        estudio.setPaciente(paciente);
        estudio.setUsuarioRealiza(usuario);
        estudio.setTipoEstudio(tipo);
        estudio.setResultadoEstudio(new ArrayList<>());
        estudio.getResultadoEstudio().add(resultado);
        estudio.setAdjuntos(new ArrayList<>());
        return estudio;
    }
}
