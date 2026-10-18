package sn.uasz.xamle_sante.urgence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import sn.uasz.xamle_sante.sms.SmsService;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UrgenceService {

    private final JdbcClient jdbc;
    private final SmsService sms;

    public record UrgenceDto(Long etablissementId, String nom, String type, String adresse, String telephone,
                             String serviceType, Boolean disponible24h, Double latitude, Double longitude,
                             Double distanceM) {}

    public record SosRequest(@NotNull Double latitude, @NotNull Double longitude,
                             @NotBlank String telephoneDestinataire) {}

    public record SosResponse(boolean smsEnvoye, String message, String erreur, List<UrgenceDto> urgences) {}

    /** Un seul résultat par établissement (les services d'urgence sont regroupés). */
    /** Un établissement n'apparaît qu'une fois, avec la liste de ses services d'urgence. */
    public List<UrgenceDto> plusProches(double lat, double lon, int limite) {
        int n = Math.max(1, Math.min(limite, 20));
        return jdbc.sql("""
                SELECT e.id AS etablissement_id, e.nom, e.type, e.adresse,
                       COALESCE(MAX(su.telephone), e.telephone) AS telephone,
                       string_agg(su.type, ', ' ORDER BY su.type) AS service_type,
                       bool_or(su.disponible_24h) AS disponible24h,
                       ST_Y(e.position::geometry) AS latitude,
                       ST_X(e.position::geometry) AS longitude,
                       ST_Distance(e.position, ST_SetSRID(ST_MakePoint(CAST(:lon AS double precision),
                           CAST(:lat AS double precision)), 4326)::geography) AS distance_m
                FROM service_urgence su
                JOIN etablissement e ON e.id = su.etablissement_id
                GROUP BY e.id
                ORDER BY distance_m
                LIMIT %d""".formatted(n))
                .param("lat", lat).param("lon", lon)
                .query(UrgenceDto.class).list();
    }

    /** Bouton SOS : journalise l'appel, cherche les urgences proches, envoie la position par SMS. */
    public SosResponse declencher(SosRequest r) {
        long logId = jdbc.sql("INSERT INTO sos_log (position, telephone_dest) "
                        + "VALUES (ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :tel) RETURNING id")
                .param("lon", r.longitude()).param("lat", r.latitude()).param("tel", r.telephoneDestinataire())
                .query(Long.class).single();

        List<UrgenceDto> urgences = plusProches(r.latitude(), r.longitude(), 3);

        StringBuilder sb = new StringBuilder("URGENCE (Xamlé Santé). Position : https://www.google.com/maps?q=")
                .append(String.format(Locale.US, "%.6f,%.6f", r.latitude(), r.longitude()))
                .append(". Urgences proches :");
        for (UrgenceDto u : urgences) {
            sb.append(' ').append(u.nom());
            if (u.telephone() != null) sb.append(' ').append(u.telephone());
            if (u.distanceM() != null) sb.append(String.format(Locale.US, " (%.1f km);", u.distanceM() / 1000.0));
        }
        String message = sb.toString();

        SmsService.Resultat res = sms.envoyer(r.telephoneDestinataire(), message);
        jdbc.sql("UPDATE sos_log SET sms_envoye = :ok, erreur_sms = :err WHERE id = :id")
                .param("ok", res.envoye()).param("err", res.erreur()).param("id", logId).update();
        return new SosResponse(res.envoye(), message, res.erreur(), urgences);
    }
}
