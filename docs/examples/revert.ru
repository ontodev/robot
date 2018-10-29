PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

DELETE { ?s rdfs:label 'obsolete nuclear lumen' ;
			owl:deprecated 'true'^^xsd:boolean }
INSERT { ?s rdfs:label 'nuclear lumen' ;
            rdfs:subClassOf <http://purl.obolibrary.org/obo/GO_0070013> ;
        	rdfs:subClassOf <http://purl.obolibrary.org/obo/GO_0044428> ;
        	rdfs:subClassOf [ a owl:Restriction ; 
        					  owl:onProperty <http://purl.obolibrary.org/obo/BFO_0000050> ;
        					  owl:someValuesFrom <http://purl.obolibrary.org/obo/GO_0005634> ]}
WHERE { ?s rdfs:label 'obsolete nuclear lumen' }