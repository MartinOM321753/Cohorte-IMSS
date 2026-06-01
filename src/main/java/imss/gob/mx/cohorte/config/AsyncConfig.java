package imss.gob.mx.cohorte.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Thread pool dedicado para envío de notificaciones.
     * Evita bloquear el hilo HTTP mientras se envían emails.
     */
    @Bean(name = "notificacionesExecutor")
    public Executor notificacionesExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(5);
        exec.setQueueCapacity(50);
        exec.setThreadNamePrefix("notif-");
        exec.initialize();
        return exec;
    }

    /**
     * Thread pool dedicado para persistencia de registros de auditoría.
     * Separado del de notificaciones para que la carga de auditoría no interfiera
     * con el envío de correos ni con el hilo HTTP principal.
     */
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(10);
        exec.setQueueCapacity(200);
        exec.setThreadNamePrefix("audit-");
        exec.initialize();
        return exec;
    }
}
