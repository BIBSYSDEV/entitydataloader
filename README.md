# Entity data dataloader

A command line interface for loading data sequentially into the entity data platform.

# Usage

java -jar App.jar --api-key <API-KEY>  --url <URL-for-registry-POST-endpoint> --input <input-file> --serialization turtle

Note that the CLI supports multiple serializations, but turtle is recommended as this is relatively human readable.

# Data requirements

The data must be processed to conform with the ontology for the entity data platform and the ShaCL schema for the given registry, an example data file can be found in ```src/test/resources/humord.ttl```.

