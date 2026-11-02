package imss.gob.mx.cohorte.controllers.analisis.dto;

/** Una barra del histograma de distribución: [desde, hasta) con su conteo. */
public record BinHistogramaDTO(double desde, double hasta, long conteo) {}
