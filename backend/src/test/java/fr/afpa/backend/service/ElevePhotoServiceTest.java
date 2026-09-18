package fr.afpa.backend.service;

import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.exception.InvalidPhotoException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.repository.EleveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElevePhotoServiceTest {
    @TempDir Path directory;
    @Mock EleveRepository repository;
    ElevePhotoService service;
    Eleve eleve;

    @BeforeEach void setUp() {
        service = new ElevePhotoService(repository, directory.toString());
        eleve = new Eleve("Dupont", "Alice", null, null, null, "E1", LocalDate.of(2010, 1, 1), null);
    }

    @Test void uploadsReadsReplacesAndDeletesPhoto() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(eleve));
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
        MockMultipartFile file = new MockMultipartFile("file", "../../danger.png", "image/png", png);
        service.upload(1L, file);
        String first = eleve.getPhotoUrl();
        assertThat(first).matches("[0-9a-fA-F-]{36}\\.png");
        assertThat(service.read(1L).bytes()).isEqualTo(png);
        service.upload(1L, file);
        assertThat(Files.exists(directory.resolve(first))).isFalse();
        String second = eleve.getPhotoUrl();
        service.delete(1L);
        assertThat(eleve.getPhotoUrl()).isNull();
        assertThat(Files.exists(directory.resolve(second))).isFalse();
    }

    @Test void rejectsWrongMimeAndOversize() {
        when(repository.findById(1L)).thenReturn(Optional.of(eleve));
        assertThatThrownBy(() -> service.upload(1L,
                new MockMultipartFile("file", "x.txt", "text/plain", new byte[]{1})))
                .isInstanceOf(InvalidPhotoException.class);
        assertThatThrownBy(() -> service.upload(1L,
                new MockMultipartFile("file", "x.png", "image/png", new byte[5 * 1024 * 1024 + 1])))
                .isInstanceOf(InvalidPhotoException.class);
        verify(repository, never()).save(any());
    }

    @Test void missingStudentReturnsNotFound() {
        assertThatThrownBy(() -> service.upload(9L,
                new MockMultipartFile("file", "x.png", "image/png", new byte[]{1})))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
