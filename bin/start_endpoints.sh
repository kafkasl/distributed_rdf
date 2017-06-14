#!/bin/bash


export FUSEKI_HOME=$HOME/projects/distributed_rdf/resources/jena-fuseki
export FUSEKI_PORT="3030"


nohup ${FUSEKI_HOME}/fuseki-server --update --port="${FUSEKI_PORT}" --loc=../data/endpoint_1/GrossDomesticProduct /Data1 > ../log/endpoint1.out 2> ../log/endpoint1.err &
