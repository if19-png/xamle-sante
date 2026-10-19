package sn.uasz.xamle_sante;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle_sante.annuaire.FicheService;
import sn.uasz.xamle_sante.annuaire.FicheService.AvisRequest;
import sn.uasz.xamle_sante.annuaire.FicheService.HoraireDto;
import sn.uasz.xamle_sante.annuaire.FicheService.HoraireRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Règles métier des horaires et des avis, avec un JdbcClient simulé (aucune base nécessaire). */
@SuppressWarnings({"unchecked", "rawtypes"})
class FicheServiceTest {

    private JdbcClient jdbc;
    private JdbcClient.StatementSpec spec;
    private FicheService service;

    @BeforeEach
    void init() {
        jdbc = mock(JdbcClient.class);
        spec = mock(JdbcClient.StatementSpec.class, Mockito.withSettings().defaultAnswer(Answers.RETURNS_SELF));
        when(jdbc.sql(anyString())).thenReturn(spec);
        service = new FicheService(jdbc);
    }

    private void profil(Optional<Long> id) {
        JdbcClient.MappedQuerySpec q = mock(JdbcClient.MappedQuerySpec.class);
        doReturn(q).when(spec).query(Long.class);
        when(q.optional()).thenReturn(id);
    }

    private void compteValide(long n) {
        JdbcClient.MappedQuerySpec q = mock(JdbcClient.MappedQuerySpec.class);
        doReturn(q).when(spec).query(Long.class);
        when(q.single()).thenReturn(n);
    }

    private int statut(ResponseStatusException e) {
        return e.getStatusCode().value();
    }

    @Test
    void refuseUnJourInvalide() {
        profil(Optional.of(7L));
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.remplacerHoraires(1, List.of(new HoraireRequest(9, "08:00", "12:00"))));
        assertEquals(400, statut(e));
        verify(spec, never()).update();
    }

    @Test
    void refuseUneHeureMalFormee() {
        profil(Optional.of(7L));
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.remplacerHoraires(1, List.of(new HoraireRequest(1, "25:00", "12:00"))));
        assertEquals(400, statut(e));
    }

    @Test
    void refuseUneFinAvantLeDebut() {
        profil(Optional.of(7L));
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.remplacerHoraires(1, List.of(new HoraireRequest(1, "14:00", "09:00"))));
        assertEquals(400, statut(e));
        assertTrue(e.getReason().contains("fin"));
    }

    @Test
    void refuseSansProfilProfessionnel() {
        profil(Optional.empty());
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.remplacerHoraires(1, List.of(new HoraireRequest(1, "08:00", "12:00"))));
        assertEquals(404, statut(e));
    }

    @Test
    void remplaceLesHorairesQuandTousSontValides() {
        profil(Optional.of(7L));
        JdbcClient.MappedQuerySpec liste = mock(JdbcClient.MappedQuerySpec.class);
        doReturn(liste).when(spec).query(HoraireDto.class);
        when(liste.list()).thenReturn(List.of(new HoraireDto(1, "08:00", "12:00")));

        List<HoraireDto> res = service.remplacerHoraires(1, List.of(new HoraireRequest(1, "08:00", "12:00")));

        assertEquals(1, res.size());
        verify(spec, times(2)).update(); // suppression des anciens horaires + insertion du nouveau
    }

    @Test
    void refuseUnAvisSurUnSpecialisteNonValide() {
        compteValide(0L);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.donnerAvis(5, 7, new AvisRequest(4, "Bien")));
        assertEquals(404, statut(e));
        verify(spec, never()).update();
    }

    @Test
    void enregistreUnAvisSurUnSpecialisteValide() {
        compteValide(1L);
        assertDoesNotThrow(() -> service.donnerAvis(5, 7, new AvisRequest(4, "  Très bien  ")));
        verify(spec, times(1)).update();
    }

    @Test
    void aucuneRequetePourUneListeVide() {
        assertTrue(service.complements(List.of()).isEmpty());
        assertTrue(service.complements(null).isEmpty());
        verifyNoInteractions(jdbc);
    }
}
