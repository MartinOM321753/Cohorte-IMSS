package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.adjuntos.EstudioAdjunto;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.estudios.EstudioService;
import imss.gob.mx.cohorte.services.estudios.ParametroEstudioService;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstudiosApplicationServiceTest {

    @Mock
    private EstudioService estudioService;
    @Mock
    private TipoService tipoService;
    @Mock
    private PacienteService pacienteService;
    @Mock
    private UserService userService;
    @Mock
    private ParametroEstudioService parametroService;

    @InjectMocks
    private EstudiosApplicationService service;

    @Test
    void createEstudioNormalizesAndPersistsChildren() {
        TipoEstudio tipo = tipo(10L);
        ParametroEstudio parametro = parametro(100L, tipo);
        EstudioMedico request = estudioBase(10L);
        request.setResultadoEstudio(List.of(resultado(100L, 123.4, null, null, null, null)));
        request.setAdjuntos(List.of(adjunto(" PDF ", " reporte.pdf ", " application/pdf ", " /tmp/reporte.pdf ", "  nota  ", 0)));

        when(pacienteService.getByUUID("paciente-1")).thenReturn(paciente("paciente-1"));
        when(userService.getByUUID("usuario-1")).thenReturn(usuario("usuario-1"));
        when(tipoService.getOne(10L)).thenReturn(tipo);
        when(parametroService.getOne(100L)).thenReturn(parametro);
        when(estudioService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EstudioMedico created = service.createEstudio(request);

        assertNotNull(created.getFechaRegistro());
        assertEquals("ROOT", created.getResultadoEstudio().getFirst().getGrupoCodigo());
        assertEquals(0, created.getResultadoEstudio().getFirst().getOrdenResultado());
        assertSame(created, created.getResultadoEstudio().getFirst().getEstudio());
        assertEquals("PDF", created.getAdjuntos().getFirst().getTipo());
        assertEquals("reporte.pdf", created.getAdjuntos().getFirst().getNombreOriginal());
        assertEquals("application/pdf", created.getAdjuntos().getFirst().getMimeType());
        assertEquals("/tmp/reporte.pdf", created.getAdjuntos().getFirst().getRutaUrl());
        assertEquals("nota", created.getAdjuntos().getFirst().getDescripcion());
        assertSame(created, created.getAdjuntos().getFirst().getEstudio());
    }

    @Test
    void updateEstudioReplacesResultsAndAdjuntos() {
        TipoEstudio tipo = tipo(10L);
        ParametroEstudio parametro = parametro(101L, tipo);
        EstudioMedico existing = estudioBase(10L);
        existing.setId(1L);
        existing.setResultadoEstudio(new ArrayList<>(List.of(resultado(999L, 1.0, null, null, "ROOT", 0))));
        existing.setAdjuntos(new ArrayList<>(List.of(adjunto("PDF", "old.pdf", "application/pdf", "/tmp/old.pdf", null, 0))));

        EstudioMedico incoming = estudioBase(10L);
        incoming.setResultadoEstudio(List.of(resultado(101L, null, "texto", null, "ETAPA_1", 1)));
        incoming.setAdjuntos(List.of(adjunto("PDF", "nuevo.pdf", "application/pdf", "/tmp/nuevo.pdf", null, 0)));

        when(pacienteService.getByUUID("paciente-1")).thenReturn(paciente("paciente-1"));
        when(userService.getByUUID("usuario-1")).thenReturn(usuario("usuario-1"));
        when(tipoService.getOne(10L)).thenReturn(tipo);
        when(parametroService.getOne(101L)).thenReturn(parametro);
        when(estudioService.getOne(1L)).thenReturn(existing);
        when(estudioService.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EstudioMedico updated = service.updateEstudio(1L, incoming);

        assertEquals(1, updated.getResultadoEstudio().size());
        assertEquals("ETAPA_1", updated.getResultadoEstudio().getFirst().getGrupoCodigo());
        assertEquals(1, updated.getResultadoEstudio().getFirst().getOrdenResultado());
        assertSame(updated, updated.getResultadoEstudio().getFirst().getEstudio());
        assertNull(updated.getResultadoEstudio().getFirst().getId());
        assertEquals(1, updated.getAdjuntos().size());
        assertEquals("nuevo.pdf", updated.getAdjuntos().getFirst().getNombreOriginal());
        assertSame(updated, updated.getAdjuntos().getFirst().getEstudio());
    }

    @Test
    void createEstudioRejectsParametroFromDifferentStudyType() {
        TipoEstudio tipo = tipo(10L);
        TipoEstudio otroTipo = tipo(20L);
        EstudioMedico request = estudioBase(10L);
        request.setResultadoEstudio(List.of(resultado(100L, 123.4, null, null, null, null)));

        when(pacienteService.getByUUID("paciente-1")).thenReturn(paciente("paciente-1"));
        when(userService.getByUUID("usuario-1")).thenReturn(usuario("usuario-1"));
        when(tipoService.getOne(10L)).thenReturn(tipo);
        when(parametroService.getOne(100L)).thenReturn(parametro(100L, otroTipo));

        assertThrows(ObjConflictException.class, () -> service.createEstudio(request));
    }

    @Test
    void createEstudioRejectsDuplicatedResultCompositeKey() {
        TipoEstudio tipo = tipo(10L);
        ParametroEstudio parametro = parametro(100L, tipo);
        EstudioMedico request = estudioBase(10L);
        request.setResultadoEstudio(List.of(
                resultado(100L, 123.4, null, null, "BASAL", 1),
                resultado(100L, null, "texto", null, "BASAL", 1)
        ));

        when(pacienteService.getByUUID("paciente-1")).thenReturn(paciente("paciente-1"));
        when(userService.getByUUID("usuario-1")).thenReturn(usuario("usuario-1"));
        when(tipoService.getOne(10L)).thenReturn(tipo);
        when(parametroService.getOne(100L)).thenReturn(parametro);

        assertThrows(ObjConflictException.class, () -> service.createEstudio(request));
    }

    private EstudioMedico estudioBase(Long tipoId) {
        EstudioMedico estudio = new EstudioMedico();
        Paciente paciente = new Paciente();
        paciente.setUuid("paciente-1");
        estudio.setPaciente(paciente);
        BeanUser usuario = new BeanUser();
        usuario.setUUID("usuario-1");
        estudio.setUsuarioRealiza(usuario);
        TipoEstudio tipo = new TipoEstudio();
        tipo.setId(tipoId);
        estudio.setTipoEstudio(tipo);
        estudio.setFechaEstudio(LocalDate.of(2026, 4, 20));
        estudio.setResultadoEstudio(new ArrayList<>());
        estudio.setAdjuntos(new ArrayList<>());
        return estudio;
    }

    private ResultadoEstudio resultado(Long parametroId, Double valorNumerico, String valorTexto, Boolean valorBooleano, String grupo, Integer orden) {
        ResultadoEstudio resultado = new ResultadoEstudio();
        ParametroEstudio parametro = new ParametroEstudio();
        parametro.setId(parametroId);
        resultado.setParametro(parametro);
        resultado.setValorNumerico(valorNumerico);
        resultado.setValorTexto(valorTexto);
        resultado.setValorBooleano(valorBooleano);
        resultado.setGrupoCodigo(grupo);
        resultado.setOrdenResultado(orden);
        return resultado;
    }

    private EstudioAdjunto adjunto(String tipo, String nombreOriginal, String mimeType, String rutaUrl, String descripcion, int orden) {
        EstudioAdjunto adjunto = new EstudioAdjunto();
        adjunto.setTipo(tipo);
        adjunto.setNombreOriginal(nombreOriginal);
        adjunto.setMimeType(mimeType);
        adjunto.setRutaUrl(rutaUrl);
        adjunto.setDescripcion(descripcion);
        adjunto.setOrden(orden);
        return adjunto;
    }

    private Paciente paciente(String uuid) {
        Paciente paciente = new Paciente();
        paciente.setUuid(uuid);
        paciente.setActivo(true);
        return paciente;
    }

    private BeanUser usuario(String uuid) {
        BeanUser usuario = new BeanUser();
        usuario.setUUID(uuid);
        usuario.setActivo(true);
        return usuario;
    }

    private TipoEstudio tipo(Long id) {
        TipoEstudio tipo = new TipoEstudio();
        tipo.setId(id);
        tipo.setActivo(true);
        tipo.setNombre("Tipo " + id);
        return tipo;
    }

    private ParametroEstudio parametro(Long id, TipoEstudio tipo) {
        ParametroEstudio parametro = new ParametroEstudio();
        parametro.setId(id);
        parametro.setTipoEstudio(tipo);
        parametro.setNombre("Parametro " + id);
        return parametro;
    }
}
