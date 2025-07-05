package tech.ipim.sweng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.ipim.sweng.dto.CartellaDto;
import tech.ipim.sweng.service.CartellaService;
import tech.ipim.sweng.service.UserService;
import tech.ipim.sweng.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartellaController.class)
class CartellaControllerSearchMethodsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartellaService cartellaService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private CartellaDto testCartellaDto;
    private List<CartellaDto> testCartelle;
    private String validToken;
    private String testUsername;

    @BeforeEach
    void setUp() {
        testUsername = "testuser";
        validToken = "Bearer valid-jwt-token";

        testCartellaDto = new CartellaDto();
        testCartellaDto.setId(1L);
        testCartellaDto.setNome("Test Cartella");
        testCartellaDto.setDescrizione("Descrizione test");
        testCartellaDto.setColore("#ff6b6b");
        testCartellaDto.setProprietario("testuser");
        testCartellaDto.setNumeroNote(5);
        testCartellaDto.setDataCreazione(LocalDateTime.of(2024, 1, 15, 10, 0));
        testCartellaDto.setDataModifica(LocalDateTime.of(2024, 1, 16, 12, 0));

        testCartelle = Arrays.asList(testCartellaDto);

        // Mock JWT
        when(jwtUtil.extractTokenFromHeader(validToken)).thenReturn("valid-jwt-token");
        when(jwtUtil.isTokenValid("valid-jwt-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-jwt-token")).thenReturn(testUsername);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetAllCartelle() throws Exception {
        // Given
        when(cartellaService.getUserCartelle("testuser")).thenReturn(testCartelle);

        // When & Then
        mockMvc.perform(get("/api/cartelle")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartelle").isArray())
                .andExpect(jsonPath("$.cartelle", hasSize(1)))
                .andExpect(jsonPath("$.cartelle[0].id").value(1))
                .andExpect(jsonPath("$.cartelle[0].nome").value("Test Cartella"))
                .andExpect(jsonPath("$.cartelle[0].numeroNote").value(5))
                .andExpect(jsonPath("$.count").value(1));

        verify(cartellaService).getUserCartelle("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnEmptyListWhenNoCartelle() throws Exception {
        // Given
        when(cartellaService.getUserCartelle("testuser")).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/cartelle")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartelle").isArray())
                .andExpect(jsonPath("$.cartelle").isEmpty())
                .andExpect(jsonPath("$.count").value(0));

        verify(cartellaService).getUserCartelle("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetCartellaById() throws Exception {
        // Given
        when(cartellaService.getCartellaById(1L, "testuser")).thenReturn(Optional.of(testCartellaDto));

        // When & Then
        mockMvc.perform(get("/api/cartelle/1")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cartella trovata"))
                .andExpect(jsonPath("$.cartella.id").value(1))
                .andExpect(jsonPath("$.cartella.nome").value("Test Cartella"))
                .andExpect(jsonPath("$.cartella.numeroNote").value(5));

        verify(cartellaService).getCartellaById(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnNotFoundWhenCartellaDoesNotExist() throws Exception {
        // Given
        when(cartellaService.getCartellaById(999L, "testuser")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/cartelle/999")
                .header("Authorization", validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cartella non trovata"));

        verify(cartellaService).getCartellaById(999L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetCartelleStats() throws Exception {
        // Given
        CartellaService.CartelleStats stats = new CartellaService.CartelleStats(3L, 
                Arrays.asList("Lavoro", "Personale", "Studio"));
        when(cartellaService.getUserCartelleStats("testuser")).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/cartelle/stats")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.numeroCartelle").value(3))
                .andExpect(jsonPath("$.stats.nomiCartelle").isArray())
                .andExpect(jsonPath("$.stats.nomiCartelle", hasSize(3)))
                .andExpect(jsonPath("$.stats.nomiCartelle[0]").value("Lavoro"))
                .andExpect(jsonPath("$.stats.nomiCartelle[1]").value("Personale"))
                .andExpect(jsonPath("$.stats.nomiCartelle[2]").value("Studio"));

        verify(cartellaService).getUserCartelleStats("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetEmptyStats() throws Exception {
        // Given
        CartellaService.CartelleStats emptyStats = new CartellaService.CartelleStats(0L, Collections.emptyList());
        when(cartellaService.getUserCartelleStats("testuser")).thenReturn(emptyStats);

        // When & Then
        mockMvc.perform(get("/api/cartelle/stats")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.numeroCartelle").value(0))
                .andExpect(jsonPath("$.stats.nomiCartelle").isArray())
                .andExpect(jsonPath("$.stats.nomiCartelle").isEmpty());

        verify(cartellaService).getUserCartelleStats("testuser");
    }

    @Test
    void shouldRequireAuthenticationForCartellaEndpoints() throws Exception {
        // Test get all cartelle
        mockMvc.perform(get("/api/cartelle"))
                .andExpect(status().isUnauthorized());

        // Test get cartella by id
        mockMvc.perform(get("/api/cartelle/1"))
                .andExpect(status().isUnauthorized());

        // Test get stats
        mockMvc.perform(get("/api/cartelle/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleServiceExceptionGracefully() throws Exception {
        // Given
        when(cartellaService.getUserCartelle("testuser")).thenThrow(new RuntimeException("Errore database"));

        // When & Then
        mockMvc.perform(get("/api/cartelle")
                .header("Authorization", validToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Errore durante il recupero delle cartelle"));

        verify(cartellaService).getUserCartelle("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleStatsServiceException() throws Exception {
        // Given
        when(cartellaService.getUserCartelleStats("testuser")).thenThrow(new RuntimeException("Utente non trovato"));

        // When & Then
        mockMvc.perform(get("/api/cartelle/stats")
                .header("Authorization", validToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Errore durante il recupero delle statistiche"));

        verify(cartellaService).getUserCartelleStats("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleMultipleCartelle() throws Exception {
        // Given
        CartellaDto cartella1 = new CartellaDto();
        cartella1.setId(1L);
        cartella1.setNome("Lavoro");
        cartella1.setNumeroNote(10);

        CartellaDto cartella2 = new CartellaDto();
        cartella2.setId(2L);
        cartella2.setNome("Personale");
        cartella2.setNumeroNote(5);

        CartellaDto cartella3 = new CartellaDto();
        cartella3.setId(3L);
        cartella3.setNome("Studio");
        cartella3.setNumeroNote(0);

        List<CartellaDto> multipleCartelle = Arrays.asList(cartella1, cartella2, cartella3);
        when(cartellaService.getUserCartelle("testuser")).thenReturn(multipleCartelle);

        // When & Then
        mockMvc.perform(get("/api/cartelle")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartelle", hasSize(3)))
                .andExpect(jsonPath("$.cartelle[0].nome").value("Lavoro"))
                .andExpect(jsonPath("$.cartelle[0].numeroNote").value(10))
                .andExpect(jsonPath("$.cartelle[1].nome").value("Personale"))
                .andExpect(jsonPath("$.cartelle[1].numeroNote").value(5))
                .andExpect(jsonPath("$.cartelle[2].nome").value("Studio"))
                .andExpect(jsonPath("$.cartelle[2].numeroNote").value(0))
                .andExpect(jsonPath("$.count").value(3));

        verify(cartellaService).getUserCartelle("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldVerifyCorrectJWTExtraction() throws Exception {
        // Given
        when(cartellaService.getUserCartelle("testuser")).thenReturn(testCartelle);

        // When
        mockMvc.perform(get("/api/cartelle")
                .header("Authorization", validToken))
                .andExpect(status().isOk());

        // Then
        verify(jwtUtil).extractTokenFromHeader(validToken);
        verify(jwtUtil).isTokenValid("valid-jwt-token");
        verify(jwtUtil).extractUsername("valid-jwt-token");
        verify(cartellaService).getUserCartelle("testuser");
    }
}