PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

DELETE { ?s rdfs:label 'nuclear lumen' ;
			rdfs:subClassOf ?super }
INSERT { ?s rdfs:label 'obsolete nuclear lumen' ;
            owl:deprecated 'true'^^xsd:boolean }
WHERE { ?s rdfs:label 'nuclear lumen';
		   rdfs:subClassOf ?super }