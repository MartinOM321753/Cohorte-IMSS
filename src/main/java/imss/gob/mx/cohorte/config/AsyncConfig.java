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
     * Hilos para las cargas masivas que corren en segundo plano.
     *
     * <p>Chico a propósito. Una carga de volcado no es un correo: está escribiendo
     * cientos de miles de filas, y varias a la vez no acabarían antes —se pelearían
     * por la base y todas irían más despacio— además de multiplicar la memoria, porque
     * cada una tiene su archivo cargado. Dos en paralelo es para que una carga de un
     * instrumento no tenga que esperar a la de otro.</p>
     *
     * <p>La cola es corta porque pasada de ahí lo correcto es decir «ya hay cargas en
     * proceso, espera» y no aceptar trabajo que va a tardar en empezar: una tarea
     * encolada se ve igual que una corriendo, y quien la subió creería que avanza.</p>
     */
    @Bean(name = "cargasExecutor")
    public Executor cargasExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(1);
        exec.setMaxPoolSize(2);
        exec.setQueueCapacity(4);
        exec.setThreadNamePrefix("carga-");
        exec.initialize();
        return exec;
    }

    /**
     * Reasignaciones por rango de folios en segundo plano.
     *
     * <p>Una sola a la vez: cada una recorre el padrón y mueve participantes de uno
     * en uno, y dos a la vez sobre los mismos registros se estorbarían. La cola es
     * corta por la misma razón que en {@code cargasExecutor}: lo que no puede empezar
     * pronto se rechaza con un mensaje, no se deja esperando en silencio.</p>
     */
    @Bean(name = "reasignacionesExecutor")
    public Executor reasignacionesExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(1);
        exec.setMaxPoolSize(1);
        exec.setQueueCapacity(5);
        exec.setThreadNamePrefix("reasig-");
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
