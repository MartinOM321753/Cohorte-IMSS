package imss.gob.mx.cohorte.services.analisis;

import imss.gob.mx.cohorte.controllers.analisis.dto.CategoriaConteoDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.CeldaAgregadaDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.SerieAgregadaDTO;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Exporta las series agregadas a un archivo tabular. Fase 4: CSV (UTF-8 con BOM,
 * para que Excel respete los acentos). Es el carril de "agregados": baja lo mismo
 * que se ve en la gráfica, ya depurado por la supresión k-anónima. El carril
 * crudo (nivel participante) es aparte y con permiso mayor.
 */
@Service
public class AnalisisExportService {

    /** Resultado de una descarga: el nombre sugerido y el contenido en bytes. */
    public record Descarga(String nombreArchivo, byte[] contenido) {}

    private static final String[] COLS_NUM = {
            "n", "media", "mediana", "p25", "p75", "min", "max", "desv",
            "en_rango", "fuera_de_rango", "sin_referencia"
    };

    public Descarga aCsv(List<SerieAgregadaDTO> series, String separador) {
        String sep = ";".equals(separador) ? ";" : ",";

        // Columnas de dimensión: unión de las dimensiones pedidas (todas las series las comparten).
        LinkedHashSet<String> dims = new LinkedHashSet<>();
        for (SerieAgregadaDTO s : series) dims.addAll(s.dimensiones());

        List<String> cabecera = new ArrayList<>();
        cabecera.add("metrica");
        cabecera.add("unidad");
        cabecera.addAll(dims);
        for (String c : COLS_NUM) cabecera.add(c);
        cabecera.add("categoria");
        cabecera.add("conteo");
        cabecera.add("proporcion");

        StringBuilder sb = new StringBuilder("﻿");
        sb.append(unir(cabecera, sep)).append("\r\n");

        for (SerieAgregadaDTO serie : series) {
            for (CeldaAgregadaDTO celda : serie.celdas()) {
                if (celda.categorias() != null && !celda.categorias().isEmpty()) {
                    for (CategoriaConteoDTO cat : celda.categorias()) {
                        sb.append(fila(serie, celda, dims, cat, sep)).append("\r\n");
                    }
                } else {
                    sb.append(fila(serie, celda, dims, null, sep)).append("\r\n");
                }
            }
        }

        String nombre = "analisis_" + System.currentTimeMillis() + ".csv";
        return new Descarga(nombre, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String fila(SerieAgregadaDTO serie, CeldaAgregadaDTO celda, Set<String> dims,
                        CategoriaConteoDTO cat, String sep) {
        List<String> campos = new ArrayList<>();
        campos.add(serie.metricaEtiqueta());
        campos.add(serie.unidad() != null ? serie.unidad() : "");
        for (String d : dims) campos.add(celda.dimensiones().getOrDefault(d, ""));

        campos.add(Long.toString(celda.n()));
        campos.add(num(celda.media()));
        campos.add(num(celda.mediana()));
        campos.add(num(celda.p25()));
        campos.add(num(celda.p75()));
        campos.add(num(celda.min()));
        campos.add(num(celda.max()));
        campos.add(num(celda.desv()));
        campos.add(celda.enRango() != null ? Long.toString(celda.enRango()) : "");
        campos.add(celda.fueraDeRango() != null ? Long.toString(celda.fueraDeRango()) : "");
        campos.add(celda.sinReferencia() != null ? Long.toString(celda.sinReferencia()) : "");

        if (cat != null) {
            campos.add(cat.categoria());
            campos.add(Long.toString(cat.conteo()));
            campos.add(num(cat.proporcion()));
        } else {
            campos.add("");
            campos.add("");
            campos.add("");
        }
        return unir(campos, sep);
    }

    private String unir(List<String> campos, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(escapar(campos.get(i), sep));
        }
        return sb.toString();
    }

    private String escapar(String v, String sep) {
        if (v == null) return "";
        boolean necesita = v.contains(sep) || v.contains("\"") || v.contains("\n") || v.contains("\r");
        if (!necesita) return v;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private String num(Double v) {
        return v != null ? v.toString() : "";
    }
}
