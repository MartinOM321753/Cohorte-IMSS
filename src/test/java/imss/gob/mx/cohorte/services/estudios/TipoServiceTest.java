package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipoServiceTest {

    @Mock
    private TipoEstudioRepository repository;

    @InjectMocks
    private TipoService service;

    @Test
    void updatePersistsChangedName() {
        TipoEstudio persisted = new TipoEstudio();
        persisted.setId(1L);
        persisted.setNombre("Anterior");
        persisted.setDescripcion("desc");
        persisted.setActivo(true);

        TipoEstudio incoming = new TipoEstudio();
        incoming.setId(1L);
        incoming.setNombre("Nuevo");
        incoming.setDescripcion("actualizada");
        incoming.setActivo(true);

        when(repository.findById(1L)).thenReturn(Optional.of(persisted));
        when(repository.findByNombre("Nuevo")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TipoEstudio updated = service.update(incoming);

        assertEquals("Nuevo", updated.getNombre());
        assertEquals("actualizada", updated.getDescripcion());
    }
}
