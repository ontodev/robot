package org.obolibrary.robot.query;

import org.apache.jena.riot.Lang;

import java.io.File;
import java.util.Optional;

/**
 * Created by edouglass on 9/6/17.
 *
 * Represents and abstract result from a sparql query
 */
public interface QueryResult {

    void writeResults(File outfile, Optional<Lang> format);

}
