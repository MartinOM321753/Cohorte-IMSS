package imss.gob.mx.cohorte.controllers.analisis.dto;

/** Conteo de una categoría dentro de una celda (métricas categóricas). */
public record CategoriaConteoDTO(String categoria, long conteo, double proporcion) {}
