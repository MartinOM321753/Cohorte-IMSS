package imss.gob.mx.cohorte.modules.paciente;

/** Proyección ligera de un participante para operaciones masivas por folio. */
public interface PacienteFolioProyeccion {
    String getUuid();
    String getFolio();
    Long getIdInstitucion();
}
