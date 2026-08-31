package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Qué se puede insertar en un reporte.
 *
 * <p>El catálogo se arma <b>desde el catálogo real de la institución</b>: sus tipos
 * de estudio y los parámetros de cada uno. No es una lista fija, porque lo que se
 * puede imprimir depende de lo que esa institución mide.</p>
 *
 * <p>Vive en el servidor y se expone por API en lugar de estar escrito en el editor.
 * Con dos listas separadas —la que se ofrece y la que se sabe resolver— acabarían
 * divergiendo: se ofrecería un campo que nadie resuelve, o quedaría uno resoluble
 * que no se puede elegir. Las claves las construye {@link ClaveCampo}, la misma
 * clase que las interpreta, para que no se separen.</p>
 */
@Service
@AllArgsConstructor
public class CatalogoCamposReporte {

    /** Un campo suelto se mete dentro de un texto; un bloque ocupa su propia caja. */
    public enum Clase { CAMPO, BLOQUE }

    /**
     * @param clave       lo que se guarda en la plantilla
     * @param rotulo      lo que ve quien diseña
     * @param grupo       para agrupar el panel; con estudios, el nombre del estudio
     * @param clase       campo suelto o bloque
     * @param ayuda       qué es, cuando el rótulo no basta
     * @param idTipoEstudio de qué estudio viene, si viene de alguno
     * @param seleccionable si el bloque permite elegir qué filas muestra
     */
    public record Campo(String clave, String rotulo, String grupo, Clase clase,
                        String ayuda, Long idTipoEstudio, boolean seleccionable) {}

    private static final String G_PARTICIPANTE = "Participante";
    private static final String G_GENERAL      = "Generales";
    private static final String G_TOTALES      = "Totales";
    private static final String G_ESTUDIOS     = "Estudios";

    private final TipoService tipoService;

    /**
     * Todo lo insertable: los datos del participante y, por cada tipo de estudio del
     * catálogo, sus campos, sus parámetros uno a uno, su tabla y sus evidencias.
     *
     * <p>Que cada parámetro aparezca por separado es lo que permite escribir «la
     * densidad fue {{…}} y la masa magra {{…}}» dentro de un párrafo, en vez de
     * verse obligado a meter la tabla entera.</p>
     */
    @Transactional(readOnly = true)
    public List<Campo> todos() {
        List<Campo> campos = new ArrayList<>(fijos());

        // Activos e inactivos: una plantilla puede necesitar un estudio retirado del
        // catálogo si los participantes antiguos lo tienen hecho.
        for (TipoEstudio tipo : tipoService.getAllByInstitucion()) {
            if (tipo.getId() == null) continue;
            long id = tipo.getId();
            String grupo = tipo.getNombre();

            campos.add(campo(ClaveCampo.deCampoEstudio(id, "fecha"), "Fecha del estudio", grupo, id));
            campos.add(campo(ClaveCampo.deCampoEstudio(id, "realizo"), "Quién lo realizó", grupo, id));
            campos.add(campo(ClaveCampo.deCampoEstudio(id, "observaciones"), "Observaciones", grupo, id));

            for (ParametroEstudio p : parametrosDe(tipo)) {
                if (p.getId() == null) continue;
                String rotulo = p.getUnidad() != null && !p.getUnidad().isBlank()
                        ? p.getNombre() + " (" + p.getUnidad() + ")"
                        : p.getNombre();
                String ayuda = Boolean.FALSE.equals(p.getActivo())
                        ? "Fuera de uso — solo saldrá en estudios que ya lo midieron"
                        : null;
                campos.add(new Campo(ClaveCampo.deParametro(id, p.getId()),
                        rotulo, grupo, Clase.CAMPO, ayuda, id, false));
            }

            campos.add(new Campo(ClaveCampo.deBloqueResultados(id),
                    "Tabla de resultados", grupo, Clase.BLOQUE,
                    "Los parámetros de este estudio, con unidad y referencia. Puedes elegir cuáles.",
                    id, true));
            campos.add(new Campo(ClaveCampo.deBloqueEvidencias(id),
                    "Evidencias adjuntas", grupo, Clase.BLOQUE,
                    "Los archivos adjuntos de este estudio.", id, false));
        }

        return campos;
    }

    /** Los que no dependen de ningún estudio. */
    private List<Campo> fijos() {
        return List.of(
                campo(ResolvedorCampos.PARTICIPANTE_NOMBRE, "Nombre completo", G_PARTICIPANTE, null),
                campo(ResolvedorCampos.PARTICIPANTE_FOLIO, "Folio", G_PARTICIPANTE, null),
                new Campo(ResolvedorCampos.PARTICIPANTE_EDAD, "Edad", G_PARTICIPANTE, Clase.CAMPO,
                        "Se calcula a la fecha de emisión; no se guarda en el expediente", null, false),
                campo(ResolvedorCampos.PARTICIPANTE_SEXO, "Sexo", G_PARTICIPANTE, null),
                campo(ResolvedorCampos.PARTICIPANTE_CURP, "CURP", G_PARTICIPANTE, null),
                campo(ResolvedorCampos.PARTICIPANTE_NACIMIENTO, "Fecha de nacimiento", G_PARTICIPANTE, null),

                campo(ResolvedorCampos.INSTITUCION_NOMBRE, "Institución", G_GENERAL, null),
                new Campo(ResolvedorCampos.EMISION_FECHA, "Fecha de emisión", G_GENERAL, Clase.CAMPO,
                        "Cuándo se generó el documento, no cuándo se hizo el estudio", null, false),

                campo(ResolvedorCampos.TOTAL_ESTUDIOS, "Total de estudios", G_TOTALES, null),
                campo(ResolvedorCampos.TOTAL_EXAMENES, "Total de exámenes", G_TOTALES, null),
                campo(ResolvedorCampos.TOTAL_MUESTRAS, "Total de muestras", G_TOTALES, null),

                new Campo(ClaveCampo.BLOQUE_LISTADO_ESTUDIOS, "Listado de estudios", G_ESTUDIOS,
                        Clase.BLOQUE, "Una tabla con los estudios del participante y su fecha.",
                        null, false));
    }

    /**
     * Los parámetros del tipo. Se piden a la entidad, que los trae consigo; si la
     * colección viniera vacía por no estar cargada, el estudio aparecería sin
     * parámetros y nadie entendería por qué.
     */
    private List<ParametroEstudio> parametrosDe(TipoEstudio tipo) {
        return tipo.getParametros() == null ? List.of() : tipo.getParametros();
    }

    private Campo campo(String clave, String rotulo, String grupo, Long idTipo) {
        return new Campo(clave, rotulo, grupo, Clase.CAMPO, null, idTipo, false);
    }
}
