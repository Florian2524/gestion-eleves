package fr.afpa.backend.service;

import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.exception.InvalidPhotoException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.repository.EleveRepository;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;

@Service
public class ElevePhotoService {
    public record Photo(byte[] bytes, MediaType mediaType) {}
    private static final Logger LOG = LoggerFactory.getLogger(ElevePhotoService.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/gif", "gif", "image/webp", "webp");
    private final EleveRepository repository;
    private final Path directory;
    private final ProfilPersonneService profils;
    private final TransactionTemplate referenceCheck;

    public ElevePhotoService(EleveRepository repository,
                             @Value("${application.upload.directory:uploads}") String directory,
                             ProfilPersonneService profils,
                             PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.profils = profils;
        this.referenceCheck = new TransactionTemplate(transactionManager);
        this.referenceCheck.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.referenceCheck.setReadOnly(true);
    }

    @Transactional
    public void upload(Long id, MultipartFile file) {
        String mime = file.getContentType();
        if (file.isEmpty() || file.getSize() > MAX_BYTES || mime == null || !EXTENSIONS.containsKey(mime)) {
            throw new InvalidPhotoException("Photo invalide : image JPEG, PNG, GIF ou WebP de 5 Mo maximum attendue.");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_BYTES || !matchesSignature(mime, bytes)) {
                throw new InvalidPhotoException("Le contenu du fichier ne correspond pas à son type image.");
            }
            Eleve eleve = findForMutation(id);
            String filename = UUID.randomUUID() + "." + EXTENSIONS.get(mime);
            PhotoChange change = new PhotoChange(eleve.getPhotoUrl(), filename);
            // Enregistrer la compensation avant le premier effet de bord sur disque.
            TransactionSynchronizationManager.registerSynchronization(change);
            Files.createDirectories(directory);
            Path destination = safePath(filename);
            Files.createFile(destination); // Création exclusive : ne jamais écraser un fichier existant.
            change.created = true;
            try (var output = Files.newOutputStream(destination, StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS)) {
                output.write(bytes);
            }
            eleve.setPhotoUrl(filename);
            repository.save(eleve);
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
            try (var input = Files.newInputStream(safePath(filename), LinkOption.NOFOLLOW_LINKS)) {
                return new Photo(input.readAllBytes(), MediaType.parseMediaType(mime));
            }
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Photo introuvable.");
        }
    }

    @Transactional
    public void delete(Long id) {
        Eleve eleve = findForMutation(id);
        String previous = eleve.getPhotoUrl();
        TransactionSynchronizationManager.registerSynchronization(new PhotoChange(previous, null));
        eleve.setPhotoUrl(null);
        repository.save(eleve);
    }

    private Eleve findForMutation(Long id) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException("La modification d'une photo exige une transaction d'écriture synchronisée.");
        }
        // Même verrou que les autres mutations de profil ; charger l'élève après acquisition.
        profils.verrouiller(id);
        return find(id);
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

    private void cleanUnreferenced(String filename, String outcome) {
        if (filename == null) return;
        try {
            // Les ressources de la transaction terminée peuvent encore être liées au thread.
            // REQUIRES_NEW relit les références réellement validées, y compris après rollback.
            Boolean referenced = referenceCheck.execute(status -> repository.existsByPhotoUrl(filename));
            if (Boolean.TRUE.equals(referenced)) return;
            if (referenced == null) throw new IllegalStateException("État des références indéterminé.");
            Files.deleteIfExists(safePath(filename));
        } catch (IOException | RuntimeException exception) {
            // Le résultat SQL est définitif : une erreur de nettoyage ne doit pas le masquer.
            LOG.error("PHOTO_CLEANUP_FAILED outcome={} file={} : fichier conservé, vérifier les références et reprendre le nettoyage.",
                    outcome, filename, exception);
        }
    }

    private final class PhotoChange implements TransactionSynchronization {
        private final String previous;
        private final String replacement;
        private boolean created;

        private PhotoChange(String previous, String replacement) {
            this.previous = previous;
            this.replacement = replacement;
        }

        @Override
        public void afterCompletion(int status) {
            if (status == STATUS_COMMITTED) {
                cleanUnreferenced(previous, "COMMITTED");
                // Une autre opération de la même transaction peut avoir remplacé/annulé ce choix.
                if (created) cleanUnreferenced(replacement, "COMMITTED");
            } else if (status == STATUS_ROLLED_BACK) {
                if (created) cleanUnreferenced(replacement, "ROLLED_BACK");
            } else {
                // Ne rien effacer lorsque le résultat du commit est inconnu.
                LOG.error("PHOTO_CLEANUP_UNKNOWN previous={} replacement={} : résultat SQL indéterminé, fichiers conservés pour vérification.",
                        previous, replacement);
            }
        }
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
