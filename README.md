# elasticsearch-queryexpansion-plugin
Work in Progress - A simple ElasticSearch plugin for exploration, which hopefully evolves into an implementation of QE in ElasticSearch

# Prerequisites
* Docker (recommended) or Maven
* A running instance of ElasticSearch

## Docker
To easily start up an ElasticSearch container in Docker:
```bash
docker run --name=elastic-qe -d -p 9200:9200 -v $(pwd)/target:/plugins  -e "http.host=0.0.0.0" -e "transport.host=127.0.0.1" docker.elastic.co/elasticsearch/elasticsearch:5.3.2
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
bash-4.3$ curl -u elastic:changeme localhost:9200/_hello/Mike --header "Content-Type: application/json" -vvv
*   Trying 127.0.0.1...
* Connected to localhost (127.0.0.1) port 9200 (#0)
* Server auth using Basic with user 'elastic'
> GET /_hello/Mike HTTP/1.1
> Host: localhost:9200
> Authorization: Basic ZWxhc3RpYzpjaGFuZ2VtZQ==
> User-Agent: curl/7.47.0
> Accept: */*
> Content-Type: application/json
> 
< HTTP/1.1 200 OK
< content-type: application/json; charset=UTF-8
< content-length: 25
< 
* Connection #0 to host localhost left intact
{"message":"Hello Mike!"}
```

# TODO
* Write some unit / integration tests
* Publish release artifacts so users don't need to build the source by hand
