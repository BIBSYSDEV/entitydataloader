package no.greenall.entitydataloader.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.greenall.entitydataloader.entity.exceptions.ValidationSchemaSyntaxErrorException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


/**
 * Copied from authority-registry
 */
public class ModelParser {

    @JsonIgnore
    private Model parseModel(InputStream stream, Lang lang) {

        try {
            Model model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, stream, lang);
            return model;
        } catch (RiotException e) {
            throw new ValidationSchemaSyntaxErrorException(e);
        }
    }

    protected Model parseModel(String dataString, Lang lang) {
        InputStream stream = new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8));
        return parseModel(stream, lang);
    }
}
