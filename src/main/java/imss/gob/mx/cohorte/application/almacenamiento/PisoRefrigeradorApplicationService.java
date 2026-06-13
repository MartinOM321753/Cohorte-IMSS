package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.controllers.DTO.PisosDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPiso;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.PisoRefrigeradorService;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.PosicionPisoService;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.RefrigeradorService;


import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;


@Service
@RequireModulo(ModuloSistema.BIOBANCO)
public class PisoRefrigeradorApplicationService {

    private final PisoRefrigeradorService pisoService;
    private final RefrigeradorService refrigeradorService;
    private final PosicionPisoService posicionPisoService;

    @Autowired
    public PisoRefrigeradorApplicationService(PisoRefrigeradorService pisoService, 
                                            RefrigeradorService refrigeradorService, 
                                            PosicionPisoService posicionPisoService) {
        this.pisoService = pisoService;
        this.refrigeradorService = refrigeradorService;
        this.posicionPisoService = posicionPisoService;
    }

    @Transactional(readOnly = true)
    public List<PisoRefrigerador> getAllPisos(Long idRefrigerador) {
        return pisoService.getAllPisos(idRefrigerador);
    }

    @Transactional(readOnly = true)
    public PisoRefrigerador getPiso(Long id) {
        return pisoService.getPiso(id);
    }

    @Transactional(readOnly = true)
    public PisoRefrigerador getPisoByNumber(String number) {
        return pisoService.getPisoByNumber(number);
    }

    @Transactional(readOnly = true)
    public List<PosicionPiso> getPosiciones(Long idPiso) {
        return posicionPisoService.getPosicionesPorPiso(idPiso);
    }


    @Transactional
    public List<PisoRefrigerador> createPisos(PisosDTO pisosDTO) {

        Refrigerador refBD = refrigeradorService.getRefrigerador(pisosDTO.getIdRefrigerador());
        if (refBD == null) { throw new ObjNotFoundException("El refrigerador no existe");}

        for (PisoRefrigerador piso : pisosDTO.getPisos()) {
            if (piso.getNumeroPiso() != null && !piso.getNumeroPiso().isBlank()) {
                var existing = pisoService.findByNumber(piso.getNumeroPiso());
                if (existing.isPresent()) {
                    throw new ObjConflictException("Ya existe un piso con el número: " + piso.getNumeroPiso());
                }
            }

            piso.setRefrigerador(refBD);
            piso.setActivo(true);
            PisoRefrigerador pisoRefrigerador = pisoService.createPiso(piso);

            posicionPisoService.generarPosicionesParaPiso(pisoRefrigerador.getId(), pisoRefrigerador.getFilas(), pisoRefrigerador.getColumnas(), pisoRefrigerador.getAltura());

        }
        Refrigerador refrigerador = refrigeradorService.getRefrigerador(pisosDTO.getIdRefrigerador());

        return refrigerador.getPisos();
    }

    @Transactional
    public PisoRefrigerador updatePiso(Long id, PisoRefrigerador piso) {
        PisoRefrigerador pisoBD = pisoService.getPiso(id);

        int nuevasFilas = piso.getFilas() != null ? piso.getFilas() : pisoBD.getFilas();
        int nuevasColumnas = piso.getColumnas() != null ? piso.getColumnas() : pisoBD.getColumnas();
        int nuevaAltura = piso.getAltura() != null ? piso.getAltura() : pisoBD.getAltura();
        boolean cambiaronDimensiones = (nuevasFilas != pisoBD.getFilas()
            || nuevasColumnas != pisoBD.getColumnas()
            || nuevaAltura != pisoBD.getAltura());

        if (cambiaronDimensiones) {
            List<PosicionPiso> todasPosiciones = posicionPisoService.getPosicionesPorPiso(id);

            List<PosicionPiso> afectadas = todasPosiciones.stream()
                .filter(p -> p.getOcupada() && (
                    posicionPisoService.fromAlphabetLabel(p.getFila()) > nuevasFilas ||
                    posicionPisoService.fromAlphabetLabel(p.getColumna()) > nuevasColumnas ||
                    Integer.parseInt(p.getAltura()) > nuevaAltura
                ))
                .toList();

            if (!afectadas.isEmpty()) {
                String detalles = afectadas.stream()
                    .map(p -> "F" + p.getFila() + "-C" + p.getColumna() + "-A" + p.getAltura())
                    .collect(Collectors.joining(", "));
                throw new ObjConflictException(
                    "No se puede actualizar el piso: posiciones ocupadas fuera del nuevo rango " +
                    nuevasFilas + "x" + nuevasColumnas + "x" + nuevaAltura + " → " + detalles);
            }

            List<PosicionPiso> aEliminar = todasPosiciones.stream()
                .filter(p ->
                    posicionPisoService.fromAlphabetLabel(p.getFila()) > nuevasFilas ||
                    posicionPisoService.fromAlphabetLabel(p.getColumna()) > nuevasColumnas ||
                    Integer.parseInt(p.getAltura()) > nuevaAltura
                )
                .toList();
            posicionPisoService.deletePositions(aEliminar);

            piso.setId(id);
            PisoRefrigerador pisoActualizado = pisoService.updatePiso(piso);

            Set<String> existentes = todasPosiciones.stream()
                .filter(p ->
                    posicionPisoService.fromAlphabetLabel(p.getFila()) <= nuevasFilas &&
                    posicionPisoService.fromAlphabetLabel(p.getColumna()) <= nuevasColumnas &&
                    Integer.parseInt(p.getAltura()) <= nuevaAltura
                )
                .map(p -> p.getFila() + "-" + p.getColumna() + "-" + p.getAltura())
                .collect(Collectors.toSet());

            for (int f = 1; f <= nuevasFilas; f++) {
                String strFila = posicionPisoService.toAlphabetLabel(f);
                for (int c = 1; c <= nuevasColumnas; c++) {
                    String strColumna = posicionPisoService.toAlphabetLabel(c);
                    for (int a = 1; a <= nuevaAltura; a++) {
                        String strAltura = String.valueOf(a);
                        if (!existentes.contains(strFila + "-" + strColumna + "-" + strAltura)) {
                            posicionPisoService.crearPosicionSiNoExiste(pisoActualizado, strFila, strColumna, strAltura);
                        }
                    }
                }
            }

            return pisoActualizado;
        }

        piso.setId(id);
        return pisoService.updatePiso(piso);
    }

    @Transactional
    public void deletePiso(Long id) {
        PisoRefrigerador findPiso = pisoService.getPiso(id);
        if (!findPiso.getPosiciones().isEmpty()) {
            throw new ObjConflictException("No se puede eliminar el piso porque tiene posiciones asociadas.");
        }
        pisoService.deletePiso(id);
    }

}
