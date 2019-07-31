package no.greenall.entitydataloader;


import no.greenall.entitydataloader.entity.EntityDataManager;
import no.greenall.entitydataloader.entity.util.Serialization;
import org.apache.jena.riot.Lang;
import picocli.CommandLine;

import java.net.UnknownHostException;
import java.util.Optional;

import static java.util.Objects.nonNull;

@CommandLine.Command(name = "App")
public class App implements Runnable {
    private static final String EXTENSION_SEPARATOR = ".";
    private static final String SERIALIZATION_PROVIDED_ERROR_TEMPLATE = "The provided serialization %s was not recognized and the serialization could not be determined from the file extension %s";
    private static final String FILE_EXTENSION_UNRECOGNIZED_TEMPLATE = "The serialization of the input file could not be recognized from the extension %s";

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display help for command")
    private boolean showHelp = false;

    @CommandLine.Option(names = {"-i", "--input"}, description = "Path to input file", paramLabel = "FILE", required = true)
    private String inputFilePath;

    @CommandLine.Option(names = {"-s", "--serialization"},
            description = "RDF serialization of input file", paramLabel = "SERIALIZATION", required = true)
    private String serialization;

    @CommandLine.Option(names = {"-u", "--url"}, description = "API url", paramLabel = "URL", required = true)
    private String baseUrl;

    @CommandLine.Option(names = {"-k", "--api-key"}, description = "API key", paramLabel = "KEY", required = true)
    private String apiKey;

    public static void main(String[] args) {
        if (args.length == 0) {
            // a small hack to show help on empty args
            args = new String[]{"-h"};
        }

        CommandLine.run(new App(), args);
    }

    @Override
    public void run() {
        Lang rdfSerialization = (nonNull(serialization)) ?
                Optional.ofNullable(Serialization.getByName(serialization)).orElse(null)
                :
                Optional.ofNullable(Serialization.getByName(findFileExtension())).orElse(null);

        if (nonNull(rdfSerialization)) {
            new EntityDataManager(inputFilePath, rdfSerialization, baseUrl, apiKey);
        } else {
            throw new RuntimeException(getErrorMessage());
        }
    }

    private String getErrorMessage() {

        String fileExtension = findFileExtension();

        return (nonNull(serialization)) ? String.format(
                SERIALIZATION_PROVIDED_ERROR_TEMPLATE, serialization, fileExtension)
                : String.format(FILE_EXTENSION_UNRECOGNIZED_TEMPLATE, fileExtension);
    }

    private String findFileExtension() {
        return inputFilePath.substring(inputFilePath.lastIndexOf(EXTENSION_SEPARATOR) + 1);
    }
}
