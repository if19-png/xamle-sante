package sn.uasz.xamle_sante;

import org.junit.jupiter.api.Test;
import sn.uasz.xamle_sante.sms.SmsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsServiceTest {

    @Test
    void normaliseUnNumeroSenegalais() {
        assertEquals("+221771234567", SmsService.normaliser("77 123 45 67"));
        assertEquals("+221771234567", SmsService.normaliser("771234567"));
    }

    @Test
    void conserveUnNumeroInternational() {
        assertEquals("+33612345678", SmsService.normaliser("+33612345678"));
        assertEquals("+221771234567", SmsService.normaliser("00221771234567"));
    }

    @Test
    void refuseUnNumeroInvalide() {
        assertNull(SmsService.normaliser("abc"));
        assertNull(SmsService.normaliser(null));
        assertNull(SmsService.normaliser("1234"));
    }

    @Test
    void modeSimulationNEnvoieRienMaisReussit() {
        SmsService.Resultat ok = new SmsService().envoyer("771234567", "test");
        assertTrue(ok.envoye());
        SmsService.Resultat ko = new SmsService().envoyer("pas-un-numero", "test");
        assertFalse(ko.envoye());
    }
}
