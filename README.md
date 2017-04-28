# elasticsearch-queryexpansion-plugin
Work in Progress - A simple ElasticSearch plugin for exploration, which hopefully evolves into an implementation of QE in ElasticSearch

# Prerequisites
* Docker (recommended) or Maven
* A running instance of ElasticSearch

## Docker
To easily start up an ElasticSearch container in Docker:
```bash
docker run --name=elastic-qe -d -p 8000:9200 -v $(pwd)/target:/plugins  -e "http.host=0.0.0.0" -e "transport.host=127.0.0.1" docker.elastic.co/elasticsearch/elasticsearch:5.3.2
docker exec -it elastic-qe bash
```

# Usage
For now, cloning the source is required to run the plugin (see TODOs):
```bash
git clone bodom0015/elasticsearch-queryexpansion-plugin
```

## Build
To build the source using Maven:
```bash
mvn package
```

To build the source using Docker:
```bash
docker run -it -v $(pwd):/workspace maven:3-jdk-8
```

## Load
Start up an ElasticSearch instance (or use an existing one).

Then load the `file:///full/path/to/queryexpansion-5.3.2-SNAPSHOT.zip` into ElasticSearch
```bash
bash-4.3$ bin/elasticsearch-plugin install file:///plugins/queryexpansion-5.3.2-SNAPSHOT.zip
-> Downloading file:///plugins/queryexpansion-5.3.2-SNAPSHOT.zip
[=================================================] 100%   
-> Installed queryexpansion
```

## Test
Currently broken, but still making progress:
```bash
bash-4.3$ curl -u elastic:changeme localhost:9200/_hello
{"error":{"root_cause":[{"type":"illegal_argument_exception","reason":"No endpoint or operation is available at [_hello]"}],"type":"illegal_argument_exception","reason":"No endpoint or operation is available at [_hello]"},"status":400}
```
# TODO
* Get the REST endpoint working
* Write some unit / integration tests
* Publish release artifacts so users don't need to build the source by hand
