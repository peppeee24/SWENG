package tech.ipim.sweng.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tech.ipim.sweng.util.JwtUtil;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * Costruttore che riceve un'istanza di JwtUtil per la gestione dei token.
     *
     * @param jwtUtil utility per parsing e validazione dei JWT
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Filtra ogni richiesta HTTP per verificare la presenza e la validità di un token JWT.
     * Se il token è valido e l'utente non è ancora autenticato, imposta l'autenticazione nel SecurityContext.
     *
     * @param request      la richiesta HTTP in arrivo
     * @param response     la risposta HTTP in uscita
     * @param filterChain  la catena di filtri da proseguire
     * @throws ServletException in caso di errore nella catena dei filtri
     * @throws IOException      in caso di errore I/O
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("=== JWT FILTER DEBUG ===");
        System.out.println("Request: " + request.getMethod() + " " + request.getRequestURI());

        final String authorizationHeader = request.getHeader("Authorization");
        System.out.println("Auth Header: " + (authorizationHeader != null ? "Present (length=" + authorizationHeader.length() + ")" : "Missing"));

        String username = null;
        String jwt = null;

        // Estrae il token JWT dall'header Authorization
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            System.out.println("WT Token extracted (length=" + jwt.length() + ")");
            try {
                username = jwtUtil.extractUsername(jwt);
                System.out.println("Username extracted: " + username);
            } catch (Exception e) {
                System.err.println("Errore parsing JWT: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No Bearer token found");
        }

        // Se il token è valido e non c'è già un'autenticazione
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("Validating token for user: " + username);

            try {
                if (jwtUtil.isTokenValid(jwt)) {
                    // Crea un'autenticazione semplice senza caricare l'utente dal database
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    System.out.println("*********Utente autenticato via JWT: " + username);
                } else {
                    System.out.println("**********Token non valido per utente: " + username);
                }
            } catch (Exception e) {
                System.out.println("***********Errore validazione token: " + e.getMessage());
                e.printStackTrace();
            }
        } else if (username == null) {
            System.out.println(" Username è null - token non estratto");
        } else {
            System.out.println("Utente già autenticato: " + SecurityContextHolder.getContext().getAuthentication().getName());
        }

        System.out.println("=== END JWT FILTER DEBUG ===");
        filterChain.doFilter(request, response);
    }
}
