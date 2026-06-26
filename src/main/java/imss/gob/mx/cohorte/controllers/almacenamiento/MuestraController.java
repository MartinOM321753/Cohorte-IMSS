package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.MuestraApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.*;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/almacenamiento/muestras")
@AllArgsConstructor
@Tag(name = "Muestras", description = "Gestión de muestras biológicas")
@SecurityRequirement(name = "bearerAuth")
public class  MuestraController {

    private final MuestraApplicationService muestraApplicationService;

    @GetMapping
    @Operation(summary = "Listar todas las muestras", description = "Obtiene una lista completa de todas las muestras biológicas registradas en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAll() {
        List<Muestra> list = muestraApplicationService.getAllMuestras();
        return ResponseEntity.ok(new APIResponse("Muestras encontradas", MuestraMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/paginado")
    @Operation(summary = "Listar muestras paginadas", description = "Obtiene las muestras en páginas (parámetros estándar de Spring: page, size, sort) para evitar cargar toda la tabla en una sola respuesta")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAllPaginado(Pageable pageable) {
        Page<Muestra> page = muestraApplicationService.getAllMuestrasPaginado(pageable);
        Map<String, Object> body = Map.of(
            "content", MuestraMapper.toResponseDTOList(page.getContent()),
            "page", page.getNumber(),
            "size", page.getSize(),
            "totalElements", page.getTotalElements(),
            "totalPages", page.getTotalPages()
        );
        return ResponseEntity.ok(new APIResponse("Muestras encontradas", body, false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener muestra por ID", description = "Obtiene los detalles de una muestra biológica específica mediante su identificador único")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getById(
        @Parameter(description = "ID numérico de la muestra biológica", required = true)
        @PathVariable Long id) {
        Muestra muestra = muestraApplicationService.getMuestra(id);
        return ResponseEntity.ok(new APIResponse("Muestra encontrada", MuestraMapper.toResponseDTO(muestra), false, HttpStatus.OK));
    }

    @GetMapping("/paciente/uuid/{uuid}/count")
    @Operation(summary = "Contar muestras de un paciente por UUID")
    public ResponseEntity<APIResponse> countByPacienteUUID(@PathVariable String uuid) {
        long count = muestraApplicationService.countMuestrasByPacienteUuid(uuid);
        return ResponseEntity.ok(new APIResponse("Conteo de muestras", count, false, HttpStatus.OK));
    }

    @GetMapping("/paciente/uuid/{uuid}")
    @Operation(summary = "Obtener muestras de un paciente por UUID", description = "Obtiene todas las muestras biológicas asociadas a un paciente específico mediante su UUID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getByPacienteUUID(
        @Parameter(description = "UUID único del paciente", required = true)
        @PathVariable String uuid) {
        List<Muestra> list = muestraApplicationService.getMuestrasByPacienteUUID(uuid);
        return ResponseEntity.ok(new APIResponse("Muestras del participante encontradas", MuestraMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Registrar nueva muestra", description = "Registra una nueva muestra biológica en el sistema con su ubicación, paciente y usuario recolector")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Recurso creado exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> create(@Validated @RequestBody MuestraRequestDTO dto) {
        if (dto.getPacienteUUID() == null || dto.getPacienteUUID().isBlank()) {
            throw new imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException("El UUID del participante es obligatorio");
        }
        if (dto.getUsuarioRecolectaUUID() == null || dto.getUsuarioRecolectaUUID().isBlank()) {
            throw new imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException("El UUID del usuario que recolecta es obligatorio");
        }
        if (dto.getIdTipoMuestra() == null) {
            throw new imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException("El tipo de muestra es obligatorio");
        }
        if (dto.getIdTuboMuestra() == null) {
            throw new imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException("El tubo de muestra es obligatorio");
        }
        Muestra entity = MuestraMapper.toEntity(dto);
        if (dto.getIdPosicionCaja() != null) {
            PosicionCaja pos = new PosicionCaja();
            pos.setId(dto.getIdPosicionCaja());
            entity.setPosicionCaja(pos);
        }
        Paciente paciente = new Paciente();
        paciente.setUuid(dto.getPacienteUUID());
        entity.setPaciente(paciente);

        BeanUser usuario = new BeanUser();
        usuario.setUUID(dto.getUsuarioRecolectaUUID());
        entity.setUsuarioRecolecta(usuario);

        if (dto.getIdTipoMuestra() != null) {
            TipoMuestra tm = new TipoMuestra();
            tm.setId(dto.getIdTipoMuestra());
            entity.setTipoMuestra(tm);
        }
        if (dto.getIdTuboMuestra() != null) {
            TuboMuestra tb = new TuboMuestra();
            tb.setId(dto.getIdTuboMuestra());
            entity.setTuboMuestra(tb);
        }

        Muestra saved = muestraApplicationService.createMuestra(entity);
        MuestraResponseDTO responseDTO = MuestraMapper.toResponseDTO(saved);
        // Si el tubo generó alícuotas, indicarlo en la respuesta
        if (saved.getTuboMuestra() != null
                && saved.getTuboMuestra().getNumeroAlicuotas() != null
                && saved.getTuboMuestra().getNumeroAlicuotas() > 0) {
            responseDTO.setAlicuotasGeneradas(saved.getTuboMuestra().getNumeroAlicuotas());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Muestra registrada exitosamente", responseDTO, false, HttpStatus.CREATED));
    }

    @GetMapping("/biobanco")
    @Operation(summary = "Muestras en el biobanco de mi institución",
               description = "Muestras cuyo tenedor actual es la institución del usuario logueado (incluye SIN_POSICION, EN_BIOBANCO y PRESTADAS recibidas).")
    public ResponseEntity<APIResponse> getMuestrasEnBiobanco(Pageable pageable) {
        Page<Muestra> page = muestraApplicationService.getMuestrasEnBiobancoPage(pageable);
        Map<String, Object> body = Map.of(
            "content", MuestraMapper.toResponseDTOList(page.getContent()),
            "page", page.getNumber(),
            "size", page.getSize(),
            "totalElements", page.getTotalElements(),
            "totalPages", page.getTotalPages()
        );
        return ResponseEntity.ok(new APIResponse("Muestras en biobanco", body, false, HttpStatus.OK));
    }

    @GetMapping("/{id}/alicuotas")
    @Operation(summary = "Alícuotas de una muestra padre",
               description = "Retorna las alícuotas derivadas de una muestra primaria, con su estado y posición actuales.")
    public ResponseEntity<APIResponse> getAlicuotas(@PathVariable Long id) {
        List<Muestra> alicuotas = muestraApplicationService.getAlicuotas(id);
        return ResponseEntity.ok(new APIResponse("Alícuotas encontradas",
            MuestraMapper.toResponseDTOList(alicuotas), false, HttpStatus.OK));
    }

    @PutMapping("/{id}/posicion")
    @Operation(summary = "Asignar / mover posición en biobanco",
               description = "Asigna o cambia la PosicionCaja de la muestra dentro del biobanco de su institución actual. Libera la posición anterior automáticamente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Posición asignada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "Posición ocupada o muestra prestada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> asignarPosicion(
            @PathVariable Long id,
            @Validated @RequestBody AsignarPosicionRequestDTO dto) {
        Muestra updated = muestraApplicationService.asignarPosicion(id, dto.getIdPosicionCaja(), dto.getMotivo());
        return ResponseEntity.ok(new APIResponse("Posición asignada",
            MuestraMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}/posicion")
    @Operation(summary = "Liberar posición en biobanco",
               description = "Libera la PosicionCaja actual de la muestra sin asignarla a otra. La muestra pasa a SIN_POSICION.")
    public ResponseEntity<APIResponse> liberarPosicion(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo) {
        Muestra updated = muestraApplicationService.liberarPosicion(id, motivo);
        return ResponseEntity.ok(new APIResponse("Posición liberada",
            MuestraMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar muestra",
               description = "Elimina una muestra y sus alícuotas. Requiere que ninguna alícuota tenga posición asignada y que la muestra no esté en préstamo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Muestra eliminada"),
        @ApiResponse(responseCode = "409", description = "No se puede eliminar (alícuota con posición o muestra prestada)")
    })
    public ResponseEntity<APIResponse> delete(@PathVariable Long id) {
        muestraApplicationService.deleteMuestra(id);
        return ResponseEntity.ok(new APIResponse("Muestra eliminada exitosamente", null, false, HttpStatus.OK));
    }

    @PostMapping("/{id}/generar-alicuotas")
    @Operation(summary = "Generar alícuotas en institución receptora",
               description = "Genera alícuotas de una muestra padre recibida usando un tipo+tubo seleccionado por la institución receptora.")
    public ResponseEntity<APIResponse> generarAlicuotasEnReceptora(
            @PathVariable Long id,
            @Validated @RequestBody GenerarAlicuotasRequestDTO dto) {
        List<Muestra> alicuotas = muestraApplicationService.generarAlicuotasEnReceptora(
            id, dto.getIdTipoMuestra(), dto.getIdTuboMuestra());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Alícuotas generadas",
                MuestraMapper.toResponseDTOList(alicuotas), false, HttpStatus.CREATED));
    }

    @GetMapping("/{id}/tipo-institucion")
    @Operation(summary = "Tipo/tubo asignado por mi institución a esta muestra",
               description = "Obtiene el tipo y tubo que mi institución asignó a una muestra padre recibida.")
    public ResponseEntity<APIResponse> getTipoInstitucion(@PathVariable Long id) {
        var mapping = muestraApplicationService.getTipoInstitucion(id);
        if (mapping.isEmpty()) {
            return ResponseEntity.ok(new APIResponse("Sin tipo asignado por esta institución", null, false, HttpStatus.OK));
        }
        var mti = mapping.get();
        MuestraTipoInstitucionResponseDTO dto = MuestraTipoInstitucionResponseDTO.builder()
            .id(mti.getId())
            .tipoMuestra(TipoMuestraResumenDTO.builder()
                .id(mti.getTipoMuestra().getId())
                .nombre(mti.getTipoMuestra().getNombre())
                .temperaturaAlmacenamiento(mti.getTipoMuestra().getTemperaturaAlmacenamiento())
                .build())
            .tuboMuestra(TuboMuestraResumenDTO.builder()
                .id(mti.getTuboMuestra().getId())
                .nombre(mti.getTuboMuestra().getNombre())
                .prefijoCodigo(mti.getTuboMuestra().getPrefijoCodigo())
                .numeroAlicuotas(mti.getTuboMuestra().getNumeroAlicuotas())
                .build())
            .nombreInstitucion(mti.getInstitucion().getNombre())
            .build();
        return ResponseEntity.ok(new APIResponse("Tipo asignado encontrado", dto, false, HttpStatus.OK));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar muestra / reubicar", description = "Actualiza la información de una muestra biológica existente o la reubica en una nueva posición de almacenamiento")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> update(
        @Parameter(description = "ID numérico de la muestra biológica", required = true)
        @PathVariable Long id, @Validated @RequestBody MuestraRequestDTO dto) {
        Muestra updated = muestraApplicationService.updateMuestra(id, dto);
        return ResponseEntity.ok(new APIResponse("Muestra actualizada", MuestraMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    // ── Impresión ZPL ────────────────────────────────────────────────────────

    @GetMapping("/{id}/etiqueta/zpl")
    @Operation(summary = "Generar ZPL para etiqueta de muestra")
    public ResponseEntity<APIResponse> getZplEtiqueta(
            @PathVariable Long id,
            @RequestParam(required = false) Long configuracionId) {
        String zpl = muestraApplicationService.generarZplEtiqueta(id, configuracionId);
        return ResponseEntity.ok(new APIResponse("ZPL generado", zpl, false, HttpStatus.OK));
    }

    @GetMapping("/{id}/alicuotas/etiquetas/zpl")
    @Operation(summary = "Generar ZPL para etiquetas de alícuotas, agrupado por fila")
    public ResponseEntity<APIResponse> getZplAlicuotas(
            @PathVariable Long id,
            @RequestParam(required = false) Long configuracionId) {
        ZplLoteResponseDTO zpl = muestraApplicationService.generarZplAlicuotas(id, configuracionId);
        return ResponseEntity.ok(new APIResponse("ZPL generado para alícuotas", zpl, false, HttpStatus.OK));
    }

    @GetMapping("/{id}/lote-completo/zpl")
    @Operation(summary = "Generar ZPL para etiqueta padre + alícuotas, agrupado por fila")
    public ResponseEntity<APIResponse> getZplLoteCompleto(
            @PathVariable Long id,
            @RequestParam(required = false) Long configuracionId) {
        ZplLoteResponseDTO zpl = muestraApplicationService.generarZplLoteCompleto(id, configuracionId);
        return ResponseEntity.ok(new APIResponse("ZPL generado para lote completo", zpl, false, HttpStatus.OK));
    }

    // ── Impresión directa ───────────────────────────────────────────────────

    @GetMapping("/impresoras")
    @Operation(summary = "Listar impresoras disponibles en el servidor")
    public ResponseEntity<APIResponse> listarImpresoras() {
        List<String> impresoras = muestraApplicationService.listarImpresoras();
        return ResponseEntity.ok(new APIResponse("Impresoras disponibles", impresoras, false, HttpStatus.OK));
    }

    @PostMapping("/{id}/etiqueta/imprimir")
    @Operation(summary = "Imprimir etiqueta directamente en impresora")
    public ResponseEntity<APIResponse> imprimirEtiqueta(
            @PathVariable Long id,
            @RequestParam String impresora,
            @RequestParam(required = false) Long configuracionId) {
        muestraApplicationService.imprimirEtiqueta(id, impresora, configuracionId);
        return ResponseEntity.ok(new APIResponse("Etiqueta enviada a impresora", null, false, HttpStatus.OK));
    }

    @PostMapping("/{id}/alicuotas/etiquetas/imprimir")
    @Operation(summary = "Imprimir etiquetas de alícuotas directamente en impresora")
    public ResponseEntity<APIResponse> imprimirAlicuotas(
            @PathVariable Long id,
            @RequestParam String impresora,
            @RequestParam(required = false) Long configuracionId) {
        int total = muestraApplicationService.imprimirAlicuotas(id, impresora, configuracionId);
        return ResponseEntity.ok(new APIResponse(total + " etiqueta(s) enviada(s) a impresora", total, false, HttpStatus.OK));
    }

    @PostMapping("/{id}/lote-completo/imprimir")
    @Operation(summary = "Imprimir etiqueta padre + todas las alícuotas")
    public ResponseEntity<APIResponse> imprimirLoteCompleto(
            @PathVariable Long id,
            @RequestParam String impresora,
            @RequestParam(required = false) Long configuracionId) {
        int total = muestraApplicationService.imprimirLoteCompleto(id, impresora, configuracionId);
        return ResponseEntity.ok(new APIResponse(total + " etiqueta(s) enviada(s) a impresora", total, false, HttpStatus.OK));
    }
}
