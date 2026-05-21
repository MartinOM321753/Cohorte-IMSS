package imss.gob.mx.cohorte.security;

import imss.gob.mx.cohorte.security.filters.JWTFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class MainSecurity {

    @Autowired
    private JWTFilter jwtFilter;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain doFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsRegistry()))
                .authorizeHttpRequests(auth -> auth
                        // Los dispatches ASYNC (StreamingResponseBody) no deben re-evaluarse
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // Gestión de usuarios: solo ADMINISTRADOR puede crear/modificar
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMINISTRADOR")

                        // Pacientes: lectura para USER y ADMINISTRADOR, escritura solo ADMINISTRADOR
                        .requestMatchers(HttpMethod.GET, "/api/pacientes/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/pacientes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/pacientes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/pacientes/**").hasRole("ADMINISTRADOR")

                        // Citas: lectura y creación para USER y ADMINISTRADOR
                        .requestMatchers(HttpMethod.GET, "/api/citas/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/citas/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/citas/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/citas/**").hasRole("ADMINISTRADOR")

                        // Estudios médicos
                        .requestMatchers(HttpMethod.GET, "/api/estudios/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/estudios/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/estudios/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/estudios/**").hasRole("ADMINISTRADOR")

                        // Exámenes
                        .requestMatchers(HttpMethod.GET, "/api/examenes/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/examenes/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/examenes/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/examenes/**").hasRole("ADMINISTRADOR")

                        // Almacenamiento (refrigeradores, cajas, muestras)
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/almacenam   iento/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/almacenamiento/**").hasRole("ADMINISTRADOR")

                        // Prueba escalón
                        .requestMatchers(HttpMethod.GET, "/api/prueba-escalon/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/prueba-escalon/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/prueba-escalon/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/prueba-escalon/**").hasRole("ADMINISTRADOR")

                        // Documentos (archivos en MinIO): lectura y subida para USER y ADMINISTRADOR, borrado solo ADMINISTRADOR
                        .requestMatchers(HttpMethod.GET, "/api/documentos/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.POST, "/api/documentos/**").hasAnyRole("ADMINISTRADOR", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/documentos/**").hasRole("ADMINISTRADOR")

                        // Cualquier otro endpoint no especificado: requiere autenticación
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsRegistry() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH" , "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
