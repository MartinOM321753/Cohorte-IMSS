package imss.gob.mx.cohorte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "imss.gob.mx.cohorte")
public class CohorteApplication {

    public static void main(String[] args) {
        SpringApplication.run(CohorteApplication.class, args);
    }

}
