package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.persona.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PacienteImportRowService {

    private final PacienteRepository pacienteRepository;
    private final PersonaRepository personaRepository;
    private final FolioGeneratorService folioGeneratorService;

    public enum Estado { EXITOSO, DUPLICADO, ERROR, ADVERTENCIA }

    public record Resultado(Estado estado, String folio, String motivo) {}

    /**
     * Cada fila se procesa en su propia transaccion (REQUIRES_NEW), aislada de
     * las demas filas del lote. Esto evita dos problemas: (1) generarFolio()
     * corre en su propia transaccion y solo ve folios ya confirmados — si todo
     * el lote compartiera una transaccion, nunca veria los folios asignados a
     * filas previas del mismo lote y repetiria el mismo folio; (2) si una fila
     * falla (ej. folio duplicado), solo se revierte esa fila — no deja la
     * sesion de Hibernate en estado inconsistente para las filas siguientes.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Resultado guardarFila(Map<String, String> fila, Institucion institucion) {
        String folioRaw = fila.getOrDefault("folio", "").trim();

        String nombreVal = normalizarCampoNombre(fila.getOrDefault("nombre", ""));

        String segundoNombreVal = fila.getOrDefault("segundoNombre", "").trim();
        String tercerNombreVal = fila.getOrDefault("tercerNombre", "").trim();
        if (!tercerNombreVal.isEmpty()) {
            segundoNombreVal = (segundoNombreVal + " " + tercerNombreVal).trim();
        }
        segundoNombreVal = normalizarCampoNombre(segundoNombreVal);

        String apPaterno = normalizarCampoNombre(fila.getOrDefault("apellidoPaterno", ""));
        String apMaterno = normalizarCampoNombre(fila.getOrDefault("apellidoMaterno", ""));

        if (nombreVal == null || apPaterno == null) {
            return new Resultado(Estado.ERROR, folioRaw, "Nombre y apellido paterno son obligatorios");
        }

        String folio;
        if (folioRaw.isEmpty()) {
            folio = folioGeneratorService.generarFolio();
        } else {
            folio = folioGeneratorService.normalizar(folioRaw);
            if (pacienteRepository.existsByFolio(folio)) {
                return new Resultado(Estado.DUPLICADO, folio, "El folio ya existe");
            }
        }

        String curp = fila.getOrDefault("curp", "").trim().toUpperCase();
        boolean curpDuplicadoEnBD = false;
        if (!curp.isEmpty()) {
            if (curp.length() > 18) {
                return new Resultado(Estado.ERROR, folio, "El CURP '" + curp + "' excede los 18 caracteres permitidos");
            }
            curpDuplicadoEnBD = personaRepository.existsByCurp(curp);
        }

        String emailVal = fila.getOrDefault("email", "").trim();
        if (!emailVal.isEmpty() && personaRepository.findByEmail(emailVal).isPresent()) {
            return new Resultado(Estado.DUPLICADO, folio, "El email '" + emailVal + "' ya existe");
        }

        String telefonoVal = fila.getOrDefault("telefono", "").trim();
        if (!telefonoVal.isEmpty() && personaRepository.findByTelefono(telefonoVal).isPresent()) {
            return new Resultado(Estado.DUPLICADO, folio, "El teléfono '" + telefonoVal + "' ya existe");
        }

        Persona persona = new Persona();
        persona.setNombre(nombreVal);
        persona.setSegundoNombre(segundoNombreVal);
        persona.setApellidoPaterno(apPaterno);
        persona.setApellidoMaterno(apMaterno);
        persona.setCurp(curp.isEmpty() ? null : curp);
        persona.setTelefono(telefonoVal.isEmpty() ? null : telefonoVal);
        persona.setEmail(emailVal.isEmpty() ? null : emailVal);

        String fechaNacStr = fila.getOrDefault("fechaNacimiento", "").trim();
        if (!fechaNacStr.isEmpty()) {
            persona.setFechaNacimiento(parsearFecha(fechaNacStr));
        }

        String sexoStr = fila.getOrDefault("sexo", "").trim().toUpperCase();
        if ("M".equals(sexoStr) || "F".equals(sexoStr)) {
            persona.setSexo(Persona.Sexo.valueOf(sexoStr));
        }

        String estadoStr = fila.getOrDefault("estado", "").trim();
        boolean activo = "ACTIVO".equalsIgnoreCase(estadoStr);

        persona.setFechaRegistro(LocalDateTime.now());
        persona.setFechaActualizacion(LocalDateTime.now());
        persona = personaRepository.save(persona);

        Paciente paciente = new Paciente();
        paciente.setFolio(folio);
        paciente.setPersona(persona);
        paciente.setInstitucion(institucion);
        paciente.setActivo(activo);
        paciente.setFechaRegistro(LocalDateTime.now());
        paciente.setFechaActualizacion(LocalDateTime.now());
        paciente.setUuid(UUID.randomUUID().toString());
        pacienteRepository.save(paciente);

        if (curpDuplicadoEnBD) {
            return new Resultado(Estado.ADVERTENCIA, folio, "El CURP '" + curp + "' ya existe en otro registro");
        }
        return new Resultado(Estado.EXITOSO, folio, null);
    }

    private LocalDate parsearFecha(String valor) {
        List<DateTimeFormatter> formatos = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
        );
        for (DateTimeFormatter fmt : formatos) {
            try {
                return LocalDate.parse(valor, fmt);
            } catch (Exception ignored) {}
        }
        throw new RuntimeException("Formato de fecha no reconocido: " + valor);
    }

    private String emptyToNull(String valor) {
        if (valor == null) return null;
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String sanitizarNombre(String valor) {
        if (valor == null) return null;
        return valor.replaceAll("[^\\p{L}\\s']", "").replaceAll("\\s+", " ").trim();
    }

    private String titleCase(String valor) {
        if (valor == null || valor.isBlank()) return valor;
        String[] palabras = valor.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : palabras) {
            if (!p.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) sb.append(p.substring(1));
            }
        }
        return sb.toString();
    }

    private String normalizarCampoNombre(String valor) {
        if (valor == null) return null;
        String limpio = sanitizarNombre(valor.trim());
        if (limpio == null || limpio.isEmpty()) return null;
        return titleCase(limpio);
    }
}
