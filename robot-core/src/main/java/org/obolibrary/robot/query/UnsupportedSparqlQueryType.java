package org.obolibrary.robot.query;

import com.hp.hpl.jena.query.Query;

/**
 * Created by edouglass on 9/7/17.
 *
 * Thrown if a SPARQL query is attempted with a type that's not supported.
 */
public class UnsupportedSparqlQueryType extends RuntimeException {

    public UnsupportedSparqlQueryType(String message) {
        super(message);
    }

    public static String queryTypeName(Query query) {
        String queryTypeName;
        switch (query.getQueryType()) {
            case Query.QueryTypeAsk:
                queryTypeName = "ASK";
                break;
            case Query.QueryTypeConstruct:
                queryTypeName = "CONSTRUCT";
                break;
            case Query.QueryTypeDescribe:
                queryTypeName = "DESCRIBE";
                break;
            case Query.QueryTypeSelect:
                queryTypeName = "SELECT";
                break;
            case Query.QueryTypeUnknown:
                queryTypeName = "Unknown";
                break;
            default:
                queryTypeName = "Unknown";
        }
        return queryTypeName;
    }
}
