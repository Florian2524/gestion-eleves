package fr.afpa.backend.repository;

import fr.afpa.backend.entity.ResponsabiliteLegale;
import fr.afpa.backend.entity.ResponsabiliteLegaleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResponsabiliteLegaleRepository
        extends JpaRepository<ResponsabiliteLegale, ResponsabiliteLegaleId> {
}