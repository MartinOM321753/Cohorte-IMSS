package imss.gob.mx.cohorte.services.impresion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.print.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class DirectPrintService {

    public List<String> listarImpresoras() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        return Arrays.stream(services)
                .map(PrintService::getName)
                .toList();
    }

    public void imprimir(String datos, String nombreImpresora) {
        PrintService printer = buscarImpresora(nombreImpresora);
        if (printer == null) {
            throw new RuntimeException("Impresora no encontrada: " + nombreImpresora);
        }
        try {
            DocPrintJob job = printer.createPrintJob();
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(datos.getBytes(StandardCharsets.UTF_8), flavor, null);
            job.print(doc, null);
            log.info("ZPL enviado a impresora: {}", printer.getName());
        } catch (PrintException e) {
            throw new RuntimeException("Error al imprimir: " + e.getMessage(), e);
        }
    }

    private PrintService buscarImpresora(String nombre) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService s : services) {
            if (s.getName().equalsIgnoreCase(nombre)) return s;
        }
        String lower = nombre.toLowerCase();
        for (PrintService s : services) {
            if (s.getName().toLowerCase().contains(lower)) return s;
        }
        return null;
    }
}
