package imss.gob.mx.cohorte.modules.cuestionarios.variables;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.utils.texto.NormalizadorAlias;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * Una pregunta del instrumento: {@code P13_9}, {@code P6}, {@code P52_1}.
 *
 * <h3>El código es del investigador, no nuestro</h3>
 *
 * <p>{@code codigo} es el que aparece en el catálogo que se entregará a los
 * investigadores para pedir información. No se puede renombrar por comodidad
 * interna: una solicitud que pida «P13_9» tiene que resolverse a esta fila, hoy y
 * en cinco años. Lo que sí podemos es extenderlo con todo lo que el catálogo no
 * dice —el tipo real, la obligatoriedad, la dependencia—, y eso es justamente lo
 * que guardan los demás campos.</p>
 *
 * <h3>Por qué el código y la columna del volcado son dos cosas</h3>
 *
 * <p>Casi siempre coinciden, pero no siempre, y las excepciones no son erratas que
 * se puedan corregir: el catálogo dice {@code P12_ 1} con un espacio, dice
 * {@code P29_1} donde el archivo trae {@code P29_2}, y el archivo trae una columna
 * {@code P10x1} que el catálogo llama {@code P130_7}. Las dos formas son reales
 * —una la usa quien pide datos, la otra la escribe el escáner— y cualquiera de
 * las dos que se elimine deja un lado del puente sin resolver. La columna además
 * se guarda normalizada, igual que los alias de estudios, para que el
 * emparejamiento no dependa de espacios ni acentos.</p>
 *
 * <h3>Opciones en LAZY, al contrario que en estudios</h3>
 *
 * <p>{@code ParametroEstudio} carga sus opciones en EAGER porque un tipo de
 * estudio tiene decenas de parámetros y siempre se necesitan juntos. Aquí sería
 * ruinoso: el instrumento principal pasa de dos mil variables y más de doce mil
 * opciones, y una pantalla que muestra una sección de cuarenta preguntas no tiene
 * por qué traer el resto. Van en LAZY con {@code @BatchSize}, que agrupa las
 * consultas de las variables que sí se están mostrando.</p>
 */
@Entity
@Table(name = "variable_cuestionario",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_variable_cuestionario_codigo",
                        columnNames = {"id_tipo_cuestionario", "codigo"}),
                @UniqueConstraint(name = "uk_variable_cuestionario_columna",
                        columnNames = {"id_tipo_cuestionario", "columna_normalizada"})
        },
        indexes = {
                @Index(name = "ix_variable_cuestionario_seccion", columnList = "id_seccion, orden")
        })
