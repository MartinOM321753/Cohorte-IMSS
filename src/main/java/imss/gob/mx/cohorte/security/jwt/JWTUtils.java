package imss.gob.mx.cohorte.security.jwt;

import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTUtils {

    @Value("${secret.key}")
    private String SECRET_KEY;

    // La clave debe tener al menos 32 caracteres (256 bits) para HS256
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public String extractUserUuid(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpirationDate(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Boolean isTokenExpired(String token) {
        return extractExpirationDate(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUserUuid(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Extrae la fecha de emisión del token (claim "iat").
     * Se usa para calcular la duración de la sesión en el evento LOGOUT.
     */
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    /**
     * Extrae el claim "name" del token (nombre completo del usuario).
     */
    public String extractNombreCompleto(String token) {
        return extractClaim(token, claims -> claims.get("name", String.class));
    }

    /**
     * Extrae el claim "role" del token.
     *
     * <p><b>No usar para autorización.</b> Es solo informativo (frontend/logging):
     * si el rol del usuario cambia en BD, este claim queda obsoleto hasta que se
     * emita un token nuevo. Las decisiones de autorización (hasRole/@PreAuthorize)
     * usan UDService#loadUserByUsername, que consulta el rol vigente en BD
     * en cada request — por eso un cambio de rol surte efecto de inmediato sin
     * esperar a que expire el token.
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 10))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateToken(BeanUser beanUser) {
        Map<String, Object> claims = new HashMap<>();
        String fullName = beanUser.getPersona().getNombre() + " " + beanUser.getPersona().getApellidoPaterno();
        if (beanUser.getPersona().getApellidoMaterno() != null && !beanUser.getPersona().getApellidoMaterno().isEmpty()) {
            fullName += " " + beanUser.getPersona().getApellidoMaterno();
        }
        claims.put("name", fullName);
        claims.put("activo", beanUser.getActivo());
        claims.put("role", beanUser.getRol().getRole());
        // Indica al frontend si debe forzar cambio de contraseña en el primer login
        claims.put("mustChangePassword", Boolean.TRUE.equals(beanUser.getDebeResetear()));
        // Contexto de institución — usado para aislar datos por sede/sucursal.
        // Sólo informativo en el token (igual que "role"): la autorización real
        // de acceso a módulos se resuelve en cada request contra BD vigente.
        if (beanUser.getInstitucion() != null) {
            claims.put("institucionId", beanUser.getInstitucion().getId());
            claims.put("institucionUuid", beanUser.getInstitucion().getUuid());
            claims.put("institucionNombre", beanUser.getInstitucion().getNombre());
        }
        return createToken(claims, beanUser.getUUID());
    }
}
