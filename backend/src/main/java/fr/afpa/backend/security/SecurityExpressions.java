package fr.afpa.backend.security;

import fr.afpa.backend.entity.RoleUtilisateur;
import fr.afpa.backend.repository.EnseignementRepository;
import fr.afpa.backend.repository.EvaluationRepository;
import fr.afpa.backend.repository.NoteRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SecurityExpressions {

    private final EnseignementRepository enseignementRepository;
    private final EvaluationRepository evaluationRepository;
    private final NoteRepository noteRepository;

    public SecurityExpressions(
            EnseignementRepository enseignementRepository,
            EvaluationRepository evaluationRepository,
            NoteRepository noteRepository
    ) {
        this.enseignementRepository = enseignementRepository;
        this.evaluationRepository = evaluationRepository;
        this.noteRepository = noteRepository;
    }

    public boolean estProprietaireEnseignement(
            Long idEnseignement,
            Authentication authentication
    ) {
        CompteUtilisateurPrincipal principal =
                getEnseignantPrincipal(authentication);

        return principal != null
                && idEnseignement != null
                && enseignementRepository
                        .existsByIdEnseignementAndEnseignant_IdPersonne(
                                idEnseignement,
                                principal.getIdPersonne()
                        );
    }

    public boolean estProprietaireEvaluation(
            Long idEvaluation,
            Authentication authentication
    ) {
        CompteUtilisateurPrincipal principal =
                getEnseignantPrincipal(authentication);

        return principal != null
                && idEvaluation != null
                && evaluationRepository
                        .existsByIdEvaluationAndEnseignement_Enseignant_IdPersonne(
                                idEvaluation,
                                principal.getIdPersonne()
                        );
    }

    public boolean estProprietaireNote(
            Long idNote,
            Authentication authentication
    ) {
        CompteUtilisateurPrincipal principal =
                getEnseignantPrincipal(authentication);

        return principal != null
                && idNote != null
                && noteRepository
                        .existsByIdNoteAndEvaluation_Enseignement_Enseignant_IdPersonne(
                                idNote,
                                principal.getIdPersonne()
                        );
    }

    private CompteUtilisateurPrincipal getEnseignantPrincipal(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CompteUtilisateurPrincipal principal)
                || principal.getRole() != RoleUtilisateur.ENSEIGNANT) {
            return null;
        }

        return principal;
    }
}
