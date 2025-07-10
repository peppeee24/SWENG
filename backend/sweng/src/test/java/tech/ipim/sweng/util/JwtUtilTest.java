package tech.ipim.sweng.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.ipim.sweng.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test unitari per la classe {@link JwtUtil}, che gestisce la generazione,
 * validazione e parsing dei token JWT.
 * <p>
 * I test coprono la generazione del token, l’estrazione di informazioni dal token,
 * la validazione e la gestione dell’header "Authorization".
 */
public class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setNome("Mario");
        testUser.setCognome("Rossi");
    }


    /**
     * Verifica che il metodo generateToken produca un token JWT valido
     * e conforme al formato standard (3 parti separate da '.').
     */
    @Test
    void shouldGenerateValidJwtToken() {
        String token = jwtUtil.generateToken(testUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));  // JWT format has dots
        assertEquals(3, token.split("\\.").length);  // JWT has 3 parts
    }


    /**
     * Verifica che il metodo extractUsername estragga correttamente
     * il nome utente dal token JWT.
     */
    @Test
    void shouldExtractUsernameFromToken() {
        String token = jwtUtil.generateToken(testUser);

        String extractedUsername = jwtUtil.extractUsername(token);

        assertEquals("testuser", extractedUsername);
    }


    /**
     * Verifica che extractUserId restituisca correttamente l’ID utente
     * dal token generato.
     */
    @Test
    void shouldExtractUserIdFromToken() {
        String token = jwtUtil.generateToken(testUser);

        Long extractedUserId = jwtUtil.extractUserId(token);

        assertEquals(1L, extractedUserId);
    }

    /**
     * Verifica che validateToken confermi la validità del token
     * se corrisponde all’utente corretto.
     */
    @Test
    void shouldValidateTokenCorrectly() {
        String token = jwtUtil.generateToken(testUser);

        Boolean isValid = jwtUtil.validateToken(token, testUser);

        assertTrue(isValid);
    }


    /**
     * Verifica che validateToken restituisca false se il token
     * è stato generato per un utente diverso.
     */
    @Test
    void shouldRejectTokenForWrongUser() {
        String token = jwtUtil.generateToken(testUser);
        
        User differentUser = new User();
        differentUser.setId(2L);
        differentUser.setUsername("differentuser");

        Boolean isValid = jwtUtil.validateToken(token, differentUser);

        assertFalse(isValid);
    }


    /**
     * Verifica che isTokenExpired restituisca false subito dopo la generazione
     * del token (non è ancora scaduto).
     */
    @Test
    void shouldDetectExpiredToken() {
        String token = jwtUtil.generateToken(testUser);

        Boolean isExpired = jwtUtil.isTokenExpired(token);

        assertFalse(isExpired);
    }


    /**
     * Verifica che isTokenValid confermi che il token generato è ben formato e valido.
     */
    @Test
    void shouldValidateTokenFormat() {
        String token = jwtUtil.generateToken(testUser);

        Boolean isTokenValid = jwtUtil.isTokenValid(token);

        assertTrue(isTokenValid);
    }


    /**
     * Verifica che isTokenValid restituisca false per un token malformato.
     */
    @Test
    void shouldRejectInvalidTokenFormat() {
        String invalidToken = "invalid.token.format";

        Boolean isTokenValid = jwtUtil.isTokenValid(invalidToken);

        assertFalse(isTokenValid);
    }


    /**
     * Verifica che extractTokenFromHeader estragga correttamente il token JWT
     * da un header Authorization valido con prefisso "Bearer ".
     */
    @Test
    void shouldExtractBearerTokenFromHeader() {
        String authHeader = "Bearer jwt.token.here";

        String extractedToken = jwtUtil.extractTokenFromHeader(authHeader);

        assertEquals("jwt.token.here", extractedToken);
    }


    /**
     * Verifica che extractTokenFromHeader restituisca null
     * se il formato dell’header è errato.
     */
    @Test
    void shouldReturnNullForInvalidBearerHeader() {
        String invalidHeader = "InvalidHeader jwt.token.here";

        String extractedToken = jwtUtil.extractTokenFromHeader(invalidHeader);

        assertNull(extractedToken);
    }


    /**
     * Verifica che extractTokenFromHeader gestisca correttamente
     * il caso in cui l’header sia null.
     */
    @Test
    void shouldReturnNullForNullHeader() {
        String extractedToken = jwtUtil.extractTokenFromHeader(null);

        assertNull(extractedToken);
    }
}