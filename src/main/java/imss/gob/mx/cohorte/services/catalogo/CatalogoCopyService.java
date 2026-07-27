package imss.gob.mx.cohorte.services.catalogo;

import imss.gob.mx.cohorte.controllers.catalogo.dto.ItemCatalogo;
import imss.gob.mx.cohorte.controllers.catalogo.dto.PreviewCatalogo;
import imss.gob.mx.cohorte.controllers.catalogo.dto.ResultadoCopia;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.*;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.catalogo.TipoCatalogo;
import imss.gob.mx.cohorte.modules.catalogo.UnidadMedida;
import imss.gob.mx.cohorte.modules.catalogo.UnidadMedidaRepository;
import imss.gob.mx.cohorte.modules.estudios.parametros.OpcionParametro;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudioRepository;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudioRepository;
import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.ExamenRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogoCopyService {

    private final UnidadMedidaRepository unidadMedidaRepository;
    private final TipoEstudioRepository tipoEstudioRepository;
    private final ParametroEstudioRepository parametroEstudioRepository;
    private final ExamenRepository examenRepository;
    private final TipoMuestraRepository tipoMuestraRepository;
    private final TipoEstudioMuestraRepository tipoEstudioMuestraRepository;

    // ── Copia ────────────────────────────────────────────────────────────────

    public ResultadoCopia copiarUnidades(Institucion origen, Institucion destino, List<Long> ids) {
        List<UnidadMedida> originales = filtrar(
                unidadMedidaRepository.findAllByInstitucion_IdOrderByNombreAsc(origen.getId()),
                ids, UnidadMedida::getId);
        int copiados = 0;
        List<String> omitidos = new ArrayList<>();

        for (UnidadMedida original : originales) {
            if (unidadMedidaRepository.findByNombreIgnoreCaseAndInstitucion_Id(original.getNombre(), destino.getId()).isPresent()) {
                omitidos.add(original.getNombre());
                continue;
            }
            UnidadMedida clon = new UnidadMedida();
            clon.setNombre(original.getNombre());
            clon.setActivo(true);
            clon.setInstitucion(destino);
            unidadMedidaRepository.save(clon);
            copiados++;
        }

        return ResultadoCopia.builder()
                .catalogo(TipoCatalogo.UNIDADES)
                .copiados(copiados).omitidos(omitidos.size()).detalleOmitidos(omitidos)
                .build();
    }

    public ResultadoCopia copiarTiposEstudio(Institucion origen, Institucion destino, List<Long> ids) {
        List<TipoEstudio> originales = filtrar(
                tipoEstudioRepository.findAllByInstitucion_Id(origen.getId()),
                ids, TipoEstudio::getId);
        int copiados = 0;
        List<String> omitidos = new ArrayList<>();

        for (TipoEstudio original : originales) {
            if (tipoEstudioRepository.findByNombreIgnoreCaseAndInstitucion_Id(original.getNombre(), destino.getId()).isPresent()) {
                omitidos.add(original.getNombre());
                continue;
            }

            TipoEstudio clon = new TipoEstudio();
            clon.setNombre(original.getNombre());
            clon.setDescripcion(original.getDescripcion());
            clon.setActivo(true);
            clon.setFechaCreacion(LocalDateTime.now());
            clon.setInstitucion(destino);
            tipoEstudioRepository.save(clon);

            List<ParametroEstudio> parametrosOriginales = parametroEstudioRepository.findAllByTipoEstudio_Id(original.getId());
            for (ParametroEstudio paramOriginal : parametrosOriginales) {
                ParametroEstudio paramClon = new ParametroEstudio();
                paramClon.setTipoEstudio(clon);
                paramClon.setNombre(paramOriginal.getNombre());
                paramClon.setUnidad(paramOriginal.getUnidad());
                paramClon.setTipo(paramOriginal.getTipo());
                paramClon.setValorMinMujeres(paramOriginal.getValorMinMujeres());
                paramClon.setValorMaxMujeres(paramOriginal.getValorMaxMujeres());
                paramClon.setValorMinHombres(paramOriginal.getValorMinHombres());
                paramClon.setValorMaxHombres(paramOriginal.getValorMaxHombres());

                List<OpcionParametro> opcionesClonadas = new ArrayList<>();
                for (OpcionParametro opOriginal : paramOriginal.getOpciones()) {
                    OpcionParametro opClon = new OpcionParametro();
                    opClon.setParametro(paramClon);
                    opClon.setValor(opOriginal.getValor());
                    opClon.setOrden(opOriginal.getOrden());
                    opcionesClonadas.add(opClon);
                }
                paramClon.setOpciones(opcionesClonadas);
                parametroEstudioRepository.save(paramClon);
            }

            copiados++;
        }

        return ResultadoCopia.builder()
                .catalogo(TipoCatalogo.TIPOS_ESTUDIO)
                .copiados(copiados).omitidos(omitidos.size()).detalleOmitidos(omitidos)
                .build();
    }

    public ResultadoCopia copiarExamenes(Institucion origen, Institucion destino, List<Long> ids) {
        List<Examen> originales = filtrar(
                examenRepository.findAllByInstitucion_Id(origen.getId()),
                ids, Examen::getId);
        int copiados = 0;
        List<String> omitidos = new ArrayList<>();

        for (Examen original : originales) {
            if (examenRepository.existsByParametroIgnoreCaseAndInstitucion_Id(original.getParametro(), destino.getId())) {
                omitidos.add(original.getParametro());
                continue;
            }

            Examen clon = new Examen();
            clon.setParametro(original.getParametro());
            clon.setDescripcion(original.getDescripcion());
            clon.setUnidad(original.getUnidad());
            clon.setValorMinMujeres(original.getValorMinMujeres());
            clon.setValorMaxMujeres(original.getValorMaxMujeres());
            clon.setValorMinHombres(original.getValorMinHombres());
            clon.setValorMaxHombres(original.getValorMaxHombres());
            clon.setActivo(true);
            clon.setFechaCreacion(new Timestamp(System.currentTimeMillis()));
            clon.setInstitucion(destino);
            examenRepository.save(clon);
            copiados++;
        }

        return ResultadoCopia.builder()
                .catalogo(TipoCatalogo.EXAMENES)
                .copiados(copiados).omitidos(omitidos.size()).detalleOmitidos(omitidos)
                .build();
    }

    public ResultadoCopia copiarTiposMuestra(Institucion origen, Institucion destino, List<Long> ids) {
        List<TipoMuestra> originales = filtrar(
                tipoMuestraRepository.findAllByInstitucion_IdOrderByNombreAsc(origen.getId()),
                ids, TipoMuestra::getId);
        int copiados = 0;
        List<String> omitidos = new ArrayList<>();

        for (TipoMuestra original : originales) {
            if (tipoMuestraRepository.findByNombreIgnoreCaseAndInstitucion_Id(original.getNombre(), destino.getId()).isPresent()) {
                omitidos.add(original.getNombre());
                continue;
            }

            TipoMuestra clon = new TipoMuestra();
            clon.setNombre(original.getNombre());
            clon.setDescripcion(original.getDescripcion());
            clon.setTemperaturaAlmacenamiento(original.getTemperaturaAlmacenamiento());
            clon.setActivo(true);
            clon.setInstitucion(destino);

            List<TuboMuestra> tubosClonados = new ArrayList<>();
            for (TuboMuestra tuboOriginal : original.getTubos()) {
                TuboMuestra tuboClon = new TuboMuestra();
                tuboClon.setTipoMuestra(clon);
                tuboClon.setNombre(tuboOriginal.getNombre());
                tuboClon.setPrefijoCodigo(tuboOriginal.getPrefijoCodigo());
                tuboClon.setNumeroAlicuotas(tuboOriginal.getNumeroAlicuotas());
                tuboClon.setVolumenAlicuota(tuboOriginal.getVolumenAlicuota());
                tuboClon.setUnidadVolumen(tuboOriginal.getUnidadVolumen());
                tuboClon.setDestinoSugerido(tuboOriginal.getDestinoSugerido());
                tuboClon.setOrden(tuboOriginal.getOrden());
                tuboClon.setActivo(true);
                tubosClonados.add(tuboClon);
            }
            clon.setTubos(tubosClonados);
            tipoMuestraRepository.save(clon);
            copiados++;
        }

        return ResultadoCopia.builder()
                .catalogo(TipoCatalogo.TIPOS_MUESTRA)
                .copiados(copiados).omitidos(omitidos.size()).detalleOmitidos(omitidos)
                .build();
    }

    public ResultadoCopia copiarEstudiosMuestra(Institucion origen, Institucion destino, List<Long> ids) {
        List<TipoEstudioMuestra> originales = filtrar(
                tipoEstudioMuestraRepository.findAllByInstitucion_IdOrderByNombreAsc(origen.getId()),
                ids, TipoEstudioMuestra::getId);
        int copiados = 0;
        List<String> omitidos = new ArrayList<>();

        for (TipoEstudioMuestra original : originales) {
            if (tipoEstudioMuestraRepository.findByNombreIgnoreCaseAndInstitucion_Id(original.getNombre(), destino.getId()).isPresent()) {
                omitidos.add(original.getNombre());
                continue;
            }

            TipoEstudioMuestra clon = new TipoEstudioMuestra();
            clon.setNombre(original.getNombre());
            clon.setDescripcion(original.getDescripcion());
            clon.setActivo(true);
            clon.setFechaCreacion(LocalDateTime.now());
            clon.setInstitucion(destino);

            List<ParametroEstudioMuestra> parametrosClonados = new ArrayList<>();
            for (ParametroEstudioMuestra paramOriginal : original.getParametros()) {
                ParametroEstudioMuestra paramClon = new ParametroEstudioMuestra();
                paramClon.setTipoEstudioMuestra(clon);
                paramClon.setNombre(paramOriginal.getNombre());
                paramClon.setUnidad(paramOriginal.getUnidad());
                paramClon.setTipo(paramOriginal.getTipo());
                paramClon.setValorMinimo(paramOriginal.getValorMinimo());
                paramClon.setValorMaximo(paramOriginal.getValorMaximo());

                List<OpcionParametroEstudioMuestra> opcionesClonadas = new ArrayList<>();
                for (OpcionParametroEstudioMuestra opOriginal : paramOriginal.getOpciones()) {
                    OpcionParametroEstudioMuestra opClon = new OpcionParametroEstudioMuestra();
                    opClon.setParametro(paramClon);
                    opClon.setValor(opOriginal.getValor());
                    opClon.setOrden(opOriginal.getOrden());
                    opcionesClonadas.add(opClon);
                }
                paramClon.setOpciones(opcionesClonadas);
                parametrosClonados.add(paramClon);
            }
            clon.setParametros(parametrosClonados);
            tipoEstudioMuestraRepository.save(clon);
            copiados++;
        }

        return ResultadoCopia.builder()
                .catalogo(TipoCatalogo.ESTUDIOS_MUESTRA)
                .copiados(copiados).omitidos(omitidos.size()).detalleOmitidos(omitidos)
                .build();
    }

    // ── Preview ──────────────────────────────────────────────────────────────

    public PreviewCatalogo previewUnidades(Institucion origen, Institucion destino) {
        List<UnidadMedida> originales = unidadMedidaRepository.findAllByInstitucion_IdOrderByNombreAsc(origen.getId());
        List<ItemCatalogo> items = originales.stream().map(u -> ItemCatalogo.builder()
                .id(u.getId()).nombre(u.getNombre()).hijos(0)
                .existeEnDestino(unidadMedidaRepository.findByNombreIgnoreCaseAndInstitucion_Id(u.getNombre(), destino.getId()).isPresent())
                .build()).toList();
        return PreviewCatalogo.builder().catalogo(TipoCatalogo.UNIDADES).items(items).build();
    }

    public PreviewCatalogo previewTiposEstudio(Institucion origen, Institucion destino) {
        List<TipoEstudio> originales = tipoEstudioRepository.findAllByInstitucion_Id(origen.getId());
        List<ItemCatalogo> items = originales.stream().map(t -> {
            List<ParametroEstudio> params = parametroEstudioRepository.findAllByTipoEstudio_Id(t.getId());
            return ItemCatalogo.builder()
                    .id(t.getId()).nombre(t.getNombre()).hijos(params.size())
                    .existeEnDestino(tipoEstudioRepository.findByNombreIgnoreCaseAndInstitucion_Id(t.getNombre(), destino.getId()).isPresent())
                    .build();
        }).toList();
        return PreviewCatalogo.builder().catalogo(TipoCatalogo.TIPOS_ESTUDIO).items(items).build();
    }

    public PreviewCatalogo previewExamenes(Institucion origen, Institucion destino) {
        List<Examen> originales = examenRepository.findAllByInstitucion_Id(origen.getId());
        List<ItemCatalogo> items = originales.stream().map(e -> ItemCatalogo.builder()
                .id(e.getId()).nombre(e.getParametro()).hijos(0)
                .existeEnDestino(examenRepository.existsByParametroIgnoreCaseAndInstitucion_Id(e.getParametro(), destino.getId()))
                .build()).toList();
        return PreviewCatalogo.builder().catalogo(TipoCatalogo.EXAMENES).items(items).build();
    }

    public PreviewCatalogo previewTiposMuestra(Institucion origen, Institucion destino) {
        List<TipoMuestra> originales = tipoMuestraRepository.findAllByInstitucion_IdOrderByNombreAsc(origen.getId());
        List<ItemCatalogo> items = originales.stream().map(t -> ItemCatalogo.builder()
                .id(t.getId()).nombre(t.getNombre()).hijos(t.getTubos() != null ? t.getTubos().size() : 0)
                .existeEnDestino(tipoMuestraRepository.findByNombreIgnoreCaseAndInstitucion_Id(t.getNombre(), destino.getId()).isPresent())
                .build()).toList();
        return PreviewCatalogo.builder().catalogo(TipoCatalogo.TIPOS_MUESTRA).items(items).build();
    }

    public PreviewCatalogo previewEstudiosMuestra(Institucion origen, Institucion destino) {
        List<TipoEstudioMuestra> originales = tipoEstudioMuestraRepository.findAllByInstitucion_IdOrderByNombreAsc(origen.getId());
        List<ItemCatalogo> items = originales.stream().map(t -> ItemCatalogo.builder()
                .id(t.getId()).nombre(t.getNombre()).hijos(t.getParametros() != null ? t.getParametros().size() : 0)
                .existeEnDestino(tipoEstudioMuestraRepository.findByNombreIgnoreCaseAndInstitucion_Id(t.getNombre(), destino.getId()).isPresent())
                .build()).toList();
        return PreviewCatalogo.builder().catalogo(TipoCatalogo.ESTUDIOS_MUESTRA).items(items).build();
    }

    // ── Utilidad ─────────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface IdExtractor<T> {
        Long getId(T entity);
    }

    private <T> List<T> filtrar(List<T> todos, List<Long> ids, IdExtractor<T> extractor) {
        if (ids == null || ids.isEmpty()) return todos;
        Set<Long> idSet = Set.copyOf(ids);
        return todos.stream().filter(e -> idSet.contains(extractor.getId(e))).collect(Collectors.toList());
    }
}
