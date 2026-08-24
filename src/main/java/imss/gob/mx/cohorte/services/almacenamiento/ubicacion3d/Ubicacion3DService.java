package imss.gob.mx.cohorte.services.almacenamiento.ubicacion3d;

import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d.*;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenicaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCajaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigeradorRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPiso;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPisoRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestraRepository;
import imss.gob.mx.cohorte.modules.persona.Persona;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Arma, en una sola pasada, la escena completa que consume el visualizador 3D
 * de ubicacion: refrigerador -> piso -> caja -> posicion.
 *
 * <p>Todos los conteos, porcentajes y coordenadas derivadas se calculan aqui.
 * El frontend recibe numeros listos para pintar y no vuelve a recorrer nada.
 *
 * <p>Las coordenadas de {@code PosicionPiso} se guardan como etiquetas de letras
 * (A, B, C...) para fila y columna, y como numero en texto para la altura. La
 * escena necesita indices, asi que se derivan aqui y viajan junto a la etiqueta
 * original: el panel sigue mostrando la coordenada que el usuario conoce.
 */
@Service
@RequiredArgsConstructor
public class Ubicacion3DService {

    /** Estados de traslado que significan "la muestra no esta fisicamente aqui". */
    private static final Set<EstadoTraslado> TRASLADOS_VIGENTES =
            EnumSet.of(EstadoTraslado.ENVIADA, EstadoTraslado.RECIBIDA, EstadoTraslado.EN_DEVOLUCION);

    private final MuestraRepository muestraRepository;
    private final PosicionCajaRepository posicionCajaRepository;
    private final CajaCriogenicaRepository cajaCriogenicaRepository;
    private final PisoRefrigeradorRepository pisoRefrigeradorRepository;
    private final PosicionPisoRepository posicionPisoRepository;
    private final TrasladoMuestraRepository trasladoMuestraRepository;

    @Transactional(readOnly = true)
    public Ubicacion3DDTO construir(Muestra muestra) {
        Ubicacion3DMuestraDTO fichaMuestra = toFichaMuestra(muestra);

        if (muestra.getEstadoMuestra() == EstadoMuestra.PRESTADA) {
            return noDisponible(fichaMuestra, "PRESTADA",
                    "La muestra esta prestada y no se encuentra en este biobanco.",
                    prestamoVigente(muestra.getId()));
        }
        if (muestra.getEstadoMuestra() == EstadoMuestra.BAJA) {
            return noDisponible(fichaMuestra, "BAJA",
                    "La muestra fue dada de baja: ya no ocupa una posicion fisica.", null);
        }
        PosicionCaja posicion = muestra.getPosicionCaja();
        if (posicion == null) {
            return noDisponible(fichaMuestra, "SIN_POSICION",
                    "La muestra aun no tiene posicion asignada en una caja.", null);
        }
        CajaCriogenica caja = posicion.getCaja();
        PosicionPiso hueco = caja != null ? caja.getPosicionPiso() : null;
        PisoRefrigerador piso = hueco != null ? hueco.getPiso() : null;
        Refrigerador refrigerador = piso != null ? piso.getRefrigerador() : null;
        if (refrigerador == null) {
            // La caja existe pero nadie la ha colocado en un piso: no hay escena que
            // recorrer, y fingir una jerarquia completa seria inventar la ubicacion.
            return noDisponible(fichaMuestra, "SIN_POSICION",
                    "La caja " + (caja != null ? caja.getCodigoCaja() : "")
                            + " todavia no esta colocada en un piso de refrigerador.", null);
        }

        return Ubicacion3DDTO.builder()
                .muestra(fichaMuestra)
                .disponible(true)
                .refrigerador(toRefrigerador(refrigerador, piso.getId()))
                .piso(toPiso(piso, hueco, caja))
                .caja(toCaja(caja, posicion))
                .build();
    }

    /**
     * Vista de un refrigerador sin muestra objetivo: el usuario solo recorre el
     * mueble. Se devuelve la misma proyeccion, con el destino en null.
     */
    @Transactional(readOnly = true)
    public Ubicacion3DRefrigeradorDTO explorarRefrigerador(Refrigerador refrigerador) {
        return toRefrigerador(refrigerador, null);
    }

