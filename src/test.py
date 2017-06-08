from pycompss.api.task import task
from pycompss.api.parameter import *
from pycompss.api.api import compss_wait_on
from rdflib import Graph


def get_worker_fuseki_endpoint():
    import os
    port = os.environ['FUSEKI_PORT']
    return "http://localhost:%s/compssW" % port

def join_triplets(data):
    from rdflib import URIRef, BNode, Literal
    result = Graph()
    for graph in data:
        result += graph
    return result

@task(returns=Graph)
def get_consolidated_triplets(query, source_endpoints):
    data = []
    for i, endpoint in enumerate(source_endpoints):
        data.append(read(endpoint, query))

    joined_graph = join_triplets(data)

    return joined_graph




@task(returns=bool)
def insert_to_local_fuseki(data):
    query = """
    PREFIX dc: <http://purl.org/dc/elements/1.1/>
    INSERT DATA
    {
    """
    for s, p, o in data:
        query += "%s %s %s." % (s.n3(), p.n3(), o.n3())

    query += "}"
    print("")
    endpoint = get_worker_fuseki_endpoint()
    print("Insert query to be submited to %s:\n[%s]" % (endpoint, query))
    insert(endpoint, query)
    return True

# @task(returns=bool)
# def insert_bullshit_to_local_fuseki():
#     import os
#     port = os.environ['FUSEKI_PORT']
#     query = """
#     PREFIX dc: <http://purl.org/dc/elements/1.1/>
#     INSERT DATA
#     {
#      <http://example/book1> dc:title "A new book" ;
#                             dc:creator "A.N.Other" .
#     }
#     """
#     insert("http://localhost:%s/compssW" % port, query)
#
#     return True

def insert(endpoint, query):
    from SPARQLWrapper import SPARQLWrapper
    print("Inserting into endpoint %s" % endpoint)
    sparql = SPARQLWrapper("%s/update" % endpoint)
    sparql.setQuery(query)
    sparql.method = 'POST'
    sparql.query()

@task()
def read_from_local_fuskeki(start):
    import os
    port = os.environ['FUSEKI_PORT']
    query = """
    CONSTRUCT {?s ?p ?o}
    WHERE {
        ?s ?p ?o
    }
    """
    endpoint = get_worker_fuseki_endpoint()
    data = read(endpoint, query)
    for s, p, o in data:
        print("%s %s %s" % (s, p, o))


def read(endpoint, query):
    from SPARQLWrapper import SPARQLWrapper, XML
    print("Querying into endpoint %s" % endpoint)
    sparql = SPARQLWrapper("%s/query" % endpoint)
    sparql.setQuery(query)
    sparql.setReturnFormat(XML)
    results = sparql.query()
    data = results.convert()

    return data

@task()
def start():
    print("Starting the execution")

if __name__ == "__main__":

    start()
    global_endpoints = []
    global_endpoints.append("http://localhost:3030/Data1")

    test_query = "CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}"
    data = get_consolidated_triplets(test_query, global_endpoints)
    print("Query successful for endpoints:\n%s" % global_endpoints)
    data = compss_wait_on(data)
    print("Found data:")
    for s, p, o in data:
        print("%s %s %s" % (s, p, o))


    lock = insert_to_local_fuseki(data)
    # lock = insert_bullshit_to_local_fuseki()
    read_from_local_fuskeki(lock)
