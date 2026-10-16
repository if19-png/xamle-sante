package sn.uasz.xamle_sante.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.xamle_sante.Utilisateur.Role;
import sn.uasz.xamle_sante.Utilisateur.Utilisateur;
import sn.uasz.xamle_sante.Utilisateur.UtilisateurRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Données de démonstration (comptes, spécialistes, signalements, campagnes).
 * Actif seulement si app.seed-demo = true (variable SEED_DEMO). À DÉSACTIVER en production.
 * Mot de passe de tous les comptes de démo : Xamle2026!
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements ApplicationRunner {

    private final UtilisateurRepository users;
    private final PasswordEncoder encoder;
    private final JdbcClient jdbc;

    @Value("${app.seed-demo:false}")
    private boolean seed;

    private static final String MDP = "Xamle2026!";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seed || users.existsByEmail("admin@xamle.sn")) {
            return;
        }
        Utilisateur admin = creer("admin@xamle.sn", "Diop", "Admin", Role.ADMIN, "770000001");
        creer("moderateur@xamle.sn", "Ba", "Modou", Role.MODERATEUR, "770000003");
        creer("patient@xamle.sn", "Diallo", "Awa", Role.PATIENT, "770000002");

        creerPro("pro1@xamle.sn", "Sané", "Fatou", "770000011", "Médecine générale",
                "Centre de santé de Boucotte (démo)", 12.5806, -16.2602,
                List.of("Français", "Wolof", "Diola"), true, "VALIDE",
                "Médecin généraliste, consultations et suivi de grossesse.");
        creerPro("pro2@xamle.sn", "Diatta", "Mamadou", "770000012", "Pédiatrie",
                "Hôpital Régional de Ziguinchor (Hôpital de la Paix)", 12.5637, -16.2689,
                List.of("Français", "Diola"), true, "VALIDE", "Pédiatre, suivi des nourrissons et vaccination.");
        creerPro("pro3@xamle.sn", "Badji", "Awa", "770000013", "Gynécologie-obstétrique",
                "Clinique Casamance Santé (démo)", 12.5702, -16.2741,
                List.of("Français", "Mandingue"), false, "VALIDE", "Gynécologue-obstétricienne.");
        creerPro("pro4@xamle.sn", "Sagna", "Ibrahima", "770000014", "Cardiologie",
                "Clinique du Fleuve (démo)", 12.5748, -16.2689,
                List.of("Français", "Wolof", "Anglais"), true, "EN_ATTENTE",
                "Cardiologue. Profil en attente de validation (pour la démonstration du workflow).");

        // Signalements validés : Boucotte (5) et Lyndiane (4) dépassent le seuil d'anonymat (3) ;
        // Kandé (2) reste masqué. Deux signalements restent à modérer.
        signalements("Boucotte", "FIEVRE", 5, "VALIDE");
        signalements("Lyndiane", "DIARRHEE", 4, "VALIDE");
        signalements("Kandé", "TOUX", 2, "VALIDE");
        signalements("Escale", "FIEVRE", 2, "EN_ATTENTE");

        // Campagne dans moins de 24 h : le rappel SMS part dès l'abonnement (au prochain passage de la tâche).
        campagne(admin.getId(), "VACCINATION", "Campagne de vaccination (démo)", "Centre de santé de Boucotte",
                12.5806, -16.2602, LocalDateTime.now().plusHours(20));
        campagne(admin.getId(), "DEPISTAGE", "Journée de dépistage (démo)", "Place de l'Escale",
                12.5712, -16.2748, LocalDateTime.now().plusDays(7));

        jdbc.sql("INSERT INTO alerte (quartier_id, niveau, titre, message, cree_par) "
                        + "SELECT id, 'VIGILANCE', 'Hausse des cas de fièvre', "
                        + "'Restez vigilants et consultez en cas de symptômes persistants.', :u "
                        + "FROM quartier WHERE nom = 'Boucotte'")
                .param("u", admin.getId()).update();

        log.info("Données de démonstration créées (mot de passe des comptes : {})", MDP);
    }

    private Utilisateur creer(String email, String nom, String prenom, Role role, String tel) {
        return users.save(Utilisateur.builder().email(email).motDePasse(encoder.encode(MDP))
                .nom(nom).prenom(prenom).telephone(tel).role(role).build());
    }

    private void creerPro(String email, String nom, String prenom, String tel, String specialite,
                          String etablissement, double lat, double lon, List<String> langues,
                          boolean video, String statut, String bio) {
        Utilisateur u = creer(email, nom, prenom, Role.PROFESSIONNEL, tel);
        long id = jdbc.sql("""
                INSERT INTO professionnel (utilisateur_id, specialite_id, etablissement_id, bio, position,
                                           consultation_video, statut_validation, valide_le)
                VALUES (:uid, (SELECT id FROM specialite WHERE nom = :sp),
                        (SELECT id FROM etablissement WHERE nom = :et), :bio,
                        ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :video, :statut, :valideLe)
                RETURNING id""")
                .param("uid", u.getId()).param("sp", specialite).param("et", etablissement)
                .param("bio", bio).param("lon", lon).param("lat", lat).param("video", video)
                .param("statut", statut).param("valideLe", "VALIDE".equals(statut) ? LocalDateTime.now() : null)
                .query(Long.class).single();
        for (String l : langues) {
            jdbc.sql("INSERT INTO professionnel_langue (professionnel_id, langue_id) "
                            + "SELECT :id, id FROM langue WHERE nom = :n")
                    .param("id", id).param("n", l).update();
        }
        for (int jour = 1; jour <= 5; jour++) {
            jdbc.sql("INSERT INTO horaire (professionnel_id, jour_semaine, heure_debut, heure_fin) "
                            + "VALUES (:id, :j, :d, :f)")
                    .param("id", id).param("j", jour).param("d", LocalTime.of(8, 0)).param("f", LocalTime.of(17, 0))
                    .update();
        }
    }

    private void signalements(String quartier, String symptome, int nombre, String statut) {
        for (int i = 0; i < nombre; i++) {
            jdbc.sql("INSERT INTO signalement (quartier_id, symptome, statut, cree_le) "
                            + "SELECT id, :s, :st, :d FROM quartier WHERE nom = :q")
                    .param("s", symptome).param("st", statut).param("q", quartier)
                    .param("d", LocalDateTime.now().minusDays(i % 6)).update();
        }
    }

    private void campagne(long adminId, String type, String titre, String lieu, double lat, double lon,
                          LocalDateTime debut) {
        jdbc.sql("INSERT INTO campagne (type, titre, description, lieu, position, date_debut, cree_par) "
                        + "VALUES (:t, :ti, :de, :li, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :d, :u)")
                .param("t", type).param("ti", titre).param("de", "Campagne de démonstration.")
                .param("li", lieu).param("lon", lon).param("lat", lat).param("d", debut).param("u", adminId)
                .update();
    }
}
