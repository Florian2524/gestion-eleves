package fr.afpa.backend.integration;

import fr.afpa.backend.entity.Eleve;
import fr.afpa.backend.entity.Personne;
import fr.afpa.backend.exception.InvalidPhotoException;
import fr.afpa.backend.exception.ResourceNotFoundException;
import fr.afpa.backend.service.ElevePhotoService;
import fr.afpa.backend.support.IsolatedPostgres;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Aucun @Transactional de test : les callbacks passent de vraies frontières PostgreSQL. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ElevePhotoTransactionIntegrationTest extends IsolatedPostgres {
    private static final Path ROOT = temporaryDirectory();
    private static final Path DIRECTORY = ROOT.resolve("uploads");
    private static final byte[] IMAGE = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+kVfoAAAAASUVORK5CYII=");

    @Autowired ElevePhotoService photos;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired MockMvc mvc;
    private TransactionTemplate tx;
    private final List<Long> fixtures = new ArrayList<>();
    private Long pupil;

    private static Path temporaryDirectory() {
        try { return Files.createTempDirectory("gestion-eleves-photo-tx-"); }
        catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }

    @DynamicPropertySource
    static void uploads(DynamicPropertyRegistry registry) {
        registry.add("application.upload.directory", DIRECTORY::toString);
    }

    @BeforeEach
    void prepare() throws IOException {
        Files.createDirectories(DIRECTORY);
        tx = new TransactionTemplate(manager);
        pupil = createPupil();
    }

    private Long createPupil() {
        Long id = tx.execute(status -> {
            var person = new Personne("Photo", "Synthétique", null, null, null);
            em.persist(person);
            var eleve = new Eleve(person, "PHOTO-" + UUID.randomUUID(), LocalDate.of(2012, 1, 1), null);
            em.persist(eleve);
            em.flush();
            return eleve.getIdPersonne();
        });
        fixtures.add(id);
        return id;
    }

    @AfterEach
    void cleanFixtures() throws IOException {
        tx.executeWithoutResult(status -> {
            for (Long id : fixtures) {
                em.remove(em.find(Eleve.class, id));
                em.flush();
                em.remove(em.find(Personne.class, id));
            }
        });
        try (var paths = Files.walk(ROOT)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (!path.equals(ROOT)) Files.deleteIfExists(path);
            }
        }
    }

    @AfterAll
    static void removeOwnDirectory() throws IOException { Files.deleteIfExists(ROOT); }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "../../client.png", "image/png", IMAGE);
    }

    private String storedPhoto(Long id) {
        return tx.execute(status -> {
            em.clear();
            return em.find(Eleve.class, id).getPhotoUrl();
        });
    }

    private String upload() {
        photos.upload(pupil, file());
        return storedPhoto(pupil);
    }

    private void flushClear() { em.flush(); em.clear(); }

    private void assertNoOrphans() throws IOException {
        Set<String> references = new HashSet<>();
        for (Long id : fixtures) {
            String reference = storedPhoto(id);
            if (reference != null) references.add(reference);
        }
        try (var files = Files.list(DIRECTORY)) {
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                    .containsExactlyInAnyOrderElementsOf(references);
        }
    }

    @Test
    void firstUploadCommitsAndCanBeReloaded() throws IOException {
        String name = upload();
        assertThat(name).matches("[0-9a-fA-F-]{36}\\.png");
        assertThat(photos.read(pupil).bytes()).isEqualTo(IMAGE);
        assertThat(Files.readAllBytes(DIRECTORY.resolve(name))).isEqualTo(IMAGE);
        assertNoOrphans();
    }

    @Test
    void successfulReplacementKeepsOldFileUntilCommit() throws IOException {
        String old = upload();
        String replacement = tx.execute(status -> {
            photos.upload(pupil, file());
            flushClear();
            String current = em.find(Eleve.class, pupil).getPhotoUrl();
            assertThat(Files.exists(DIRECTORY.resolve(old))).isTrue();
            assertThat(Files.exists(DIRECTORY.resolve(current))).isTrue();
            return current;
        });
        assertThat(storedPhoto(pupil)).isEqualTo(replacement).isNotEqualTo(old);
        assertThat(Files.exists(DIRECTORY.resolve(old))).isFalse();
        assertNoOrphans();
    }

    @Test
    void auditRegressionReplacementRollbackRestoresReferenceAndFiles() throws IOException {
        String old = upload();
        String replacement = tx.execute(status -> {
            photos.upload(pupil, file());
            flushClear();
            String current = em.find(Eleve.class, pupil).getPhotoUrl();
            assertThat(Files.exists(DIRECTORY.resolve(old))).isTrue();
            status.setRollbackOnly();
            return current;
        });
        assertThat(storedPhoto(pupil)).isEqualTo(old);
        assertThat(Files.exists(DIRECTORY.resolve(old))).isTrue();
        assertThat(Files.exists(DIRECTORY.resolve(replacement))).isFalse();
        assertThat(photos.read(pupil).bytes()).isEqualTo(IMAGE);
        assertNoOrphans();
    }

    @Test
    void firstUploadRollbackRemovesNewFile() throws IOException {
        tx.executeWithoutResult(status -> { photos.upload(pupil, file()); flushClear(); status.setRollbackOnly(); });
        assertThat(storedPhoto(pupil)).isNull();
        assertNoOrphans();
    }

    @Test
    void lateCommitVetoCleansReplacementAndRetainsOldFile() throws IOException {
        String old = upload();
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            photos.upload(pupil, file());
            flushClear();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) { throw new IllegalStateException("Commit refusé pour le test"); }
            });
        })).isInstanceOf(IllegalStateException.class).hasMessage("Commit refusé pour le test");
        assertThat(storedPhoto(pupil)).isEqualTo(old);
        assertThat(photos.read(pupil).bytes()).isEqualTo(IMAGE);
        assertNoOrphans();
    }

    @Test
    void databaseFailureAfterFlushCompensatesNewFile() throws IOException {
        String old = upload();
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            photos.upload(pupil, file()); flushClear();
            jdbc.update("UPDATE personne SET nom = NULL WHERE id_personne = ?", pupil);
        })).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(storedPhoto(pupil)).isEqualTo(old);
        assertThat(photos.read(pupil).bytes()).isEqualTo(IMAGE);
        assertNoOrphans();
    }

    @Test
    void successfulDeletionWaitsForCommit() throws IOException {
        String old = upload();
        tx.executeWithoutResult(status -> {
            photos.delete(pupil); flushClear();
            assertThat(em.find(Eleve.class, pupil).getPhotoUrl()).isNull();
            assertThat(Files.exists(DIRECTORY.resolve(old))).isTrue();
        });
        assertThat(storedPhoto(pupil)).isNull();
        assertThatThrownBy(() -> photos.read(pupil)).isInstanceOf(ResourceNotFoundException.class);
        assertNoOrphans();
    }

    @Test
    void deletionRollbackRetainsReferenceAndFile() throws IOException {
        String old = upload();
        tx.executeWithoutResult(status -> { photos.delete(pupil); flushClear(); status.setRollbackOnly(); });
        assertThat(storedPhoto(pupil)).isEqualTo(old);
        assertThat(photos.read(pupil).bytes()).isEqualTo(IMAGE);
        assertNoOrphans();
    }

    @Test
    void invalidFilesLeaveExistingPhotoUntouched() throws IOException {
        String old = upload();
        for (MockMultipartFile invalid : List.of(
                new MockMultipartFile("file", "x.txt", "text/plain", new byte[]{1}),
                new MockMultipartFile("file", "x.png", "image/png", new byte[]{1}),
                new MockMultipartFile("file", "x.png", null, IMAGE),
                new MockMultipartFile("file", "x.png", "image/png", new byte[5 * 1024 * 1024 + 1]))) {
            assertThatThrownBy(() -> photos.upload(pupil, invalid)).isInstanceOf(InvalidPhotoException.class);
        }
        assertThat(storedPhoto(pupil)).isEqualTo(old);
        assertNoOrphans();
    }

    @Test
    void diskWriteFailureRollsBackWithoutPhotoReference() throws IOException {
        Files.delete(DIRECTORY);
        Files.writeString(DIRECTORY, "Emplacement indisponible : fichier au lieu d'un répertoire");
        assertThatThrownBy(() -> photos.upload(pupil, file())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Impossible de stocker");
        assertThat(storedPhoto(pupil)).isNull();
        assertThat(Files.isRegularFile(DIRECTORY)).isTrue();
    }

    @Test
    void missingPupilCannotCreateOrDeleteFiles() throws IOException {
        assertThatThrownBy(() -> photos.upload(Long.MAX_VALUE, file())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> photos.delete(Long.MAX_VALUE)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> photos.read(Long.MAX_VALUE)).isInstanceOf(ResourceNotFoundException.class);
        assertNoOrphans();
    }

    private void blockDeletion(Path path) throws IOException {
        Files.delete(path);
        Files.createDirectory(path);
        Files.writeString(path.resolve("blocked"), "Erreur de suppression reproductible");
    }

    @Test
    void cleanupFailureAfterCommitIsLoggedButHttpUploadSucceeds(CapturedOutput output) throws Exception {
        String old = upload();
        blockDeletion(DIRECTORY.resolve(old));
        mvc.perform(multipart("/eleves/{id}/photo", pupil).file(file())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
        assertThat(storedPhoto(pupil)).isNotEqualTo(old);
        assertThat(photos.read(pupil).bytes()).isEqualTo(IMAGE);
        assertThat(output.getOut()).contains("PHOTO_CLEANUP_FAILED outcome=COMMITTED file=" + old);
        assertThat(Files.isDirectory(DIRECTORY.resolve(old))).isTrue();
    }

    @Test
    void cleanupFailureAfterRollbackIsLoggedWithoutLosingOldPhoto(CapturedOutput output) throws IOException {
        String old = upload();
        String replacement = tx.execute(status -> {
            photos.upload(pupil, file()); flushClear();
            String current = em.find(Eleve.class, pupil).getPhotoUrl();
            try { blockDeletion(DIRECTORY.resolve(current)); }
            catch (IOException e) { throw new IllegalStateException(e); }
            status.setRollbackOnly(); return current;
        });
        assertThat(storedPhoto(pupil)).isEqualTo(old);
        assertThat(photos.read(pupil).bytes()).isEqualTo(IMAGE);
        assertThat(output.getOut()).contains("PHOTO_CLEANUP_FAILED outcome=ROLLED_BACK file=" + replacement);
    }

    @Test
    void cleanupFailureAfterDeletionDoesNotTurnCommittedHttpDeleteIntoFailure(CapturedOutput output) throws Exception {
        String old = upload(); blockDeletion(DIRECTORY.resolve(old));
        mvc.perform(delete("/eleves/{id}/photo", pupil)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
        assertThat(storedPhoto(pupil)).isNull();
        assertThat(output.getOut()).contains("PHOTO_CLEANUP_FAILED outcome=COMMITTED file=" + old);
    }

    @Test
    void referencedOldFileIsRetainedForAnotherPupil() throws IOException {
        String old = upload(); Long other = createPupil();
        tx.executeWithoutResult(status -> em.find(Eleve.class, other).setPhotoUrl(old));
        photos.upload(pupil, file());
        assertThat(photos.read(other).bytes()).isEqualTo(IMAGE);
        photos.delete(pupil);
        assertThat(Files.exists(DIRECTORY.resolve(old))).isTrue();
        assertNoOrphans();
    }

    @Test
    void rollbackNeverDeletesNewFileReferencedByACommittedTransaction() throws IOException {
        String old = upload(); Long other = createPupil();
        var independent = new TransactionTemplate(manager);
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        String replacement = tx.execute(status -> {
            photos.upload(pupil, file()); flushClear();
            String current = em.find(Eleve.class, pupil).getPhotoUrl();
            independent.executeWithoutResult(inner -> em.find(Eleve.class, other).setPhotoUrl(current));
            status.setRollbackOnly(); return current;
        });
        assertThat(storedPhoto(pupil)).isEqualTo(old);
        assertThat(storedPhoto(other)).isEqualTo(replacement);
        assertThat(photos.read(other).bytes()).isEqualTo(IMAGE);
        assertNoOrphans();
    }

    @Test
    void multipleChangesWithinOneTransactionHaveNoOrphans() throws IOException {
        upload();
        tx.executeWithoutResult(status -> {
            photos.upload(pupil, file()); photos.delete(pupil); photos.upload(pupil, file()); flushClear();
        });
        assertNoOrphans();
        String committed = storedPhoto(pupil);
        tx.executeWithoutResult(status -> {
            photos.upload(pupil, file()); photos.delete(pupil); photos.upload(pupil, file()); flushClear(); status.setRollbackOnly();
        });
        assertThat(storedPhoto(pupil)).isEqualTo(committed);
        assertNoOrphans();
    }

    @Test
    void committedReferenceDeterminesCleanupEvenIfChangedAgain() throws IOException {
        String old = upload();
        tx.executeWithoutResult(status -> {
            photos.upload(pupil, file()); flushClear();
            em.find(Eleve.class, pupil).setPhotoUrl(old);
        });
        assertThat(storedPhoto(pupil)).isEqualTo(old);
        assertThat(photos.read(pupil).bytes()).isEqualTo(IMAGE);
        assertNoOrphans();
    }

    @Test
    void traversalAndSymlinksCannotReadOrDeleteOutsideUploads() throws IOException {
        Path outside = ROOT.resolve("outside.png"); Files.write(outside, IMAGE);
        tx.executeWithoutResult(status -> em.find(Eleve.class, pupil).setPhotoUrl("../outside.png"));
        assertThatThrownBy(() -> photos.read(pupil)).isInstanceOf(ResourceNotFoundException.class);
        String link = UUID.randomUUID() + ".png";
        Files.createSymbolicLink(DIRECTORY.resolve(link), outside);
        tx.executeWithoutResult(status -> em.find(Eleve.class, pupil).setPhotoUrl(link));
        assertThatThrownBy(() -> photos.read(pupil)).isInstanceOf(ResourceNotFoundException.class);
        photos.delete(pupil);
        assertThat(Files.exists(DIRECTORY.resolve(link), LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(Files.readAllBytes(outside)).isEqualTo(IMAGE);
        assertNoOrphans();
    }

    @Test
    void mutationCannotJoinAReadOnlyTransaction() throws IOException {
        var readOnly = new TransactionTemplate(manager); readOnly.setReadOnly(true);
        assertThatThrownBy(() -> readOnly.executeWithoutResult(status -> photos.upload(pupil, file())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("transaction d'écriture");
        assertNoOrphans();
    }

    @Test
    void concurrentReplacementsAreSerializedWithoutOrphans() throws Exception {
        upload();
        var uploaded = new CountDownLatch(1); var release = new CountDownLatch(1);
        var attempting = new CountDownLatch(1);
        var firstPid = new AtomicInteger(); var secondPid = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> tx.executeWithoutResult(status -> {
                firstPid.set(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                photos.upload(pupil, file()); uploaded.countDown(); await(release);
            }));
            assertThat(uploaded.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> second = executor.submit(() -> tx.executeWithoutResult(status -> {
                secondPid.set(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                attempting.countDown(); photos.upload(pupil, file());
            }));
            assertThat(attempting.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(secondPid.get()).isNotEqualTo(firstPid.get());
            // Observer le verrou réellement attendu, avant de laisser la première transaction finir.
            org.awaitility.Awaitility.await().pollInSameThread().atMost(10, TimeUnit.SECONDS).until(() -> Boolean.TRUE.equals(
                    jdbc.queryForObject("SELECT ? = ANY(pg_blocking_pids(?))", Boolean.class,
                            firstPid.get(), secondPid.get())));
            assertThat(second.isDone()).isFalse();
            release.countDown();
            first.get(15, TimeUnit.SECONDS); second.get(15, TimeUnit.SECONDS);
            assertThat(photos.read(pupil).bytes()).isEqualTo(IMAGE);
            assertNoOrphans();
        } finally {
            release.countDown(); executor.shutdownNow();
            assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static void await(CountDownLatch latch) {
        try { if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Délai de test dépassé"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }
}
