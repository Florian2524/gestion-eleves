package fr.afpa.backend.security;

import fr.afpa.backend.dto.eleve.EleveResponse;
import fr.afpa.backend.dto.note.NoteResponse;
import fr.afpa.backend.dto.scolarite.ScolariteResponse;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.BulletinCalculService;
import fr.afpa.backend.service.EleveService;
import fr.afpa.backend.service.EvaluationService;
import fr.afpa.backend.service.NoteService;
import fr.afpa.backend.service.ScolariteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean SecurityExpressions expressions;
    @MockitoBean EleveService eleves;
    @MockitoBean EvaluationService evaluations;
    @MockitoBean NoteService notes;
    @MockitoBean ScolariteService scolarites;
    @MockitoBean BulletinCalculService bulletins;

    private static final String NOTE = """
            {"idScolarite":10,"idEvaluation":20,"valeur":15,"statutNote":"SAISIE"}
            """;
    private static final String EVALUATION = """
            {"idEnseignement":30,"idPeriode":1,"libelle":"Contrôle","dateEvaluation":"2026-09-18","typeEvaluation":"DEVOIR","coefficientEvaluation":1,"bareme":20}
            """;

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor role(String role) {
        return jwt().jwt(token -> token.claim("idPersonne", 5L))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test void enseignantModifieSeulementSesEvaluations() throws Exception {
        when(expressions.estProprietaireOuAbsenteEvaluation(eq(2L), any())).thenReturn(true);
        when(expressions.estProprietaireEnseignement(eq(30L), any())).thenReturn(true);
        mvc.perform(put("/evaluations/2").with(role("ENSEIGNANT"))
                .contentType(MediaType.APPLICATION_JSON).content(EVALUATION))
                .andExpect(status().isOk());
        mvc.perform(put("/evaluations/3").with(role("ENSEIGNANT"))
                .contentType(MediaType.APPLICATION_JSON).content(EVALUATION))
                .andExpect(status().isForbidden());
        when(expressions.estProprietaireOuAbsenteEvaluation(eq(999L), any())).thenReturn(true);
        when(evaluations.update(eq(999L), any())).thenThrow(new ResourceNotFoundException("Évaluation absente"));
        mvc.perform(put("/evaluations/999").with(role("ENSEIGNANT"))
                .contentType(MediaType.APPLICATION_JSON).content(EVALUATION))
                .andExpect(status().isNotFound());
    }

    @Test void adminPeutAdministrerNotesEtEvaluationsSansPropriete() throws Exception {
        mvc.perform(delete("/notes/99").with(role("ADMIN")))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/evaluations/99").with(role("ADMIN")))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/evaluations/99").with(role("ENSEIGNANT")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/comptes-utilisateurs").with(role("ENSEIGNANT")))
                .andExpect(status().isForbidden());
    }

    @Test void enseignantGereSeulementSesNotes() throws Exception {
        when(notes.create(any())).thenReturn(new NoteResponse(2L,10L,20L,BigDecimal.TEN,null,"SAISIE",null));
        when(expressions.estProprietaireEvaluation(eq(20L), any())).thenReturn(true);
        when(expressions.estProprietaireOuAbsenteNote(eq(2L), any())).thenReturn(true);
        mvc.perform(post("/notes").with(role("ENSEIGNANT"))
                .contentType(MediaType.APPLICATION_JSON).content(NOTE))
                .andExpect(status().isCreated());
        mvc.perform(put("/notes/2").with(role("ENSEIGNANT"))
                .contentType(MediaType.APPLICATION_JSON).content(NOTE))
                .andExpect(status().isOk());
        mvc.perform(delete("/notes/2").with(role("ENSEIGNANT")))
                .andExpect(status().isNoContent());
        mvc.perform(put("/notes/3").with(role("ENSEIGNANT"))
                .contentType(MediaType.APPLICATION_JSON).content(NOTE))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/notes/3").with(role("ENSEIGNANT")))
                .andExpect(status().isForbidden());
    }

    @Test void responsableConsulteUniquementSesElevesEtNotes() throws Exception {
        var eleve = new EleveResponse(1L,"Nom","Prenom",null,null,null,"M1",null,null);
        var autre = new EleveResponse(2L,"Autre","Eleve",null,null,null,"M2",null,null);
        when(eleves.findAll()).thenReturn(List.of(eleve, autre));
        when(eleves.findById(1L)).thenReturn(eleve);
        when(eleves.findById(2L)).thenReturn(autre);
        when(expressions.peutConsulterEleve(eq(1L), any())).thenReturn(true);
        mvc.perform(get("/eleves").with(role("RESPONSABLE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idPersonne").value(1));
        mvc.perform(get("/eleves/1").with(role("RESPONSABLE"))).andExpect(status().isOk());
        mvc.perform(get("/eleves/2").with(role("RESPONSABLE"))).andExpect(status().isForbidden());
        when(eleves.findById(999L)).thenThrow(new ResourceNotFoundException("Élève absent"));
        mvc.perform(get("/eleves/999").with(role("RESPONSABLE"))).andExpect(status().isNotFound());

        var note1 = new NoteResponse(11L,10L,20L,BigDecimal.TEN,null,"SAISIE",null);
        var note2 = new NoteResponse(12L,20L,20L,BigDecimal.ONE,null,"SAISIE",null);
        when(notes.findAll()).thenReturn(List.of(note1,note2));
        when(notes.findById(11L)).thenReturn(note1);
        when(notes.findById(12L)).thenReturn(note2);
        when(expressions.peutConsulterNote(eq(11L), any())).thenReturn(true);
        mvc.perform(get("/notes").with(role("RESPONSABLE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idNote").value(11));
        mvc.perform(get("/notes/11").with(role("RESPONSABLE"))).andExpect(status().isOk());
        mvc.perform(get("/notes/12").with(role("RESPONSABLE"))).andExpect(status().isForbidden());

        when(scolarites.findAll()).thenReturn(List.of(
                new ScolariteResponse(10L,1L,1L,null,null,"ACTIVE"),
                new ScolariteResponse(20L,2L,2L,null,null,"ACTIVE")));
        when(expressions.peutConsulterScolarite(eq(10L), any())).thenReturn(true);
        mvc.perform(get("/scolarites").with(role("RESPONSABLE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idScolarite").value(10));
    }

    @Test void responsableVoitSeulementLeBulletinDeSaScolariteEtNeModifiePasLesNotes() throws Exception {
        when(expressions.peutConsulterScolariteOuAbsente(eq(10L), any())).thenReturn(true);
        mvc.perform(get("/bulletins/calcul/10/1").with(role("RESPONSABLE")))
                .andExpect(status().isOk());
        mvc.perform(get("/bulletins/calcul/20/1").with(role("RESPONSABLE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/bulletins/calcul/20/1/pdf").with(role("RESPONSABLE")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/notes").with(role("RESPONSABLE"))
                .contentType(MediaType.APPLICATION_JSON).content(NOTE))
                .andExpect(status().isForbidden());
        mvc.perform(put("/notes/2").with(role("RESPONSABLE"))
                .contentType(MediaType.APPLICATION_JSON).content(NOTE))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/notes/2").with(role("RESPONSABLE")))
                .andExpect(status().isForbidden());
    }
}
