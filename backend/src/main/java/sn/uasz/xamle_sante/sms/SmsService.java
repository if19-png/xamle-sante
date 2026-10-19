package sn.uasz.xamle_sante.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Envoi de SMS. Mode "simulation" (défaut) : le SMS est journalisé, pas envoyé.
 * Mode "twilio" : appel de l'API REST Twilio (variables TWILIO_*).
 */
@Service
@Slf4j
public class SmsService {

    @Value("${app.sms.mode:simulation}")
    private String mode;
    @Value("${app.sms.twilio.account-sid:}")
    private String sid;
    @Value("${app.sms.twilio.auth-token:}")
    private String token;
    @Value("${app.sms.twilio.from:}")
    private String from;

    public record Resultat(boolean envoye, String erreur) {}

    /** Vrai tant que Twilio n'est pas activé : aucun SMS réel n'est alors envoyé. */
    public boolean estSimule() {
        return !"twilio".equalsIgnoreCase(mode);
    }

    public Resultat envoyer(String telephone, String message) {
        String to = normaliser(telephone);
        if (to == null) {
            return new Resultat(false, "Numéro de téléphone invalide");
        }
        if (!"twilio".equalsIgnoreCase(mode)) {
            log.info("[SMS SIMULÉ] à {} : {}", to, message);
            return new Resultat(true, null);
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", to);
            form.add("From", from);
            form.add("Body", message);
            RestClient.create().post()
                    .uri("https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json", sid)
                    .headers(h -> h.setBasicAuth(sid, token))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[SMS TWILIO] envoyé à {}", to);
            return new Resultat(true, null);
        } catch (Exception e) {
            log.warn("[SMS TWILIO] échec vers {} : {}", to, e.getMessage());
            return new Resultat(false, e.getMessage());
        }
    }

    /** Numéros sénégalais : 77 123 45 67 devient +221771234567. */
    public static String normaliser(String tel) {
        if (tel == null) return null;
        String t = tel.replaceAll("[\\s.\\-()]", "");
        if (t.startsWith("+")) return t.length() >= 9 ? t : null;
        if (t.startsWith("00")) return "+" + t.substring(2);
        if (t.matches("7\\d{8}")) return "+221" + t;
        return null;
    }
}
