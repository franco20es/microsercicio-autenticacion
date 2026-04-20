package com.example.AUHT_SERVICE.CONFIG;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

// Configuración de Jackson para la serialización/deserialización JSON
@Configuration
public class JacksonConfiguration {

    /**
     * Bean de ObjectMapper para deserialización/serialización JSON
     * @return ObjectMapper configurado
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
