package imss.gob.mx.cohorte.application.catalogo;

import imss.gob.mx.cohorte.controllers.catalogo.dto.*;
import imss.gob.mx.cohorte.modules.catalogo.TipoCatalogo;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.catalogo.CatalogoCopyService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoCopyApplicationService {

    private final CatalogoCopyService catalogoCopyService;
    private final InstitucionRepository institucionRepository;
    private final InstitucionContextService institucionContextService;

    @Transactional
    public CopiarCatalogosResponseDTO copiar(CopiarCatalogosRequestDTO request) {
        Institucion origen = resolverInstitucion(request.getUuidInstitucionOrigen(), "origen");
        Institucion destino = resolverInstitucion(request.getUuidInstitucionDestino(), "destino");
        validar(origen, destino);

        List<ResultadoCopia> resultados = new ArrayList<>();
        for (SeleccionCatalogo seleccion : request.getCatalogos()) {
            resultados.add(ejecutarCopia(seleccion.getTipo(), seleccion.getIds(), origen, destino));
        }

        int totalCopiados = resultados.stream().mapToInt(ResultadoCopia::getCopiados).sum();
        int totalOmitidos = resultados.stream().mapToInt(ResultadoCopia::getOmitidos).sum();

        return CopiarCatalogosResponseDTO.builder()
                .resultados(resultados)
                .totalCopiados(totalCopiados)
                .totalOmitidos(totalOmitidos)
                .build();
    }

    @Transactional(readOnly = true)
    public PreviewCopiaResponseDTO preview(CopiarCatalogosRequestDTO request) {
        Institucion origen = resolverInstitucion(request.getUuidInstitucionOrigen(), "origen");
        Institucion destino = resolverInstitucion(request.getUuidInstitucionDestino(), "destino");
        validar(origen, destino);

        List<PreviewCatalogo> previews = new ArrayList<>();
        for (SeleccionCatalogo seleccion : request.getCatalogos()) {
            previews.add(ejecutarPreview(seleccion.getTipo(), origen, destino));
        }

        return PreviewCopiaResponseDTO.builder()
                .previews(previews)
                .build();
    }

    private Institucion resolverInstitucion(String uuid, String label) {
        Institucion inst = institucionRepository.findByUuid(uuid)
                .orElseThrow(() -> new ObjNotFoundException("Institución " + label + " no encontrada: " + uuid));
        institucionContextService.verificarPerteneceOAncestra(inst.getId());
        return inst;
    }

    private void validar(Institucion origen, Institucion destino) {
        if (origen.getId().equals(destino.getId())) {
            throw new ValidationException("La institución origen y destino no pueden ser la misma");
        }
        if (!Boolean.TRUE.equals(destino.getActivo())) {
            throw new ValidationException("La institución destino no está activa");
        }
    }

    private ResultadoCopia ejecutarCopia(TipoCatalogo tipo, List<Long> ids, Institucion origen, Institucion destino) {
        return switch (tipo) {
            case UNIDADES -> catalogoCopyService.copiarUnidades(origen, destino, ids);
            case TIPOS_ESTUDIO -> catalogoCopyService.copiarTiposEstudio(origen, destino, ids);
            case EXAMENES -> catalogoCopyService.copiarExamenes(origen, destino, ids);
            case TIPOS_MUESTRA -> catalogoCopyService.copiarTiposMuestra(origen, destino, ids);
            case ESTUDIOS_MUESTRA -> catalogoCopyService.copiarEstudiosMuestra(origen, destino, ids);
        };
    }

    private PreviewCatalogo ejecutarPreview(TipoCatalogo tipo, Institucion origen, Institucion destino) {
        return switch (tipo) {
            case UNIDADES -> catalogoCopyService.previewUnidades(origen, destino);
            case TIPOS_ESTUDIO -> catalogoCopyService.previewTiposEstudio(origen, destino);
            case EXAMENES -> catalogoCopyService.previewExamenes(origen, destino);
            case TIPOS_MUESTRA -> catalogoCopyService.previewTiposMuestra(origen, destino);
            case ESTUDIOS_MUESTRA -> catalogoCopyService.previewEstudiosMuestra(origen, destino);
        };
    }
}
