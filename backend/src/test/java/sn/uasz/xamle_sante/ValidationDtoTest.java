package sn.uasz.xamle_sante;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sn.uasz.xamle_sante.annuaire.AnnuaireDtos.ProfilRequest;
import sn.uasz.xamle_sante.annuaire.AnnuaireDtos.RejetRequest;
import sn.uasz.xamle_sante.annuaire.FicheService.AvisRequest;
import sn.uasz.xamle_sante.annuaire.FicheService.HoraireRequest;
import sn.uasz.xamle_sante.epidemio.EpidemioService.AlerteRequest;
import sn.uasz.xamle_sante.epidemio.EpidemioService.SignalementRequest;
import sn.uasz.xamle_sante.urgence.UrgenceService.SosRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Règles de validation des données reçues par l'API (aucune base de données nécessaire). */
class ValidationDtoTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static boolean valide(Object o) {
        return validator.validate(o).isEmpty();
    }

    @Test
    void avisAccepteUneNoteDeUnACinq() {
        for (int n = 1; n <= 5; n++) assertTrue(valide(new AvisRequest(n, "Très bien")));
    }

    @Test
    void avisRefuseUneNoteHorsLimites() {
        assertFalse(valide(new AvisRequest(0, null)));
        assertFalse(valide(new AvisRequest(6, null)));
        assertFalse(valide(new AvisRequest(null, null)));
    }

    @Test
    void avisRefuseUnCommentaireTropLong() {
        assertFalse(valide(new AvisRequest(4, "x".repeat(501))));
        assertTrue(valide(new AvisRequest(4, "x".repeat(500))));
    }

    @Test
    void signalementAccepteUnSymptomeConnu() {
        for (String s : List.of("FIEVRE", "DIARRHEE", "TOUX", "VOMISSEMENTS", "ERUPTION", "AUTRE"))
            assertTrue(valide(new SignalementRequest(1, s, null)), s);
    }

    @Test
    void signalementRefuseUnSymptomeInconnuOuUnQuartierAbsent() {
        assertFalse(valide(new SignalementRequest(1, "GRIPPE", null)));
        assertFalse(valide(new SignalementRequest(null, "FIEVRE", null)));
        assertFalse(valide(new SignalementRequest(1, "", null)));
    }

    @Test
    void alerteExigeUnNiveauValideUnTitreEtUnMessage() {
        assertTrue(valide(new AlerteRequest(1, "VIGILANCE", "Titre", "Message")));
        assertFalse(valide(new AlerteRequest(1, "ROUGE", "Titre", "Message")));
        assertFalse(valide(new AlerteRequest(1, "INFO", " ", "Message")));
        assertFalse(valide(new AlerteRequest(1, "INFO", "Titre", "")));
    }

    @Test
    void sosExigeUnePositionEtUnNumero() {
        assertTrue(valide(new SosRequest(12.56, -16.27, "771234567")));
        assertFalse(valide(new SosRequest(null, -16.27, "771234567")));
        assertFalse(valide(new SosRequest(12.56, null, "771234567")));
        assertFalse(valide(new SosRequest(12.56, -16.27, " ")));
    }

    @Test
    void profilExigeUneSpecialite() {
        assertTrue(valide(new ProfilRequest(1, null, null, null, null, null, null, null, false)));
        assertFalse(valide(new ProfilRequest(null, null, null, null, null, null, null, null, false)));
    }

    @Test
    void rejetExigeUnMotif() {
        assertTrue(valide(new RejetRequest("Diplôme illisible")));
        assertFalse(valide(new RejetRequest("")));
    }

    @Test
    void horaireExigeUnJourEtDesHeures() {
        assertTrue(valide(new HoraireRequest(1, "08:00", "12:00")));
        assertFalse(valide(new HoraireRequest(null, "08:00", "12:00")));
        assertFalse(valide(new HoraireRequest(1, "", "12:00")));
    }
}
