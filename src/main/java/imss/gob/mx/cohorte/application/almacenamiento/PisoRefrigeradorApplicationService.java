package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.controllers.DTO.PisosDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.PisoRefrigeradorService;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.PosicionPisoService;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.RefrigeradorService;


import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@AllArgsConstructor
public class PisoRefrigeradorApplicationService {

    private final PisoRefrigeradorService pisoService;
    private final RefrigeradorService refrigeradorService;

    private final PosicionPisoService posicionPisoService;

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


    @Transactional
    public List<PisoRefrigerador> createPisos(PisosDTO pisosDTO) {

        Refrigerador refBD = refrigeradorService.getRefrigerador(pisosDTO.getIdRefrigerador());
        if (refBD == null) { throw new ObjNotFoundException("El refrigerador no existe");}

        for (PisoRefrigerador piso : pisosDTO.getPisos()) {
            var existing = pisoService.findByNumber(piso.getNumeroPiso());
            if (existing.isPresent()) {
                throw new ObjConflictException("Ya existe un piso con el número: " + piso.getNumeroPiso());
            }

            piso.setRefrigerador(refBD);
            piso.setActivo(true);
            PisoRefrigerador pisoRefrigerador = pisoService.createPiso(piso);

            posicionPisoService.generarPosicionesParaPiso(pisoRefrigerador.getId(), pisoRefrigerador.getFilas(), pisoRefrigerador.getColumnas(), pisoRefrigerador.getAltura());

        }
        Refrigerador refrigerador = refrigeradorService.getRefrigerador(pisosDTO.getIdRefrigerador());

        return refrigerador.getPisos();
    }

//    @Transactional
//    public PisoRefrigerador updatePiso(PisoRefrigerador piso) {
//
//        PisoRefrigerador pisoBD = pisoService.getPiso(piso.getId());
//
//        if (!pisoBD.getNumeroPiso().equals(piso.getNumeroPiso())) {
//            pisoService.getPisoByNumber(piso.getNumeroPiso());
//        }
//
//        pisoBD.setNumeroPiso(piso.getNumeroPiso());
//        pisoBD.setFilas(piso.getFilas());
//        pisoBD.setColumnas(piso.getColumnas());
//        pisoBD.setAltura(piso.getAltura());
//        pisoBD.setActivo(piso.getActivo());
//
//        return pisoService.updatePiso(pisoBD);
//    }

    @Transactional
    public void deletePiso(Long id) {
        PisoRefrigerador findPiso = pisoService.getPiso(id);
        if (!findPiso.getPosiciones().isEmpty()) {
            throw new ObjConflictException("No se puede eliminar el piso porque tiene posiciones asociadas.");
        }
        pisoService.deletePiso(id);
    }

}
