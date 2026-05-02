package imss.gob.mx.cohorte.services.Escalonpruebas;

import imss.gob.mx.cohorte.modules.escalonPrueba.medicion.PruebaEscalonMedicion;
import imss.gob.mx.cohorte.modules.escalonPrueba.medicion.PruebaEscalonMedicionRepository;
import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicionServiceTest {

    @Mock
    private PruebaEscalonMedicionRepository medicionRepository;
    @Mock
    private PruebaEscalonEtapaRepository etapaRepository;

    @InjectMocks
    private MedicionService service;

    @Test
    void updateSavesManagedEntity() {
        PruebaEscalonMedicion persisted = new PruebaEscalonMedicion();
        persisted.setId(1L);
        persisted.setParametro(PruebaEscalonMedicion.Parametro.PULSO);
        persisted.setValor(10.0);

        PruebaEscalonMedicion incoming = new PruebaEscalonMedicion();
        incoming.setId(1L);
        incoming.setParametro(PruebaEscalonMedicion.Parametro.FCM);
        incoming.setValor(88.0);
        incoming.setUnidad("lpm");

        when(medicionRepository.findById(1L)).thenReturn(Optional.of(persisted));
        when(medicionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(incoming);

        ArgumentCaptor<PruebaEscalonMedicion> captor = ArgumentCaptor.forClass(PruebaEscalonMedicion.class);
        verify(medicionRepository).save(captor.capture());
        assertEquals(PruebaEscalonMedicion.Parametro.FCM, captor.getValue().getParametro());
        assertEquals(88.0, captor.getValue().getValor());
        assertEquals("lpm", captor.getValue().getUnidad());
    }
}
