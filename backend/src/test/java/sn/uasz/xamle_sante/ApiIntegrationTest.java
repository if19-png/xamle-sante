package sn.uasz.xamle_sante;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests d'intégration : l'application démarre pour de vrai (serveur, sécurité, SQL, PostGIS) et on appelle l'API.
 *
 * Ils ne tournent que si la variable d'environnement DB_PASSWORD est définie, et utilisent une base DÉDIÉE
 * (variable DB_TEST_NAME, par défaut "xamle_sante_test"), jamais la base de développement ni celle du déploiement.
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
class ApiIntegrationTest {

    private static final String MDP = "Xamle2026!";

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
        List<String> out = new java.util.ArrayList<>();
        while (m.find()) out.add(m.group(1));
        return out;
    }

    // ---------- Lecture publique ----------

    @Test @Order(1)
    void laDocumentationOpenApiEstPubliee() throws Exception {
        HttpResponse<String> r = get("/v3/api-docs");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"openapi\""));
        assertTrue(r.body().contains("/api/public/sos"));
    }

    @Test @Order(2)
    void lesSpecialitesSontPubliques() throws Exception {
        HttpResponse<String> r = get("/api/public/specialites");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("Dermatologie"));
    }

    @Test @Order(3)
    void laRechercheRenvoieDesSpecialistesValidesAvecLeurDistance() throws Exception {
        HttpResponse<String> r = get("/api/public/professionnels?lat=12.5681&lon=-16.2719");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().startsWith("["));
        assertTrue(r.body().contains("distanceM"));
        assertFalse(r.body().contains("EN_ATTENTE"), "un profil non validé ne doit jamais être public");
    }

    @Test @Order(4)
    void lesUrgencesProchesSontSansDoublonEtTrieesParDistance() throws Exception {
        HttpResponse<String> r = get("/api/public/urgences?lat=12.5681&lon=-16.2719");
        assertEquals(200, r.statusCode());
        List<String> ids = trouverTous(r.body(), "\"etablissementId\":(\\d+)");
        assertFalse(ids.isEmpty());
        assertEquals(ids.size(), new HashSet<>(ids).size(), "un établissement ne doit apparaître qu'une fois");
        List<Double> distances = trouverTous(r.body(), "\"distanceM\":([0-9.eE+-]+)").stream().map(Double::valueOf).toList();
        for (int i = 1; i < distances.size(); i++) assertTrue(distances.get(i - 1) <= distances.get(i));
    }

    @Test @Order(5)
    void lesCampagnesEtAlertesSontPubliques() throws Exception {
        assertEquals(200, get("/api/public/campagnes").statusCode());
        assertEquals(200, get("/api/public/alertes").statusCode());
    }

    // ---------- SOS ----------

    @Test @Order(10)
    void leSosEnregistreLAppelEtSimuleLeSms() throws Exception {
        HttpResponse<String> r = appel("POST", "/api/public/sos", null,
                "{\"latitude\":12.5681,\"longitude\":-16.2719,\"telephoneDestinataire\":\"771234567\"}");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"smsEnvoye\":true"));
        assertTrue(r.body().contains("\"simule\":true"));
        assertTrue(r.body().contains("google.com/maps"));
    }

    @Test @Order(11)
    void leSosSignaleUnNumeroInvalideSansPlanter() throws Exception {
        HttpResponse<String> r = appel("POST", "/api/public/sos", null,
                "{\"latitude\":12.5681,\"longitude\":-16.2719,\"telephoneDestinataire\":\"abc\"}");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"smsEnvoye\":false"));
    }

    @Test @Order(12)
    void leSosRefuseUneRequeteIncomplete() throws Exception {
        assertEquals(400, appel("POST", "/api/public/sos", null, "{\"latitude\":12.5}").statusCode());
    }

    // ---------- Épidémiologie ----------

    @Test @Order(20)
    void unSignalementAnonymeEstAccepte() throws Exception {
        HttpResponse<String> r = appel("POST", "/api/public/signalements", null,
                "{\"quartierId\":1,\"symptome\":\"FIEVRE\",\"description\":\"test\"}");
        assertEquals(201, r.statusCode());
        assertFalse(r.body().contains("utilisateur"), "aucune identité ne doit apparaître");
    }

    @Test @Order(21)
    void unSymptomeInconnuEstRefuse() throws Exception {
        assertEquals(400, appel("POST", "/api/public/signalements", null,
                "{\"quartierId\":1,\"symptome\":\"GRIPPE\"}").statusCode());
    }

    @Test @Order(22)
    void laCarteDeChaleurAppliqueLeSeuilDAnonymat() throws Exception {
        HttpResponse<String> r = get("/api/public/heatmap");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("Boucotte"), "5 signalements validés : visible");
        assertTrue(r.body().contains("Lyndiane"), "4 signalements validés : visible");
        assertFalse(r.body().contains("Kand"), "2 signalements seulement : masqué pour protéger l'anonymat");
    }

    // ---------- Authentification et rôles ----------

    @Test @Order(30)
    void uneRouteProtegeeRefuseLesVisiteursSansJeton() throws Exception {
        assertEquals(401, get("/api/me").statusCode());
    }

    @Test @Order(31)
    void unPatientVoitSonProfilMaisPasLEspaceAdmin() throws Exception {
        String patient = connexion("patient@xamle.sn", MDP);
        assertEquals(200, appel("GET", "/api/me", patient, null).statusCode());
        assertEquals(403, appel("GET", "/api/admin/dashboard", patient, null).statusCode());
        assertEquals(403, appel("GET", "/api/pro/horaires", patient, null).statusCode());
    }

    @Test @Order(32)
    void lAdminAccedeAuTableauDeBordEtALExportCsv() throws Exception {
        String admin = connexion("admin@xamle.sn", MDP);
        assertEquals(200, appel("GET", "/api/admin/dashboard", admin, null).statusCode());
        assertEquals(200, appel("GET", "/api/admin/export/signalements.csv", admin, null).statusCode());
    }

    @Test @Order(33)
    void uneInscriptionCreeUnCompteQuiPeutSeConnecter() throws Exception {
        String email = "test" + System.currentTimeMillis() + "@xamle.test";
        HttpResponse<String> r = appel("POST", "/api/auth/register", null,
                "{\"email\":\"" + email + "\",\"motDePasse\":\"" + MDP + "\",\"nom\":\"Test\","
                        + "\"prenom\":\"Auto\",\"telephone\":\"771234567\",\"role\":\"PATIENT\"}");
        assertTrue(r.statusCode() == 200 || r.statusCode() == 201, "inscription : " + r.statusCode());
        assertNotNull(connexion(email, MDP));
    }

    @Test @Order(34)
    void unMauvaisMotDePasseEstRefuse() throws Exception {
        int code = appel("POST", "/api/auth/login", null,
                "{\"email\":\"patient@xamle.sn\",\"motDePasse\":\"mauvais\"}").statusCode();
        assertTrue(code >= 400 && code < 500, "code reçu : " + code);
    }

    @Test @Order(35)
    void onNePeutPasSInscrireCommeAdministrateur() throws Exception {
        assertEquals(403, appel("POST", "/api/auth/register", null,
                "{\"email\":\"pirate" + System.currentTimeMillis() + "@xamle.test\",\"motDePasse\":\"" + MDP
                        + "\",\"nom\":\"X\",\"prenom\":\"Y\",\"telephone\":\"771234567\",\"role\":\"ADMIN\"}").statusCode());
    }

    // ---------- Avis et horaires ----------

    @Test @Order(40)
    void lePatientDonneUnAvisEtLaMoyenneSeMetAJour() throws Exception {
        String patient = connexion("patient@xamle.sn", MDP);
        String liste = get("/api/public/professionnels?lat=12.5681&lon=-16.2719").body();
        List<String> ids = trouverTous(liste, "\"id\":(\\d+)");
        assertFalse(ids.isEmpty(), "aucun spécialiste validé dans les données de démonstration");
        String pro = ids.get(0);

        assertEquals(200, appel("POST", "/api/patient/professionnels/" + pro + "/avis", patient,
                "{\"note\":4,\"commentaire\":\"Très bon accueil\"}").statusCode());
        String avis = get("/api/public/professionnels/" + pro + "/avis").body();
        assertTrue(avis.contains("\"total\":1"), avis);
        assertTrue(avis.contains("Très bon accueil"));

        assertEquals(400, appel("POST", "/api/patient/professionnels/" + pro + "/avis", patient,
                "{\"note\":9}").statusCode());
    }

    @Test @Order(41)
    void leProfessionnelGereSesHoraires() throws Exception {
        String pro = connexion("pro1@xamle.sn", MDP);
        HttpResponse<String> ok = appel("PUT", "/api/pro/horaires", pro,
                "[{\"jour\":1,\"debut\":\"08:00\",\"fin\":\"12:00\"},{\"jour\":3,\"debut\":\"14:00\",\"fin\":\"18:00\"}]");
        assertEquals(200, ok.statusCode());
        assertTrue(ok.body().contains("08:00"));
        assertTrue(appel("GET", "/api/pro/horaires", pro, null).body().contains("14:00"));

        assertEquals(400, appel("PUT", "/api/pro/horaires", pro,
                "[{\"jour\":1,\"debut\":\"15:00\",\"fin\":\"09:00\"}]").statusCode());
    }

    @Test @Order(42)
    void lesComplementsDesFichesSontPublics() throws Exception {
        HttpResponse<String> r = get("/api/public/professionnels/complements?ids=1,2,3");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().startsWith("{"));
    }
}
