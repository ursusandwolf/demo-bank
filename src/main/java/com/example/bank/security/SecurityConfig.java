package com.example.bank.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Отключаем CSRF для REST API на начальном этапе
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll() // Доступ всем
                .requestMatchers("/api/private/**").authenticated() // Только аутентифицированным
                .anyRequest().authenticated() // Все остальные запросы тоже защищены
            )
            .httpBasic(Customizer.withDefaults()); // Используем HTTP Basic

        return http.build();
    }
}
