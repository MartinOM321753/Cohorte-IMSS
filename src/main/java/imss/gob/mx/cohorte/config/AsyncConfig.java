package imss.gob.mx.cohorte.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Thread pool dedicado para envío de notificaciones.
     * Evita bloquear el hilo HTTP mientras se envían emails/WhatsApp.
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

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
