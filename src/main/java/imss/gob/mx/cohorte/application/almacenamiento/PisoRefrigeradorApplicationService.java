package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.controllers.DTO.PisosDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.PisoRefrigeradorRequestDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPiso;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigeradorRepository;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.PisoRefrigeradorService;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.PosicionPisoService;
import imss.gob.mx.cohorte.services.almacenamiento.refrigerador.RefrigeradorService;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;


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
import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenicaRepository;


@Service
@RequireModulo(ModuloSistema.BIOBANCO)
public class PisoRefrigeradorApplicationService {

    private final PisoRefrigeradorService pisoService;
    /** Directo, para poder buscar el numero dentro del propio refrigerador. */
    private final PisoRefrigeradorRepository pisoRepository;
    private final RefrigeradorService refrigeradorService;
    private final PosicionPisoService posicionPisoService;
    private final InstitucionContextService institucionContextService;
    /** Para contar las cajas que hay de verdad en el piso, no las marcas de ocupado. */
    private final CajaCriogenicaRepository cajaCriogenicaRepository;

    @Autowired
    public PisoRefrigeradorApplicationService(PisoRefrigeradorService pisoService, 
                                            RefrigeradorService refrigeradorService, 
                                            PosicionPisoService posicionPisoService,
                                            InstitucionContextService institucionContextService,
                                            PisoRefrigeradorRepository pisoRepository,
                                            CajaCriogenicaRepository cajaCriogenicaRepository) {
        this.pisoService = pisoService;
        this.pisoRepository = pisoRepository;
        this.refrigeradorService = refrigeradorService;
        this.posicionPisoService = posicionPisoService;
        this.institucionContextService = institucionContextService;
        this.cajaCriogenicaRepository = cajaCriogenicaRepository;
    }

    /**
     * Un piso no guarda institucion propia: la hereda de su refrigerador, y por
     * eso PisoRefrigeradorService.getPiso no puede comprobarla. Quien lo use
     * tiene que hacerlo aqui, como ya hace RefrigeradorApplicationService en la
     * vista 3D. Sin esto bastaba pasar un id ajeno para leer la rejilla de otro
     * biobanco: sus dimensiones, sus huecos y cuales estan ocupados.
     */
    private PisoRefrigerador getPisoConAcceso(Long idPiso) {
        PisoRefrigerador piso = pisoService.getPiso(idPiso);
        institucionContextService.verificarPertenece(piso.getRefrigerador().getInstitucion());
        return piso;
    }

    @Transactional(readOnly = true)
    public List<PisoRefrigerador> getAllPisos(Long idRefrigerador) {
        // El refrigerador si conoce su institucion; se comprueba en el.
        refrigeradorService.getRefrigerador(idRefrigerador);
        return pisoService.getAllPisos(idRefrigerador);
    }

    @Transactional(readOnly = true)
    public PisoRefrigerador getPiso(Long id) {
        return getPisoConAcceso(id);
    }

    @Transactional(readOnly = true)
    public PisoRefrigerador getPisoByNumber(String number) {
        PisoRefrigerador piso = pisoService.getPisoByNumber(number);
        institucionContextService.verificarPertenece(piso.getRefrigerador().getInstitucion());
        return piso;
    }

    @Transactional(readOnly = true)
    public List<PosicionPiso> getPosiciones(Long idPiso) {
        getPisoConAcceso(idPiso);
        return posicionPisoService.getPosicionesPorPiso(idPiso);
    }


    @Transactional
    public List<PisoRefrigerador> createPisos(PisosDTO pisosDTO) {

        Refrigerador refBD = refrigeradorService.getRefrigerador(pisosDTO.getIdRefrigerador());
        if (refBD == null) { throw new ObjNotFoundException("El refrigerador no existe");}

        for (PisoRefrigerador piso : pisosDTO.getPisos()) {
            if (piso.getNumeroPiso() != null && !piso.getNumeroPiso().isBlank()) {
                // Unico dentro del refrigerador, no en toda la base. Los numeros se
                // generan por refrigerador —P-0001, P-0002…—, asi que buscarlos
                // globalmente hacia que la colision fuera la norma: en cuanto otra
                // institucion tenia un P-0001, nadie mas podia crear el suyo.
                var existing = pisoRepository.findByNumeroPisoAndRefrigerador_Id(
                        piso.getNumeroPiso(), refBD.getId());
                if (existing.isPresent()) {
                    throw new ObjConflictException(
                            "Este refrigerador ya tiene un piso con el número: " + piso.getNumeroPiso());
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
    public PisoRefrigerador updatePiso(Long id, PisoRefrigeradorRequestDTO dto) {
        PisoRefrigerador pisoBD = pisoService.getPiso(id);
        institucionContextService.verificarPertenece(pisoBD.getRefrigerador().getInstitucion());

        PisoRefrigerador piso = new PisoRefrigerador();
        piso.setNumeroPiso(dto.getNumeroPiso());
        piso.setFilas(dto.getFilas());
        piso.setColumnas(dto.getColumnas());
        piso.setAltura(dto.getAltura());
        piso.setActivo(pisoBD.getActivo());

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
        // El guarda de institucion va primero: un borrado no debe depender de que
        // otra comprobacion lo detenga por casualidad.
        PisoRefrigerador findPiso = getPisoConAcceso(id);

        // Antes se rechazaba el borrado en cuanto el piso tuviera posiciones, y un
        // piso SIEMPRE las tiene: se generan solas al crearlo. La regla no protegia
        // nada, simplemente hacia imposible borrar un piso.
        //
        // Lo que de verdad hay que proteger son las cajas, que si las puso alguien.
        // Las posiciones son rejilla vacia y se van con el piso: la relacion tiene
        // orphanRemoval, asi que no hace falta borrarlas a mano.
        long cajas = cajaCriogenicaRepository.countByPosicionPiso_Piso_Id(id);
        long marcadas = findPiso.getPosiciones().stream()
                .filter(pos -> Boolean.TRUE.equals(pos.getOcupada()))
                .count();
        verificarPisoVaciable(cajas, marcadas);

        pisoService.deletePiso(id);
    }

    /**
     * La regla que decide si un piso puede eliminarse, aparte para poder probarla.
     *
     * @param cajas    cajas realmente colocadas en el piso
     * @param marcadas posiciones con la marca de ocupada
     */
    static void verificarPisoVaciable(long cajas, long marcadas) {
        if (cajas > 0) {
            throw new ObjConflictException(
                    "No se puede eliminar el piso porque todavía tiene " + cajas
                    + (cajas == 1 ? " caja dentro." : " cajas dentro.")
                    + " Sácalas o cámbialas de piso antes de eliminarlo.");
        }
        // Si una posición quedó marcada como ocupada sin caja, el dato está
        // inconsistente: no es este el sitio para arreglarlo a ciegas borrando.
        if (marcadas > 0) {
            throw new ObjConflictException(
                    "El piso tiene " + marcadas + " posición(es) marcadas como ocupadas pero sin caja dentro. "
                    + "Revisa la ubicación antes de eliminarlo.");
        }
    }
}
