cd build/jigasi/
mvn clean install -Dassembly.skipAssembly=false -DskipTests
docker build -t linto-jigasi .
