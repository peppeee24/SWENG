package tech.ipim.sweng.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;


/**
 * Configurazione di sicurezza per l’ambiente di test.
 * <p>
 * Questa configurazione viene attivata solo con il profilo {@code "test"} e serve a disabilitare
 * i meccanismi di sicurezza (autenticazione, autorizzazione e CSRF) per consentire l'esecuzione
 * dei test senza dover gestire autenticazioni reali.
 * <p>
 * Caratteristiche principali:
 * <ul>
 *   <li>Disabilita CSRF</li>
 *   <li>Consente tutte le richieste senza autenticazione</li>
 *   <li>Disabilita gli header frameOptions (utile per H2 Console)</li>
 * </ul>
 */
@TestConfiguration
@EnableWebSecurity
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() 
                )
                .headers(headers -> headers.frameOptions().disable());

        return http.build();
    }
}