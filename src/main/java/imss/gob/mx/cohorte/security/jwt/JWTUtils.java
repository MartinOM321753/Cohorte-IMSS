package imss.gob.mx.cohorte.security.jwt;

import imss.gob.mx.cohorte.models.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JWTUtils {
    @Value("${secret.key}")
    private String SECRET_KEY;

    public Claims extractAllclaims(String token){
        return  Jwts.parser()
                .setSigningKey(SECRET_KEY.getBytes())
                .parseClaimsJws(token)
                .getBody();

    }

    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver){
        final Claims CLAIMS = extractAllclaims(token);
        return claimsResolver.apply(CLAIMS);
    }

    public String extractUsername(String token) {return extractClaim(token, Claims:: getSubject);}
    public Date extractExpirationDate(String token) { return extractClaim(token, Claims:: getExpiration);}
    public Boolean isTokenExpired(String token) {return extractExpirationDate(token).before(new Date());}
    public Boolean validateToken(String token, UserDetails userDetails){
        final String USERNAME = extractUsername(token);
        return (USERNAME.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private String createToken(Map<String, Object> claims, String subject){
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 *10))
                .signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
    }

    public String generateToken(User user){
        Map<String , Object > claims = new HashMap<>();
        claims.put("uuid",user.getUUID());
        claims.put("username",user.getId());
        return createToken(claims,user.getUsername());


    }

}
