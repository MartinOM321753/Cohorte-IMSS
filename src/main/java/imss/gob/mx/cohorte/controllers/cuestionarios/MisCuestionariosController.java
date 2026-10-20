package imss.gob.mx.cohorte.controllers.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.Cuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OpcionVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.services.cuestionarios.MisCuestionariosService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Lo que el propio participante usa desde su cuenta para contestar sus cuestionarios.
 *
 * <h3>Nunca recibe el id de un participante</h3>
 *
 * <p>Ninguna ruta lleva folio ni uuid de participante. El expediente se resuelve siempre
 * desde la sesión, así que no hay forma de pedir el cuestionario de otra persona ni
 * equivocándose ni a propósito. Es la diferencia con el controlador del personal, donde el
 * participante sí viaja en la URL porque quien consulta es alguien distinto de quien
 * contestó.</p>
 *
 * <h3>Sin {@code @RequireModulo}</h3>
 *
 * <p>Ese aspecto comprueba el módulo contra la institución del <em>usuario</em>, y una
 * cuenta de participante puede no tener ninguna —no es personal de ninguna sede—. La
 * comprobación se hace igual, pero contra la sede de su expediente, dentro del servicio.</p>
 */
@RestController
@RequestMapping("/api/mis-cuestionarios")
@AllArgsConstructor
@Validated
@Tag(name = "Mis cuestionarios",
        description = "El participante contesta sus propios cuestionarios, con autoguardado")
@SecurityRequirement(name = "bearerAuth")
public class MisCuestionariosController {

    private final MisCuestionariosService service;