@Getter
@Setter
@NoArgsConstructor
public class VariableCuestionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_variable")
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_cuestionario", nullable = false)
    private TipoCuestionario tipoCuestionario;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_seccion")
    private SeccionCuestionario seccion;

    /** Código del catálogo de los investigadores. Ver la nota de la clase. */
    @Column(name = "codigo", nullable = false, length = 60)
    private String codigo;

    /**
     * Encabezado con el que el volcado de Remark titula esta variable. Vacío cuando
     * la variable existe en el catálogo pero el instrumento no la produce.
     */
    @Column(name = "columna_volcado", length = 100)
    private String columnaVolcado;

    /** Forma canónica de {@link #columnaVolcado}, contra la que se emparejan los encabezados. */
    @Column(name = "columna_normalizada", length = 100)
    private String columnaNormalizada;

    /** Enunciado tal como está impreso. */
    @Column(name = "pregunta", length = 2000)
    private String pregunta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoVariable tipo = TipoVariable.OPCION_UNICA;

    @Column(name = "orden", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer orden = 0;

    /**
     * Página del papel donde está impresa. Redundante con la de la sección y aun
     * así necesaria: hay bloques que cruzan el salto de página, y quien cotejea una
     * hoja escaneada necesita el número de <em>esta</em> pregunta.
     */
    @Column(name = "pagina")
    private Integer pagina;

    /**
     * Grupo de selección múltiple al que pertenece. Las variables que comparten
     * grupo son casillas de una misma pregunta de la que se puede marcar más de una.
     *
     * <p>Es un texto y no una relación porque el grupo no tiene atributos propios:
     * es el código de la pregunta madre ({@code P19}, {@code P35}), que ya existe
     * como variable o como sección. Añadir una tabla para eso solo agregaría un
     * salto más a cada lectura.</p>
     */
    @Column(name = "grupo_multiple", length = 60)
    private String grupoMultiple;

    /**
     * Código de la variable que habilita a esta. Si la llave no viene contestada
     * como {@link #valorHabilitante}, esta pregunta no se muestra ni se exige.
     *
     * <p>Es el «si contestó sí, pase a…» del papel. Guardarlo permite dos cosas que
     * el papel no da: que el formulario en pantalla no pregunte lo que no toca, y
     * que al importar se detecte la incoherencia —una dependiente contestada con la
     * llave en «no»— en vez de archivarla en silencio. Esa misma coherencia fue la
     * que permitió determinar el sentido de los códigos de P14, P19, P23 y P183.</p>
     */
    @Column(name = "depende_de", length = 60)
    private String dependeDe;

    /** Código de respuesta de {@link #dependeDe} que habilita esta variable. */
    @Column(name = "valor_habilitante", length = 60)
    private String valorHabilitante;

    /**
     * Si la llave tiene que valer el valor habilitante o, al revés, cualquier cosa
     * menos ese. Ver {@link OperadorDependencia}: el «Ninguna» de la 112 no se puede
     * expresar con una igualdad.
     *
     * <h3>Por qué lleva el valor por omisión escrito en la columna</h3>
     *
     * <p>El esquema se actualiza solo, y al añadir una columna obligatoria a una tabla
     * que ya tiene filas, MySQL las rellena con el <b>primer valor</b> del tipo. Para un
     * enum ese orden lo decide el alfabeto, no nosotros: sin este {@code DEFAULT}, las
     * preguntas que ya existían nacieron con {@code DISTINTO} —la D va antes que la I—,
     * que es justo lo contrario de lo que significa no tener dependencia. Pasó de
     * verdad, en las diecisiete preguntas de prueba.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operador_dependencia", nullable = false,
            columnDefinition = "VARCHAR(10) DEFAULT 'IGUAL'")
    private OperadorDependencia operadorDependencia = OperadorDependencia.IGUAL;

    @Column(name = "obligatoria", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean obligatoria = false;

    /** Mínimo admitido, para NUMERO y NUMERO_CON_ESCAPE. */
    @Column(name = "valor_min")
    private Double valorMin;

    @Column(name = "valor_max")
    private Double valorMax;

    /** Largo máximo del texto manuscrito que cabe en la rejilla del papel. */
    @Column(name = "longitud_max")
    private Integer longitudMax;

    /** Unidad impresa junto a la rejilla: años, kg, veces por semana. */
    @Column(name = "unidad", length = 30)
    private String unidad;

    /**
     * Si el enunciado y las opciones están confirmados contra el papel.
     *
     * <p>Hay sesenta variables que el volcado produce y ningún catálogo documenta
     * —{@code P52_1} a {@code P52_56} y {@code P159_3} a {@code P159_6}—. Dejarlas
     * fuera perdería datos que ya llegan; darlas por buenas las mostraría al
     * capturista sin enunciado. Se guardan marcadas, se importan, y la pantalla las
     * señala como pendientes de documentar.</p>
     */
    @Column(name = "documentada", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean documentada = true;

    /**
     * Una variable fuera de uso no se pide al capturar, pero sus respuestas
     * anteriores siguen siendo válidas. Es la alternativa al borrado, imposible en
     * cuanto existe una sola respuesta.
     */
    @Column(name = "activa", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean activa = true;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    /**
     * La aclaración que el papel imprime junto a la pregunta, para mostrársela a
     * quien la contesta.
     *
     * <h3>Por qué no vale {@link #observaciones}</h3>
     *
     * <p>Son dos cosas distintas y mezclarlas acabaría enseñándole al participante
     * notas que no son para él. {@code observaciones} es para nosotros —«este código
     * no cuadra con el volcado», «falta cotejar contra el papel»—; esto es parte del
     * instrumento y sin ello la pregunta cambia de significado.</p>
     *
     * <p>Ejemplos reales: «considerando horario de 24 hrs» en la hora de acostarse,
     * «esto puede ser diferente del número de horas que pasó en la cama» en las horas
     * de sueño, «5 años equivalen a 60 meses» en los anticonceptivos. En papel van
     * impresas al lado; al quitar el papel hay que seguir dándolas.</p>
     */
    @Column(name = "instruccion", length = 1000)
    private String instruccion;

    /**
     * La figura sin la cual la pregunta no se puede contestar, por su ruta.
     *
     * <p>No es decoración: hay preguntas que <b>son</b> la imagen. «¿Qué figura
     * representa mejor su cuerpo?» no significa nada sin las nueve siluetas, y
     * «suponga qué tanto vello presenta» tampoco sin la rejilla de treinta y seis
     * dibujos. Son quince en el instrumento principal.</p>
     */
    @Column(name = "figura", length = 200)
    private String figura;

    /**
     * Fila y columna que ocupa la pregunta cuando su bloque se pinta como rejilla.
     *
     * <p>Se derivan del enunciado al cargar el catálogo —{@code Caminar Lunes} es la
     * fila {@code Caminar} y la columna {@code Lunes}— y <b>se guardan</b> en lugar
     * de recalcularse al pintar. Adivinar con el texto en cada pantallazo sería
     * frágil y, sobre todo, no habría dónde corregir el caso en que el enunciado no
     * coopera: así alguien lo revisa una vez y queda.</p>
     *
     * <p>Nulos en la inmensa mayoría de las preguntas, que no viven en ninguna
     * rejilla. Nulos también dentro de un bloque de rejilla cuando la pregunta no es
     * una celda, como el «Ninguna» que encabeza la 112.</p>
     */
    @Column(name = "fila_matriz", length = 150)
    private String filaMatriz;

    @Column(name = "columna_matriz", length = 150)
    private String columnaMatriz;

    @OneToMany(mappedBy = "variable", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("orden ASC, id ASC")
    @BatchSize(size = 50)
    private List<OpcionVariable> opciones = new ArrayList<>();

    /**
     * La forma normalizada se deriva siempre de la columna, nunca se recibe de
     * fuera: si dependiera de que quien crea la fila se acuerde de calcularla,
     * bastaría un alta por otro camino para romper el emparejamiento en silencio.
     */
    @PrePersist
    @PreUpdate
    private void derivarNormalizada() {
        this.columnaNormalizada = (this.columnaVolcado == null || this.columnaVolcado.isBlank())
                ? null
                : NormalizadorAlias.normalizar(this.columnaVolcado);
    }

    /** Si admite más de una respuesta a la vez. */
    @Transient
    public boolean admiteVarias() {
        return this.tipo == TipoVariable.OPCION_MULTIPLE;
    }

    /** Si se contesta. Los encabezados de grupo no. */
    @Transient
    public boolean esContestable() {
        return this.tipo != TipoVariable.GRUPO;
    }
}
