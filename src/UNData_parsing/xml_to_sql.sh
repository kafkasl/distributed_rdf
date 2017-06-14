#!/bin/bash

/usr/sbin/rcmysql start
sleep 2s

files=$( ls xmlFiles )
counter=0
for i in $files ; do
  echo "Treating file "$i
  name=$(echo $i | sed 's/\.xml$//')
  pathToXML=$(pwd)"/$i"
  java -jar ConverterXmlToSQL.jar $pathToXML $1 $2 $3
  configName=$(pwd)"/"${name}".properties"
  rm $configName
  cat r2rml.properties > $(echo $configName)
  echo "db.url="$1 >> $(echo $configName)
  echo "db.login="$2 >> $(echo $configName)
  echo "db.password="$3 >> $(echo $configName)
  echo "db.driver=com.mysql.jdbc.Driver" >> $(echo $configName)
  echo "mapping.file=$(pwd)/configFiles/${name}.rdf" >> $(echo $configName)
  echo "jena.tdb.directory=customRDF/${name}" >> $(echo $configName)
  #echo $configName
  java -cp "./lib/*;" -jar lib/r2rml-parser-0.8.jar -p ${configName}
  #rm $(echo $configName)
done

