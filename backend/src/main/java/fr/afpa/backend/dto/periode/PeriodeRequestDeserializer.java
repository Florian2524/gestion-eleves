package fr.afpa.backend.dto.periode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;

/** Distingue un ancien client omettant l'année d'un effacement JSON explicite. */
public class PeriodeRequestDeserializer extends StdDeserializer<PeriodeRequest> {
    public PeriodeRequestDeserializer() {
        super(PeriodeRequest.class);
    }

    @Override
    public PeriodeRequest deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (!node.isObject()) {
            return context.reportInputMismatch(PeriodeRequest.class, "Un objet JSON de période est attendu.");
        }
        Payload body = parser.getCodec().treeToValue(node, Payload.class);
        return new PeriodeRequest(body.libelle(), body.dateDebut(), body.dateFin(),
                body.dateDebutSaisie(), body.dateFinSaisie(), body.statut(), body.anneeScolaire(),
                node.has("anneeScolaire"));
    }

    // Cette information interne n'est jamais prise d'un indicateur envoyé par le client.
    @JsonIgnoreProperties("anneeScolairePresente")
    private record Payload(String libelle, LocalDate dateDebut, LocalDate dateFin,
                           LocalDate dateDebutSaisie, LocalDate dateFinSaisie,
                           String statut, String anneeScolaire) { }
}
