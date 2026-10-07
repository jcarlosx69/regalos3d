package es.labjc.regalos3d.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Reloj inyectable: los tests pueden fijar la fecha de hoy (fechas de entrega y cobro). */
@Configuration
public class RelojConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
