package fr.afpa.backend.service;

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
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Files;
import java.nio.file.Path;
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
    @Mock ProfilPersonneService profils;
    @Mock PlatformTransactionManager transactionManager;
    ElevePhotoService service;

    @BeforeEach void setUp() {
        service = new ElevePhotoService(repository, directory.toString(), profils, transactionManager);
    }

    @Test void refusesMutationWithoutTransactionBeforeWritingAnyFile() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
        MockMultipartFile file = new MockMultipartFile("file", "../../danger.png", "image/png", png);
        assertThatThrownBy(() -> service.upload(1L, file)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(IllegalStateException.class);
        try (var files = Files.list(directory)) { assertThat(files).isEmpty(); }
        verify(repository, never()).save(any());
    }

    @Test void rejectsWrongMimeAndOversize() {
        assertThatThrownBy(() -> service.upload(1L,
                new MockMultipartFile("file", "x.txt", "text/plain", new byte[]{1})))
                .isInstanceOf(InvalidPhotoException.class);
        assertThatThrownBy(() -> service.upload(1L,
                new MockMultipartFile("file", "x.png", "image/png", new byte[5 * 1024 * 1024 + 1])))
                .isInstanceOf(InvalidPhotoException.class);
        assertThatThrownBy(() -> service.upload(1L,
                new MockMultipartFile("file", "x.png", null, new byte[]{1})))
                .isInstanceOf(InvalidPhotoException.class);
        verify(repository, never()).save(any());
    }

    @Test void readingMissingStudentReturnsNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.read(9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
