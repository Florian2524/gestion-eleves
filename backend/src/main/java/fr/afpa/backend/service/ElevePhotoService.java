package fr.afpa.backend.service;

import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.exception.InvalidPhotoException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.repository.EleveRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
public class ElevePhotoService {
    public record Photo(byte[] bytes, MediaType mediaType) {}
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/gif", "gif", "image/webp", "webp");
    private final EleveRepository repository;
    private final Path directory;

    public ElevePhotoService(EleveRepository repository,
                             @Value("${application.upload.directory:uploads}") String directory) {
        this.repository = repository;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    @Transactional
    public void upload(Long id, MultipartFile file) {
        Eleve eleve = find(id);
        String mime = file.getContentType();
        if (file.isEmpty() || file.getSize() > MAX_BYTES || !EXTENSIONS.containsKey(mime)) {
            throw new InvalidPhotoException("Photo invalide : image JPEG, PNG, GIF ou WebP de 5 Mo maximum attendue.");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_BYTES || !matchesSignature(mime, bytes)) {
                throw new InvalidPhotoException("Le contenu du fichier ne correspond pas à son type image.");
            }
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + "." + EXTENSIONS.get(mime);
            Path destination = safePath(filename);
            Files.write(destination, bytes);
            String previous = eleve.getPhotoUrl();
            try {
                eleve.setPhotoUrl(filename);
                repository.save(eleve);
            } catch (RuntimeException exception) {
                Files.deleteIfExists(destination);
                throw exception;
            }
            deleteFile(previous);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de stocker la photo de l'élève.", exception);
        }
    }

    @Transactional(readOnly = true)
    public Photo read(Long id) {
        String filename = find(id).getPhotoUrl();
        if (filename == null) throw new ResourceNotFoundException("Cet élève n'a pas de photo.");
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        String mime = EXTENSIONS.entrySet().stream()
                .filter(entry -> entry.getValue().equals(extension))
                .map(Map.Entry::getKey).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Photo introuvable."));
        try {
            return new Photo(Files.readAllBytes(safePath(filename)), MediaType.parseMediaType(mime));
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Photo introuvable.");
        }
    }

    @Transactional
    public void delete(Long id) {
        Eleve eleve = find(id);
        String previous = eleve.getPhotoUrl();
        eleve.setPhotoUrl(null);
        repository.save(eleve);
        deleteFile(previous);
    }

    private Eleve find(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("L'élève ayant l'identifiant " + id + " est introuvable."));
    }

    private Path safePath(String filename) {
        if (!filename.matches("[0-9a-fA-F-]{36}\\.(jpg|png|gif|webp)")) {
            throw new ResourceNotFoundException("Photo introuvable.");
        }
        Path path = directory.resolve(filename).normalize();
        if (!path.startsWith(directory)) throw new ResourceNotFoundException("Photo introuvable.");
        return path;
    }

    private void deleteFile(String filename) {
        if (filename == null) return;
        try { Files.deleteIfExists(safePath(filename)); }
        catch (IOException exception) { throw new IllegalStateException("Impossible de supprimer l'ancienne photo.", exception); }
    }

    private boolean matchesSignature(String mime, byte[] bytes) {
        return switch (mime) {
            case "image/jpeg" -> bytes.length >= 3 && (bytes[0] & 255) == 0xff
                    && (bytes[1] & 255) == 0xd8 && (bytes[2] & 255) == 0xff;
            case "image/png" -> bytes.length >= 8 && (bytes[0] & 255) == 0x89
                    && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
            case "image/gif" -> bytes.length >= 6 && bytes[0] == 'G'
                    && bytes[1] == 'I' && bytes[2] == 'F';
            case "image/webp" -> bytes.length >= 12 && bytes[0] == 'R'
                    && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }
}