    // ── Lo que se le muestra ─────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Mis cuestionarios", description = "El pendiente de contestar primero")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_AUTOCONTESTAR')")
    public ResponseEntity<APIResponse> mios() {
        List<MiCuestionarioResponse> dtos = service.mios().stream().map(this::aDTO).toList();
        return ResponseEntity.ok(new APIResponse(dtos,
                "Cuestionarios obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/disponibles")
    @Operation(summary = "Los que puedo empezar por mi cuenta",
            description = "Solo los que permiten autollenado, me corresponden por sexo y edad, "
                    + "y todavia no tengo")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_AUTOCONTESTAR')")
    public ResponseEntity<APIResponse> disponibles() {
        List<DisponibleResponse> dtos = service.disponibles().stream()
                .map(d -> new DisponibleResponse(
                        d.tipo().getId(), d.tipo().getNombre(), d.tipo().getDescripcion(),
                        d.indeterminado()))
                .toList();
        return ResponseEntity.ok(new APIResponse(dtos,
                "Cuestionarios disponibles obtenidos correctamente", HttpStatus.OK, false));
    }

    @PostMapping("/iniciar/{idTipo}")
    @Operation(summary = "Empezar un cuestionario por mi cuenta",
            description = "Vuelve a comprobar las tres condiciones: la pantalla solo sugiere, "
                    + "la regla vive en el servidor")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_AUTOCONTESTAR')")
    public ResponseEntity<APIResponse> iniciar(@PathVariable Long idTipo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse(
                aDTO(service.iniciar(idTipo)), "Cuestionario iniciado correctamente",
                HttpStatus.CREATED, false));
    }

    @GetMapping("/{id}/avance")
    @Operation(summary = "Los bloques con lo que lleva contestado de cada uno",
            description = "Las preguntas que su pregunta llave dejo fuera no cuentan: mostrarian "
                    + "un bloque incompleto que no se puede completar")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_AUTOCONTESTAR')")
    public ResponseEntity<APIResponse> avance(@PathVariable Long id) {
        List<AvanceResponse> dtos = service.avance(id).stream()
                .map(a -> new AvanceResponse(
                        a.seccion().getId(), a.seccion().getNombre(), a.seccion().getDescripcion(),
                        a.seccion().getOrden(), a.aplicables(), a.respondidas()))
                .toList();
        return ResponseEntity.ok(new APIResponse(dtos,
                "Avance obtenido correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/{id}/bloques/{idBloque}")
    @Operation(summary = "Un bloque para contestar")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_AUTOCONTESTAR')")
    public ResponseEntity<APIResponse> bloque(@PathVariable Long id, @PathVariable Long idBloque) {
        var b = service.bloque(id, idBloque);
        var dto = new BloqueResponse(
                b.seccion().getId(), b.seccion().getNombre(), b.seccion().getTitulo(),
                b.seccion().getDescripcion(),
                b.seccion().getPresentacion() == null ? null : b.seccion().getPresentacion().name(),
                b.variables().stream().map(this::aDTO).toList(),
                b.respuestas().stream()
                        .filter(r -> r.getOrdenRespuesta() == null || r.getOrdenRespuesta() == 0)
                        .map(this::aDTO).toList(),
                b.variablesHabilitadas().stream().toList());
        return ResponseEntity.ok(new APIResponse(dto,
                "Bloque obtenido correctamente", HttpStatus.OK, false));
    }

    // ── Autoguardado ─────────────────────────────────────────────────────────

    @PatchMapping("/{id}/respuestas")
    @Operation(summary = "Guardar los cambios del autoguardado",
            description = "Combina: nunca borra una respuesta que no venga en la lista, al contrario "
                    + "que el guardado por bloque del personal. Cada cambio lleva un contador que "
                    + "hace el guardado idempotente y a prueba de llegadas fuera de orden")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_AUTOCONTESTAR')")
    public ResponseEntity<APIResponse> autoguardar(@PathVariable Long id,
                                                   @Valid @RequestBody AutoguardarRequest dto) {
        List<MisCuestionariosService.Cambio> cambios = dto.getCambios().stream()
                .map(c -> new MisCuestionariosService.Cambio(
                        c.getIdVariable(), c.getValor(), c.getSecuencia()))
                .toList();

        var r = service.autoguardar(id, cambios);
        return ResponseEntity.ok(new APIResponse(
                new AutoguardadoResponse(r.aceptados(), r.rechazados(), r.invalidos(),
                        r.avisos().stream()
                                .map(a -> new AvisoResponse(a.idVariable(), a.codigo(),
                                        a.impideEnviar(), a.mensaje()))
                                .toList(),
                        r.guardadoEn()),
                "Cambios guardados", HttpStatus.OK, false));
    }

    // ── Envio ────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/faltantes")
    @Operation(summary = "Que obligatorias le faltan, de las que si le aplican")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_AUTOCONTESTAR')")
    public ResponseEntity<APIResponse> faltantes(@PathVariable Long id) {
        List<String> faltan = service.faltantes(id);
        return ResponseEntity.ok(new APIResponse(
                Map.of("total", faltan.size(), "codigos", faltan),
                "Faltantes obtenidos correctamente", HttpStatus.OK, false));
    }

    @PatchMapping("/{id}/enviar")
    @Operation(summary = "Dar por terminado el cuestionario",
            description = "Lo deja en COMPLETO, nunca en VALIDADO: validar es responsabilidad del personal")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_AUTOCONTESTAR')")
    public ResponseEntity<APIResponse> enviar(@PathVariable Long id) {
        return ResponseEntity.ok(new APIResponse(aDTO(service.enviar(id)),
                "Cuestionario enviado correctamente", HttpStatus.OK, false));
    }

    // ── Formas que viajan ────────────────────────────────────────────────────
    //
    // Deliberadamente mas estrechas que las del personal. Al participante no se le manda
    // la celda del volcado, el valor crudo, la marca de ausencia del escaner ni si la
    // pregunta esta sin documentar: no significan nada para el y revelan el funcionamiento
    // interno del instrumento. Reutilizar el DTO del personal habria sido mas corto y
    // habria filtrado todo eso.

    public record MiCuestionarioResponse(
            Long id, String nombre, String descripcion, String estado,
            LocalDateTime fechaRegistro, LocalDateTime fechaActualizacion,
            /** Si se puede contestar en linea. Falso para los que vienen de una hoja escaneada. */
            boolean contestableEnLinea
    ) {}

    /**
     * Un instrumento que el participante puede empezar.
     *
     * @param faltanDatos su expediente no permite comprobar del todo si le corresponde.
     *                    No lo impide; queda señalado al crearlo.
     */
    public record DisponibleResponse(
            Long idTipoCuestionario, String nombre, String descripcion, boolean faltanDatos
    ) {}

    public record AvanceResponse(
            Long idBloque, String nombre, String descripcion, Integer orden,
            int aplicables, int respondidas
    ) {}

    public record OpcionResponse(String codigo, String etiqueta) {}

    /**
     * @param dependeDe        codigo de la pregunta llave, si esta solo se hace segun lo
     *                         que se haya contestado en otra. Van tambien el valor y el
     *                         operador porque la pantalla tiene que poder resolver la
     *                         condicion sin volver a preguntarle al servidor: con solo
     *                         {@code preguntasHabilitadas}, lo que el participante marca
     *                         no habilita ni deshabilita nada hasta que guarda.
     * @param valorHabilitante el valor de la llave que hace que esta se pregunte. Con
     *                         {@code EN} son varios, separados por {@code |}.
     */
    public record PreguntaResponse(
            Long id, String codigo, String pregunta, String tipo,
            boolean obligatoria, Double valorMin, Double valorMax,
            Integer longitudMax, String unidad, List<OpcionResponse> opciones,
            String filaMatriz, String columnaMatriz,
            String instruccion, String figura,
            String dependeDe, String valorHabilitante, String operadorDependencia
    ) {}

    public record RespuestaResponse(
            Long idVariable, String valor, Long secuencia, LocalDateTime actualizadoEn
    ) {}

    public record BloqueResponse(
            Long idBloque, String nombre, String titulo, String descripcion,
            String presentacion,
            List<PreguntaResponse> preguntas,
            List<RespuestaResponse> respuestas,
            List<Long> preguntasHabilitadas
    ) {}

    public record AutoguardadoResponse(
            Map<Long, Long> aceptados,
            List<Long> rechazados,
            Map<Long, String> invalidos,
            List<AvisoResponse> avisos,
            LocalDateTime guardadoEn
    ) {}

    /**
     * Lo que hay que advertirle de lo que lleva contestado.
     *
     * <p>No impide guardar —lo guardado esta guardado— ni hace falta que lo resuelva
     * en ese momento. Se le ensena mientras contesta para que no tenga que volver
     * sobre sus pasos al final.</p>
     *
     * @param impideEnviar si ademas le va a impedir terminar el cuestionario
     */
    public record AvisoResponse(Long idPregunta, String codigo, boolean impideEnviar,
                                String mensaje) {}

    @Data
    public static class CambioRequest {
        @NotNull
        private Long idVariable;

        /** Vacio significa que borro lo que habia; no se ignora. */
        @Size(max = 1000)
        private String valor;

        /** Contador del cliente para esta respuesta. Sin el, el orden de llegada manda. */
        @NotNull
        private Long secuencia;
    }

    @Data
    public static class AutoguardarRequest {
        @NotNull
        @Size(max = 500, message = "Demasiados cambios en un solo guardado")
        @Valid
        private List<CambioRequest> cambios;
    }

    // ── Mapeo ────────────────────────────────────────────────────────────────

    private MiCuestionarioResponse aDTO(Cuestionario c) {
        var tipo = c.getTipoCuestionario();
        boolean enLinea = c.getOrigen() == imss.gob.mx.cohorte.modules.cuestionarios
                .OrigenCuestionario.APLICACION
                && c.getEstado() == imss.gob.mx.cohorte.modules.cuestionarios
                .EstadoCuestionario.BORRADOR;
        return new MiCuestionarioResponse(
                c.getId(), tipo.getNombre(), tipo.getDescripcion(),
                c.getEstado() == null ? null : c.getEstado().name(),
                c.getFechaRegistro(), c.getFechaActualizacion(), enLinea);
    }

    private PreguntaResponse aDTO(VariableCuestionario v) {
        return new PreguntaResponse(
                v.getId(), v.getCodigo(), v.getPregunta(),
                v.getTipo() == null ? null : v.getTipo().name(),
                Boolean.TRUE.equals(v.getObligatoria()),
                v.getValorMin(), v.getValorMax(), v.getLongitudMax(), v.getUnidad(),
                v.getOpciones() == null ? List.of() : v.getOpciones().stream()
                        .map(this::aDTO).toList(),
                v.getFilaMatriz(), v.getColumnaMatriz(),
                v.getInstruccion(), v.getFigura(),
                v.getDependeDe(), v.getValorHabilitante(),
                v.getOperadorDependencia() == null ? null
                        : v.getOperadorDependencia().name());
    }

    private OpcionResponse aDTO(OpcionVariable o) {
        return new OpcionResponse(o.getCodigo(), o.getEtiqueta());
    }

    /**
     * La respuesta como un solo texto, que es como la pinta el formulario.
     *
     * <p>Una respuesta ausente sale vacia a proposito: sus marcas —en blanco, cero marcas,
     * sin autorizar— describen como leyo el escaner una hoja de papel, y al participante
     * que contesta en linea no le dicen nada.</p>
     */
    private RespuestaResponse aDTO(RespuestaCuestionario r) {
        String valor = "";
        if (r.getMarcaAusencia() == MarcaAusencia.NINGUNA) {
            if (r.getValorCodigo() != null) valor = r.getValorCodigo();
            else if (r.getValorNumerico() != null) valor = textoDe(r.getValorNumerico());
            else if (r.getValorFecha() != null) valor = String.valueOf((LocalDate) r.getValorFecha());
            else if (r.getValorTexto() != null) valor = r.getValorTexto();
        }
        return new RespuestaResponse(
                r.getVariable().getId(), valor, r.getSecuenciaCliente(), r.getFechaActualizacion());
    }

    /** Sin el {@code .0} que arrastra un double: un 20 tiene que volver como "20". */
    private String textoDe(Double numero) {
        return (numero == Math.floor(numero) && !numero.isInfinite())
                ? String.valueOf(numero.longValue())
                : String.valueOf(numero);
    }
}
