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

        String userUuid = null;
        String token = extractTokenFromCookie(request);

        if (token != null && !token.isEmpty()) {
            try {
                userUuid = jwtUtils.extractUserUuid(token);
            } catch (Exception e) {
                System.out.println("JWT inválido: " + e.getMessage());
            }
        }

        if (userUuid != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Carga el rol/autoridades desde BD en cada request (no del claim "role" del JWT),
            // así un cambio de rol en BD aplica de inmediato sin esperar a que expire el token.
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

//    protected  void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        final String AUTHORIZATION_HEADER = request.getHeader("Authorization");
//        String username  = null;
//        String token = null ;
//
//        if (AUTHORIZATION_HEADER != null  && AUTHORIZATION_HEADER.startsWith("Bearer ")){
//            token = AUTHORIZATION_HEADER.substring(7).trim();
//            username = jwtUtils.extractUsername(token);
//        }
//        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null){
//
//            UserDetails userDetails = udService.loadUserByUsername(username);
//
//            if (jwtUtils.validateToken(token,userDetails)){
//                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
//                        userDetails,null,userDetails.getAuthorities()
//                );
//                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                SecurityContextHolder.getContext().setAuthentication(authToken);
//
//            }
//
//        }
//
//        filterChain.doFilter(request,response);
//    }

}
