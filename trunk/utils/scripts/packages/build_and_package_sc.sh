#!/bin/bash -e

  #Get parameters
  package_folder=$1
  
  #Define script variables
  COMPSs_version=2.0.rc1704

  #---------------------------------------------------------------------------------------------------------------------
  #Create packages
  echo -e "\e[0m"
  echo "****************************"
  echo "*** Compile and Package  ***"
  echo "****************************"
  echo -e "\e[0m"

  # Build and deploy package for SC
  cd ${package_folder}/../specs/sc/
  ./buildsc ${COMPSs_version}
  cd -

  #All process OK. Exit
  exit 0

