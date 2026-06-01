package imss.gob.mx.cohorte.security;

import imss.gob.mx.cohorte.security.filters.JWTFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class MainSecurity {

    @Autowired
    private JWTFilter jwtFilter;

    /** Lista de orígenes CORS permitidos, separados por coma (property: app.cors-origins). */
    @Value("${app.cors-origins:http://localhost:5173,http://localhost}")
    private String corsOrigins;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/logout",          // registro de logout no requiere token válido
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/reset-password/validate",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain doFilterChain(HttpSecurity http) throws Exception {
        // Entry point para REST: 401 (no 302 redirect a login, no 403)
        // Se activa cuando el token es inválido/expirado/ausente → el interceptor
        // de axios detecta 401 y hace logout automático en el frontend.
        AuthenticationEntryPoint restEntryPoint = (req, res, ex) ->
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsRegistry()))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(restEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Los dispatches ASYNC (StreamingResponseBody) no deben re-evaluarse
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // Perfil propio: cualquier usuario autenticado puede cambiar su contraseña
                        // DEBE ir ANTES de las reglas generales de /api/users/**
                        .requestMatchers(HttpMethod.PUT, "/api/users/me/**").authenticated()

                        // Gestión de usuarios: solo ADMINISTRADOR puede crear/modificar
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA", "ENCARGADO")
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMINISTRADOR")

                        // Pacientes: lectura para RECEPCIONISTA y ADMINISTRADOR, escritura solo ADMINISTRADOR
                        .requestMatchers(HttpMethod.GET, "/api/pacientes/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.POST, "/api/pacientes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/pacientes/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/pacientes/**").hasRole("ADMINISTRADOR")

                        // Citas: lectura y creación para RECEPCIONISTA y ADMINISTRADOR
                        .requestMatchers(HttpMethod.GET, "/api/citas/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.POST, "/api/citas/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.PUT, "/api/citas/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.DELETE, "/api/citas/**").hasRole("ADMINISTRADOR")

                        // Estudios médicos
                        .requestMatchers(HttpMethod.GET, "/api/estudios/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.POST, "/api/estudios/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.PUT, "/api/estudios/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.DELETE, "/api/estudios/**").hasRole("ADMINISTRADOR")

                        // Exámenes
                        .requestMatchers(HttpMethod.GET, "/api/examenes/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.POST, "/api/examenes/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.PUT, "/api/examenes/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.DELETE, "/api/examenes/**").hasRole("ADMINISTRADOR")

                        // Traslados y almacén propio: el ENCARGADO puede consultar y gestionar sus muestras.
                        // DEBE ir ANTES de la regla general de /api/almacenamiento/**.
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/traslados/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA", "ENCARGADO")
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/almacenes/encargado/**").hasAnyRole("ADMINISTRADOR", "ENCARGADO")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/traslados/*/confirmar-recepcion").hasAnyRole("ADMINISTRADOR", "ENCARGADO")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/traslados/*/iniciar-devolucion").hasAnyRole("ADMINISTRADOR", "ENCARGADO")

                        // Almacenamiento general (refrigeradores, cajas, muestras, almacenes)
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.POST, "/api/almacenamiento/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.PATCH, "/api/almacenamiento/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/almacenamiento/**").hasRole("ADMINISTRADOR")

                        // Prueba escalón
                        .requestMatchers(HttpMethod.GET, "/api/prueba-escalon/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.POST, "/api/prueba-escalon/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.PUT, "/api/prueba-escalon/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.DELETE, "/api/prueba-escalon/**").hasRole("ADMINISTRADOR")

                        // Documentos (archivos en MinIO): lectura y subida para RECEPCIONISTA y ADMINISTRADOR, borrado solo ADMINISTRADOR
                        .requestMatchers(HttpMethod.GET, "/api/documentos/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.POST, "/api/documentos/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.DELETE, "/api/documentos/**").hasRole("ADMINISTRADOR")

                        // Bitácora: solo ADMINISTRADOR puede consultar los registros de auditoría
                        .requestMatchers("/api/bitacora/**").hasRole("ADMINISTRADOR")

                        // Dashboard: accesible para cualquier usuario autenticado
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**").authenticated()

                        // Cualquier otro endpoint no especificado: requiere autenticación
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsRegistry() {
        List<String> origins = Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
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
