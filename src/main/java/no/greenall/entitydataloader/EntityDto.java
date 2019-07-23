package no.greenall.entitydataloader;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.XmlRootElement;

import no.greenall.entitydataloader.entity.JsonAsStringDeserializer;
import no.greenall.entitydataloader.entity.ModelParser;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


/**
 * Copied from authoriy-registry
 */

@XmlRootElement
public class EntityDto extends ModelParser {

    private String id;
    private String created;
    private String modified;
    private String path;
    private String body;

    @JsonIgnore
    public String getEtagValue() {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(getBody().getBytes(StandardCharsets.UTF_8));
            String hex = DatatypeConverter.printHexBinary(hash);
            return hex;
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    @JsonRawValue
    @JsonDeserialize(using = JsonAsStringDeserializer.class)
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getCreated() != null ? getCreated().hashCode() : 0);
        result = 31 * result + (getModified() != null ? getModified().hashCode() : 0);
        result = 31 * result + (getPath() != null ? getPath().hashCode() : 0);
        return result;
    }

    /**
     * Checks for equality according to the values of the fields. The checked fields are: {@code id}, {@code created},
     * {@code modified},  and {@code body}. The {@code body} field is parsed into model and the two bodies are equal if
     * their respective models are isomorphic.
     *
     * @param o The object to compare with
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityDto)) {
            return false;
        }

        EntityDto other = (EntityDto) o;
        return Objects.equals(id, other.getId()) && Objects.equals(created, other.getCreated()) && Objects
                .equals(modified, other.getModified()) && isIsomorphic(other);
    }

    @JsonIgnore
    public boolean isIsomorphic(EntityDto other) {
        if (Objects.isNull(this.body) && Objects.isNull(other.getBody())) {
            return true;
        } else if (Objects.nonNull(this.body) && Objects.nonNull(other.getBody())) {
            Model thisModel = parseModel(getBody(), Lang.JSONLD);
            Model thatModel = parseModel(other.getBody(), Lang.JSONLD);
            return thisModel.isIsomorphicWith(thatModel);
        } else {
            // one null and the other is not
            return false;
        }
    }

    @Override
    public String toString() {
        return "EntityDto [id=" + id + ", created=" + created + ", modified=" + modified + ", body=" + body + "]";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    /**
     * Relative path to this resource, set in the API level.
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
