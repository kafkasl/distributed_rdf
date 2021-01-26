# Distributed RDF project

This project implements a distributed RDF data lake. Its purpose is to be able to have many data nodes containing RDF graphs, which can be different from sources or smaller partitions of the global (which may be too big to fit in a single node), that can be queried in a regular fashion.

The integration will be the main focus of the project describing the mappings in RDF and allowing distributed SPARQL queries without needing to merge all datasets into a single source. The only requirement for the distributed queries is that the user must explicitly define the class of subjects and objects of the query. 
  
## More

For more information read the report `report/OD final report.pdf`

# Execution

In order to run distributed rdf code a modified version of COMPSs is required. Email us if you are interested:

pol.alvarez@bsc.es
ramon.amela@bsc.es
