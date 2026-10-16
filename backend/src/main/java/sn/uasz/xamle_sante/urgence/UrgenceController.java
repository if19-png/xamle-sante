package sn.uasz.xamle_sante.urgence;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.uasz.xamle_sante.urgence.UrgenceService.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Urgences")
public class UrgenceController {

    private final UrgenceService service;

    @Operation(summary = "Services d'urgence les plus proches d'une position")
    @GetMapping("/urgences")
    public List<UrgenceDto> urgences(@RequestParam(name = "lat") double lat,
                                     @RequestParam(name = "lon") double lon) {
        return service.plusProches(lat, lon, 5);
    }

    @Operation(summary = "Bouton SOS : envoie la position et les urgences proches par SMS")
    @PostMapping("/sos")
    public SosResponse sos(@Valid @RequestBody SosRequest r) {
        return service.declencher(r);
    }
}
