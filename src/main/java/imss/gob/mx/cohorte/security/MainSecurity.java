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
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
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
            "/api/auth/clear-session",   // limpieza de cookie stale antes del login
            "/api/auth/logout",          // registro de logout no requiere token válido
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/reset-password/validate",
            "/reset-password",
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

        // CSRF: la API es 100% stateless (sin sesión de servidor) y la cookie de
        // sesión se emite con SameSite=Lax/Strict + HttpOnly + Secure (ver
        // AuthController/application.properties). SameSite ya impide que un sitio
        // de terceros provoque que el navegador envíe la cookie en peticiones
        // cross-site, que es precisamente el vector que CSRF explota — por eso se
        // mantiene deshabilitado el mecanismo de token CSRF (evita complejidad
        // adicional sin aportar protección extra en este escenario).
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsRegistry()))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(restEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Los dispatches ASYNC (StreamingResponseBody) no deben re-evaluarse
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // Perfil propio: cualquier usuario autenticado puede cambiar su contraseña
                        // DEBE ir ANTES de las reglas generales de /api/users/**
                        .requestMatchers(HttpMethod.PUT, "/api/users/me/**").authenticated()

                        // Gestión de usuarios
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAuthority("USUARIOS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasAuthority("USUARIOS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAuthority("USUARIOS_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasAuthority("USUARIOS_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority("USUARIOS_ELIMINAR")

                        // Pacientes — crear-acceso DEBE ir ANTES de la regla general de POST
                        .requestMatchers(HttpMethod.POST, "/api/pacientes/uuid/*/crear-acceso").hasAuthority("PACIENTES_CREAR_ACCESO")
                        .requestMatchers(HttpMethod.GET, "/api/pacientes/**").hasAuthority("PACIENTES_VER")
                        .requestMatchers(HttpMethod.POST, "/api/pacientes/**").hasAuthority("PACIENTES_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/pacientes/**").hasAuthority("PACIENTES_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/pacientes/**").hasAuthority("PACIENTES_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/pacientes/**").hasAuthority("PACIENTES_ELIMINAR")

                        // Configuración de horario de citas
                        .requestMatchers(HttpMethod.GET, "/api/citas/configuracion-horario/**").hasAuthority("CITAS_CONFIGURACION_VER")
                        .requestMatchers(HttpMethod.POST, "/api/citas/configuracion-horario/**").hasAuthority("CITAS_CONFIGURACION_EDITAR")
                        .requestMatchers(HttpMethod.PUT, "/api/citas/configuracion-horario/**").hasAuthority("CITAS_CONFIGURACION_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/configuracion-horario/**").hasAuthority("CITAS_CONFIGURACION_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/citas/configuracion-horario/**").hasAuthority("CITAS_CONFIGURACION_EDITAR")

                        // Citas
                        .requestMatchers(HttpMethod.GET, "/api/citas/**").hasAuthority("CITAS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/citas/**").hasAuthority("CITAS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/citas/**").hasAuthority("CITAS_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/citas/**").hasAuthority("CITAS_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/citas/**").hasAuthority("CITAS_ELIMINAR")

                        // Estudios médicos
                        .requestMatchers(HttpMethod.GET, "/api/estudios/**").hasAuthority("ESTUDIOS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/estudios/**").hasAuthority("ESTUDIOS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/estudios/**").hasAuthority("ESTUDIOS_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/estudios/**").hasAuthority("ESTUDIOS_ELIMINAR")

                        // Exámenes
                        .requestMatchers(HttpMethod.GET, "/api/examenes/**").hasAuthority("EXAMENES_VER")
                        .requestMatchers(HttpMethod.POST, "/api/examenes/**").hasAuthority("EXAMENES_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/examenes/**").hasAuthority("EXAMENES_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/examenes/**").hasAuthority("EXAMENES_ELIMINAR")

                        // Traslados — DEBE ir ANTES de /api/almacenamiento/**
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/traslados/**").hasAuthority("TRASLADOS_VER")
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/almacenes/encargado/**").hasAuthority("TRASLADOS_VER")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/traslados/*/confirmar-recepcion").hasAuthority("TRASLADOS_CONFIRMAR")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/traslados/*/iniciar-devolucion").hasAuthority("TRASLADOS_DEVOLVER")

                        // Almacenamiento general (refrigeradores, cajas, muestras, almacenes)
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/**").hasAuthority("BIOBANCO_VER")
                        .requestMatchers(HttpMethod.POST, "/api/almacenamiento/**").hasAuthority("BIOBANCO_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/**").hasAuthority("BIOBANCO_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/almacenamiento/**").hasAuthority("BIOBANCO_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/almacenamiento/**").hasAuthority("BIOBANCO_ELIMINAR")

                        // Prueba escalón
                        .requestMatchers(HttpMethod.GET, "/api/prueba-escalon/**").hasAuthority("SOMATOMETRIA_VER")
                        .requestMatchers(HttpMethod.POST, "/api/prueba-escalon/**").hasAuthority("SOMATOMETRIA_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/prueba-escalon/**").hasAuthority("SOMATOMETRIA_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/prueba-escalon/**").hasAuthority("SOMATOMETRIA_ELIMINAR")

                        // Somatometría
                        .requestMatchers(HttpMethod.GET, "/api/somatometria/**").hasAuthority("SOMATOMETRIA_VER")
                        .requestMatchers(HttpMethod.POST, "/api/somatometria/**").hasAuthority("SOMATOMETRIA_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/somatometria/**").hasAuthority("SOMATOMETRIA_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/somatometria/**").hasAuthority("SOMATOMETRIA_ELIMINAR")

                        // Visualización por token temporal (escaneo QR) — público porque el token ES la autenticación
                        .requestMatchers(HttpMethod.GET, "/api/documentos/ver/**").permitAll()

                        // Documentos (archivos en MinIO)
                        .requestMatchers(HttpMethod.GET, "/api/documentos/**").hasAuthority("DOCUMENTOS_VER_METADATA")
                        .requestMatchers(HttpMethod.POST, "/api/documentos/**").hasAuthority("DOCUMENTOS_SUBIR")
                        .requestMatchers(HttpMethod.DELETE, "/api/documentos/**").hasAuthority("DOCUMENTOS_ELIMINAR")

                        // Bitácora
                        .requestMatchers(HttpMethod.GET, "/api/bitacora/**").hasAnyAuthority("BITACORA_ACCESOS_VER", "BITACORA_ACCIONES_VER")

                        // Copia de catálogos
                        .requestMatchers(HttpMethod.POST, "/api/catalogos/copiar/**").hasAuthority("CATALOGOS_EDITAR")

                        // Permisos admin API
                        .requestMatchers(HttpMethod.GET, "/api/permisos/**").hasAuthority("PERMISOS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/permisos/**").hasAuthority("PERMISOS_EDITAR")
                        .requestMatchers(HttpMethod.PUT, "/api/permisos/**").hasAuthority("PERMISOS_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/permisos/**").hasAuthority("PERMISOS_EDITAR")

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
        // Expose response headers that the browser JS can read (needed for blob downloads)
        configuration.setExposedHeaders(List.of(
                "Content-Disposition",
                "Content-Type",
                "Content-Length"
        ));
        // Necesario para que el navegador envíe/reciba la cookie httpOnly de sesión
        // en peticiones cross-origin (frontend y backend en distinto puerto/dominio).
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
