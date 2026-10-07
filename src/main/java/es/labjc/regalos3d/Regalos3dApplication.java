package es.labjc.regalos3d;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Regalos3dApplication {

    public static void main(String[] args) {
        SpringApplication.run(Regalos3dApplication.class, args);
    }
}
