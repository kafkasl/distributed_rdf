=================
COMP Superscalar
=================

COMP Superscalar (COMPSs) is a framework which aims to ease the development and execution of applications for distributed infrastructures, such as Clusters, Grids and Clouds.

Overview
-------------
The COMP Superscalar (COMPSs) framework is mainly composed of a programming model which aims to ease the development of applications for distributed infrastructures, such as Clusters, Grids and Clouds and a runtime system that exploits the inherent parallelism of applications at execution time. The framework is complemented by a set of tools for facilitating the development, execution monitoring and post-mortem performance analysis.

Official web page: http://compss.bsc.es

Documentation
-------------
COMPSs documentation can be found at http://compss.bsc.es (Documentation tab)

* COMPSs_Installation_Manual.pdf

* COMPSs_User_Manual_App_Development.pdf
* COMPSs_User_Manual_App_Execution.pdf

* COMPSs_MareNostrum_Manual.pdf
* Tracing_Manual.pdf

* COMPSs_Developer_Manual.pdf

Please, pay special attention to the "PIP" section in the installation manual.

Installation
-------------
First, be sure that the target machine satisfies the mentioned dependencies on
the installation manual.

The installation can be done in various different ways.

1) Use PIP to install the official COMPSs version from the pypi live repository:
sudo -E python2.7 -m pip install compss -v
2) Use PIP to install COMPSs from a compss .tar.gz
sudo -E python2.7 -m pip install compss-version.tar.gz -v
3) Use the setup.py script
sudo -E python2.7 setup.py install



*******************************************
** Department of Computer Science **
** Barcelona Supercomputing Center **
*******************************************  