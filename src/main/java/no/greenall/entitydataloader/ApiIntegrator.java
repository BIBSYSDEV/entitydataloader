package no.greenall.entitydataloader;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.HttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ApiIntegrator {

    private static final String API_KEY = "api-key";
    private static final String LOCATION = "Location";
    private static final String PATH_SEPARATOR = "/";
    private static final String ENTITY = "entity";
    private static final String IMPROPERLY_FORMED_URI_TEMPLATE = "The URL %s was not a properly formed URI";
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
        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON_TYPE);
        invocationBuilder.header(API_KEY, apiKey);
        EntityDto entityDto = new EntityDto();
        entityDto.setId(id);
        entityDto.setBody(entity);
        Response createResponse = invocationBuilder.post(Entity.entity(entityDto, MediaType.APPLICATION_JSON_TYPE));
        if (createResponse.getStatus()!= Status.CREATED.getStatusCode()) {
            System.out.println( createResponse.readEntity(String.class));
            return null;
        }
        return createResponse.getHeaderString(LOCATION);
    }

    public void updateEntity(String id, String entity) {
        String url = updateUrl(ENTITY, id);
        WebTarget webTarget = client.target(url);
        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON_TYPE);
        invocationBuilder.header(API_KEY, apiKey);
        EntityDto entityDto = new EntityDto();
        entityDto.setId(id);
        entityDto.setBody(entity);

        Response createResponse = invocationBuilder.put(Entity.entity(entityDto, MediaType.APPLICATION_JSON_TYPE));
        if (createResponse.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new RuntimeException(String.format("Attempting to update %s failed with status code %d",
                    url, createResponse.getStatus()));
        }
    }
}
