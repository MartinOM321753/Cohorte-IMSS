package imss.gob.mx.cohorte.config;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Migra TipoMuestra.temperaturaAlmacenamiento (texto libre) al nuevo
 * campo numérico TipoMuestra.temperatura (Double, en °C).
 * Solo procesa registros que tengan texto pero aún no tengan valor numérico.
 * Es idempotente: una vez migrado, no vuelve a tocar el registro.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemperaturaMigrationRunner implements ApplicationRunner {

    private final TipoMuestraRepository tipoMuestraRepository;

    private static final Pattern NUMERO = Pattern.compile("-?\\d+\\.?\\d*");

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<TipoMuestra> pendientes = tipoMuestraRepository.findAll().stream()
                .filter(t -> t.getTemperaturaAlmacenamiento() != null
                        && !t.getTemperaturaAlmacenamiento().isBlank()
                        && t.getTemperatura() == null)
                .toList();

        if (pendientes.isEmpty()) return;

        log.info("Migrando temperatura de {} tipos de muestra (texto → numérico)", pendientes.size());

        for (TipoMuestra t : pendientes) {
            Double valor = parsearTemperatura(t.getTemperaturaAlmacenamiento());
            if (valor != null) {
                t.setTemperatura(valor);
                tipoMuestraRepository.save(t);
                log.info("  {} → {}°C (era: '{}')", t.getNombre(), valor, t.getTemperaturaAlmacenamiento());
            } else {
                log.warn("  {} — no se pudo parsear '{}', se deja en null", t.getNombre(), t.getTemperaturaAlmacenamiento());
            }
        }
    }

    static Double parsearTemperatura(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String limpio = texto.trim().toLowerCase();

        if (limpio.startsWith("ambiente") || limpio.equals("ta") || limpio.equals("room")) {
            return 22.0;
        }

        Matcher m = NUMERO.matcher(limpio);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
