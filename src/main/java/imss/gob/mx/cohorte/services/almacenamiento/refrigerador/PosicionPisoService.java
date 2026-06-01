package imss.gob.mx.cohorte.services.almacenamiento.refrigerador;

import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPiso;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPisoRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigeradorRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PosicionPisoService {

    private final PosicionPisoRepository posicionPisoRepository;
    private final PisoRefrigeradorRepository pisoRefrigeradorRepository;

    @Autowired
    public PosicionPisoService(PosicionPisoRepository posicionPisoRepository, 
                               PisoRefrigeradorRepository pisoRefrigeradorRepository) {
        this.posicionPisoRepository = posicionPisoRepository;
        this.pisoRefrigeradorRepository = pisoRefrigeradorRepository;
    }

    @Transactional(readOnly = true)
    public List<PosicionPiso> getAllPosiciones() {
        return posicionPisoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PosicionPiso getPosicion(Long id) {
        return posicionPisoRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la posición en piso con id: " + id));
    }
    

    @Transactional(readOnly = true)
    public List<PosicionPiso> getPosicionesPorPiso(Long idPiso) {
        return posicionPisoRepository.findAllByPiso_Id(idPiso);
    }

    @Transactional
    public PosicionPiso updatePosicion(PosicionPiso posicion) {
        PosicionPiso posBD = posicionPisoRepository.findById(posicion.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la posición en piso con id: " + posicion.getId()));

        // Validar si cambia el piso asociado
        if (posicion.getPiso() != null) {
            Long idPiso = posicion.getPiso().getId();
            PisoRefrigerador piso = pisoRefrigeradorRepository.findById(idPiso)
                    .orElseThrow(() -> new ObjNotFoundException("No se encontró el piso de refrigerador con id: " + idPiso));
            posBD.setPiso(piso);
        }

        // Asumiendo estos atributos comunes
        posBD.setFila(posicion.getFila());
        posBD.setColumna(posicion.getColumna());
        posBD.setAltura(posicion.getAltura());
        posBD.setOcupada(posicion.getOcupada());
        return posicionPisoRepository.save(posBD);
    }

    /**
     * Genera automáticamente todas las posiciones de un piso una vez creado, 
     * según los parámetros dados (filas, columnas, alturas).
     * 
     * @param idPiso   ID del PisoRefrigerador al que se le generarán posiciones.
     * @param filas    Número de filas.
     * @param columnas Número de columnas.
     * @param alturas  Número de alturas.
     */
    @Transactional
    public void generarPosicionesParaPiso(Long idPiso, Integer filas, Integer columnas, Integer alturas) {
        PisoRefrigerador piso = pisoRefrigeradorRepository.findById(idPiso)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el piso de refrigerador con id: " + idPiso));
        if (filas == null || filas < 1 || columnas == null || columnas < 1 || alturas == null || alturas < 1) {
            throw new IllegalArgumentException("Parámetros de filas, columnas y alturas deben ser >= 1");
        }


        for (int f = 1; f <= filas; f++) {
            String strFila = toAlphabetLabel(f);
            for (int c = 1; c <= columnas; c++) {
                String strColumna = toAlphabetLabel(c);
                for (int a = 1; a <= alturas; a++) {
                    String strAltura = String.valueOf(a); // Altura como número en string
                    PosicionPiso nueva = new PosicionPiso();
                    nueva.setPiso(piso);
                    nueva.setFila(strFila);
                    nueva.setColumna(strColumna);
                    nueva.setAltura(strAltura);
                    nueva.setOcupada(false);
                    posicionPisoRepository.save(nueva);
                }
            }
        }
    }


    @Transactional
    public void deletePositions(List<PosicionPiso> positions) {
        posicionPisoRepository.deleteAll(positions);
    }

    @Transactional
    public void crearPosicionSiNoExiste(PisoRefrigerador piso, String fila, String columna, String altura) {
        if (posicionPisoRepository.findByPiso_IdAndFilaAndColumnaAndAltura(piso.getId(), fila, columna, altura).isEmpty()) {
            PosicionPiso nueva = new PosicionPiso();
            nueva.setPiso(piso);
            nueva.setFila(fila);
            nueva.setColumna(columna);
            nueva.setAltura(altura);
            nueva.setOcupada(false);
            posicionPisoRepository.save(nueva);
        }
    }

    public String toAlphabetLabel(int num) {
        StringBuilder sb = new StringBuilder();
        int n = num;
        while (n > 0) {
            n--; // Ajuste para 1-based index
            char ch = (char) ('A' + (n % 26));
            sb.insert(0, ch);
            n /= 26;
        }
        return sb.toString();
    }

    public int fromAlphabetLabel(String label) {
        int num = 0;
        for (char c : label.toCharArray()) {
            num = num * 26 + (c - 'A' + 1);
        }
        return num;
    }
}