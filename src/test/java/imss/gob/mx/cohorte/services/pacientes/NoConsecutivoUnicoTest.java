package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.services.pacientes.FolioGeneratorService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * El número consecutivo del participante: opcional, pero único cuando viene.
 *
 * <p>Lo que se fija aquí no es que la comprobación exista —el índice único de la
 * columna la respalda de todas formas— sino cuándo NO debe dispararse. Comprobar
 * de más rompe cosas que sí tienen que funcionar: guardar un participante sin
 * tocarle el número chocaría contra su propio registro, y varios participantes sin
 * número tienen que poder convivir.</p>
 */
class NoConsecutivoUnicoTest {

    private PacienteRepository repositorio;
    private FolioGeneratorService folios;
    private PacienteService servicio;

    private static final Long INSTITUCION = 1L;

    @BeforeEach
    void setUp() {
        repositorio = mock(PacienteRepository.class);
        folios = mock(FolioGeneratorService.class);
        servicio = new PacienteService(repositorio, folios);

        when(folios.generarFolio()).thenReturn("COH-26-00001");
        when(folios.normalizar(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(repositorio.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Paciente participante(Long id, String folio, Long noConsecutivo) {
        Paciente p = new Paciente();
        p.setId(id);
        p.setFolio(folio);
        p.setNoConsecutivo(noConsecutivo);
        p.setActivo(true);

        Persona persona = new Persona();
        persona.setNombre("Ana");
        persona.setApellidoPaterno("López");
        persona.setCurp("LOAN900101MDFPNA01");
        persona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        persona.setSexo(Persona.Sexo.F);
        p.setPersona(persona);

        Institucion institucion = new Institucion();
        institucion.setId(INSTITUCION);
        p.setInstitucion(institucion);
        return p;
    }

    // ── Al registrar ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Al registrar, un consecutivo ya usado se rechaza")
    void rechazaConsecutivoRepetidoAlRegistrar() {
        when(repositorio.existsByNoConsecutivo(42L)).thenReturn(true);

        ObjConflictException error = assertThrows(ObjConflictException.class,
                () -> servicio.cretePatient(participante(null, "COH-26-00002", 42L)));

        assertTrue(error.getMessage().contains("42"), "El mensaje debe decir cuál es el número en conflicto");
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("Al registrar sin consecutivo no se consulta la unicidad")
    void sinConsecutivoNoPregunta() {
        servicio.cretePatient(participante(null, "COH-26-00002", null));

        // Varios participantes sin número tienen que poder convivir: preguntar por
        // null encontraría al primero que tampoco lo tiene y bloquearía a todos los
        // demás.
        verify(repositorio, never()).existsByNoConsecutivo(any());
        verify(repositorio).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Al registrar, un consecutivo libre se guarda")
    void aceptaConsecutivoLibre() {
        when(repositorio.existsByNoConsecutivo(42L)).thenReturn(false);

        Paciente guardado = servicio.cretePatient(participante(null, "COH-26-00002", 42L));

        assertEquals(42L, guardado.getNoConsecutivo());
    }

    // ── Al actualizar ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Guardar sin cambiar el número no choca contra el propio registro")
    void elMismoNumeroNoChocaConsigoMismo() {
        Paciente enBD = participante(7L, "COH-26-00007", 42L);
        when(repositorio.findByIdAndInstitucion_Id(7L, INSTITUCION)).thenReturn(Optional.of(enBD));
        when(repositorio.existsByNoConsecutivo(42L)).thenReturn(true);   // sí existe: es él

        Paciente resultado = servicio.updatePatient(participante(7L, "COH-26-00007", 42L), INSTITUCION);

        assertEquals(42L, resultado.getNoConsecutivo());
        verify(repositorio, never()).existsByNoConsecutivo(any());
    }

    @Test
    @DisplayName("Cambiar a un número ya usado por otro se rechaza")
    void rechazaConsecutivoDeOtroAlActualizar() {
        Paciente enBD = participante(7L, "COH-26-00007", 42L);
        when(repositorio.findByIdAndInstitucion_Id(7L, INSTITUCION)).thenReturn(Optional.of(enBD));
        when(repositorio.existsByNoConsecutivo(99L)).thenReturn(true);

        assertThrows(ObjConflictException.class,
                () -> servicio.updatePatient(participante(7L, "COH-26-00007", 99L), INSTITUCION));

        assertEquals(42L, enBD.getNoConsecutivo(), "El número anterior no se toca si el nuevo se rechaza");
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("Cambiar a un número libre lo guarda")
    void aceptaCambioAConsecutivoLibre() {
        Paciente enBD = participante(7L, "COH-26-00007", 42L);
        when(repositorio.findByIdAndInstitucion_Id(7L, INSTITUCION)).thenReturn(Optional.of(enBD));
        when(repositorio.existsByNoConsecutivo(99L)).thenReturn(false);

        Paciente resultado = servicio.updatePatient(participante(7L, "COH-26-00007", 99L), INSTITUCION);

        assertEquals(99L, resultado.getNoConsecutivo());
    }

    @Test
    @DisplayName("Mandarlo vacío quita el número, sin preguntar por la unicidad de nada")
    void vaciarloLoQuita() {
        Paciente enBD = participante(7L, "COH-26-00007", 42L);
        when(repositorio.findByIdAndInstitucion_Id(7L, INSTITUCION)).thenReturn(Optional.of(enBD));

        Paciente resultado = servicio.updatePatient(participante(7L, "COH-26-00007", null), INSTITUCION);

        assertNull(resultado.getNoConsecutivo(), "Un número mal capturado tiene que poder deshacerse");
        verify(repositorio, never()).existsByNoConsecutivo(any());
    }

    @Test
    @DisplayName("Asignar un número a quien no tenía sí comprueba la unicidad")
    void asignarloPorPrimeraVezComprueba() {
        Paciente enBD = participante(7L, "COH-26-00007", null);
        when(repositorio.findByIdAndInstitucion_Id(7L, INSTITUCION)).thenReturn(Optional.of(enBD));
        when(repositorio.existsByNoConsecutivo(42L)).thenReturn(true);

        assertThrows(ObjConflictException.class,
                () -> servicio.updatePatient(participante(7L, "COH-26-00007", 42L), INSTITUCION));
    }
}
