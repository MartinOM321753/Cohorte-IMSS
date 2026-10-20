package imss.gob.mx.cohorte.controllers.cuestionarios;

import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;

import imss.gob.mx.cohorte.controllers.cuestionarios.dto.CuestionarioDTOs.*;
import imss.gob.mx.cohorte.controllers.cuestionarios.dto.CuestionarioMapper;
import imss.gob.mx.cohorte.modules.cuestionarios.Cuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.OrigenCuestionario;
import imss.gob.mx.cohorte.services.cuestionarios.CriterioAplicabilidad;
import imss.gob.mx.cohorte.services.cuestionarios.CuestionarioService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequireModulo(ModuloSistema.CUESTIONARIOS)
@RequestMapping("/api/cuestionarios")
@AllArgsConstructor
@Validated
@Tag(name = "Cuestionarios", description = "Cuestionarios contestados: llenado, cierre y validacion")
@SecurityRequirement(name = "bearerAuth")
public class CuestionarioController {

    private static final int MAX_POR_PAGINA = 100;

    private final CuestionarioService service;

    @GetMapping
    @Operation(summary = "Listar cuestionarios contestados")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_LLENADO_ACCEDER')")
    public ResponseEntity<APIResponse> listar(
            @RequestParam(required = false) Long idTipo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {

        Page<Cuestionario> resultado = service.listar(idTipo,
                PageRequest.of(Math.max(0, pagina), Math.min(Math.max(1, tamano), MAX_POR_PAGINA)));
        return ResponseEntity.ok(new APIResponse(pagina(resultado),
                "Cuestionarios obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/pendientes")
    @Operation(summary = "Cuestionarios con lecturas que hay que resolver",
            description = "Es la bandeja de recaptura: celdas que el reconocimiento de escritura no leyo")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_LLENADO_ACCEDER')")
    public ResponseEntity<APIResponse> pendientes(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {

        Page<Cuestionario> resultado = service.conPendientes(
                PageRequest.of(Math.max(0, pagina), Math.min(Math.max(1, tamano), MAX_POR_PAGINA)));
        return ResponseEntity.ok(new APIResponse(pagina(resultado),
                "Cuestionarios con pendientes obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/participante/{uuid}")
    @Operation(summary = "Cuestionarios de un participante")
    @PreAuthorize("hasAnyAuthority('CUESTIONARIOS_LLENADO_ACCEDER', 'EXPEDIENTE_CUESTIONARIOS')")
    public ResponseEntity<APIResponse> deParticipante(@PathVariable String uuid) {
        return ResponseEntity.ok(new APIResponse(
                service.deParticipante(uuid).stream().map(CuestionarioMapper::cuestionario).toList(),
                "Cuestionarios del participante obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/tipos-para/{uuid}")
    @Operation(summary = "Que cuestionarios le tocan a un participante",
            description = "Devuelve TODOS los activos con su veredicto, no solo los que aplican: "
                    + "esconder los que no aplican dejaria sin saber por que falta el que se esperaba, "
                    + "y el motivo suele ser un dato del expediente que falta")
    @PreAuthorize("hasAnyAuthority('CUESTIONARIOS_CREAR', 'CUESTIONARIOS_LLENADO_ACCEDER')")
    public ResponseEntity<APIResponse> tiposPara(@PathVariable String uuid) {
        // Forma delgada a proposito: aqui no se consultan los conteos del catalogo, y
        // devolver el DTO completo con ceros pondria en pantalla «0 preguntas» para un
        // instrumento que tiene dos mil.
        List<TipoParaParticipanteResponse> dtos = service.tiposPara(uuid).stream()
                .map(t -> new TipoParaParticipanteResponse(
                        t.tipo().getId(), t.tipo().getClave(), t.tipo().getNombre(),
                        t.tipo().getVersion(),
                        CriterioAplicabilidad.publicoComoTexto(t.tipo()),
                        t.veredicto().aplica(),
                        t.veredicto().indeterminado(),
                        t.veredicto().motivo(),
                        t.veredicto().edad(),
                        t.yaRegistrado()))
                .toList();
        return ResponseEntity.ok(new APIResponse(dtos,
                "Cuestionarios del participante obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un cuestionario")
    @PreAuthorize("hasAnyAuthority('CUESTIONARIOS_LLENADO_ACCEDER', 'EXPEDIENTE_CUESTIONARIOS')")
    public ResponseEntity<APIResponse> uno(@PathVariable Long id) {
        return ResponseEntity.ok(new APIResponse(
                CuestionarioMapper.cuestionario(service.uno(id)),
                "Cuestionario obtenido correctamente", HttpStatus.OK, false));
    }

    @PostMapping
    @Operation(summary = "Abrir un cuestionario para contestar")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_CREAR')")
    public ResponseEntity<APIResponse> abrir(@Valid @RequestBody AbrirRequest dto) {
        OrigenCuestionario origen = dto.getOrigen() == null
                ? OrigenCuestionario.APLICACION : OrigenCuestionario.valueOf(dto.getOrigen());
        Cuestionario creado = service.abrir(dto.getIdTipoCuestionario(),
                dto.getUuidParticipante(), origen);
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse(
                CuestionarioMapper.cuestionario(creado), "Cuestionario abierto correctamente",
                HttpStatus.CREATED, false));
    }

    // ── Llenado por bloques ──────────────────────────────────────────────────

    @GetMapping("/{id}/bloques/{idBloque}")
    @Operation(summary = "Un bloque del formulario",
            description = "Trae las preguntas, lo contestado y cuales estan habilitadas por su pregunta llave")
    @PreAuthorize("hasAnyAuthority('CUESTIONARIOS_LLENADO_ACCEDER', 'EXPEDIENTE_CUESTIONARIOS')")
    public ResponseEntity<APIResponse> bloque(@PathVariable Long id, @PathVariable Long idBloque) {
        return ResponseEntity.ok(new APIResponse(aDTO(service.bloque(id, idBloque)),
                "Bloque obtenido correctamente", HttpStatus.OK, false));
    }

    @PutMapping("/{id}/bloques/{idBloque}")
    @Operation(summary = "Guardar lo contestado en un bloque",
            description = "Se guarda el bloque completo: lo que no venga en la lista se borra, "
                    + "que es la unica forma de que desmarcar una casilla tenga efecto")
    @PreAuthorize("hasAnyAuthority('CUESTIONARIOS_CREAR', 'CUESTIONARIOS_EDITAR')")
    public ResponseEntity<APIResponse> guardarBloque(
            @PathVariable Long id, @PathVariable Long idBloque,
            @Valid @RequestBody GuardarBloqueRequest dto) {

        List<CuestionarioService.RespuestaEntrante> entrantes =
                dto.getRespuestas() == null ? List.of() : dto.getRespuestas().stream()
                        .map(r -> new CuestionarioService.RespuestaEntrante(
                                r.getIdVariable(), r.getOrdenRespuesta(), r.getValor()))
                        .toList();

        return ResponseEntity.ok(new APIResponse(aDTO(service.guardarBloque(id, idBloque, entrantes)),
                "Bloque guardado correctamente", HttpStatus.OK, false));
    }

    // ── Cierre ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}/faltantes")
    @Operation(summary = "Que falta para poder cerrar",
            description = "Solo las obligatorias que la pregunta llave si habilito")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_LLENADO_ACCEDER')")
    public ResponseEntity<APIResponse> faltantes(@PathVariable Long id) {
        List<String> faltan = service.faltantes(id);
        return ResponseEntity.ok(new APIResponse(Map.of("total", faltan.size(), "codigos", faltan),
                "Faltantes obtenidos correctamente", HttpStatus.OK, false));
    }

    @PatchMapping("/{id}/cerrar")
    @Operation(summary = "Dar el cuestionario por completo")
    @PreAuthorize("hasAnyAuthority('CUESTIONARIOS_CREAR', 'CUESTIONARIOS_EDITAR')")
    public ResponseEntity<APIResponse> cerrar(@PathVariable Long id) {
        return ResponseEntity.ok(new APIResponse(CuestionarioMapper.cuestionario(service.cerrar(id)),
                "Cuestionario cerrado correctamente", HttpStatus.OK, false));
    }

    @PatchMapping("/{id}/validar")
    @Operation(summary = "Dar el cuestionario por revisado contra la hoja original",
            description = "Exige que no queden lecturas sin resolver: no se puede responder por una celda que el escaner nunca leyo")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_VALIDAR')")
    public ResponseEntity<APIResponse> validar(@PathVariable Long id,
                                               @Valid @RequestBody(required = false) NotaRequest dto) {
        return ResponseEntity.ok(new APIResponse(
                CuestionarioMapper.cuestionario(service.validar(id, dto == null ? null : dto.getNota())),
                "Cuestionario validado correctamente", HttpStatus.OK, false));
    }

    @PatchMapping("/{id}/reabrir")
    @Operation(summary = "Devolver un cuestionario validado a borrador")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_VALIDAR')")
    public ResponseEntity<APIResponse> reabrir(@PathVariable Long id) {
        return ResponseEntity.ok(new APIResponse(CuestionarioMapper.cuestionario(service.reabrir(id)),
                "Cuestionario reabierto correctamente", HttpStatus.OK, false));
    }

    @PatchMapping("/{id}/anular")
    @Operation(summary = "Retirar el cuestionario del analisis",
            description = "Conserva lo capturado: la hoja de papel existe y alguien la contesto")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_ELIMINAR')")
    public ResponseEntity<APIResponse> anular(@PathVariable Long id,
                                              @Valid @RequestBody AnularRequest dto) {
        return ResponseEntity.ok(new APIResponse(
                CuestionarioMapper.cuestionario(service.anular(id, dto.getMotivo())),
                "Cuestionario anulado", HttpStatus.OK, false));
    }

    // ── Lecturas pendientes y en desacuerdo ──────────────────────────────────

    @GetMapping("/{id}/lecturas-pendientes")
    @Operation(summary = "Respuestas que el escaner no pudo leer")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_LLENADO_ACCEDER')")
    public ResponseEntity<APIResponse> lecturasPendientes(@PathVariable Long id) {
        return ResponseEntity.ok(new APIResponse(
                service.pendientesDeLectura(id).stream().map(CuestionarioMapper::respuesta).toList(),
                "Lecturas pendientes obtenidas correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/{id}/discrepancias")
    @Operation(summary = "Celdas con mas de una lectura",
            description = "La hoja Duplicados del volcado: desacuerdos sin adjudicar, no correcciones aplicadas")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_LLENADO_ACCEDER')")
    public ResponseEntity<APIResponse> discrepancias(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean soloSinResolver) {
        return ResponseEntity.ok(new APIResponse(
                service.discrepancias(id, soloSinResolver).stream()
                        .map(CuestionarioMapper::discrepancia).toList(),
                "Discrepancias obtenidas correctamente", HttpStatus.OK, false));
    }

    @PatchMapping("/discrepancias/{idDiscrepancia}")
    @Operation(summary = "Adjudicar una celda en desacuerdo",
            description = "Escribe tambien la respuesta: anotar la decision sin aplicarla dejaria la base diciendo otra cosa")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_VALIDAR')")
    public ResponseEntity<APIResponse> resolver(@PathVariable Long idDiscrepancia,
                                                @Valid @RequestBody ResolverRequest dto) {
        return ResponseEntity.ok(new APIResponse(
                CuestionarioMapper.discrepancia(service.resolverDiscrepancia(
                        idDiscrepancia, dto.getValorElegido(), dto.getNota())),
                "Lectura adjudicada correctamente", HttpStatus.OK, false));
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    private Map<String, Object> pagina(Page<Cuestionario> resultado) {
        return Map.of(
                "contenido", resultado.getContent().stream()
                        .map(CuestionarioMapper::cuestionario).toList(),
                "pagina", resultado.getNumber(),
                "tamano", resultado.getSize(),
                "total", resultado.getTotalElements(),
                "totalPaginas", resultado.getTotalPages());
    }

    private BloqueResponse aDTO(CuestionarioService.BloqueLleno bloque) {
        return new BloqueResponse(
                CuestionarioMapper.seccion(bloque.seccion(), bloque.variables().size()),
                bloque.variables().stream().map(CuestionarioMapper::variable).toList(),
                bloque.respuestas().stream().map(CuestionarioMapper::respuesta).toList(),
                List.copyOf(bloque.variablesHabilitadas()));
    }
}
