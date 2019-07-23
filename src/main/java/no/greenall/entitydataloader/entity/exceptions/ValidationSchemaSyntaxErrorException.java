package no.greenall.entitydataloader.entity.exceptions;

import org.apache.jena.riot.RiotException;

/**
 * Copied from authority-registry
 */

public class ValidationSchemaSyntaxErrorException extends RiotException {

    public ValidationSchemaSyntaxErrorException(RiotException e) {
        super(e);
    }
}
