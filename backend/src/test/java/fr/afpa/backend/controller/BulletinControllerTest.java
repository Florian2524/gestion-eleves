package fr.afpa.backend.controller;

import fr.afpa.backend.dto.bulletin.BulletinDto;
import fr.afpa.backend.dto.bulletin.LigneBulletinDto;
import fr.afpa.backend.exception.GlobalExceptionHandler;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.BulletinCalculService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BulletinController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BulletinControllerTest {

    private static final Long ID_SCOLARITE = 10L;
    private static final Long ID_PERIODE = 20L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BulletinCalculService bulletinCalculService;

    private BulletinDto bulletin;

    @BeforeEach
    void setUp() {
        LigneBulletinDto ligne =
                new LigneBulletinDto(
                        100L,
                        1000L,
                        "MATH",
                        "Mathématiques",
                        BigDecimal.valueOf(4),
                        2,
                        BigDecimal.valueOf(15.33)
                );

        bulletin = new BulletinDto(
                ID_SCOLARITE,
                ID_PERIODE,
                40L,
                "ELV-001",
                "Martin",
                "Léa",
                30L,
                "6A",
                "Sixième",
                "2026-2027",
                "Trimestre 1",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 20),
                List.of(ligne),
                BigDecimal.valueOf(15.33)
        );
    }

    @Test
    void shouldCalculateBulletin() throws Exception {
        when(bulletinCalculService.calculer(
                ID_SCOLARITE,
                ID_PERIODE
        )).thenReturn(bulletin);

        mockMvc.perform(get(
                        "/bulletins/calcul/{idScolarite}/{idPeriode}",
                        ID_SCOLARITE,
                        ID_PERIODE
                ))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(jsonPath(
                        "$.idScolarite"
                ).value(10))
                .andExpect(jsonPath(
                        "$.idPeriode"
                ).value(20))
                .andExpect(jsonPath(
                        "$.idEleve"
                ).value(40))
                .andExpect(jsonPath(
                        "$.matriculeEleve"
                ).value("ELV-001"))
                .andExpect(jsonPath(
                        "$.nomEleve"
                ).value("Martin"))
                .andExpect(jsonPath(
                        "$.prenomEleve"
                ).value("Léa"))
                .andExpect(jsonPath(
                        "$.nomClasse"
                ).value("6A"))
                .andExpect(jsonPath(
                        "$.libellePeriode"
                ).value("Trimestre 1"))
                .andExpect(jsonPath(
                        "$.lignes"
                ).isArray())
                .andExpect(jsonPath(
                        "$.lignes.length()"
                ).value(1))
                .andExpect(jsonPath(
                        "$.lignes[0].codeMatiere"
                ).value("MATH"))
                .andExpect(jsonPath(
                        "$.lignes[0].nomMatiere"
                ).value("Mathématiques"))
                .andExpect(jsonPath(
                        "$.lignes[0].coefficientMatiere"
                ).value(4))
                .andExpect(jsonPath(
                        "$.lignes[0].nombreNotes"
                ).value(2))
                .andExpect(jsonPath(
                        "$.lignes[0].moyenneSur20"
                ).value(15.33))
                .andExpect(jsonPath(
                        "$.moyenneGenerale"
                ).value(15.33));

        verify(bulletinCalculService).calculer(
                ID_SCOLARITE,
                ID_PERIODE
        );
    }

    @Test
    void shouldReturnNotFoundWhenScolariteDoesNotExist()
            throws Exception {

        when(bulletinCalculService.calculer(
                ID_SCOLARITE,
                ID_PERIODE
        )).thenThrow(
                new ResourceNotFoundException(
                        "La scolarité ayant l'identifiant "
                                + ID_SCOLARITE
                                + " est introuvable."
                )
        );

        mockMvc.perform(get(
                        "/bulletins/calcul/{idScolarite}/{idPeriode}",
                        ID_SCOLARITE,
                        ID_PERIODE
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(
                        "$.status"
                ).value(404))
                .andExpect(jsonPath(
                        "$.error"
                ).value("Not Found"))
                .andExpect(jsonPath(
                        "$.message"
                ).value(
                        "La scolarité ayant l'identifiant "
                                + ID_SCOLARITE
                                + " est introuvable."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value(
                        "/bulletins/calcul/10/20"
                ));

        verify(bulletinCalculService).calculer(
                ID_SCOLARITE,
                ID_PERIODE
        );
    }

    @Test
    void shouldReturnNotFoundWhenPeriodDoesNotExist()
            throws Exception {

        when(bulletinCalculService.calculer(
                ID_SCOLARITE,
                ID_PERIODE
        )).thenThrow(
                new ResourceNotFoundException(
                        "La période ayant l'identifiant "
                                + ID_PERIODE
                                + " est introuvable."
                )
        );

        mockMvc.perform(get(
                        "/bulletins/calcul/{idScolarite}/{idPeriode}",
                        ID_SCOLARITE,
                        ID_PERIODE
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(
                        "$.status"
                ).value(404))
                .andExpect(jsonPath(
                        "$.error"
                ).value("Not Found"))
                .andExpect(jsonPath(
                        "$.message"
                ).value(
                        "La période ayant l'identifiant "
                                + ID_PERIODE
                                + " est introuvable."
                ))
                .andExpect(jsonPath(
                        "$.path"
                ).value(
                        "/bulletins/calcul/10/20"
                ));

        verify(bulletinCalculService).calculer(
                ID_SCOLARITE,
                ID_PERIODE
        );
    }
}
