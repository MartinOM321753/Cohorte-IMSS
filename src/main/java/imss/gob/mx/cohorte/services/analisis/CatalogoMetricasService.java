package imss.gob.mx.cohorte.services.analisis;

import imss.gob.mx.cohorte.modules.analisis.FuenteMetrica;
import imss.gob.mx.cohorte.modules.analisis.Metrica;
import imss.gob.mx.cohorte.modules.analisis.TipoMetrica;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudioRepository;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.ExamenRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Construye el catálogo de {@link Metrica} analizables para un alcance de
 * instituciones. El catálogo es metadato (qué variables existen), no datos de
 * participantes, así que puede listar todos los parámetros/exámenes activos del
 * alcance sin comprometer la privacidad; la supresión y el aislamiento operan
 * después, al agregar los resultados.
 *
 * <p>Fase 1 cubre estudios médicos, exámenes y somatometría. Los estudios de
 * muestra se suman en la Fase 4, cuando se cablea su agregación.</p>
 */
@Service
@RequiredArgsConstructor
public class CatalogoMetricasService {

    private final ParametroEstudioRepository        parametroEstudioRepository;
    private final ExamenRepository                  examenRepository;
    private final ParametroEstudioMuestraRepository parametroEstudioMuestraRepository;

    /** Prefijos de clave por fuente — también los usa el motor de agregación para enrutar. */
    public static final String PREFIJO_ESTUDIO = "estudio.param.";
    public static final String PREFIJO_EXAMEN  = "examen.";
    public static final String PREFIJO_SOMATO  = "somato.";
    public static final String PREFIJO_MUESTRA = "muestra.param.";

    @Transactional(readOnly = true)
    public List<Metrica> construir(Collection<Long> instituciones) {
        List<Metrica> catalogo = new ArrayList<>();
        if (instituciones == null || instituciones.isEmpty()) {
            return catalogo;
        }

        // ── Estudios médicos: cada parámetro activo es una métrica ──────────────
        for (ParametroEstudio p : parametroEstudioRepository.findActivosByInstituciones(instituciones)) {
            catalogo.add(new Metrica(
                    PREFIJO_ESTUDIO + p.getId(),
                    FuenteMetrica.ESTUDIO_MEDICO,
                    p.getNombre(),
                    p.getTipoEstudio() != null ? p.getTipoEstudio().getNombre() : "Estudio",
                    p.getUnidad(),
                    aTipoMetrica(p.getTipo()),
                    p.getValorMinHombres(), p.getValorMaxHombres(),
                    p.getValorMinMujeres(), p.getValorMaxMujeres(),
                    p.getId(),
                    p.getTipoEstudio() != null && p.getTipoEstudio().getInstitucion() != null
                            ? p.getTipoEstudio().getInstitucion().getId() : null
            ));
        }

        // ── Exámenes de laboratorio: numéricos con rango por sexo ───────────────
        for (Examen e : examenRepository.findActivosByInstituciones(instituciones)) {
            catalogo.add(new Metrica(
                    PREFIJO_EXAMEN + e.getId(),
                    FuenteMetrica.EXAMEN,
                    e.getParametro(),
                    "Exámenes de laboratorio",
                    e.getUnidad(),
                    TipoMetrica.NUMERICO,
                    e.getValorMinHombres(), e.getValorMaxHombres(),
                    e.getValorMinMujeres(), e.getValorMaxMujeres(),
                    e.getId(),
                    e.getInstitucion() != null ? e.getInstitucion().getId() : null
            ));
        }

        // ── Estudios de muestra: cada parámetro activo es una métrica ───────────
        for (ParametroEstudioMuestra p : parametroEstudioMuestraRepository.findActivosByInstituciones(instituciones)) {
            catalogo.add(new Metrica(
                    PREFIJO_MUESTRA + p.getId(),
                    FuenteMetrica.ESTUDIO_MUESTRA,
                    p.getNombre(),
                    p.getTipoEstudioMuestra() != null ? p.getTipoEstudioMuestra().getNombre() : "Estudio de muestra",
                    p.getUnidad(),
                    aTipoMetricaNombre(p.getTipo() != null ? p.getTipo().name() : "TEXTO"),
                    p.getValorMinHombres(), p.getValorMaxHombres(),
                    p.getValorMinMujeres(), p.getValorMaxMujeres(),
                    p.getId(),
                    p.getTipoEstudioMuestra() != null && p.getTipoEstudioMuestra().getInstitucion() != null
                            ? p.getTipoEstudioMuestra().getInstitucion().getId() : null
            ));
        }

        // ── Somatometría: columnas fijas, todas numéricas, sin rango por sexo ───
        catalogo.addAll(metricasSomatometria());

        return catalogo;
    }

    /** Las columnas fijas de somatometría como métricas numéricas. */
    private List<Metrica> metricasSomatometria() {
        return List.of(
                somato("pesoKg",                    "Peso",                   "kg"),
                somato("tallaM",                    "Talla",                  "m"),
                somato("imc",                       "IMC",                    "kg/m²"),
                somato("presionSistolica",          "Presión sistólica",      "mmHg"),
                somato("presionDiastolica",         "Presión diastólica",     "mmHg"),
                somato("circunferenciaAbdominalCm", "Circunferencia abdominal", "cm"),
                somato("frecuenciaCardiacaReposo",  "Frecuencia cardiaca en reposo", "lpm")
        );
    }

    private Metrica somato(String campo, String etiqueta, String unidad) {
        return new Metrica(
                PREFIJO_SOMATO + campo,
                FuenteMetrica.SOMATOMETRIA,
                etiqueta,
                "Somatometría",
                unidad,
                TipoMetrica.NUMERICO,
                null, null, null, null,
                null,   // sin id de catálogo: es columna fija
                null    // no pertenece a una institución concreta
        );
    }

    /** Devuelve una métrica puntual por su clave, o vacío si no está en el alcance. */
    @Transactional(readOnly = true)
    public Optional<Metrica> buscar(String clave, Collection<Long> instituciones) {
        return construir(instituciones).stream()
                .filter(m -> m.clave().equals(clave))
                .findFirst();
    }

    private TipoMetrica aTipoMetrica(TipoParametro tipo) {
        return tipo == TipoParametro.NUMERICO ? TipoMetrica.NUMERICO : TipoMetrica.CATEGORICO;
    }

    /** Variante por nombre, para parámetros de muestra cuyo enum de tipo es independiente. */
    private TipoMetrica aTipoMetricaNombre(String nombre) {
        return "NUMERICO".equals(nombre) ? TipoMetrica.NUMERICO : TipoMetrica.CATEGORICO;
    }
}

