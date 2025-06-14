package tech.ipim.sweng.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.ipim.sweng.model.User;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void shouldGenerateValidJwtToken() {
        String token = jwtUtil.generateToken(testUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));  // JWT format has dots
        assertEquals(3, token.split("\\.").length);  // JWT has 3 parts
    }

    @Test
    void shouldExtractUsernameFromToken() {
        String token = jwtUtil.generateToken(testUser);

        String extractedUsername = jwtUtil.extractUsername(token);

        assertEquals("testuser", extractedUsername);
    }

    @Test
    void shouldExtractUserIdFromToken() {
        String token = jwtUtil.generateToken(testUser);

        Long extractedUserId = jwtUtil.extractUserId(token);

        assertEquals(1L, extractedUserId);
    }

    @Test
    void shouldValidateTokenCorrectly() {
        String token = jwtUtil.generateToken(testUser);

        Boolean isValid = jwtUtil.validateToken(token, testUser);

        assertTrue(isValid);
    }

    @Test
    void shouldRejectTokenForWrongUser() {
        String token = jwtUtil.generateToken(testUser);
        
        User differentUser = new User();
        differentUser.setId(2L);
        differentUser.setUsername("differentuser");

        Boolean isValid = jwtUtil.validateToken(token, differentUser);

        assertFalse(isValid);
    }

    @Test
    void shouldDetectExpiredToken() {
        String token = jwtUtil.generateToken(testUser);

        Boolean isExpired = jwtUtil.isTokenExpired(token);

        assertFalse(isExpired);
    }

    @Test
    void shouldValidateTokenFormat() {
        String token = jwtUtil.generateToken(testUser);

        Boolean isTokenValid = jwtUtil.isTokenValid(token);

        assertTrue(isTokenValid);
    }

    @Test
    void shouldRejectInvalidTokenFormat() {
        String invalidToken = "invalid.token.format";

        Boolean isTokenValid = jwtUtil.isTokenValid(invalidToken);

        assertFalse(isTokenValid);
    }

    @Test
    void shouldExtractBearerTokenFromHeader() {
        String authHeader = "Bearer jwt.token.here";

        String extractedToken = jwtUtil.extractTokenFromHeader(authHeader);

        assertEquals("jwt.token.here", extractedToken);
    }

    @Test
    void shouldReturnNullForInvalidBearerHeader() {
        String invalidHeader = "InvalidHeader jwt.token.here";

        String extractedToken = jwtUtil.extractTokenFromHeader(invalidHeader);

        assertNull(extractedToken);
    }

    @Test
    void shouldReturnNullForNullHeader() {
        String extractedToken = jwtUtil.extractTokenFromHeader(null);

        assertNull(extractedToken);
    }
}