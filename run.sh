#!/bin/bash
#
# Build jedibot locally using git, wget, javac commands and then run it.
#
git pull
if [ $? -ne 0 ]
then
  echo -e "git pull failed.\n\nMake sure you have cloned jedibot and you are in its directory."
  echo -e "Example:\n\ngit clone https://github.com/pla1/jedibot.git\n./run.sh"
  exit -1
fi
jsoup="jsoup-1.12.1.jar"
gson="gson-2.8.6.jar"
if [ ! -f "$jsoup" ]
then
  echo "Downloading $jsoup."
  wget 'https://jsoup.org/packages/jsoup-1.12.1.jar' --output-document="$jsoup"
fi
if [ ! -f "$gson" ]
then
  echo "Downloading $gson."
  wget 'https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.6/gson-2.8.6.jar' --output-document="$gson"
fi
javac  -encoding UTF-8 -cp .:* src/main/java/social/pla/jedibot/*.java
java -cp src/main/java:.:* social.pla.jedibot.Main

