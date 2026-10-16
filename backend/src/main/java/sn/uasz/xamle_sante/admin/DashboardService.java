package sn.uasz.xamle_sante.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import sn.uasz.xamle_sante.epidemio.EpidemioService;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tableau de bord santé publique : toutes les données viennent de la base (aucune donnée simulée). */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcClient jdbc;
    private final EpidemioService epidemio;

    public Map<String, Object> synthese(int jours) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("periodeJours", jours);
        m.put("professionnelsParStatut", jdbc.sql(
                "SELECT statut_validation AS cle, COUNT(*) AS nb FROM professionnel GROUP BY 1 ORDER BY 1")
                .query().listOfRows());
        m.put("signalementsParStatut", jdbc.sql(
                "SELECT statut AS cle, COUNT(*) AS nb FROM signalement GROUP BY 1 ORDER BY 1")
                .query().listOfRows());
        m.put("signalementsParSymptome", jdbc.sql("""
                SELECT symptome AS cle, COUNT(*) AS nb FROM signalement
                WHERE statut = 'VALIDE' AND cree_le > LOCALTIMESTAMP - (:jours * INTERVAL '1 day')
                GROUP BY 1 ORDER BY 2 DESC""").param("jours", jours).query().listOfRows());
        m.put("zonesARisque", epidemio.zonesARisque(jours));
        m.put("sosTotal", jdbc.sql("SELECT COUNT(*) FROM sos_log").query(Long.class).single());
        m.put("sosSmsEnvoyes", jdbc.sql("SELECT COUNT(*) FROM sos_log WHERE sms_envoye").query(Long.class).single());
        m.put("rappelsParStatut", jdbc.sql(
                "SELECT statut AS cle, COUNT(*) AS nb FROM rappel_sms GROUP BY 1 ORDER BY 1").query().listOfRows());
        m.put("rendezVousParStatut", jdbc.sql(
                "SELECT statut AS cle, COUNT(*) AS nb FROM rendez_vous GROUP BY 1 ORDER BY 1").query().listOfRows());
        m.put("paiementsParStatut", jdbc.sql(
                "SELECT statut AS cle, COUNT(*) AS nb FROM paiement GROUP BY 1 ORDER BY 1").query().listOfRows());
        return m;
    }
}
