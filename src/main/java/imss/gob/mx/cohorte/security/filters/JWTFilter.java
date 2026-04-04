package imss.gob.mx.cohorte.security.filters;

import imss.gob.mx.cohorte.security.jwt.JWTUtils;
import imss.gob.mx.cohorte.security.jwt.UDService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

        final String AUTHORIZATION_HEADER = request.getHeader("Authorization");

        String username = null;
        String token = null;
        System.out.println("HEADER: [" + AUTHORIZATION_HEADER + "]");
        System.out.println("TOKEN: [" + token + "]");
        if (AUTHORIZATION_HEADER != null && AUTHORIZATION_HEADER.startsWith("Bearer ")) {

            token = AUTHORIZATION_HEADER.substring(7).trim();

            if (!token.isEmpty()) {
                try {
                    username = jwtUtils.extractUsername(token);
                } catch (Exception e) {
                    System.out.println("JWT inválido: " + e.getMessage());
                }
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = udService.loadUserByUsername(username);

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