    /** Vista de un piso concreto, con todas sus cajas y sin destino. */
    @Transactional(readOnly = true)
    public Ubicacion3DPisoDTO explorarPiso(PisoRefrigerador piso) {
        return toPiso(piso, null, null);
    }

    /** Vista de una caja concreta, con todas sus posiciones y sin destino. */
    @Transactional(readOnly = true)
    public Ubicacion3DCajaDTO explorarCaja(CajaCriogenica caja) {
        return toCaja(caja, null);
    }

    // -- Muestra --------------------------------------------------------------

    private Ubicacion3DMuestraDTO toFichaMuestra(Muestra m) {
        var b = Ubicacion3DMuestraDTO.builder()
                .id(m.getId())
                .etiqueta(m.getEtiqueta())
                .estadoMuestra(m.getEstadoMuestra() != null ? m.getEstadoMuestra().name() : null)
                .valor(m.getValor())
                .unidad(m.getUnidad())
                .fechaRecoleccion(m.getFechaRecoleccion())
                .observaciones(m.getObservaciones())
                .numeroAlicuota(m.getNumeroAlicuota())
                .totalAlicuotas(m.getTotalAlicuotas());

        if (m.getTipoMuestra() != null) {
            b.tipoMuestra(m.getTipoMuestra().getNombre())
             .temperaturaAlmacenamiento(m.getTipoMuestra().getTemperaturaAlmacenamiento());
        }
        if (m.getTuboMuestra() != null) {
            b.tuboMuestra(m.getTuboMuestra().getNombre());
        }
        if (m.getMuestraPadre() != null) {
            b.idMuestraPadre(m.getMuestraPadre().getId())
             .etiquetaMuestraPadre(m.getMuestraPadre().getEtiqueta());
        }
        if (m.getPaciente() != null) {
            b.pacienteFolio(m.getPaciente().getFolio())
             .pacienteNombre(nombreCompleto(m.getPaciente().getPersona()));
        }
        if (m.getUsuarioRecolecta() != null) {
            b.usuarioRecolecta(nombreCompleto(m.getUsuarioRecolecta().getPersona()));
        }
        if (m.getInstitucion() != null) {
            b.institucionPropietaria(m.getInstitucion().getNombre());
        }
        if (m.getInstitucionActual() != null) {
            b.institucionActual(m.getInstitucionActual().getNombre());
        }
        return b.build();
    }

