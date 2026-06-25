package imss.gob.mx.cohorte.security.filters;

import imss.gob.mx.cohorte.controllers.auth.AuthController;
import imss.gob.mx.cohorte.security.jwt.JWTUtils;
import imss.gob.mx.cohorte.security.jwt.UDService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@AllArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final UDService udService; // Sirve para validar el token
    private final JWTUtils jwtUtils; // Sirve para manipular el token

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractTokenFromCookie(request);

            if (token != null && !token.isEmpty()) {
                String userUuid = jwtUtils.extractUserUuid(token);

                if (userUuid != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = udService.loadUserByUsername(userUuid);

                    if (jwtUtils.validateToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            // Token expirado, firma inválida o usuario no encontrado —
            // continuar sin autenticación; la cadena de seguridad decide
            // si el endpoint es público (permitAll) o requiere auth (401).
        }

        filterChain.doFilter(request, response);
    }

    /** Extrae el JWT de la cookie httpOnly establecida en el login (ver {@link AuthController}). */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (AuthController.AUTH_COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }
}
