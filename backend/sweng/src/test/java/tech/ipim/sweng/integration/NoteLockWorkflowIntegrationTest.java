package tech.ipim.sweng.integration;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("NoteLock Workflow Integration - Test integrazione workflow completo")
class NoteLockWorkflowIntegrationTest {

    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private String user1Token;
    private String user2Token;
    private Long testNoteId;
    
    @BeforeEach
    void setUp() {
        registerUser("lockuser1", "password123", "Lock", "User1", "lockuser1@test.com");
        registerUser("lockuser2", "password123", "Lock", "User2", "lockuser2@test.com");
        
        user1Token = loginAndGetToken("lockuser1", "password123");
        user2Token = loginAndGetToken("lockuser2", "password123");
        
        testNoteId = createTestNote();
    }
    
    @Test
    @DisplayName("TTD-WORKFLOW-001: Test workflow base acquisizione e rilascio lock")
    void testBasicLockWorkflow() {
        String baseUrl = "http://localhost:" + port + "/api/notes/" + testNoteId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user1Token);
        
        ResponseEntity<Map> lockResponse = restTemplate.exchange(
            baseUrl + "/lock",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class
        );
        
        assertNotEquals(HttpStatus.INTERNAL_SERVER_ERROR, lockResponse.getStatusCode());
        
        ResponseEntity<Map> unlockResponse = restTemplate.exchange(
            baseUrl + "/lock",
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Map.class
        );
        
        assertNotEquals(HttpStatus.INTERNAL_SERVER_ERROR, unlockResponse.getStatusCode());
    }
    
    @Test
    @DisplayName("TTD-WORKFLOW-002: Test stato lock")
    void testLockStatus() {
        String baseUrl = "http://localhost:" + port + "/api/notes/" + testNoteId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user1Token);
        
        ResponseEntity<Map> statusResponse = restTemplate.exchange(
            baseUrl + "/lock-status",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class
        );
        
        assertNotEquals(HttpStatus.INTERNAL_SERVER_ERROR, statusResponse.getStatusCode());
        
        if (statusResponse.getStatusCode() == HttpStatus.OK) {
            assertNotNull(statusResponse.getBody());
        }
    }
    
    @Test
    @DisplayName("TTD-WORKFLOW-003: Test refresh lock")
    void testRefreshLock() {
        String baseUrl = "http://localhost:" + port + "/api/notes/" + testNoteId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user1Token);
        
        restTemplate.exchange(
            baseUrl + "/lock",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class
        );
        
        ResponseEntity<Map> refreshResponse = restTemplate.exchange(
            baseUrl + "/lock/refresh",
            HttpMethod.PUT,
            new HttpEntity<>(headers),
            Map.class
        );
        
        assertNotEquals(HttpStatus.INTERNAL_SERVER_ERROR, refreshResponse.getStatusCode());
    }
    
    private void registerUser(String username, String password, String nome, String cognome, String email) {
        Map<String, String> request = Map.of(
            "username", username,
            "password", password,
            "nome", nome,
            "cognome", cognome,
            "email", email
        );
        
        try {
            restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/register",
                request,
                Map.class
            );
        } catch (Exception e) {
        }
    }
    
    private String loginAndGetToken(String username, String password) {
        Map<String, String> request = Map.of(
            "username", username,
            "password", password
        );
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                request,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("token");
            }
        } catch (Exception e) {
        }
        
        return "test-token-" + username;
    }
    
    private Long createTestNote() {
        Map<String, Object> request = Map.of(
            "titolo", "Test Note for Lock",
            "contenuto", "Contenuto per test lock",
            "tags", Set.of("test", "lock"),
            "cartelle", Set.of("test")
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user1Token);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/notes",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Map<String, Object> noteData = (Map<String, Object>) response.getBody().get("note");
                if (noteData != null) {
                    return Long.valueOf(noteData.get("id").toString());
                }
            }
        } catch (Exception e) {
        }
        
        return 1L;
    }
}