package sn.uasz.xamle_sante;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import sn.uasz.xamle_sante.sms.SmsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsServiceSimulationTest {

    @Test
    void parDefautLEnvoiEstSimule() {
        assertTrue(new SmsService().estSimule());
    }

    @Test
    void lePasseEnModeTwilioDesactiveLaSimulation() {
        SmsService sms = new SmsService();
        ReflectionTestUtils.setField(sms, "mode", "twilio");
        assertFalse(sms.estSimule());
    }

    @Test
    void enSimulationUnNumeroValideEstAccepteSansEnvoiReel() {
        SmsService.Resultat r = new SmsService().envoyer("77 123 45 67", "Message de test");
        assertTrue(r.envoye());
        assertNull(r.erreur());
    }

    @Test
    void unNumeroInvalideEstRefuse() {
        SmsService.Resultat r = new SmsService().envoyer("abc", "Message de test");
        assertFalse(r.envoye());
        assertEquals("Numéro de téléphone invalide", r.erreur());
    }
}
