#!/bin/bash

#/usr/sbin/rcmysql start
#sleep 2s

alreadyUp=0
UP=$(pgrep mysql | wc -l);
if [ "$UP" -ne 1 ];
then
  echo "Starting MySQL.";
  sudo /usr/sbin/rcmysql start
else
  alreadyUp=1
  echo "MySQL is already up.";
fi

turtle=true

files=$( ls $1 )
counter=0
for i in $files ; do
  echo "Treating file "$i
  name=$(echo $i | sed 's/\.xml$//')
  pathToXML="$1/$i"
  #java -jar ConverterXmlToSQL.jar $pathToXML $4 $5 $6
  configName=$(pwd)"/"${name}".properties"
  rm -rf $configName
  #cat ./r2rml-parser/r2rml.properties > $(echo $configName)
  echo "mapping.file.type=TURTLE" >> $(echo $configName)
  echo "default.namespace=http://example.com/base#" >> $(echo $configName)
  echo "default.verbose=false" >> $(echo $configName)
  echo "default.log=status.rdf" >> $(echo $configName)
  echo "default.forceURI=true" >> $(echo $configName)
  echo "default.incremental=false" >> $(echo $configName)
  if [ $turtle ];
  then
    echo "Ouput format: TURTLE"
    echo "jena.destinationFileSyntax=TURTLE" >> $(echo $configName)
    echo "jena.showXmlDeclaration=false" >> $(echo $configName)
  else
    echo "Ouput format: TDB" 
    echo "jena.storeOutputModelUsingTdb=false" >> $(echo $configName)
    echo "jena.cleanTdbOnStartup=true" >> $(echo $configName)
  fi
  echo "jena.encodeURLs=true" >> $(echo $configName)
  echo "db.url="$4 >> $(echo $configName)
  echo "db.login="$5 >> $(echo $configName)
  echo "db.password="$6 >> $(echo $configName)
  echo "db.driver=com.mysql.jdbc.Driver" >> $(echo $configName)
  echo "mapping.file=$2/${name}.rdf" >> $(echo $configName)
  echo "jena.tdb.directory=$3/${name}" >> $(echo $configName)
  echo "jena.destinationFileName=$3/${name}.rdf" >> $(echo $configName)
  java -cp "./r2rml-parser/*;" -jar ./r2rml-parser/r2rml-parser-0.8.jar -p ${configName}
  #rm $(echo $configName)
  #cat $(echo "$2/${name}.rdf")
  #predicates=$(cat $(echo "$2/${name}.rdf") | grep "predicate " | tr -d ";" | awk '{ print $2 }')
  #for pred in $predicates; do
  #  echo "$pred a rdf:Property ." >> $(echo "$3/${name}.rdf")
  #done
done

if [ "$alreadyUp" -ne 1 ];
then
  echo "Stopping MySQL."
  sudo /usr/sbin/rcmysql stop
fi
