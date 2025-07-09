package tech.ipim.sweng.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import tech.ipim.sweng.model.User;

@Component
public class JwtUtil {
    
    // Chiave segreta per firmare i JWT (in produzione usare variabile ambiente)
    private static final String SECRET_KEY = "sweng2025_project_nota_bene_very_long_secret_key_for_jwt_token_generation_256_bits_minimum";
    
    // Durata token: 24 ore
    private static final long JWT_TOKEN_VALIDITY = 24 * 60 * 60 * 1000;
    
    private final SecretKey key;
    
    /**
     * Costruttore che inizializza la chiave segreta per la firma dei JWT.
     */

    public JwtUtil() {
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }
    
    /**
     * Genera un JWT token per l'utente specificato, includendo informazioni personalizzate nei claims.
     *
     * @param user Utente per cui generare il token
     * @return Token JWT firmato
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("nome", user.getNome());
        claims.put("cognome", user.getCognome());
        
        return createToken(claims, user.getUsername());
    }
    
    /**
     * Crea un JWT token con claims e subject specificati.
     *
     * @param claims  Claims personalizzati da inserire nel token
     * @param subject Username o identificativo del soggetto del token
     * @return Token JWT firmato
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Estrae lo username dal token JWT.
     *
     * @param token Token JWT
     * @return Username contenuto nel token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Estrae la data di scadenza dal token JWT.
     *
     * @param token Token JWT
     * @return Data di scadenza del token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Estrae l'ID utente dai claims del token JWT.
     *
     * @param token Token JWT
     * @return ID dell'utente
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }
    
    /**
     * Estrae un claim specifico dal token JWT.
     *
     * @param token          Token JWT
     * @param claimsResolver Funzione per estrarre il valore desiderato dai claims
     * @param <T>            Tipo del valore restituito
     * @return Valore del claim estratto
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Estrae tutti i claims dal token JWT.
     *
     * @param token Token JWT
     * @return Claims contenuti nel token
     * @throws RuntimeException se il token è scaduto, malformato o invalido
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            System.err.println("Token scaduto: " + e.getMessage());
            throw new RuntimeException("Token scaduto", e);
        } catch (UnsupportedJwtException e) {
            System.err.println("Token non supportato: " + e.getMessage());
            throw new RuntimeException("Token non supportato", e);
        } catch (MalformedJwtException e) {
            System.err.println("Token malformato: " + e.getMessage());
            throw new RuntimeException("Token malformato", e);
        } catch (SecurityException e) {
            System.err.println("Firma token non valida: " + e.getMessage());
            throw new RuntimeException("Firma token non valida", e);
        } catch (IllegalArgumentException e) {
            System.err.println("Token vuoto: " + e.getMessage());
            throw new RuntimeException("Token vuoto", e);
        }
    }
    
    /**
     * Verifica se il token JWT è scaduto.
     *
     * @param token Token JWT
     * @return True se scaduto, false altrimenti
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true; // Se c'è un errore, considera il token scaduto
        }
    }
    
    /**
     * Valida un token JWT confrontandolo con i dati di un utente.
     *
     * @param token Token JWT da validare
     * @param user  Utente di riferimento
     * @return True se il token è valido e associato all'utente, false altrimenti
     */
    public Boolean validateToken(String token, User user) {
        try {
            final String username = extractUsername(token);
            return (username.equals(user.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            System.err.println("Errore validazione token: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica la validità formale e di firma del token JWT.
     *
     * @param token Token JWT da verificare
     * @return True se il token è valido e non scaduto, false altrimenti
     */
    public Boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Estrae il Bearer token dall'header Authorization.
     *
     * @param authHeader Header Authorization della richiesta HTTP
     * @return Token JWT oppure null se non presente o formattato male
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}