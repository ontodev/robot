package org.obolibrary.robot.query;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.core.DatasetGraph;

/**
 * Created by edouglass on 9/6/17.
 *
 * Runs a sparql query against an in memory graph of RDF triples
 */
public class SparqlQueryExecution {

    private DatasetGraph ontology;

    public SparqlQueryExecution(DatasetGraph ontology) {
        this.ontology = ontology;
    }

    public QueryResult runQuery(String query) {
        QueryExecution exec = QueryExecutionFactory.create(query, DatasetFactory.create(ontology));
        Query q = exec.getQuery();

        switch (q.getQueryType()) {
            case Query.QueryTypeSelect:
                ResultSet pattern = exec.execSelect();
                return new SelectResult(pattern);

            case Query.QueryTypeConstruct:
                Model triples = exec.execConstruct();
                return new RdfResult(triples);

            case Query.QueryTypeDescribe:
                Model descriptionTriples = exec.execDescribe();
                return new RdfResult(descriptionTriples);

            case Query.QueryTypeAsk:
                boolean answer = exec.execAsk();

            default:
                throw new UnsupportedSparqlQueryType("SPARQL " + UnsupportedSparqlQueryType.queryTypeName(q) + " queries are currently unsupported.");
        }
    }
}
