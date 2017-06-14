from pycompss.api.task import task
from pycompss.api.parameter import *
from pycompss.api.api import compss_wait_on
from rdflib import Graph
from rdflib.util import guess_format


import argparse


QUERY_SEPARATOR = "-----"

def levenshtein(seq1, seq2):
    oneago = None
    thisrow = range(1, len(seq2) + 1) + [0]
    for x in xrange(len(seq1)):
        twoago, oneago, thisrow = oneago, thisrow, [0] * len(seq2) + [x + 1]
        for y in xrange(len(seq2)):
            delcost = oneago[y] + 1
            addcost = thisrow[y - 1] + 1
            subcost = oneago[y - 1] + (seq1[x] != seq2[y])
            thisrow[y] = min(delcost, addcost, subcost)
    return thisrow[len(seq2) - 1]


def replace(s1, s2, disambiguations):
    p = disambiguations.predicates(s=s1, o=s2)
    p2 = disambiguations.predicates(s=s2, o=s1)
    if p == "owl:sameAs" or p2 == "owl:sameAs":
        return True
    if p == "owl:differentFrom" or p2 == "owl:differentFrom":
        return False

    seq1, seq2 = get_country_name(s1).lower(), get_country_name(s2).lower()
    if seq1 in seq2:
        return True
    if seq2 in seq1:
        return True
    if levenshtein(seq1, seq2) < 4:
        return True
    return False

def get_country_name(subject):
    if "#" in subject:
        return subject.split("#")[-1]
    else:
        return subject.split("/")[-1]


def choose_replacement(s1, s2):
    if "dbpedia" in s1:
        return s2, s1
    else:
        return s1, s2


def merge_graphs(g1, g2, disambiguations):
    from rdflib import URIRef
    t = URIRef("rdf:type")
    o = URIRef("dbo:Country")

    mappings = {}
    for s1 in g1.subjects(predicate=t, object=o):
        for s2 in g2.subjects(predicate=t, object=o):
            if replace(s1, s2, disambiguations):
                key, replacement = choose_replacement(s1, s2)
                mappings[key] = replacement

    merged_graph = Graph()

    for s, p, o in g1:
        if s in mappings:
            merged_graph.add((mappings[s], p, o))
        else:
            merged_graph.add((s, p, o))

    del g1

    for s, p, o in g2:
        if s in mappings:
            merged_graph.add((mappings[s], p, o))
        else:
            merged_graph.add((s, p, o))

    del g2

    return merged_graph




def get_worker_fuseki_endpoint():
    import os
    port = os.environ['FUSEKI_PORT']
    return "http://localhost:%s/compssW" % port


def join_triplets(data):
    result = Graph()
    for graph in data:
        result += graph
    return result


@task(returns=Graph)
def get_consolidated_triplets(query, source_endpoints):
    data = []
    for i, endpoint in enumerate(source_endpoints):
        data.append(do_query(endpoint, query))

    joined_graph = join_triplets(data)

    return joined_graph


def insert(endpoint, query):
    from SPARQLWrapper import SPARQLWrapper
    sparql = SPARQLWrapper("%s/update" % endpoint)
    sparql.setQuery(query)
    sparql.method = 'POST'
    sparql.query()


@task(returns=Graph)
def do_global_query(query, queries, disambiguations_file, *inputs):
    # Load disambiguation rules if present
    disambiguations = Graph()
    disambiguations.parse(disambiguations_file, format=guess_format(disambiguations_file))

    g = inputs[0]
    for i in range(1, len(inputs)):
        g = merge_graphs(g, inputs[i], disambiguations)

    # Check if all arguments are in local Fuseki graph
    for i in range(0, len(inputs)):
        if not available_in_local(queries[i]):
            print("Data was not in local Fuseki, proceeding to add it.")
            insert_to_local(g)
        else:
            print("Data was already in local Fuseki")
    # Query local Fuseki
    return query_local_fuseki(query)


def available_in_local(query):
    available = True
    endpoint = get_worker_fuseki_endpoint()
    data = do_query(endpoint, query)

    it = data.objects()
    try:
        it.next()
    except StopIteration:
        available = False

    return available


def insert_to_local(input_data):
    endpoint = get_worker_fuseki_endpoint()

    query = "INSERT DATA {"
    for s, p, o in input_data:
        query += "%s %s %s." % (s.n3(), p.n3(), o.n3())

    query += "}"
    print("Insert query to be submitted to %s:\n[%s]" % (endpoint, query))
    insert(endpoint, query)


