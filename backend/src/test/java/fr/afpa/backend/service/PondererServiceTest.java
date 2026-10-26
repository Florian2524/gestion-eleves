package fr.afpa.backend.service;

import fr.afpa.backend.dto.ponderer.PondererRequest;
import fr.afpa.backend.entity.*;
import fr.afpa.backend.exception.*;
import fr.afpa.backend.mapper.PondererMapper;
import fr.afpa.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PondererServiceTest {
    @Mock PondererRepository repository;
    @Mock ClasseRepository classes;
    @Mock MatiereRepository matieres;
    @Mock EnseignementRepository enseignements;
    PondererService service;
    final PondererId id = new PondererId(1L, 2L);
    final PondererRequest request = new PondererRequest(1L, 2L, new BigDecimal("2.00"));

    @BeforeEach void setup() { service = new PondererService(repository, classes, matieres, enseignements, new PondererMapper()); }
    private Ponderer existing() {
        Ponderer p = mock(Ponderer.class);
        lenient().when(p.getId()).thenReturn(id);
        when(p.getCoefficientMatiere()).thenReturn(new BigDecimal("2"));
        lenient().when(repository.findById(id)).thenReturn(Optional.of(p));
        lenient().when(repository.findForShare(id)).thenReturn(Optional.of(p));
        return p;
    }
    @Test void initializesThenChecksExistingPairIgnoringDecimalScale() {
        existing();
        service.ensureCompatible(1L, 2L, new BigDecimal("2.00"));
        verify(repository).insertIfAbsent(1L, 2L, new BigDecimal("2.00"));
        verify(repository).findForShare(id);
        verify(repository, never()).updateCoefficient(any(), any());
    }
    @Test void refusesDivergentLegacyCoefficient() {
        existing();
        assertThatThrownBy(() -> service.ensureCompatible(1L, 2L, new BigDecimal("3")))
                .isInstanceOf(DuplicateResourceException.class).hasMessageContaining("explicitement");
    }
    @Test void duplicateCreationIsNotSilentlyIgnored() {
        when(classes.existsById(1L)).thenReturn(true);
        when(matieres.existsById(2L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(DuplicateResourceException.class);
    }
    @Test void refusesMissingClassBeforeInsert() {
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(repository, matieres);
    }
    @Test void refusesMissingSubjectBeforeInsert() {
        when(classes.existsById(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(repository);
    }
    @Test void updateCannotMovePair() {
        assertThatThrownBy(() -> service.update(9L, 2L, request)).isInstanceOf(BusinessRuleViolationException.class);
        verifyNoInteractions(repository);
    }
    @Test void missingUpdateIsExplicit() {
        assertThatThrownBy(() -> service.update(1L, 2L, request)).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test void updateUsesAtomicRepositoryOperation() {
        existing();
        when(repository.updateCoefficient(id, request.coefficientMatiere())).thenReturn(1);
        assertThat(service.update(1L, 2L, request).coefficientMatiere()).isEqualByComparingTo("2");
        verify(repository).updateCoefficient(id, request.coefficientMatiere());
    }
    @Test void usedPondererCannotBeDeleted() {
        Ponderer p = mock(Ponderer.class);
        when(repository.findById(id)).thenReturn(Optional.of(p));
        when(enseignements.existsByClasse_IdClasseAndMatiere_IdMatiere(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 2L)).isInstanceOf(ResourceInUseException.class);
        verify(repository, never()).delete(any());
    }
}
