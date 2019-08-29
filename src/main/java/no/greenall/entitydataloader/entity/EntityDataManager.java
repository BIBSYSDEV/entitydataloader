package no.greenall.entitydataloader.entity;

import no.greenall.entitydataloader.ApiIntegrator;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.isNull;

public class EntityDataManager {
    private static final String UNIT_ONTOLOGY_IRI = "http://unit.no/entitydata#";
    private static final String CONCEPT_IRI = UNIT_ONTOLOGY_IRI + "Concept";
    private static final String FILE_NOT_FOUND_TEMPLATE = "The requested file %s was not found";
    private static final String MALFORMED_URL_TEMPLATE = "The URL %s is malformed";
    private static final String LOCATION_RESPONSE_MISSING_ERROR_TEMPLATE = "Posting data: %n%n %s %n%nto %s failed as no location header was returned";
    private static final String UPDATED_ENTITY_OUTPUT_TEMPLATE = "Updated %d entity at URL: %s";
    private final URL baseUrl;
    private final ApiIntegrator apiIntegrator;
    private Model inputModel;
    private static final Model outputModel = ModelFactory.createDefaultModel();

    private void fetchReplacementIRIs() {
        Map<String, String> mappedIRIs = new HashMap<>();

        StmtIterator concepts = listConcepts(inputModel);
        while (concepts.hasNext()) {
            Model tempModel = ModelFactory.createDefaultModel();
            Resource targetResource = concepts.nextStatement().getSubject();
            tempModel.add(getStatementsOfSubject(targetResource, inputModel));
            String replacementUri = createEntity(UUID.randomUUID().toString(), tempModel);
            mappedIRIs.put(targetResource.getURI(), replacementUri);
        }

        restructure(mappedIRIs);
        /*
        StmtIterator secondPass = inputModel.listStatements();

        while (secondPass.hasNext()) {
            Statement statement = secondPass.nextStatement();
            String oldSubject = statement.getSubject().getURI();
            Resource newSubject = ResourceFactory.createResource(mappedIRIs.get(oldSubject));
            RDFNode object = statement.getObject();

            if (object.isResource() && mappedIRIs.containsKey(object.asResource().getURI())) {
                object = ResourceFactory.createResource(mappedIRIs.get(object.asResource().getURI()));
            }

            outputModel.add(newSubject, statement.getPredicate(), object);
        }
         */
    }

    private void restructure(Map<String, String> iriMappings) {
        iriMappings.forEach((key, value) -> {
            Model model = apiIntegrator.getEntity(value);
            StmtIterator stmtIterator = model.listStatements();
            while (stmtIterator.hasNext()) {
                Statement tempStmt = stmtIterator.nextStatement();
                Property sameAs = ResourceFactory.createProperty("http://unit.no/entitydata#sameAs");
                Property property = tempStmt.getPredicate();
                RDFNode object = tempStmt.getObject();
                if (!property.getURI().equals(sameAs.getURI())
                        && object.isResource()
                        && iriMappings.containsKey(object.asResource().getURI())) {
                    object = ResourceFactory.createResource(iriMappings.get(object.asResource().getURI()));
                    outputModel.add(ResourceFactory.createStatement(tempStmt.getSubject(),
                            tempStmt.getPredicate(), object));
                } else {
                    outputModel.add(tempStmt);
                }
            }
        });

    }

    private StmtIterator getStatementsOfSubject(Resource targetResource, Model model) {
        return model.listStatements(targetResource, null, (RDFNode) null);
    }

    private StmtIterator listConcepts(Model model) {
        SimpleSelector simpleSelector = new SimpleSelector(
                null,
                RDF.type,
                ResourceFactory.createResource(CONCEPT_IRI));

        return model.listStatements(simpleSelector);
    }

    public void persistRemappedEntities() {
        if (outputModel.isEmpty()) {
            throw new RuntimeException("The output model in EntityDataManager was not populated");
        }

        StmtIterator resources = listConcepts(outputModel);
        int counter = 0;

        while (resources.hasNext()) {
            counter++;
            Statement statement = resources.nextStatement();
            Resource subject = statement.getSubject();
            String subjectString = subject.getURI();
            String resourceId = subjectString.substring(subjectString.lastIndexOf("/") + 1);
            Model tempModel = ModelFactory.createDefaultModel();
            tempModel.add(getStatementsOfSubject(subject, outputModel));
            apiIntegrator.updateEntity(resourceId, modelToString(tempModel));
            System.out.println(String.format(UPDATED_ENTITY_OUTPUT_TEMPLATE, counter, subjectString));
        }
    }

    private String createEntity(String id, Model model) {
        String outputData = modelToString(model);
        String responseUrl  = this.apiIntegrator.createEntity(id, outputData);

        if (isNull(responseUrl)) {
            throw new RuntimeException(String.format(LOCATION_RESPONSE_MISSING_ERROR_TEMPLATE, outputData, baseUrl));
        }

        return responseUrl;
    }

    private String modelToString(Model model) {
        String output;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        RDFDataMgr.write(byteArrayOutputStream, model, Lang.JSONLD);
        try {
            output = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Could not encode output as UTF-8");
        }
        return output;
    }

    private void loadData(String filepath, Lang lang) {
        try (FileInputStream fileInputStream = new FileInputStream(new File(filepath))) {
            Reader reader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
            InputStream inputStream = IOUtils.toInputStream(IOUtils.toString(reader), Charsets.UTF_8);
            RDFDataMgr.read(inputModel, inputStream, lang);
        } catch (IOException e) {
            throw new RuntimeException(String.format(FILE_NOT_FOUND_TEMPLATE, filepath));
        }
    }

    public EntityDataManager(String filepath, Lang lang, String baseUrl, String apiKey) {
        this.inputModel = ModelFactory.createDefaultModel();

        try {
            this.baseUrl = new URL(baseUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(String.format(MALFORMED_URL_TEMPLATE, baseUrl));
        }

        this.apiIntegrator = new ApiIntegrator(this.baseUrl, apiKey);

        loadData(filepath, lang);
        fetchReplacementIRIs();
        persistRemappedEntities();
    }
}
