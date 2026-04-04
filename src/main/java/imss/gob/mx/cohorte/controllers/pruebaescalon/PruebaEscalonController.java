package imss.gob.mx.cohorte.controllers.pruebaescalon;

import imss.gob.mx.cohorte.controllers.pruebaescalon.dto.*;
import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;
import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapa;
import imss.gob.mx.cohorte.modules.escalonPrueba.medicion.PruebaEscalonMedicion;
import imss.gob.mx.cohorte.services.Escalonpruebas.EtapaService;
import imss.gob.mx.cohorte.services.Escalonpruebas.MedicionService;
import imss.gob.mx.cohorte.services.Escalonpruebas.PruebaService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prueba-escalon")
@AllArgsConstructor
@Tag(name = "Prueba Escalón", description = "Gestión de pruebas escalón")
@SecurityRequirement(name = "bearerAuth")
public class PruebaEscalonController {

    private final PruebaService pruebaService;
    private final EtapaService etapaService;
    private final MedicionService medicionService;

    @GetMapping
    @Operation(summary = "Listar todas las pruebas escalón")
    public ResponseEntity<APIResponse> getAll() {
        List<PruebaEscalon> pruebas = pruebaService.getAll();
        return ResponseEntity.ok(new APIResponse("Pruebas escalón encontradas", PruebaEscalonMapper.toResponseDTOList(pruebas), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener prueba escalón por ID")
    public ResponseEntity<APIResponse> getById(@PathVariable Long id) {
        PruebaEscalon prueba = pruebaService.getOne(id);
        return ResponseEntity.ok(new APIResponse("Prueba escalón encontrada", PruebaEscalonMapper.toResponseDTO(prueba), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear nueva prueba escalón")
    public ResponseEntity<APIResponse> create(@Validated @RequestBody PruebaEscalonRequestDTO dto) {
        PruebaEscalon prueba = PruebaEscalonMapper.toEntity(dto);
        PruebaEscalon saved = pruebaService.create(prueba);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Prueba escalón registrada exitosamente", PruebaEscalonMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar prueba escalón")
    public ResponseEntity<APIResponse> update(@PathVariable Long id, @Validated @RequestBody PruebaEscalonRequestDTO dto) {
        PruebaEscalon prueba = PruebaEscalonMapper.toEntity(dto);
        prueba.setId(id);
        PruebaEscalon updated = pruebaService.update(prueba);
        return ResponseEntity.ok(new APIResponse("Prueba escalón actualizada", PruebaEscalonMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar prueba escalón")
    public ResponseEntity<APIResponse> delete(@PathVariable Long id) {
        pruebaService.delete(id);
        return ResponseEntity.ok(new APIResponse("Prueba escalón eliminada", null, false, HttpStatus.OK));
    }

    @PostMapping("/etapas")
    @Operation(summary = "Agregar etapa a prueba escalón")
    public ResponseEntity<APIResponse> addEtapa(@Validated @RequestBody EtapaRequestDTO dto) {
        PruebaEscalonEtapa etapa = new PruebaEscalonEtapa();
        PruebaEscalon prueba = new PruebaEscalon();
        prueba.setId(dto.getIdPruebaEscalon());
        etapa.setPruebaEscalon(prueba);
        etapa.setEtapa(PruebaEscalonEtapa.Etapa.valueOf(dto.getEtapa()));
        etapa.setObservaciones(dto.getObservaciones());
        PruebaEscalonEtapa saved = etapaService.create(etapa);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Etapa agregada exitosamente", PruebaEscalonMapper.toEtapaResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/etapas/{id}")
    @Operation(summary = "Actualizar etapa")
    public ResponseEntity<APIResponse> updateEtapa(@PathVariable Long id, @Validated @RequestBody EtapaRequestDTO dto) {
        PruebaEscalonEtapa etapa = new PruebaEscalonEtapa();
        etapa.setId(id);
        etapa.setEtapa(PruebaEscalonEtapa.Etapa.valueOf(dto.getEtapa()));
        etapa.setObservaciones(dto.getObservaciones());
        PruebaEscalonEtapa updated = etapaService.update(etapa);
        return ResponseEntity.ok(new APIResponse("Etapa actualizada", PruebaEscalonMapper.toEtapaResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/etapas/{id}")
    @Operation(summary = "Eliminar etapa")
    public ResponseEntity<APIResponse> deleteEtapa(@PathVariable Long id) {
        etapaService.delete(id);
        return ResponseEntity.ok(new APIResponse("Etapa eliminada", null, false, HttpStatus.OK));
    }

    @PostMapping("/mediciones")
    @Operation(summary = "Registrar medición")
    public ResponseEntity<APIResponse> addMedicion(@Validated @RequestBody MedicionRequestDTO dto) {
        PruebaEscalonMedicion medicion = new PruebaEscalonMedicion();
        PruebaEscalonEtapa etapa = new PruebaEscalonEtapa();
        etapa.setId(dto.getIdEtapa());
        medicion.setEtapa(etapa);
        medicion.setParametro(PruebaEscalonMedicion.Parametro.valueOf(dto.getParametro()));
        medicion.setValor(dto.getValor());
        medicion.setUnidad(dto.getUnidad());
        PruebaEscalonMedicion saved = medicionService.create(medicion);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Medición registrada exitosamente", PruebaEscalonMapper.toMedicionResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/mediciones/{id}")
    @Operation(summary = "Actualizar medición")
    public ResponseEntity<APIResponse> updateMedicion(@PathVariable Long id, @Validated @RequestBody MedicionRequestDTO dto) {
        PruebaEscalonMedicion medicion = new PruebaEscalonMedicion();
        medicion.setId(id);
        medicion.setParametro(PruebaEscalonMedicion.Parametro.valueOf(dto.getParametro()));
        medicion.setValor(dto.getValor());
        medicion.setUnidad(dto.getUnidad());
        PruebaEscalonMedicion updated = medicionService.update(medicion);
        return ResponseEntity.ok(new APIResponse("Medición actualizada", PruebaEscalonMapper.toMedicionResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/mediciones/{id}")
    @Operation(summary = "Eliminar medición")
    public ResponseEntity<APIResponse> deleteMedicion(@PathVariable Long id) {
        medicionService.delete(id);
        return ResponseEntity.ok(new APIResponse("Medición eliminada", null, false, HttpStatus.OK));
    }
}
