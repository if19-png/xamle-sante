package sn.uasz.xamle_sante.epidemio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.uasz.xamle_sante.epidemio.EpidemioService.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Épidémiologie (public)")
public class EpidemioController {

    private final EpidemioService service;

    @Operation(summary = "Signaler des symptômes dans un quartier (anonyme, soumis à modération)")
    @PostMapping("/signalements")
    @ResponseStatus(HttpStatus.CREATED)
    public void signaler(@Valid @RequestBody SignalementRequest r) {
        service.signaler(r);
    }

    @Operation(summary = "Carte de chaleur : signalements validés agrégés par quartier (seuil d'anonymat appliqué)")
    @GetMapping("/heatmap")
    public List<ZoneChaleur> heatmap(@RequestParam(name = "jours", defaultValue = "14") int jours,
                                     @RequestParam(name = "symptome", required = false) String symptome) {
        return service.carteChaleur(jours, symptome);
    }

    @Operation(summary = "Alertes sanitaires actives")
    @GetMapping("/alertes")
    public List<AlerteDto> alertes() {
        return service.alertesActives();
    }
}
