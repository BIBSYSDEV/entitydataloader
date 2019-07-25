package no.greenall.entitydataloader.entity;

import no.greenall.entitydataloader.ApiIntegrator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.isNull;

public class EntityDataManager {
    private static final String UNIT_ONTOLOGY_IRI = "http://unit.no/entitydata#";
    private static final String CONCEPT_IRI = UNIT_ONTOLOGY_IRI + "Concept";
    private static final String RESOURCE_NOT_PERSISTED_TETMPLATE = "The resource %s was not persisted to the database";
    private static final String FILE_NOT_FOUND_TEMPLATE = "The requested file %s was not found";
    private static final String MALFORMED_URL_TEMPLATE = "The URL %s is malformed";
    private static final String LOCATION_RESPONSE_MISSING_ERROR_TEMPLATE = "Posting data: %n%n %s %n%nto %s failed as no location header was returned";
    private static final String URL_PATH_SEPARATOR = "/";
    private static final String UPDATED_ENTITY_OUTPUT_TEMPLATE = "Updated %d entity at URL: %s";
    private final URL baseUrl;
    private Model inputModel;
    private String apiKey;

    private Map<String, String> fetchReplacementIRIs() {
        Map<String, String> mappedIRIs = new HashMap<>();

        StmtIterator statementIterator = listConcepts();

        while (statementIterator.hasNext()) {
            Statement statement = statementIterator.nextStatement();
            String currentIRI = statement.getSubject().getURI();
            if (!mappedIRIs.containsKey(currentIRI)) {
                String replacementIRI = createEntity(UUID.randomUUID().toString(), statement.getModel());
                mappedIRIs.put(currentIRI, replacementIRI);
            }
        }
        return mappedIRIs;
    }

    private StmtIterator listConcepts() {
        SimpleSelector simpleSelector = new SimpleSelector(
                null,
                RDF.type,
                (RDFNode) ResourceFactory.createResource(CONCEPT_IRI));

        return inputModel.listStatements(simpleSelector);
    }

    private String createEntity(String id, Model model) {
        String outputData = modelToString(model);
        String responseUrl;

        ApiIntegrator apiIntegrator = new ApiIntegrator(baseUrl, apiKey);
        responseUrl = apiIntegrator.createEntity(id, outputData);

        if (isNull(responseUrl)) {
            throw new RuntimeException(String.format(LOCATION_RESPONSE_MISSING_ERROR_TEMPLATE, outputData, baseUrl));
        }

        return responseUrl;
    }

    private String modelToString(Model model) {
        StringWriter stringWriter = new StringWriter();
        RDFDataMgr.write(stringWriter, model, Lang.JSONLD);
        return stringWriter.toString();
    }

    private String updateEntity(String id, Model model) {
        String outputData = modelToString(model);

        ApiIntegrator apiIntegrator = new ApiIntegrator(baseUrl, apiKey);
        apiIntegrator.updateEntity(id, outputData);
        return baseUrl.toString();
    }

    private void loadData(String filepath, Lang lang) {
        try (InputStream inputStream = new FileInputStream(new File(filepath))) {
            RDFDataMgr.read(inputModel, inputStream, lang);
        } catch (IOException e) {
            throw new RuntimeException(String.format(FILE_NOT_FOUND_TEMPLATE, filepath));
        }
    }

    private Resource remapSingleIRI(Resource resource, Map<String, String> replacementIRIs) {
        if (replacementIRIs.containsKey(resource.getURI())) {
            return ResourceFactory.createResource(replacementIRIs.get(resource.getURI()));
        } else {
            throw new RuntimeException(String.format(RESOURCE_NOT_PERSISTED_TETMPLATE, resource.getURI()));
        }
    }

    private Model remapIRIs(Map<String, String> replacementIRIs) {
        Model outputModel = ModelFactory.createDefaultModel();
        StmtIterator concepts = listConcepts();
        while (concepts.hasNext()) {
            Statement statement = concepts.nextStatement();

            Resource subject = statement.getSubject();
            Property property = statement.getPredicate();
            RDFNode object = statement.getObject();

            if (object.isURIResource()) {
                object = remapSingleIRI(statement.getObject().asResource(), replacementIRIs);
            }
            outputModel.add(outputModel.createStatement(subject, property, object));
        }
        return outputModel;
    }

    public EntityDataManager(String filepath, Lang lang, String baseUrl, String apiKey) {
        this.apiKey = apiKey;
        this.inputModel = ModelFactory.createDefaultModel();

        try {
            this.baseUrl = new URL(baseUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(String.format(MALFORMED_URL_TEMPLATE, baseUrl));
        }

        loadData(filepath, lang);
        Map<String, String> mappedIRIs = fetchReplacementIRIs();
        Model model = remapIRIs(mappedIRIs);
        writeAllDataFromModel(model);
    }

    private void writeAllDataFromModel(Model model) {
        ResIterator subjects = model.listSubjects();
        int counter = 0;

        while (subjects.hasNext()) {
            counter++;
            Resource subject = subjects.nextResource();
            Property property = ResourceFactory.createProperty(null);
            RDFNode object = ResourceFactory.createResource();
            SimpleSelector subjectQuery = new SimpleSelector(subject, property, object);
            Model singleDescription = model.query(subjectQuery);
            String id = subject.getURI().substring(subject.getURI().lastIndexOf(URL_PATH_SEPARATOR));
            String updatedUrl = updateEntity(id, singleDescription);
            System.out.println(String.format(UPDATED_ENTITY_OUTPUT_TEMPLATE, counter, updatedUrl));
        }
    }
}
