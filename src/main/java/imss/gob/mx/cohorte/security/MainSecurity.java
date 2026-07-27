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
import org.springframework.security.web.access.AccessDeniedHandler;
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

        AccessDeniedHandler restAccessDeniedHandler = (req, res, ex) ->
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");

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
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(restEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Los dispatches ASYNC (StreamingResponseBody) no deben re-evaluarse
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
    
                        // Perfil propio: cualquier usuario autenticado puede cambiar su contraseña
                        // DEBE ir ANTES de las reglas generales de /api/users/**
                        .requestMatchers(HttpMethod.PUT, "/api/users/me/**").authenticated()

                        // ═══ USUARIOS: pantalla propia vs lookup por rol ═══
                        // /rol/MEDICO se usa desde formularios de citas/estudios (asignar
                        // medico); /paginado desde la pantalla admin de usuarios.
                        .requestMatchers(HttpMethod.GET, "/api/users/rol/*")
                                .hasAnyAuthority("USUARIOS_LOOKUP_MEDICOS", "USUARIOS_ACCEDER")
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAuthority("USUARIOS_ACCEDER")
                        // reenviar-invitacion es un action-endpoint POST que debe caer en EDITAR
                        // (no en CREAR): el usuario ya existe, solo se re-envia su email.
                        .requestMatchers(HttpMethod.POST, "/api/users/*/reenviar-invitacion").hasAuthority("USUARIOS_EDITAR")
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasAuthority("USUARIOS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAuthority("USUARIOS_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasAuthority("USUARIOS_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority("USUARIOS_ELIMINAR")

                        // Expediente 360 — el rol PACIENTE reutiliza la MISMA página y los
                        // MISMOS endpoints que el staff, en modo lectura. Estas reglas van
                        // ANTES de las generales de abajo y solo abren las rutas "por UUID"
                        // (no listados/búsquedas). El controlador exige además, vía
                        // PacienteApplicationService.verificarAccesoPropioSiEsPaciente(uuid),
                        // que ese UUID sea el del propio participante — nunca el de otro.
                        .requestMatchers(HttpMethod.GET, "/api/pacientes/mi-uuid").hasAuthority("EXPEDIENTE_VER")
                        .requestMatchers(HttpMethod.GET, "/api/pacientes/uuid/*")
                                .hasAnyAuthority("PACIENTES_LOOKUP", "PACIENTES_ACCEDER", "EXPEDIENTE_VER")
                        .requestMatchers(HttpMethod.GET, "/api/citas/paciente/*/resumen")
                                .hasAnyAuthority("CITAS_VER", "EXPEDIENTE_VER")
                        .requestMatchers(HttpMethod.GET, "/api/estudios/paciente/*")
                                .hasAnyAuthority("ESTUDIOS_LLENADO_ACCEDER", "EXPEDIENTE_VER")
                        .requestMatchers(HttpMethod.GET, "/api/examenes/resultados/paciente/uuid/*")
                                .hasAnyAuthority("EXAMENES_LLENADO_ACCEDER", "EXPEDIENTE_VER")
                        .requestMatchers(HttpMethod.GET, "/api/examenes/resultados/paciente/uuid/*/count")
                                .hasAnyAuthority("EXAMENES_LLENADO_ACCEDER", "EXPEDIENTE_VER")
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/muestras/paciente/uuid/*/count")
                                .hasAnyAuthority("MUESTRAS_VER", "EXPEDIENTE_BIOBANCO")
                        .requestMatchers(HttpMethod.GET, "/api/somatometria/paciente/*/latest")
                                .hasAnyAuthority("SOMATOMETRIA_VER", "EXPEDIENTE_SOMATOMETRIA")
                        .requestMatchers(HttpMethod.GET, "/api/somatometria/paciente/*")
                                .hasAnyAuthority("SOMATOMETRIA_VER", "EXPEDIENTE_SOMATOMETRIA")

                        // ═══ PACIENTES: pantalla propia vs lookup cross-modulo ═══
                        // Endpoints POST con permiso especifico ANTES del POST general.
                        .requestMatchers(HttpMethod.POST, "/api/pacientes/uuid/*/crear-acceso").hasAuthority("PACIENTES_CREAR_ACCESO")
                        .requestMatchers(HttpMethod.POST, "/api/pacientes/importar").hasAuthority("PACIENTES_IMPORTAR")
                        // Endpoints lookup (busqueda, folio, activos, detalle por UUID) — usados por
                        // formularios de otros modulos (Estudios, Examenes, Citas, Somatometria).
                        .requestMatchers(HttpMethod.GET, "/api/pacientes/buscar",
                                                         "/api/pacientes/activos",
                                                         "/api/pacientes/folio/*")
                                .hasAnyAuthority("PACIENTES_LOOKUP", "PACIENTES_ACCEDER",
                                                 "ESTUDIOS_LLENADO_ACCEDER", "EXAMENES_LLENADO_ACCEDER")
                        // Pantalla admin: lista paginada
                        .requestMatchers(HttpMethod.GET, "/api/pacientes/**").hasAuthority("PACIENTES_ACCEDER")
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

                        // ═══ ESTUDIOS: split llenado vs catalogo ═══
                        // El catalogo de tipos (activos) es lookup — lo consumen tanto el tab
                        // Llenado (dropdown) como el tab Catalogo. Los detalles de parametros
                        // tambien son lookup.
                        .requestMatchers(HttpMethod.GET, "/api/estudios/tipos/todos")
                                .hasAuthority("ESTUDIOS_CATALOGO_ACCEDER")
                        .requestMatchers(HttpMethod.GET, "/api/estudios/tipos", "/api/estudios/tipos/*")
                                .hasAnyAuthority("ESTUDIOS_TIPOS_LOOKUP",
                                                 "ESTUDIOS_LLENADO_ACCEDER",
                                                 "ESTUDIOS_CATALOGO_ACCEDER")
                        .requestMatchers(HttpMethod.GET, "/api/estudios/tipos/*/parametros")
                                .hasAnyAuthority("ESTUDIOS_TIPOS_LOOKUP",
                                                 "ESTUDIOS_LLENADO_ACCEDER",
                                                 "ESTUDIOS_CATALOGO_ACCEDER")
                        // Escritura del catalogo
                        .requestMatchers(HttpMethod.POST, "/api/estudios/tipos",
                                                          "/api/estudios/parametros",
                                                          "/api/estudios/parametros/*/opciones")
                                .hasAuthority("ESTUDIOS_TIPOS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/estudios/tipos/*",
                                                         "/api/estudios/tipos/*/toggle",
                                                         "/api/estudios/parametros/*")
                                .hasAuthority("ESTUDIOS_TIPOS_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/estudios/tipos/*",
                                                            "/api/estudios/parametros/*",
                                                            "/api/estudios/parametros/opciones/*")
                                .hasAuthority("ESTUDIOS_TIPOS_ELIMINAR")
                        // Llenado (estudios registrados)
                        .requestMatchers(HttpMethod.GET, "/api/estudios/**").hasAuthority("ESTUDIOS_LLENADO_ACCEDER")
                        .requestMatchers(HttpMethod.POST, "/api/estudios/**").hasAuthority("ESTUDIOS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/estudios/**").hasAuthority("ESTUDIOS_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/estudios/**").hasAuthority("ESTUDIOS_ELIMINAR")

                        // ═══ EXAMENES: split llenado vs catalogo ═══
                        // Los resultados de examen son del llenado; el listado de examenes
                        // (catalogo) es lookup para el dropdown en llenado + admin en tab.
                        .requestMatchers(HttpMethod.GET, "/api/examenes/resultados/**")
                                .hasAuthority("EXAMENES_LLENADO_ACCEDER")
                        .requestMatchers(HttpMethod.POST, "/api/examenes/resultados/**")
                                .hasAuthority("EXAMENES_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/examenes/resultados/**")
                                .hasAuthority("EXAMENES_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/examenes/resultados/**")
                                .hasAuthority("EXAMENES_ELIMINAR")
                        // Catalogo lista (activos) — consumido por Llenado y Catalogo
                        .requestMatchers(HttpMethod.GET, "/api/examenes/**")
                                .hasAnyAuthority("EXAMENES_LOOKUP",
                                                 "EXAMENES_LLENADO_ACCEDER",
                                                 "EXAMENES_CATALOGO_ACCEDER")
                        .requestMatchers(HttpMethod.POST, "/api/examenes/**").hasAuthority("EXAMENES_CATALOGO_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/examenes/**").hasAuthority("EXAMENES_CATALOGO_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/examenes/**").hasAuthority("EXAMENES_CATALOGO_ELIMINAR")

                        // ═══ BIOBANCO: desglose granular por sección ═══
                        // Cada tab del frontend carga endpoints de su sección Y de secciones
                        // relacionadas (cross-deps). Los GETs cruzados usan hasAnyAuthority
                        // para evitar 403 cascada cuando un tab necesita leer datos de otro.

                        // ── Traslados (préstamos inter-institucionales) ──
                        // Tab Prestamos + MuestrasTab (para estado de traslados por muestra)
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/traslados/**")
                                .hasAnyAuthority("TRASLADOS_LOOKUP", "TRASLADOS_ACCEDER", "MUESTRAS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/almacenamiento/traslados/**").hasAuthority("TRASLADOS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/traslados/*/confirmar-recepcion").hasAuthority("TRASLADOS_CONFIRMAR")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/traslados/*/confirmar-devolucion").hasAuthority("TRASLADOS_DEVOLVER")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/traslados/*/iniciar-devolucion").hasAuthority("TRASLADOS_DEVOLVER")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/traslados/*/cancelar").hasAuthority("TRASLADOS_CANCELAR")

                        // ── Almacenes (instituciones destino) — compartido entre tabs ──
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/almacenes/**")
                                .hasAnyAuthority("TRASLADOS_ACCEDER", "TRASLADOS_LOOKUP",
                                                 "MUESTRAS_VER", "TIPOS_MUESTRA_ACCEDER", "TIPOS_MUESTRA_LOOKUP")

                        // ── Muestras — impresión (ANTES de reglas generales de muestras) ──
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/muestras/impresoras").hasAuthority("MUESTRAS_IMPRIMIR")
                        .requestMatchers(HttpMethod.POST, "/api/almacenamiento/muestras/*/etiqueta/imprimir").hasAuthority("MUESTRAS_IMPRIMIR")
                        .requestMatchers(HttpMethod.POST, "/api/almacenamiento/muestras/*/alicuotas/etiquetas/imprimir").hasAuthority("MUESTRAS_IMPRIMIR")
                        .requestMatchers(HttpMethod.POST, "/api/almacenamiento/muestras/*/lote-completo/imprimir").hasAuthority("MUESTRAS_IMPRIMIR")
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/muestras/*/etiqueta/zpl").hasAuthority("MUESTRAS_IMPRIMIR")
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/muestras/*/alicuotas/etiquetas/zpl").hasAuthority("MUESTRAS_IMPRIMIR")
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/muestras/*/lote-completo/zpl").hasAuthority("MUESTRAS_IMPRIMIR")

                        // ── Muestras — posición (PrestamosTab asigna posición al confirmar recepción) ──
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/muestras/*/posicion").hasAnyAuthority("MUESTRAS_EDITAR", "TRASLADOS_CONFIRMAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/almacenamiento/muestras/*/posicion").hasAuthority("MUESTRAS_EDITAR")

                        // ── Muestras — generar alícuotas ──
                        .requestMatchers(HttpMethod.POST, "/api/almacenamiento/muestras/*/generar-alicuotas").hasAuthority("MUESTRAS_CREAR")

                        // ── Muestras — dar de baja ──
                        .requestMatchers(HttpMethod.POST, "/api/almacenamiento/muestras/*/baja").hasAuthority("MUESTRAS_DAR_BAJA")

                        // ── Muestras — CRUD general ──
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/muestras/**").hasAuthority("MUESTRAS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/almacenamiento/muestras/**").hasAuthority("MUESTRAS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/muestras/**").hasAuthority("MUESTRAS_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/almacenamiento/muestras/**").hasAuthority("MUESTRAS_ELIMINAR")

                        // ── Refrigeradores — pantalla propia + lookup para CajaFormModal ──
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/refrigeradores/**")
                                .hasAnyAuthority("REFRIGERADORES_LOOKUP", "REFRIGERADORES_ACCEDER", "CAJAS_ACCEDER")
                        .requestMatchers(HttpMethod.POST, "/api/almacenamiento/refrigeradores/**").hasAuthority("REFRIGERADORES_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/refrigeradores/**").hasAuthority("REFRIGERADORES_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/almacenamiento/refrigeradores/**").hasAuthority("REFRIGERADORES_ELIMINAR")

                        // ── Cajas — pantalla propia + lookup para asignar posicion (MuestrasTab, PrestamosTab) ──
                        .requestMatchers(HttpMethod.GET, "/api/almacenamiento/cajas/**")
                                .hasAnyAuthority("CAJAS_LOOKUP", "CAJAS_ACCEDER", "MUESTRAS_VER", "TRASLADOS_CONFIRMAR")
                        .requestMatchers(HttpMethod.POST, "/api/almacenamiento/cajas/**").hasAuthority("CAJAS_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/almacenamiento/cajas/**").hasAuthority("CAJAS_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/almacenamiento/cajas/**").hasAuthority("CAJAS_ELIMINAR")

                        // ── Tipos de muestra — pantalla admin + lookup para MuestraFormModal ──
                        .requestMatchers(HttpMethod.GET, "/api/muestras/tipos/**")
                                .hasAnyAuthority("TIPOS_MUESTRA_LOOKUP", "TIPOS_MUESTRA_ACCEDER", "MUESTRAS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/muestras/tipos/**").hasAuthority("TIPOS_MUESTRA_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/muestras/tipos/**").hasAuthority("TIPOS_MUESTRA_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/muestras/tipos/**").hasAuthority("TIPOS_MUESTRA_ELIMINAR")

                        // ── Estudios de muestra — pantalla admin + lookup para llenado desde MuestrasTab ──
                        .requestMatchers(HttpMethod.GET, "/api/muestras/estudios/tipos/**")
                                .hasAnyAuthority("ESTUDIOS_MUESTRA_LOOKUP", "ESTUDIOS_MUESTRA_ACCEDER", "MUESTRAS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/muestras/estudios/tipos").hasAuthority("ESTUDIOS_MUESTRA_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/muestras/estudios/tipos/**").hasAuthority("ESTUDIOS_MUESTRA_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/muestras/estudios/tipos/*").hasAuthority("ESTUDIOS_MUESTRA_ELIMINAR")
                        .requestMatchers(HttpMethod.GET, "/api/muestras/estudios/parametros/**")
                                .hasAnyAuthority("ESTUDIOS_MUESTRA_LOOKUP", "ESTUDIOS_MUESTRA_ACCEDER", "MUESTRAS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/muestras/estudios/parametros/**").hasAuthority("ESTUDIOS_MUESTRA_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/muestras/estudios/parametros/**").hasAuthority("ESTUDIOS_MUESTRA_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/muestras/estudios/parametros/**").hasAuthority("ESTUDIOS_MUESTRA_ELIMINAR")

                        // ── Estudios por muestra (datos operacionales — parte del workflow de muestras) ──
                        .requestMatchers(HttpMethod.GET, "/api/muestras/*/estudios").hasAuthority("MUESTRAS_VER")
                        .requestMatchers(HttpMethod.GET, "/api/muestras/estudios/*").hasAuthority("MUESTRAS_VER")
                        .requestMatchers(HttpMethod.POST, "/api/muestras/*/estudios").hasAnyAuthority("ESTUDIOS_MUESTRA_CREAR", "MUESTRAS_EDITAR")
                        .requestMatchers(HttpMethod.PUT, "/api/muestras/estudios/*").hasAnyAuthority("ESTUDIOS_MUESTRA_EDITAR", "MUESTRAS_EDITAR")

                        // ── Historial de cambios de muestra ──
                        .requestMatchers(HttpMethod.GET, "/api/muestras/*/historial").hasAuthority("MUESTRAS_VER")

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

                        // Cobertura (bajo /api/dashboard/cobertura) — ANTES de la regla general de dashboard
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/cobertura/**").hasAuthority("COBERTURA_VER")

                        // Dashboard — widgets granulares (cada uno con su propio permiso)
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/stats").hasAuthority("DASHBOARD_KPI_VER")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/somatometria-global").hasAuthority("DASHBOARD_SOMATOMETRIA_VER")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/examenes-global").hasAuthority("DASHBOARD_EXAMENES_VER")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/examenes-calidad").hasAuthority("DASHBOARD_EXAMENES_VER")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/biobanco-ocupacion").hasAuthority("DASHBOARD_BIOBANCO_VER")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/biobanco-cajas").hasAuthority("DASHBOARD_BIOBANCO_VER")
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/agenda-hoy").hasAuthority("DASHBOARD_AGENDA_VER")

                        // ═══ INSTITUCIONES: pantalla admin vs lookup cross-modulo ═══
                        // /actual/** es bootstrap: CUALQUIER usuario autenticado lo necesita para
                        // saber que modulos estan habilitados en su institucion y renderizar la
                        // sidebar. Debe ir ANTES de las reglas por permiso.
                        .requestMatchers(HttpMethod.GET, "/api/instituciones/actual/**")
                                .authenticated()
                        // Pantalla admin: lista paginada
                        .requestMatchers(HttpMethod.GET, "/api/instituciones/paginado",
                                                         "/api/instituciones/todas")
                                .hasAuthority("INSTITUCIONES_ACCEDER")
                        // Endpoints lookup (busqueda, jerarquia, dropdowns)
                        .requestMatchers(HttpMethod.GET, "/api/instituciones/**")
                                .hasAnyAuthority("INSTITUCIONES_LOOKUP", "INSTITUCIONES_ACCEDER")
                        .requestMatchers(HttpMethod.POST, "/api/instituciones/**").hasAuthority("INSTITUCIONES_CREAR")
                        .requestMatchers(HttpMethod.PUT, "/api/instituciones/**").hasAuthority("INSTITUCIONES_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/instituciones/**").hasAuthority("INSTITUCIONES_EDITAR")
                        .requestMatchers(HttpMethod.DELETE, "/api/instituciones/**").hasAuthority("INSTITUCIONES_ELIMINAR")

                        // ═══ CATALOGOS / UNIDADES DE MEDIDA ═══
                        // Las unidades activas son lookup — las consumen formularios de parametros
                        // de estudio/examen/muestra.
                        .requestMatchers(HttpMethod.GET, "/api/catalogos/unidades/todas")
                                .hasAuthority("CATALOGOS_ACCEDER")
                        .requestMatchers(HttpMethod.GET, "/api/catalogos/unidades/**")
                                .hasAnyAuthority("UNIDADES_LOOKUP", "CATALOGOS_ACCEDER")
                        .requestMatchers(HttpMethod.POST, "/api/catalogos/unidades/**").hasAuthority("CATALOGOS_EDITAR")
                        .requestMatchers(HttpMethod.PUT, "/api/catalogos/unidades/**").hasAuthority("CATALOGOS_EDITAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/catalogos/unidades/**").hasAuthority("CATALOGOS_EDITAR")

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
