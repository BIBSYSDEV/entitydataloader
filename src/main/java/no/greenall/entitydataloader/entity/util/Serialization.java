package no.greenall.entitydataloader.entity.util;

import org.apache.jena.riot.Lang;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Serialization {
    TURTLE("turtle", Collections.singletonList("ttl"), Lang.TURTLE),
    NTRIPLES("ntriples", Collections.singletonList("nt"), Lang.NTRIPLES),
    RDFXML("rdfxml", Arrays.asList("xml", "rdf", "rdfxml"), Lang.RDFXML),
    JSONLD("jsonld", Arrays.asList("json", "jsonld"), Lang.JSONLD);

    private final Lang lang;
    private String label;
    private List<String> extension;

    Serialization(String label, List<String> extension, Lang lang) {
        this.label = label;
        this.extension = extension;
        this.lang = lang;
    }

    private Lang getLang() {
        return this.lang;
    }

    public static Lang getByName(String providedLabel) {
        return Arrays.stream(values()).filter(serialization -> serialization.label.equals(providedLabel))
                .findFirst().map(Serialization::getLang).orElse(null);
      }

    public Lang getByExtension(String extensionFromFilename) {
        return Arrays.stream(values()).filter(value -> value.extension.contains(extensionFromFilename.toLowerCase()))
                .findFirst().map(Serialization::getLang).orElse(null);
    }
}