    private String nombreCompleto(Persona p) {
        if (p == null) return null;
        StringBuilder sb = new StringBuilder();
        if (p.getNombre() != null) sb.append(p.getNombre());
        if (p.getSegundoNombre() != null) sb.append(' ').append(p.getSegundoNombre());
        if (p.getApellidoPaterno() != null) sb.append(' ').append(p.getApellidoPaterno());
        if (p.getApellidoMaterno() != null) sb.append(' ').append(p.getApellidoMaterno());
        String s = sb.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private Ubicacion3DDTO noDisponible(Ubicacion3DMuestraDTO ficha, String motivo,
                                        String mensaje, Ubicacion3DPrestamoDTO prestamo) {
        return Ubicacion3DDTO.builder()
                .muestra(ficha)
                .disponible(false)
                .motivoNoDisponible(motivo)
                .mensajeNoDisponible(mensaje)
                .prestamo(prestamo)
                .build();
    }

    private Ubicacion3DPrestamoDTO prestamoVigente(Long idMuestra) {
        return trasladoMuestraRepository.findAllByMuestra_IdOrderByFechaTrasladoDesc(idMuestra).stream()
                .filter(t -> TRASLADOS_VIGENTES.contains(t.getEstado()))
                .findFirst()
                .map(this::toPrestamo)
                .orElse(null);
    }

    private Ubicacion3DPrestamoDTO toPrestamo(TrasladoMuestra t) {
        return Ubicacion3DPrestamoDTO.builder()
                .idTraslado(t.getId())
                .estado(t.getEstado() != null ? t.getEstado().name() : null)
                .institucionOrigen(t.getInstitucionOrigen() != null ? t.getInstitucionOrigen().getNombre() : null)
                .institucionDestino(t.getInstitucionDestino() != null ? t.getInstitucionDestino().getNombre() : null)
                .autorizadoPor(t.getAutorizadoPor() != null ? nombreCompleto(t.getAutorizadoPor().getPersona()) : null)
                .fechaTraslado(t.getFechaTraslado())
                .fechaLimite(t.getFechaLimite())
                .motivo(t.getMotivo())
                .build();
    }

    // -- Vista 0 - Refrigerador -----------------------------------------------

    private Ubicacion3DRefrigeradorDTO toRefrigerador(Refrigerador ref, Long idPisoDestino) {
        List<PisoRefrigerador> pisos = pisoRefrigeradorRepository.findAllByRefrigerador_Id(ref.getId()).stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()) || Objects.equals(p.getId(), idPisoDestino))
                .sorted(Comparator.comparing(PisoRefrigerador::getNumeroPiso,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<Ubicacion3DPisoResumenDTO> resumenes = new ArrayList<>(pisos.size());
        int totalPos = 0;
        int totalOcup = 0;
        for (PisoRefrigerador piso : pisos) {
            List<PosicionPiso> huecos = posicionPisoRepository.findAllByPiso_Id(piso.getId());
            int ocupados = (int) huecos.stream().filter(h -> Boolean.TRUE.equals(h.getOcupada())).count();
            totalPos += huecos.size();
            totalOcup += ocupados;
            resumenes.add(Ubicacion3DPisoResumenDTO.builder()
                    .id(piso.getId())
                    .numeroPiso(piso.getNumeroPiso())
                    .filas(piso.getFilas())
                    .columnas(piso.getColumnas())
                    .altura(piso.getAltura())
                    .totalPosiciones(huecos.size())
                    .posicionesOcupadas(ocupados)
                    .posicionesLibres(huecos.size() - ocupados)
                    .porcentajeOcupacion(porcentaje(ocupados, huecos.size()))
                    // Huecos ocupados y cajas no son lo mismo: un hueco marcado
                    // como ocupado cuya caja ya no esta inflaria la cuenta. La
                    // vista de piso ya cuenta cajas de verdad; esta no.
                    .totalCajas((int) cajaCriogenicaRepository.countByPosicionPiso_Piso_Id(piso.getId()))
                    .reticula(reticula(piso, huecos))
                    .esDestino(Objects.equals(piso.getId(), idPisoDestino))
                    .build());
        }

        return Ubicacion3DRefrigeradorDTO.builder()
                .id(ref.getId())
                .codigo(ref.getCodigo())
                .nombre(ref.getNombre())
                .marca(ref.getMarca())
                .modelo(ref.getModelo())
                .activo(ref.getActivo())
                .nombreInstitucion(ref.getInstitucion() != null ? ref.getInstitucion().getNombre() : null)
                .totalPisos(resumenes.size())
                .totalPosiciones(totalPos)
                .posicionesOcupadas(totalOcup)
                .porcentajeOcupacion(porcentaje(totalOcup, totalPos))
                .pisos(resumenes)
                .idPisoDestino(idPisoDestino)
                .build();
    }

    /**
     * Aplana las alturas de un piso sobre su planta: cada celda lleva cuantos
     * huecos ocupados hay en esa columna vertical. Es el patron que la plancha
     * del refrigerador dibuja como reticula.
     */
    private List<Integer> reticula(PisoRefrigerador piso, List<PosicionPiso> huecos) {
        int filas = piso.getFilas() != null ? piso.getFilas() : 0;
        int columnas = piso.getColumnas() != null ? piso.getColumnas() : 0;
        int celdasTotales = Math.max(filas * columnas, 0);
        List<Integer> celdas = new ArrayList<>(celdasTotales);
        for (int i = 0; i < celdasTotales; i++) celdas.add(0);
        for (PosicionPiso h : huecos) {
            if (!Boolean.TRUE.equals(h.getOcupada())) continue;
            int f = indiceDesdeEtiqueta(h.getFila());
            int c = indiceDesdeEtiqueta(h.getColumna());
            if (f < 1 || c < 1 || f > filas || c > columnas) continue;
            int idx = (f - 1) * columnas + (c - 1);
            celdas.set(idx, celdas.get(idx) + 1);
        }
        return celdas;
    }

    // -- Vista 1 - Piso -------------------------------------------------------

    private Ubicacion3DPisoDTO toPiso(PisoRefrigerador piso, PosicionPiso huecoDestino, CajaCriogenica cajaDestino) {
        Long idHuecoDestino = huecoDestino != null ? huecoDestino.getId() : null;
        Refrigerador ref = piso.getRefrigerador();
        List<PosicionPiso> huecos = posicionPisoRepository.findAllByPiso_Id(piso.getId());

        Map<Long, CajaCriogenica> cajasPorHueco = new HashMap<>();
        for (PosicionPiso h : huecos) {
            if (!Boolean.TRUE.equals(h.getOcupada())) continue;
            cajaCriogenicaRepository.findAllByPosicionPiso_Id(h.getId()).stream()
                    .findFirst().ifPresent(c -> cajasPorHueco.put(h.getId(), c));
        }

        List<Ubicacion3DCajaEnPisoDTO> celdas = new ArrayList<>(huecos.size());
        int ocupados = 0;
        for (PosicionPiso h : huecos) {
            CajaCriogenica c = cajasPorHueco.get(h.getId());
            if (Boolean.TRUE.equals(h.getOcupada())) ocupados++;
            var b = Ubicacion3DCajaEnPisoDTO.builder()
                    .idPosicionPiso(h.getId())
                    .fila(h.getFila())
                    .columna(h.getColumna())
                    .altura(h.getAltura())
                    .filaIndex(indiceDesdeEtiqueta(h.getFila()))
                    .columnaIndex(indiceDesdeEtiqueta(h.getColumna()))
                    .alturaIndex(indiceDesdeEtiqueta(h.getAltura()))
                    .ocupada(h.getOcupada())
                    .esDestino(Objects.equals(h.getId(), idHuecoDestino));
            if (c != null) {
                b.idCaja(c.getId())
                 .codigoCaja(c.getCodigoCaja())
                 .tipoCaja(c.getTipoCaja())
                 .color(c.getColor())
                 .capacidad(capacidad(c))
                 .ocupadas((int) posicionCajaRepository.countOcupadasByCajaId(c.getId()));
            }
            celdas.add(b.build());
        }
        celdas.sort(Comparator.comparing(Ubicacion3DCajaEnPisoDTO::getAlturaIndex)
                .thenComparing(Ubicacion3DCajaEnPisoDTO::getFilaIndex)
                .thenComparing(Ubicacion3DCajaEnPisoDTO::getColumnaIndex));

        return Ubicacion3DPisoDTO.builder()
                .id(piso.getId())
                .numeroPiso(piso.getNumeroPiso())
                .filas(piso.getFilas())
                .columnas(piso.getColumnas())
                .altura(piso.getAltura())
                .totalPosiciones(huecos.size())
                .posicionesOcupadas(ocupados)
                .posicionesLibres(huecos.size() - ocupados)
                .porcentajeOcupacion(porcentaje(ocupados, huecos.size()))
                .totalCajas(cajasPorHueco.size())
                .cajas(celdas)
                .idRefrigerador(ref != null ? ref.getId() : null)
                .codigoRefrigerador(ref != null ? ref.getCodigo() : null)
                .idCajaDestino(cajaDestino != null ? cajaDestino.getId() : null)
                .filaDestinoIndex(huecoDestino != null ? indiceDesdeEtiqueta(huecoDestino.getFila()) : null)
                .columnaDestinoIndex(huecoDestino != null ? indiceDesdeEtiqueta(huecoDestino.getColumna()) : null)
                .alturaDestinoIndex(huecoDestino != null ? indiceDesdeEtiqueta(huecoDestino.getAltura()) : null)
                .build();
    }

    // -- Vistas 2 y 3 - Caja y posicion ---------------------------------------

    private Ubicacion3DCajaDTO toCaja(CajaCriogenica caja, PosicionCaja destino) {
        Long idDestino = destino != null ? destino.getId() : null;
        List<PosicionCaja> posiciones = posicionCajaRepository.findAllByCaja_Id(caja.getId());
        Map<Long, Muestra> muestraPorPosicion = new HashMap<>();
        for (Muestra m : muestraRepository.findAllByCaja_Id(caja.getId())) {
            if (m.getPosicionCaja() != null) {
                muestraPorPosicion.put(m.getPosicionCaja().getId(), m);
            }
        }

        List<Ubicacion3DPosicionDTO> celdas = posiciones.stream()
                .sorted(Comparator.comparing(PosicionCaja::getFila).thenComparing(PosicionCaja::getColumna))
                .map(p -> {
                    Muestra alojada = muestraPorPosicion.get(p.getId());
                    return Ubicacion3DPosicionDTO.builder()
                            .id(p.getId())
                            .fila(p.getFila())
                            .columna(p.getColumna())
                            .ocupada(p.getOcupada())
                            .idMuestra(alojada != null ? alojada.getId() : null)
                            .etiquetaMuestra(alojada != null ? alojada.getEtiqueta() : null)
                            .esDestino(Objects.equals(p.getId(), idDestino))
                            .build();
                })
                .toList();

        PosicionPiso hueco = caja.getPosicionPiso();
        PisoRefrigerador piso = hueco != null ? hueco.getPiso() : null;
        Refrigerador ref = piso != null ? piso.getRefrigerador() : null;

        int ocupadas = (int) celdas.stream().filter(c -> Boolean.TRUE.equals(c.getOcupada())).count();
        int capacidad = capacidad(caja);

        return Ubicacion3DCajaDTO.builder()
                .id(caja.getId())
                .codigoCaja(caja.getCodigoCaja())
                .filas(caja.getFilas())
                .columnas(caja.getColumnas())
                .tipoCaja(caja.getTipoCaja())
                .color(caja.getColor())
                .observaciones(caja.getObservaciones())
                .capacidad(capacidad)
                .ocupadas(ocupadas)
                .libres(Math.max(capacidad - ocupadas, 0))
                .porcentajeOcupacion(porcentaje(ocupadas, capacidad))
                .posiciones(celdas)
                .filaDestino(destino != null ? destino.getFila() : null)
                .columnaDestino(destino != null ? destino.getColumna() : null)
                .idPiso(piso != null ? piso.getId() : null)
                .numeroPiso(piso != null ? piso.getNumeroPiso() : null)
                .idRefrigerador(ref != null ? ref.getId() : null)
                .codigoRefrigerador(ref != null ? ref.getCodigo() : null)
                .coordenadaEnPiso(hueco != null
                        ? hueco.getFila() + hueco.getColumna() + " · altura " + hueco.getAltura()
                        : null)
                .build();
    }

    // -- Utilidades -----------------------------------------------------------

    private int capacidad(CajaCriogenica c) {
        int f = c.getFilas() != null ? c.getFilas() : 0;
        int col = c.getColumnas() != null ? c.getColumnas() : 0;
        return f * col;
    }

    private Integer porcentaje(int parte, int total) {
        return total <= 0 ? 0 : (int) Math.round(parte * 100.0 / total);
    }

    /**
     * Traduce una etiqueta de coordenada a indice 1-based. Acepta las letras que
     * genera PosicionPisoService (A=1, Z=26, AA=27) y tambien digitos, que es
     * como se guarda la altura.
     */
    private int indiceDesdeEtiqueta(String etiqueta) {
        if (etiqueta == null || etiqueta.isBlank()) return 0;
        String s = etiqueta.trim().toUpperCase();
        if (s.chars().allMatch(Character::isDigit)) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        int num = 0;
        for (char ch : s.toCharArray()) {
            if (ch < 'A' || ch > 'Z') return 0;
            num = num * 26 + (ch - 'A' + 1);
        }
        return num;
    }
}
