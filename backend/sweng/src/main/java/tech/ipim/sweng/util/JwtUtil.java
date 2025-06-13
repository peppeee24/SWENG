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
    
    public JwtUtil() {
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }
    
    /**
     * Genera un JWT token per l'utente
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
     * Crea il JWT token con claims e subject
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
     * Estrae lo username dal token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Estrae la data di scadenza dal token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Estrae l'ID utente dal token
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }
    
    /**
     * Estrae un claim specifico dal token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Estrae tutti i claims dal token
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
     * Verifica se il token è scaduto
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true; // Se c'è un errore, considera il token scaduto
        }
    }
    
    /**
     * Valida il token contro l'utente
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
     * Verifica se il token è valido (formato e firma)
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
     * Estrae il Bearer token dall'header Authorization
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}