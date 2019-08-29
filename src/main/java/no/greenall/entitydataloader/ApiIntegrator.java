package no.greenall.entitydataloader;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static javax.ws.rs.client.Entity.entity;

public class ApiIntegrator {

    private static final String API_KEY = "api-key";
    private static final String LOCATION = "Location";
    private static final String PATH_SEPARATOR = "/";
    private static final String ENTITY = "entity";
    private static final String IMPROPERLY_FORMED_URI_TEMPLATE = "The URL %s was not a properly formed URI";
    private static final String RESOURCE_NOT_FOUND = "Attempted to create resource and got ";
    private static final String APPLICATION_LD_JSON = "application/ld+json";
    private final URL apiUrl;
    private final String apiKey;
    private final Client client;

    public ApiIntegrator(URL apiUrl, String apiKey) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.client = ClientBuilder.newClient();
    }

    private String updateUrl(String... args) {
        StringBuilder uriString = new StringBuilder(apiUrl.toString());

        for (String arg : args) {
            uriString.append(PATH_SEPARATOR).append(arg);
        }

        try {
            return new URI(uriString.toString()).normalize().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format(IMPROPERLY_FORMED_URI_TEMPLATE, apiUrl.toString()));
        }
    }

    public String createEntity(String id, String entity) {
        WebTarget webTarget = client.target(updateUrl(ENTITY));
        Invocation.Builder invocationBuilder = webTarget.request();
        invocationBuilder.header(API_KEY, apiKey);
        EntityDto entityDto = new EntityDto();
        entityDto.setId(id);
        entityDto.setBody(entity);
        Response createResponse = invocationBuilder.post(entity(entityDto,
                MediaType.APPLICATION_JSON_TYPE.withCharset(StandardCharsets.UTF_8.name())));
        if (createResponse.getStatus() != Status.CREATED.getStatusCode()) {
            System.out.println(RESOURCE_NOT_FOUND + createResponse.getStatus() + " from posting data: " + createResponse.readEntity(String.class));
            return null;
        }
        String location = createResponse.getHeaderString(LOCATION);
        createResponse.close();
        return location;
    }

    public void updateEntity(String id, String entity) {
        String url = updateUrl(ENTITY, id);
        WebTarget webTarget = client.target(url);
        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON_TYPE + "; charset=utf-8");
        invocationBuilder.header(API_KEY, apiKey);
        EntityDto entityDto = new EntityDto();
        entityDto.setId(id);
        entityDto.setBody(entity);

        Response createResponse = invocationBuilder.put(entity(entityDto, MediaType.APPLICATION_JSON_TYPE + "; charset=utf-8"));
        if (createResponse.getStatus() != Response.Status.OK.getStatusCode()) {
            System.out.println("---- error in data ----");
            System.out.println(entity);
            throw new RuntimeException(String.format("Attempting to update %s failed with status code %d",
                    url, createResponse.getStatus()));
        }
    }

    public Model getEntity(String uri) {
        Model model = ModelFactory.createDefaultModel();
        WebTarget webTarget = client.target(uri);
        Invocation.Builder invocationBuilder = webTarget.request();
        invocationBuilder.accept(APPLICATION_LD_JSON + "; charset=utf-8");
        Response response = invocationBuilder.get();
        InputStream inputStream = IOUtils.toInputStream(response.readEntity(String.class), StandardCharsets.UTF_8);
        RDFDataMgr.read(model, inputStream, Lang.JSONLD);
        return model;
    }
}
