package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.MuestraApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.*;
import imss.gob.mx.cohorte.controllers.impresion.dto.PrintableLabelBatchDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.PaginaMuestras;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    @PreAuthorize("hasAuthority('MUESTRAS_VER')")
    public ResponseEntity<APIResponse> getAll(
            @Parameter(description = "Si true, incluye también muestras que solo estuvieron prestadas en el pasado (default: false).")
            @RequestParam(value = "incluirHistorico", defaultValue = "false") boolean incluirHistorico) {
        List<Muestra> list = muestraApplicationService.getAllMuestras(incluirHistorico);
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
    @PreAuthorize("hasAuthority('MUESTRAS_VER')")
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

    @GetMapping("/cursor")
    @Operation(summary = "Listar muestras por cursor",
        description = "Devuelve una ventana del listado situada por cursor en vez de por número de página, "
            + "ordenada de la muestra más reciente a la más antigua. Cada elemento es una tarjeta "
            + "—muestra sin padre, o alícuota cuyo padre no es visible para la institución— y sus "
            + "alícuotas viajan en una lista aparte. La búsqueda y los filtros se resuelven aquí: con "
            + "solo una página cargada, aplicarlos en la pantalla contestaría sobre esas filas y "
            + "escondería el resto sin avisar.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    @PreAuthorize("hasAuthority('MUESTRAS_VER')")
    public ResponseEntity<APIResponse> getPaginaCursor(
            @Parameter(description = "Cursor devuelto por una página anterior. Omitirlo empieza por el extremo.")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "SIGUIENTE avanza hacia lo más antiguo; ANTERIOR vuelve hacia lo más "
                + "reciente. ANTERIOR sin cursor entrega el final de la lista.")
            @RequestParam(value = "direccion", defaultValue = "SIGUIENTE") String direccion,
            @Parameter(description = "Tarjetas por página (default 20, máximo 100).")
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "incluirHistorico", defaultValue = "false") boolean incluirHistorico,
            @Parameter(description = "Oculta las alícuotas ajenas que ya no están en el biobanco propio.")
            @RequestParam(value = "ocultarDevueltasHuerfanas", defaultValue = "true") boolean ocultarDevueltasHuerfanas,
            @Parameter(description = "Texto libre: etiqueta, unidad, folio o nombre del participante, tipo o tubo.")
            @RequestParam(value = "busqueda", required = false) String busqueda,
            @Parameter(description = "Fecha de recolección mínima (YYYY-MM-DD).")
            @RequestParam(value = "fechaDesde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @Parameter(description = "Fecha de recolección máxima (YYYY-MM-DD), inclusiva.")
            @RequestParam(value = "fechaHasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @Parameter(description = "Nombres de tipo de muestra. Por nombre y no por id: cada institución "
                + "tiene su propio registro de «Heces», y filtrar por id escondería las de otra sede.")
            @RequestParam(value = "tipos", required = false) List<String> tipos,
            @RequestParam(value = "sexo", required = false) String sexo,
            @RequestParam(value = "folioDesde", required = false) String folioDesde,
            @RequestParam(value = "folioHasta", required = false) String folioHasta) {

        PaginaMuestras pagina = muestraApplicationService.buscarPaginaMuestras(
                cursor, "ANTERIOR".equalsIgnoreCase(direccion), size,
                incluirHistorico, ocultarDevueltasHuerfanas,
                busqueda, fechaDesde, fechaHasta, tipos, sexo, folioDesde, folioHasta);

        PaginaMuestrasResponseDTO body = PaginaMuestrasResponseDTO.builder()
                .muestras(MuestraMapper.toResponseDTOList(pagina.muestras()))
                .alicuotas(MuestraMapper.toResponseDTOList(pagina.alicuotas()))
                .cursorInicio(pagina.cursorInicio())
                .cursorFin(pagina.cursorFin())
                .hayAnteriores(pagina.hayAnteriores())
                .haySiguientes(pagina.haySiguientes())
                .total(pagina.total())
                .huerfanasDevueltas(pagina.huerfanasDevueltas())
                .build();

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
    @PreAuthorize("hasAuthority('MUESTRAS_VER')")
    public ResponseEntity<APIResponse> getById(
        @Parameter(description = "ID numérico de la muestra biológica", required = true)
        @PathVariable Long id) {
        Muestra muestra = muestraApplicationService.getMuestra(id);
        return ResponseEntity.ok(new APIResponse("Muestra encontrada", MuestraMapper.toResponseDTO(muestra), false, HttpStatus.OK));
    }

    @GetMapping("/{id}/ubicacion-3d")
    @Operation(summary = "Ubicacion de la muestra para el visualizador 3D",
        description = "Devuelve en una sola llamada refrigerador, piso, caja y posicion con sus metricas "
            + "precalculadas. Si la muestra esta prestada, dada de baja o sin posicion, responde "
            + "disponible=false con el motivo en lugar de la escena.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Exito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    @PreAuthorize("hasAuthority('MUESTRAS_VER')")
    public ResponseEntity<APIResponse> getUbicacion3D(
        @Parameter(description = "ID numerico de la muestra biologica", required = true)
        @PathVariable Long id) {
        return ResponseEntity.ok(new APIResponse("Ubicacion de la muestra",
            muestraApplicationService.getUbicacion3D(id), false, HttpStatus.OK));
    }

    // La etiqueta lleva diagonales ("C1/001103/F4"), así que viaja como parámetro
    // de consulta y no como segmento de ruta: en la ruta partiría el path, y
    // codificada como %2F la rechaza el contenedor antes de llegar aquí.
    @GetMapping("/buscar-por-etiqueta")
    @Operation(summary = "Resolver una etiqueta leída con un lector de códigos",
               description = "Devuelve la muestra cuya etiqueta coincide, dentro de las visibles para la "
                       + "institución (propia, en posesión o con traslado previo). Además indica si es "
                       + "alícuota —para poder desplegar su muestra padre— y si hace falta encender la "
                       + "vista de histórico para que aparezca en el listado.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Etiqueta resuelta",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ninguna muestra visible con esa etiqueta",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    @PreAuthorize("hasAuthority('MUESTRAS_ESCANEAR')")
    public ResponseEntity<APIResponse> resolverPorEtiqueta(
            @Parameter(description = "Texto codificado en la etiqueta impresa", required = true)
            @RequestParam("etiqueta") String etiqueta) {
        Muestra muestra = muestraApplicationService.buscarPorEtiquetaEscaneada(etiqueta);
        Long idInstitucionActual = muestraApplicationService.getIdInstitucionActual();

        boolean esPropia = muestra.getInstitucion() != null
                && muestra.getInstitucion().getId().equals(idInstitucionActual);
        boolean laTengo = muestra.getInstitucionActual() != null
                && muestra.getInstitucionActual().getId().equals(idInstitucionActual);

        MuestraEscaneadaDTO dto = MuestraEscaneadaDTO.builder()
                .muestra(MuestraMapper.toResponseDTO(muestra))
                .idMuestraPadre(muestra.getMuestraPadre() != null ? muestra.getMuestraPadre().getId() : null)
                .requiereHistorico(!esPropia && !laTengo)
                .build();

        return ResponseEntity.ok(new APIResponse("Etiqueta resuelta", dto, false, HttpStatus.OK));
    }

    @GetMapping("/paciente/uuid/{uuid}/count")
    @Operation(summary = "Contar muestras de un paciente por UUID")
    @PreAuthorize("hasAnyAuthority('MUESTRAS_VER', 'EXPEDIENTE_BIOBANCO')")
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
    @PreAuthorize("hasAuthority('MUESTRAS_VER')")
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
    @PreAuthorize("hasAuthority('MUESTRAS_CREAR')")
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

        var resultado = muestraApplicationService.createMuestra(
            entity,
            dto.getGenerarAlicuotas(),
            dto.getPlanAlicuotas() != null ? dto.getPlanAlicuotas().getVolumenes() : null);

        MuestraResponseDTO responseDTO = MuestraMapper.toResponseDTO(resultado.muestra());
        // El conteo real, no el del tubo: pueden ser menos si el volumen extraído
        // no alcanzaba para el lote completo, o ninguna si no se pidieron.
        responseDTO.setAlicuotasGeneradas(resultado.totalAlicuotas());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Muestra registrada exitosamente", responseDTO, false, HttpStatus.CREATED));
    }

    @GetMapping("/biobanco")
    @Operation(summary = "Muestras en el biobanco de mi institución",
               description = "Muestras cuyo tenedor actual es la institución del usuario logueado (incluye SIN_POSICION, EN_BIOBANCO y PRESTADAS recibidas).")
    @PreAuthorize("hasAuthority('MUESTRAS_VER')")
    public ResponseEntity<APIResponse> getMuestrasEnBiobanco(
            Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") boolean incluirAgotadas) {
        Page<Muestra> page = muestraApplicationService.getMuestrasEnBiobancoPage(pageable, incluirAgotadas);
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
    @PreAuthorize("hasAuthority('MUESTRAS_VER')")
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
    @PreAuthorize("hasAnyAuthority('MUESTRAS_EDITAR', 'TRASLADOS_CONFIRMAR')")
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
    @PreAuthorize("hasAuthority('MUESTRAS_EDITAR')")
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
    @PreAuthorize("hasAuthority('MUESTRAS_ELIMINAR')")
    public ResponseEntity<APIResponse> delete(@PathVariable Long id) {
        muestraApplicationService.deleteMuestra(id);
        return ResponseEntity.ok(new APIResponse("Muestra eliminada exitosamente", null, false, HttpStatus.OK));
    }

    @PostMapping("/{id}/baja")
    @Operation(summary = "Dar de baja muestra",
               description = "Marca la muestra como BAJA de manera irreversible. Solo la institución propietaria puede hacerlo. "
                           + "Requiere motivo obligatorio. Libera la posición si tenía. Queda registrado en el historial.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Muestra dada de baja"),
        @ApiResponse(responseCode = "409", description = "Muestra ya de baja, en tránsito, o no pertenece a la institución del usuario")
    })
    @PreAuthorize("hasAuthority('MUESTRAS_DAR_BAJA')")
    public ResponseEntity<APIResponse> darDeBaja(
            @PathVariable Long id,
            @Validated @RequestBody imss.gob.mx.cohorte.controllers.almacenamiento.dto.DarDeBajaRequestDTO dto) {
        Muestra baja = muestraApplicationService.darDeBajaMuestra(id, dto.getMotivo());
        return ResponseEntity.ok(new APIResponse("Muestra dada de baja",
            MuestraMapper.toResponseDTO(baja), false, HttpStatus.OK));
    }

    @PostMapping("/{id}/generar-alicuotas")
    @Operation(summary = "Generar un lote de alícuotas",
               description = "Genera alícuotas de una muestra padre ya registrada, con el tipo+tubo que elija "
                           + "la institución que la tiene. Sirve tanto para la receptora de un préstamo como "
                           + "para la propietaria que no alicuotó al registrar.")
    @PreAuthorize("hasAuthority('MUESTRAS_CREAR')")
    public ResponseEntity<APIResponse> generarLoteAlicuotas(
            @PathVariable Long id,
            @Validated @RequestBody GenerarAlicuotasRequestDTO dto) {
        List<Muestra> alicuotas = muestraApplicationService.generarLoteAlicuotas(
            id, dto.getIdTipoMuestra(), dto.getIdTuboMuestra(),
            dto.getPlanAlicuotas() != null ? dto.getPlanAlicuotas().getVolumenes() : null);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Alícuotas generadas",
                MuestraMapper.toResponseDTOList(alicuotas), false, HttpStatus.CREATED));
    }

    @GetMapping("/plan-alicuotas")
    @Operation(summary = "Previsualizar el lote de alícuotas",
               description = "Calcula cuántas alícuotas alcanzan con el volumen indicado, qué sobra y qué "
                           + "repartos son posibles. No crea nada.")
    @PreAuthorize("hasAuthority('MUESTRAS_CREAR')")
    public ResponseEntity<APIResponse> previsualizarPlan(
            @RequestParam Long idTuboMuestra,
            @RequestParam(required = false) Double valor,
            @RequestParam(required = false) Long idMuestra) {
        var plan = idMuestra != null
            ? muestraApplicationService.previsualizarPlanDeMuestra(idMuestra, idTuboMuestra)
            : muestraApplicationService.previsualizarPlan(idTuboMuestra, valor);
        return ResponseEntity.ok(new APIResponse("Plan de alícuotas",
            PlanAlicuotasMapper.toResponseDTO(plan), false, HttpStatus.OK));
    }

    @GetMapping("/{id}/alicuotas/pendientes")
    @Operation(summary = "Alícuotas del lote que siguen sin ubicar",
               description = "Alícuotas creadas cuyo volumen sigue reservado en la muestra padre.")
    @PreAuthorize("hasAuthority('MUESTRAS_VER')")
    public ResponseEntity<APIResponse> getAlicuotasPendientes(@PathVariable Long id) {
        List<Muestra> pendientes = muestraApplicationService.getAlicuotasPendientes(id);
        return ResponseEntity.ok(new APIResponse("Alícuotas pendientes de ubicar",
            MuestraMapper.toResponseDTOList(pendientes), false, HttpStatus.OK));
    }

    @PostMapping("/{id}/alicuotas/ubicar-lote")
    @Operation(summary = "Ubicar el lote completo de alícuotas",
               description = "Asigna posición a varias alícuotas de una misma muestra padre en una sola "
                           + "operación. Todo o nada: si un hueco se ocupó entretanto, no queda medio lote ubicado.")
    @PreAuthorize("hasAnyAuthority('MUESTRAS_EDITAR', 'TRASLADOS_CONFIRMAR')")
    public ResponseEntity<APIResponse> ubicarLote(
            @PathVariable Long id,
            @RequestBody UbicarLoteRequestDTO dto) {
        List<Muestra> ubicadas = muestraApplicationService.ubicarLote(id, dto.getAsignaciones());
        return ResponseEntity.ok(new APIResponse("Lote ubicado",
            MuestraMapper.toResponseDTOList(ubicadas), false, HttpStatus.OK));
    }

    @GetMapping("/{id}/tipo-institucion")
    @Operation(summary = "Tipo/tubo asignado por mi institución a esta muestra",
               description = "Obtiene el tipo y tubo que mi institución asignó a una muestra padre recibida.")
    @PreAuthorize("hasAuthority('MUESTRAS_VER')")
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
    @PreAuthorize("hasAuthority('MUESTRAS_EDITAR')")
    public ResponseEntity<APIResponse> update(
        @Parameter(description = "ID numérico de la muestra biológica", required = true)
        @PathVariable Long id, @Validated @RequestBody MuestraRequestDTO dto) {
        Muestra updated = muestraApplicationService.updateMuestra(id, dto);
        return ResponseEntity.ok(new APIResponse("Muestra actualizada", MuestraMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    // ── Impresión ZPL ────────────────────────────────────────────────────────

    @GetMapping("/{id}/etiqueta/zpl")
    @Operation(summary = "Generar ZPL para etiqueta de muestra")
    @PreAuthorize("hasAuthority('MUESTRAS_IMPRIMIR')")
    public ResponseEntity<APIResponse> getZplEtiqueta(
            @PathVariable Long id,
            @RequestParam(required = false) Long configuracionId) {
        String zpl = muestraApplicationService.generarZplEtiqueta(id, configuracionId);
        return ResponseEntity.ok(new APIResponse("ZPL generado", zpl, false, HttpStatus.OK));
    }

    @GetMapping("/{id}/alicuotas/etiquetas/zpl")
    @Operation(summary = "Generar ZPL para etiquetas de alícuotas, agrupado por fila")
    @PreAuthorize("hasAuthority('MUESTRAS_IMPRIMIR')")
    public ResponseEntity<APIResponse> getZplAlicuotas(
            @PathVariable Long id,
            @RequestParam(required = false) Long configuracionId) {
        ZplLoteResponseDTO zpl = muestraApplicationService.generarZplAlicuotas(id, configuracionId);
        return ResponseEntity.ok(new APIResponse("ZPL generado para alícuotas", zpl, false, HttpStatus.OK));
    }

    @GetMapping("/{id}/lote-completo/zpl")
    @Operation(summary = "Generar ZPL para etiqueta padre + alícuotas, agrupado por fila")
    @PreAuthorize("hasAuthority('MUESTRAS_IMPRIMIR')")
    public ResponseEntity<APIResponse> getZplLoteCompleto(
            @PathVariable Long id,
            @RequestParam(required = false) Long configuracionId) {
        ZplLoteResponseDTO zpl = muestraApplicationService.generarZplLoteCompleto(id, configuracionId);
        return ResponseEntity.ok(new APIResponse("ZPL generado para lote completo", zpl, false, HttpStatus.OK));
    }

    @PostMapping("/etiquetas/zpl-acomodado")
    @Operation(summary = "Generar ZPL con las etiquetas en los carriles elegidos por el operador")
    @PreAuthorize("hasAuthority('MUESTRAS_IMPRIMIR')")
    public ResponseEntity<APIResponse> getZplAcomodado(
            @Validated @RequestBody ZplAcomodoRequestDTO dto) {
        ZplLoteResponseDTO zpl = muestraApplicationService.generarZplAcomodado(
                dto.slots(), dto.configuracionId(), dto.marcoActivo());
        return ResponseEntity.ok(new APIResponse("ZPL generado con acomodo", zpl, false, HttpStatus.OK));
    }

    // ── Datos para impresión por navegador ─────────────────────────────────

    @GetMapping("/{id}/etiqueta/datos")
    @Operation(summary = "Obtener datos estructurados para etiqueta de muestra")
    @PreAuthorize("hasAuthority('MUESTRAS_IMPRIMIR')")
    public ResponseEntity<APIResponse> getDatosEtiqueta(
            @PathVariable Long id,
            @RequestParam(required = false) Long configuracionId) {
        PrintableLabelBatchDTO datos = muestraApplicationService.obtenerDatosEtiqueta(id, configuracionId);
        return ResponseEntity.ok(new APIResponse("Datos de etiqueta", datos, false, HttpStatus.OK));
    }

    @GetMapping("/{id}/alicuotas/etiquetas/datos")
    @Operation(summary = "Obtener datos estructurados para etiquetas de alícuotas")
    @PreAuthorize("hasAuthority('MUESTRAS_IMPRIMIR')")
    public ResponseEntity<APIResponse> getDatosAlicuotas(
            @PathVariable Long id,
            @RequestParam(required = false) Long configuracionId) {
        PrintableLabelBatchDTO datos = muestraApplicationService.obtenerDatosAlicuotas(id, configuracionId);
        return ResponseEntity.ok(new APIResponse("Datos de etiquetas de alícuotas", datos, false, HttpStatus.OK));
    }

    @GetMapping("/{id}/lote-completo/datos")
    @Operation(summary = "Obtener datos estructurados para etiquetas de lote completo")
    @PreAuthorize("hasAuthority('MUESTRAS_IMPRIMIR')")
    public ResponseEntity<APIResponse> getDatosLoteCompleto(
            @PathVariable Long id,
            @RequestParam(required = false) Long configuracionId) {
        PrintableLabelBatchDTO datos = muestraApplicationService.obtenerDatosLoteCompleto(id, configuracionId);
        return ResponseEntity.ok(new APIResponse("Datos de etiquetas de lote completo", datos, false, HttpStatus.OK));
    }

    // ── Impresión directa ───────────────────────────────────────────────────

    @GetMapping("/impresoras")
    @Operation(summary = "Listar impresoras disponibles en el servidor")
    @PreAuthorize("hasAuthority('MUESTRAS_IMPRIMIR')")
    public ResponseEntity<APIResponse> listarImpresoras() {
        List<String> impresoras = muestraApplicationService.listarImpresoras();
        return ResponseEntity.ok(new APIResponse("Impresoras disponibles", impresoras, false, HttpStatus.OK));
    }

    @PostMapping("/{id}/etiqueta/imprimir")
    @Operation(summary = "Imprimir etiqueta directamente en impresora")
    @PreAuthorize("hasAuthority('MUESTRAS_IMPRIMIR')")
    public ResponseEntity<APIResponse> imprimirEtiqueta(
            @PathVariable Long id,
            @RequestParam String impresora,
            @RequestParam(required = false) Long configuracionId) {
        muestraApplicationService.imprimirEtiqueta(id, impresora, configuracionId);
        return ResponseEntity.ok(new APIResponse("Etiqueta enviada a impresora", null, false, HttpStatus.OK));
    }

    @PostMapping("/{id}/alicuotas/etiquetas/imprimir")
    @Operation(summary = "Imprimir etiquetas de alícuotas directamente en impresora")
    @PreAuthorize("hasAuthority('MUESTRAS_IMPRIMIR')")
    public ResponseEntity<APIResponse> imprimirAlicuotas(
            @PathVariable Long id,
            @RequestParam String impresora,
            @RequestParam(required = false) Long configuracionId) {
        int total = muestraApplicationService.imprimirAlicuotas(id, impresora, configuracionId);
        return ResponseEntity.ok(new APIResponse(total + " etiqueta(s) enviada(s) a impresora", total, false, HttpStatus.OK));
    }

    @PostMapping("/{id}/lote-completo/imprimir")
    @Operation(summary = "Imprimir etiqueta padre + todas las alícuotas")
    @PreAuthorize("hasAuthority('MUESTRAS_IMPRIMIR')")
    public ResponseEntity<APIResponse> imprimirLoteCompleto(
            @PathVariable Long id,
            @RequestParam String impresora,
            @RequestParam(required = false) Long configuracionId) {
        int total = muestraApplicationService.imprimirLoteCompleto(id, impresora, configuracionId);
        return ResponseEntity.ok(new APIResponse(total + " etiqueta(s) enviada(s) a impresora", total, false, HttpStatus.OK));
    }
}