def query_local_fuseki(query):
    endpoint = get_worker_fuseki_endpoint()

    return do_query(endpoint, query)


def do_query(endpoint, query):
    from SPARQLWrapper import SPARQLWrapper, XML
    sparql = SPARQLWrapper("%s/query" % endpoint)
    sparql.setQuery(query)
    sparql.setReturnFormat(XML)
    results = sparql.query()
    data = results.convert()

    return data


def build_subquery(sub, pred, obj, literals=False):

    if literals:
        query = "CONSTRUCT {%s ?p ?o} WHERE { %s ?p ?o . %s a %s} FILTER(isLiteral(?o)). " %\
                (sub, sub, sub, obj)
    else:
        query = "CONSTRUCT {%s %s %s} WHERE {%s %s %s}" % (sub, pred, obj, sub, pred, obj)

    print(" * Subquery build for [%s, %s, %s] is: \n   - %s" % (sub, pred, obj, query))
    return query


def partition_query(query):
    queries = []
    try:
        subq = [tuple(e.strip().split()) for e in
                   query.strip().split("WHERE")[1].strip().split("{")[1].strip().split("}")[0].split(" .")
                   if e.strip()]
        print("SUBQ LIST: %s" % subq)

    except IndexError:
        print("[ERROR] Query format not recognized (Did you miss WHERE statement?, all where clauses need to"
              " be fully formed and dot-separated.\nOffending query:\n%s" % query)
        return queries

    for (s, p, o) in subq:
        queries.append(build_subquery(s, p, o))
        if p in ["a", "rdf:type", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"]:
            queries.append(build_subquery(s, "?p", "?o", literals=True))

    return queries


def process_queries():

    # Arguments parsing
    parser = argparse.ArgumentParser()
    parser.add_argument('-e', '--end_points', type=str, help="Comma-separated available Fuseki endpoints "
                        "containing graph data.", required=False, default="http://localhost:3030/Data1")
    parser.add_argument('-q', '--queries_file', type=str, help="File containing the queries to be performed "
                        "(each separated by '-----').", required=True)
    parser.add_argument('-o', '--output_path', type=str, help="Path were query results should be stored ("
                        "printed to stdout if not specified) ", required=False, default=None)
    parser.add_argument('-d', '--disambiguations_file', type=str, help="File containing disambiguations for"
                        "entity resolution  ", required=False, default=None)

    args = parser.parse_args()
    end_points =  args.end_points
    queries_file = args.queries_file
    output_path = args.queries_file
    disambiguations_file = args.disambiguations_file

    # Endpoints where the information resides
    global_endpoints = [e for e in end_points.split(",") if e]

    with open(queries_file) as f:
        global_queries = [q.strip() for q in f.read().split(QUERY_SEPARATOR) if q]
    print("Found %s queries" % len(global_queries))
    for q in global_queries:
        print("-------------------------------------\n%s" % q)


    results = [0] * len(global_queries)
    # Dictionary containing query->value pairs
    query_to_value = {}

    for q_index, query in enumerate(global_queries):
        # Split query and request triplets for global query
        sub_queries = partition_query(query)

        if not sub_queries:
            print("[SKIPPING QUERY] Partitioned queries set is empty, check query format.")
            continue

        queries = [0] * len(sub_queries)
        inputs = [0] * len(sub_queries)
        for i, sub_query in enumerate(sub_queries):
            if not sub_query in query_to_value:
                query_to_value[sub_query] = get_consolidated_triplets(sub_query, global_endpoints)

            inputs[i] = query_to_value[sub_query]
            queries[i] = sub_query

        results[q_index] = do_global_query(query, queries, disambiguations_file, *inputs)

    print(" - All queries processed. Waiting for results...")
    results = compss_wait_on(results)
    if output_path:
        for i, res in enumerate(results):
            with open("%s/%s.out" % (output_path, i)) as f:
                f.write("%s\n" % res)
    else:
        for i, res in enumerate(results):
            print("\t* Result for query\n\t%s" % global_queries[i])
            for s, p, o in res:
                print("\t%s %s %s" % (s, p, o))


if __name__ == "__main__":

    from time import time

    start = time()
    process_queries()
    end = time()

    print("Total elapsed time: %s" % (end - start))


