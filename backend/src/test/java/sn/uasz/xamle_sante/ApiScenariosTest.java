package sn.uasz.xamle_sante;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scénarios métier complets : rendez-vous et paiement simulé, campagnes, validation des profils,
 * modération, alertes, comptes créés par l'administrateur.
 * Même configuration que ApiIntegrationTest (donc même contexte Spring) : ne tourne que si DB_PASSWORD est défini.
 */
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.jwt.secret=cle-de-test-xamle-sante-uasz-0123456789-abcdef",
        "app.seed-demo=true",
        "app.sms.mode=simulation",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=0",
        "spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_TEST_NAME:xamle_sante_test}"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiScenariosTest {

    private static final String MDP = "Xamle2026!";
    private static final String RECHERCHE = "/api/public/professionnels?lat=12.5681&lon=-16.2719";

    @Value("${local.server.port}")
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> appel(String methode, String chemin, String token, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                .header("Content-Type", "application/json");
        if (token != null) b.header("Authorization", "Bearer " + token);
        b.method(methode, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String chemin) throws Exception {
        return appel("GET", chemin, null, null);
    }

    private String connexion(String email, String mdp) throws Exception {
        HttpResponse<String> r = appel("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"motDePasse\":\"" + mdp + "\"}");
        assertEquals(200, r.statusCode(), "connexion de " + email);
        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(r.body());
        assertTrue(m.find(), "jeton absent");
        return m.group(1);
    }

    private static List<String> trouverTous(String texte, String regex) {
        Matcher m = Pattern.compile(regex).matcher(texte);
        List<String> out = new ArrayList<>();
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static void assertSucces(HttpResponse<String> r, String quoi) {
        assertTrue(r.statusCode() >= 200 && r.statusCode() < 300, quoi + " : code " + r.statusCode() + " " + r.body());
    }

    private static String dans(int jours) {
        return LocalDateTime.now().plusDays(jours).withNano(0).toString();
    }

    private String idSpecialiste(boolean video) throws Exception {
        String liste = get(RECHERCHE).body();
        Matcher m = Pattern.compile("\\{[^{}]*\"id\":(\\d+),[^{}]*\"consultationVideo\":" + video).matcher(liste);
        Assumptions.assumeTrue(m.find(), "aucun spécialiste correspondant dans les données de démonstration");
        return m.group(1);
    }

    private Set<String> idsEnAttente(String admin) throws Exception {
        HttpResponse<String> r = appel("GET", "/api/admin/professionnels/en-attente", admin, null);
        assertEquals(200, r.statusCode());
        return new HashSet<>(trouverTous(r.body(), "\"id\":(\\d+)"));
    }

    /** Crée un compte professionnel, remplit son profil et le soumet à validation. */
    private void nouveauProSoumis() throws Exception {
        String email = "pro" + System.nanoTime() + "@xamle.test";
        assertSucces(appel("POST", "/api/auth/register", null,
                "{\"email\":\"" + email + "\",\"motDePasse\":\"" + MDP + "\",\"nom\":\"Diop\","
                        + "\"prenom\":\"Test\",\"telephone\":\"771234567\",\"role\":\"PROFESSIONNEL\"}"), "inscription pro");
        String token = connexion(email, MDP);
        assertSucces(appel("PUT", "/api/pro/mon-profil", token,
                "{\"specialiteId\":1,\"bio\":\"Profil de test\",\"telephonePro\":\"771234567\","
                        + "\"latitude\":12.5702,\"longitude\":-16.2741,\"consultationVideo\":false}"), "profil");
        assertEquals(200, appel("GET", "/api/pro/mon-profil", token, null).statusCode());
        assertSucces(appel("POST", "/api/pro/mon-profil/soumettre", token, null), "soumission");
    }

    // ---------- Rendez-vous ----------

    @Test @Order(1)
    void unRendezVousEnVisioSePayeEtDonneUnLienDeVisioconference() throws Exception {
        String patient = connexion("patient@xamle.sn", MDP);
        String pro = idSpecialiste(true);

        HttpResponse<String> r = appel("POST", "/api/patient/rendez-vous", patient,
                "{\"professionnelId\":" + pro + ",\"dateHeure\":\"" + dans(3) + "\",\"type\":\"VIDEO\",\"motif\":\"Contrôle\"}");
        assertSucces(r, "rendez-vous vidéo");
        assertTrue(r.body().contains("\"montant\":1000"), "frais de dossier de 1 000 FCFA : " + r.body());
        Matcher p = Pattern.compile("\"paiementId\":(\\d+)").matcher(r.body());
        assertTrue(p.find(), "paiement absent");

        HttpResponse<String> paye = appel("POST", "/api/patient/paiements/" + p.group(1) + "/payer", patient, null);
        assertSucces(paye, "paiement");
        assertTrue(paye.body().contains("CONFIRME"));
        assertTrue(paye.body().contains("meet.jit.si"));

        assertEquals(404, appel("POST", "/api/patient/paiements/" + p.group(1) + "/payer", patient, null).statusCode(),
                "un paiement ne se règle qu'une fois");
        assertEquals(200, appel("GET", "/api/patient/rendez-vous", patient, null).statusCode());
    }

    @Test @Order(2)
    void unRendezVousPresentielEstConfirmeParLeSpecialisteConcerne() throws Exception {
        String patient = connexion("patient@xamle.sn", MDP);
        String pro = idSpecialiste(false);
        HttpResponse<String> r = appel("POST", "/api/patient/rendez-vous", patient,
                "{\"professionnelId\":" + pro + ",\"dateHeure\":\"" + dans(4) + "\",\"type\":\"PRESENTIEL\"}");
        assertSucces(r, "rendez-vous présentiel");
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(r.body());
        assertTrue(m.find());

        int confirmations = 0;
        for (String email : List.of("pro1@xamle.sn", "pro2@xamle.sn", "pro3@xamle.sn")) {
            String token = connexion(email, MDP);
            assertEquals(200, appel("GET", "/api/pro/rendez-vous", token, null).statusCode());
            int code = appel("POST", "/api/pro/rendez-vous/" + m.group(1) + "/confirmer", token, null).statusCode();
            if (code >= 200 && code < 300) confirmations++;
        }
        assertEquals(1, confirmations, "seul le spécialiste concerné peut confirmer");
    }

    @Test @Order(3)
    void lesRendezVousInvalidesSontRefuses() throws Exception {
        String patient = connexion("patient@xamle.sn", MDP);
        String pro = idSpecialiste(false);
        assertEquals(400, appel("POST", "/api/patient/rendez-vous", patient,
                "{\"professionnelId\":" + pro + ",\"dateHeure\":\"2020-01-01T10:00:00\",\"type\":\"PRESENTIEL\"}").statusCode(),
                "date passée");
        assertEquals(404, appel("POST", "/api/patient/rendez-vous", patient,
                "{\"professionnelId\":999999,\"dateHeure\":\"" + dans(3) + "\",\"type\":\"PRESENTIEL\"}").statusCode(),
                "spécialiste inconnu");
        assertEquals(400, appel("POST", "/api/patient/rendez-vous", patient,
                "{\"professionnelId\":" + pro + ",\"dateHeure\":\"" + dans(3) + "\",\"type\":\"TELEPHONE\"}").statusCode(),
                "type inconnu");
        assertEquals(400, appel("POST", "/api/patient/rendez-vous", patient,
                "{\"professionnelId\":" + pro + ",\"dateHeure\":\"" + dans(3) + "\",\"type\":\"VIDEO\"}").statusCode(),
                "visio refusée si le spécialiste ne la propose pas");
    }

    // ---------- Campagnes et alertes ----------

    @Test @Order(10)
    void lAdminCreeUneCampagneEtLePatientSAbonne() throws Exception {
        String admin = connexion("admin@xamle.sn", MDP);
        String patient = connexion("patient@xamle.sn", MDP);

        HttpResponse<String> c = appel("POST", "/api/admin/campagnes", admin,
                "{\"type\":\"VACCINATION\",\"titre\":\"Vaccination test auto\",\"lieu\":\"Escale\","
                        + "\"latitude\":12.5712,\"longitude\":-16.2748,\"dateDebut\":\"" + dans(2) + "\"}");
        assertSucces(c, "création de campagne");
        Matcher id = Pattern.compile("\"id\":(\\d+)").matcher(c.body());
        assertTrue(id.find());

        assertSucces(appel("POST", "/api/campagnes/" + id.group(1) + "/abonnement", patient, null), "abonnement");
        assertTrue(appel("POST", "/api/campagnes/" + id.group(1) + "/abonnement", patient, null).statusCode() < 500,
                "un second abonnement ne doit pas faire planter le serveur");
        assertSucces(appel("POST", "/api/admin/rappels/declencher", admin, null), "déclenchement des rappels");
        assertTrue(get("/api/public/campagnes").body().contains("Vaccination test auto"));
    }

    @Test @Order(11)
    void lesCampagnesInvalidesOuReserveesSontRefusees() throws Exception {
        String admin = connexion("admin@xamle.sn", MDP);
        String patient = connexion("patient@xamle.sn", MDP);
        assertEquals(400, appel("POST", "/api/admin/campagnes", admin,
                "{\"type\":\"FETE\",\"titre\":\"X\",\"dateDebut\":\"" + dans(2) + "\"}").statusCode());
        assertEquals(403, appel("POST", "/api/admin/campagnes", patient,
                "{\"type\":\"VACCINATION\",\"titre\":\"X\",\"dateDebut\":\"" + dans(2) + "\"}").statusCode());
    }

    @Test @Order(12)
    void lAdminPublieUneAlerteVisibleParTous() throws Exception {
        String admin = connexion("admin@xamle.sn", MDP);
        assertSucces(appel("POST", "/api/admin/alertes", admin,
                "{\"quartierId\":1,\"niveau\":\"INFO\",\"titre\":\"Alerte test auto\",\"message\":\"Message de test\"}"),
                "création d'alerte");
        assertTrue(get("/api/public/alertes").body().contains("Alerte test auto"));
    }

    // ---------- Modération et comptes ----------

    @Test @Order(20)
    void leModerateurConsulteEtRejetteUnSignalementEnAttente() throws Exception {
        String moderateur = connexion("moderateur@xamle.sn", MDP);
        HttpResponse<String> liste = appel("GET", "/api/moderation/signalements", moderateur, null);
        assertEquals(200, liste.statusCode());
        List<String> ids = trouverTous(liste.body(), "\"id\":(\\d+)");
        Assumptions.assumeFalse(ids.isEmpty(), "aucun signalement en attente");
        assertSucces(appel("POST", "/api/moderation/signalements/" + ids.get(0) + "/rejeter", moderateur, null), "rejet");
        assertEquals(200, appel("GET", "/api/moderation/signalements?statut=REJETE", moderateur, null).statusCode());
    }

    @Test @Order(21)
    void laModerationEstReserveeAuPersonnelAutorise() throws Exception {
        String patient = connexion("patient@xamle.sn", MDP);
        assertEquals(403, appel("GET", "/api/moderation/signalements", patient, null).statusCode());
    }

    @Test @Order(22)
    void lAdminCreeUnCompteEtLePatientNePeutPas() throws Exception {
        String admin = connexion("admin@xamle.sn", MDP);
        String patient = connexion("patient@xamle.sn", MDP);
        String body = "{\"email\":\"mod" + System.nanoTime() + "@xamle.test\",\"motDePasse\":\"" + MDP
                + "\",\"nom\":\"Sow\",\"prenom\":\"Test\",\"telephone\":\"771234567\",\"role\":\"MODERATEUR\"}";
        assertSucces(appel("POST", "/api/admin/utilisateurs", admin, body), "création de compte");
        assertEquals(403, appel("POST", "/api/admin/utilisateurs", patient, body).statusCode());
    }

    // ---------- Validation des profils professionnels ----------

    @Test @Order(30)
    void unProfilSoumisEstValidePuisPublic() throws Exception {
        String admin = connexion("admin@xamle.sn", MDP);
        Set<String> avant = idsEnAttente(admin);
        nouveauProSoumis();
        Set<String> nouveaux = idsEnAttente(admin);
        nouveaux.removeAll(avant);
        assertFalse(nouveaux.isEmpty(), "le profil soumis doit apparaître dans la file de validation");
        String id = nouveaux.iterator().next();

        assertEquals(404, get("/api/public/professionnels/" + id).statusCode(), "invisible avant validation");
        assertSucces(appel("POST", "/api/admin/professionnels/" + id + "/valider", admin, null), "validation");
        assertEquals(200, get("/api/public/professionnels/" + id).statusCode(), "visible après validation");
    }

    @Test @Order(31)
    void unProfilRejeteResteInvisibleEtNePeutPasRecevoirDeRendezVous() throws Exception {
        String admin = connexion("admin@xamle.sn", MDP);
        String patient = connexion("patient@xamle.sn", MDP);
        Set<String> avant = idsEnAttente(admin);
        nouveauProSoumis();
        Set<String> nouveaux = idsEnAttente(admin);
        nouveaux.removeAll(avant);
        assertFalse(nouveaux.isEmpty());
        String id = nouveaux.iterator().next();

        assertSucces(appel("POST", "/api/admin/professionnels/" + id + "/rejeter", admin,
                "{\"motif\":\"Dossier incomplet\"}"), "rejet");
        assertEquals(404, appel("POST", "/api/patient/rendez-vous", patient,
                "{\"professionnelId\":" + id + ",\"dateHeure\":\"" + dans(3) + "\",\"type\":\"PRESENTIEL\"}").statusCode());
    }

    // ---------- Recherche avancée ----------

    @Test @Order(40)
    void laRechercheAccepteTousLesFiltres() throws Exception {
        assertEquals(200, get(RECHERCHE + "&specialiteId=1&langueId=1&rayonM=50000&disponible=true&q=a").statusCode());
        assertEquals(200, get(RECHERCHE + "&rayonM=1").statusCode());
        assertEquals(200, get("/api/public/professionnels").statusCode());
        assertEquals(200, get("/api/public/langues").statusCode());
        assertEquals(200, get("/api/public/quartiers").statusCode());
        assertEquals(200, get("/api/public/etablissements?lat=12.5681&lon=-16.2719&type=HOPITAL").statusCode());
        assertEquals(200, get("/api/public/etablissements").statusCode());
        String id = idSpecialiste(false);
        assertEquals(200, get("/api/public/professionnels/" + id).statusCode());
        assertEquals(404, get("/api/public/professionnels/999999").statusCode());
    }

    @Test @Order(41)
    void leTableauDeBordEtLExportAcceptentUnePeriode() throws Exception {
        String admin = connexion("admin@xamle.sn", MDP);
        assertEquals(200, appel("GET", "/api/admin/dashboard?jours=30", admin, null).statusCode());
        HttpResponse<String> csv = appel("GET", "/api/admin/export/signalements.csv?jours=30", admin, null);
        assertEquals(200, csv.statusCode());
        assertTrue(csv.body().toLowerCase().contains("quartier"));
    }
}
