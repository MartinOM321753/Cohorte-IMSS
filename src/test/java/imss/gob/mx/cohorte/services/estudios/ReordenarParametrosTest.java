package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudioRepository;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudioRepository;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Cómo se guarda el orden que el administrador configuró para los parámetros de un
 * tipo de estudio.
 *
 * <p>El orden se manda entero, como la lista completa de ids de la primera posición
 * a la última. Lo que estas pruebas fijan es qué pasa cuando esa lista no describe
 * el catálogo que hay: el caso interesante no es el reordenamiento feliz, sino el
 * de dos administradores con la misma pantalla abierta, donde uno agrega o borra un
 * parámetro mientras el otro arrastra. Ahí la lista que llega ya no corresponde a
 * nada y aplicarla a medias dejaría parámetros en posiciones que nadie eligió.</p>
 */
class ReordenarParametrosTest {

    private ParametroEstudioRepository repositorio;
    private ParametroEstudioService servicio;

    private static final Long TIPO = 7L;

    private final List<ParametroEstudio> catalogo = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repositorio = mock(ParametroEstudioRepository.class);
        servicio = new ParametroEstudioService(repositorio, mock(ResultadoEstudioRepository.class));

        catalogo.clear();
        catalogo.add(parametro(1L, "Glucosa", 0));
        catalogo.add(parametro(2L, "Colesterol", 1));
        catalogo.add(parametro(3L, "Triglicéridos", 2));

        when(repositorio.findAllByTipoEstudio_Id(TIPO)).thenReturn(catalogo);
        when(repositorio.saveAll(any())).thenAnswer(inv -> new ArrayList<>((List<?>) inv.getArgument(0)));
    }

    private ParametroEstudio parametro(long id, String nombre, int orden) {
        ParametroEstudio p = new ParametroEstudio();
        p.setId(id);
        p.setNombre(nombre);
        p.setTipo(TipoParametro.NUMERICO);
        p.setOrden(orden);
        TipoEstudio tipo = new TipoEstudio();
        tipo.setId(TIPO);
        p.setTipoEstudio(tipo);
        return p;
    }

    private ParametroEstudio del(long id) {
        return catalogo.stream().filter(p -> p.getId() == id).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("El orden que llega es el que queda, numerado desde cero y sin huecos")
    void aplicaElOrdenRecibido() {
        List<ParametroEstudio> resultado = servicio.reordenar(TIPO, List.of(3L, 1L, 2L));

        assertEquals(0, del(3L).getOrden());
        assertEquals(1, del(1L).getOrden());
        assertEquals(2, del(2L).getOrden());
        assertEquals(List.of(3L, 1L, 2L), resultado.stream().map(ParametroEstudio::getId).toList());
    }

    @Test
    @DisplayName("Una lista a la que le falta un parámetro se rechaza entera")
    void rechazaListaIncompleta() {
        assertThrows(ObjConflictException.class, () -> servicio.reordenar(TIPO, List.of(3L, 1L)));

        // Lo que importa no es la excepción, sino que nadie quedó movido a medias.
        assertEquals(0, del(1L).getOrden());
        assertEquals(1, del(2L).getOrden());
        assertEquals(2, del(3L).getOrden());
        verify(repositorio, never()).saveAll(any());
    }

    @Test
    @DisplayName("Una lista con un parámetro ajeno al tipo se rechaza entera")
    void rechazaParametroAjeno() {
        assertThrows(ObjConflictException.class, () -> servicio.reordenar(TIPO, List.of(3L, 1L, 2L, 99L)));
        verify(repositorio, never()).saveAll(any());
    }

    @Test
    @DisplayName("Una lista con un id repetido se rechaza: dos posiciones para el mismo parámetro")
    void rechazaRepetidos() {
        assertThrows(ObjConflictException.class, () -> servicio.reordenar(TIPO, List.of(1L, 1L, 2L)));
        verify(repositorio, never()).saveAll(any());
    }

    @Test
    @DisplayName("Un parámetro nuevo nace al final, no al principio")
    void elNuevoVaAlFinal() {
        when(repositorio.findByTipoEstudio_IdAndNombreIgnoreCase(anyLong(), any()))
                .thenReturn(Optional.empty());
        when(repositorio.save(any(ParametroEstudio.class))).thenAnswer(inv -> inv.getArgument(0));

        ParametroEstudio nuevo = parametro(4L, "Hemoglobina", 0);
        ParametroEstudio creado = servicio.create(nuevo);

        assertEquals(3, creado.getOrden(),
                "Con 0, 1 y 2 ocupados, el siguiente es el 3; en cero se habría colado al principio");
    }
}
