#!/bin/bash
#
# Build jedibot locally using git, wget, javac commands and then run it.
#
/usr/bin/git pull
if [ $? -ne 0 ]
then
  /bin/echo -e "git pull failed.\n\nMake sure you have cloned jedibot and you are in its directory."
  /bin/echo -e "Example:\n\ngit clone https://github.com/pla1/jedibot.git\n./run.sh"
  exit -1
fi
urls="https://jsoup.org/packages/jsoup-1.12.1.jar \
https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.6/gson-2.8.6.jar \
https://repo1.maven.org/maven2/org/apache/derby/derbyclient/10.15.1.3/derbyclient-10.15.1.3.jar \
https://repo1.maven.org/maven2/org/apache/derby/derbytools/10.15.1.3/derbytools-10.15.1.3.jar \
https://repo1.maven.org/maven2/org/apache/derby/derbyshared/10.15.1.3/derbyshared-10.15.1.3.jar \
https://repo1.maven.org/maven2/org/apache/derby/derby/10.15.1.3/derby-10.15.1.3.jar"
for url in $urls
do
  /bin/echo "$url"
  fileName="${url##*/}"
  if [ ! -f $fileName ]
  then
    /usr/bin/wget "$url"  --output-document="$fileName"
  fi
done

javac -encoding UTF-8 -cp .:* src/main/java/social/pla/jedibot/*.java
java -cp src/main/java:.:* social.pla.jedibot.Main


