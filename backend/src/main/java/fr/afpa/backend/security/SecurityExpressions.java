package fr.afpa.backend.security;

import fr.afpa.backend.entity.RoleUtilisateur;
import fr.afpa.backend.repository.EnseignementRepository;
import fr.afpa.backend.repository.EvaluationRepository;
import fr.afpa.backend.repository.NoteRepository;
import fr.afpa.backend.repository.EleveRepository;
import fr.afpa.backend.repository.ScolariteRepository;
import fr.afpa.backend.repository.ResponsabiliteLegaleRepository;
import fr.afpa.backend.repository.ClasseRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityExpressions {

    private final EnseignementRepository enseignementRepository;
    private final EvaluationRepository evaluationRepository;
    private final NoteRepository noteRepository;
    private final EleveRepository eleveRepository;
    private final ScolariteRepository scolariteRepository;
    private final ResponsabiliteLegaleRepository responsabiliteLegaleRepository;
    private final ClasseRepository classeRepository;

    public SecurityExpressions(
            EnseignementRepository enseignementRepository,
            EvaluationRepository evaluationRepository,
            NoteRepository noteRepository,
            EleveRepository eleveRepository,
            ScolariteRepository scolariteRepository,
            ResponsabiliteLegaleRepository responsabiliteLegaleRepository,
            ClasseRepository classeRepository
    ) {
        this.enseignementRepository = enseignementRepository;
        this.evaluationRepository = evaluationRepository;
        this.noteRepository = noteRepository;
        this.eleveRepository = eleveRepository;
        this.scolariteRepository = scolariteRepository;
        this.responsabiliteLegaleRepository = responsabiliteLegaleRepository;
        this.classeRepository = classeRepository;
    }

    public boolean estProprietaireEnseignement(
            Long idEnseignement,
            Authentication authentication
    ) {
        Long idPersonne = idPersonne(authentication, RoleUtilisateur.ENSEIGNANT);
        return idPersonne != null
                && idEnseignement != null
                && enseignementRepository
                        .existsByIdEnseignementAndEnseignant_IdPersonne(
                                idEnseignement,
                                idPersonne
                        );
    }

    public boolean estProprietaireEvaluation(
            Long idEvaluation,
            Authentication authentication
    ) {
        Long idPersonne = idPersonne(authentication, RoleUtilisateur.ENSEIGNANT);
        return idPersonne != null
                && idEvaluation != null
                && evaluationRepository
                        .existsByIdEvaluationAndEnseignement_Enseignant_IdPersonne(
                                idEvaluation,
                                idPersonne
                        );
    }

    public boolean estProprietaireOuAbsenteEvaluation(Long idEvaluation, Authentication authentication) {
        return aRole(authentication, RoleUtilisateur.ENSEIGNANT) && idEvaluation != null
                && (!evaluationRepository.existsById(idEvaluation)
                || estProprietaireEvaluation(idEvaluation, authentication));
    }

    public boolean estProprietaireNote(
            Long idNote,
            Authentication authentication
    ) {
        Long idPersonne = idPersonne(authentication, RoleUtilisateur.ENSEIGNANT);
        return idPersonne != null
                && idNote != null
                && noteRepository
                        .existsByIdNoteAndEvaluation_Enseignement_Enseignant_IdPersonne(
                                idNote,
                                idPersonne
                        );
    }

    public boolean estProprietaireOuAbsenteNote(Long idNote, Authentication authentication) {
        return aRole(authentication, RoleUtilisateur.ENSEIGNANT) && idNote != null
                && (!noteRepository.existsById(idNote)
                || estProprietaireNote(idNote, authentication));
    }

    public boolean estAdmin(Authentication authentication) {
        return aRole(authentication, RoleUtilisateur.ADMIN);
    }

    public boolean peutConsulterEleve(Long idEleve, Authentication authentication) {
        if (estAdmin(authentication)) return true;
        Long enseignant = idPersonne(authentication, RoleUtilisateur.ENSEIGNANT);
        if (enseignant != null) return eleveRepository.concerneEnseignant(idEleve, enseignant);
        Long responsable = idPersonne(authentication, RoleUtilisateur.RESPONSABLE);
        return responsable != null && responsabiliteLegaleRepository
                .existsByResponsable_IdPersonneAndEleve_IdPersonne(responsable, idEleve);
    }

    public boolean peutConsulterScolarite(Long idScolarite, Authentication authentication) {
        if (estAdmin(authentication)) return true;
        Long enseignant = idPersonne(authentication, RoleUtilisateur.ENSEIGNANT);
        if (enseignant != null) return scolariteRepository.concerneEnseignant(idScolarite, enseignant);
        Long responsable = idPersonne(authentication, RoleUtilisateur.RESPONSABLE);
        return responsable != null && scolariteRepository.estLieAuResponsable(idScolarite, responsable);
    }

    public boolean peutConsulterScolariteOuAbsente(Long idScolarite, Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && idScolarite != null && (!scolariteRepository.existsById(idScolarite)
                || peutConsulterScolarite(idScolarite, authentication));
    }

    public boolean peutConsulterNote(Long idNote, Authentication authentication) {
        if (estAdmin(authentication)) return true;
        if (estProprietaireNote(idNote, authentication)) return true;
        Long responsable = idPersonne(authentication, RoleUtilisateur.RESPONSABLE);
        return responsable != null && noteRepository.estLieAuResponsable(idNote, responsable);
    }

    public boolean peutConsulterEvaluation(Long idEvaluation, Authentication authentication) {
        if (estAdmin(authentication)) return true;
        if (estProprietaireEvaluation(idEvaluation, authentication)) return true;
        Long responsable = idPersonne(authentication, RoleUtilisateur.RESPONSABLE);
        return responsable != null && evaluationRepository.concerneResponsable(idEvaluation, responsable);
    }

    public boolean peutConsulterEnseignement(Long idEnseignement, Authentication authentication) {
        if (estAdmin(authentication)) return true;
        if (estProprietaireEnseignement(idEnseignement, authentication)) return true;
        Long responsable = idPersonne(authentication, RoleUtilisateur.RESPONSABLE);
        return responsable != null && enseignementRepository.concerneResponsable(idEnseignement, responsable);
    }

    public boolean peutConsulterClasse(Long idClasse, Authentication authentication) {
        if (estAdmin(authentication)) return true;
        Long enseignant = idPersonne(authentication, RoleUtilisateur.ENSEIGNANT);
        if (enseignant != null) return classeRepository.concerneEnseignant(idClasse, enseignant);
        Long responsable = idPersonne(authentication, RoleUtilisateur.RESPONSABLE);
        return responsable != null && classeRepository.concerneResponsable(idClasse, responsable);
    }

    private boolean aRole(Authentication authentication, RoleUtilisateur role) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream().anyMatch(authority ->
                ("ROLE_" + role.name()).equals(authority.getAuthority()));
    }

    private Long idPersonne(Authentication authentication, RoleUtilisateur role) {
        if (!aRole(authentication, role)) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof CompteUtilisateurPrincipal utilisateur) {
            return utilisateur.getRole() == role ? utilisateur.getIdPersonne() : null;
        }
        if (principal instanceof Jwt jwt) {
            Object claim = jwt.getClaim("idPersonne");
            return claim instanceof Number number ? number.longValue() : null;
        }
        return null;
    }
}
