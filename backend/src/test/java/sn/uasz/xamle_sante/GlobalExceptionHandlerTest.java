package sn.uasz.xamle_sante;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle_sante.common.GlobalExceptionHandler;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void renvoieLeStatutEtLeMessageDeLException() {
        ResponseEntity<Map<String, String>> r =
                handler.statut(new ResponseStatusException(HttpStatus.NOT_FOUND, "Spécialiste introuvable"));
        assertEquals(404, r.getStatusCode().value());
        assertEquals("Spécialiste introuvable", r.getBody().get("message"));
    }

    @Test
    void utiliseUnMessageParDefautSansMotif() {
        ResponseEntity<Map<String, String>> r = handler.statut(new ResponseStatusException(HttpStatus.FORBIDDEN));
        assertEquals(403, r.getStatusCode().value());
        assertEquals("Erreur", r.getBody().get("message"));
    }

    @Test
    void traduitUneViolationDIntegriteEnConflit() {
        ResponseEntity<Map<String, String>> r = handler.integrite(new DataIntegrityViolationException("doublon"));
        assertEquals(409, r.getStatusCode().value());
        assertEquals("Donnée invalide : référence inexistante ou doublon", r.getBody().get("message"));
    }
}
