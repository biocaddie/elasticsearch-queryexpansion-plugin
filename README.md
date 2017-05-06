# elasticsearch-queryexpansion-plugin
Work in Progress - A simple ElasticSearch plugin for exploration, which hopefully evolves into an implementation of QE in ElasticSearch

# Prerequisites
* Docker (recommended) or Maven

## Docker
# Usage
For now, cloning the source is required to run the plugin (see TODOs):
```bash
git clone bodom0015/elasticsearch-queryexpansion-plugin
```

To use:
1. [Build](README.md#build)
2. [Load](README.md#load)
3. [Test](README.md#test)

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

To easily start up an ElasticSearch container in Docker and load the plugin:
```bash
ubuntu@ml $ docker rm -f elastic-qe; docker run --name=elastic-qe -it -d -p 9200:9200 -v /home/ubuntu/plugin.zip:/plugins/plugin.zip  -e "http.host=0.0.0.0" -e "transport.host=127.0.0.1" docker.elastic.co/elasticsearch/elasticsearch:5.3.2 && docker exec -it elastic-qe bin/elasticsearch-plugin install file:///plugins/plugin.zip

bab80bcb5086da014f8c88e424aad637436722435404112b663ac3fac5650b4d
-> Downloading file:///plugins/plugin.zip
[=================================================] 100%   
-> Installed queryexpansion
```

### For now, we need to load the plugin during startup
NOTE - while the above command works, the following does not:
```bash
docker rm -f elastic-qe; docker run --name=elastic-qe -it -d -p 9200:9200 -v /home/ubuntu/plugin.zip:/plugins/plugin.zip  -e "http.host=0.0.0.0" -e "transport.host=127.0.0.1" docker.elastic.co/elasticsearch/elasticsearch:5.3.2 && sleep 15s && docker exec -it elastic-qe bin/elasticsearch-plugin install file:///plugins/plugin.zip
```

It seems the plugin needs to be installed during the initialization phase, as adding a slight delay causes it to fail.


## Test
You should now be able to test the new endpoint via curl:
```bash
bash-4.3$ curl -u elastic:changeme localhost:9200/_hello/Mike -vvv
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

You can check the container logs to see what happened under the covers:
```bash
ubuntu@ml $ docker logs -f elastic-qe
[2017-05-06T04:53:31,511][INFO ][o.e.n.Node               ] [] initializing ...
[2017-05-06T04:53:31,602][INFO ][o.e.e.NodeEnvironment    ] [p00ea4y] using [1] data paths, mounts [[/ (none)]], net usable_space [15gb], net total_space [19.3gb], spins? [possibly], types [aufs]
[2017-05-06T04:53:31,603][INFO ][o.e.e.NodeEnvironment    ] [p00ea4y] heap size [1.9gb], compressed ordinary object pointers [true]
[2017-05-06T04:53:31,607][INFO ][o.e.n.Node               ] node name [p00ea4y] derived from node ID [p00ea4ykQ4mSIgbb8E2MQQ]; set [node.name] to override
[2017-05-06T04:53:31,607][INFO ][o.e.n.Node               ] version[5.3.2], pid[1], build[3068195/2017-04-24T16:15:59.481Z], OS[Linux/4.4.0-22-generic/amd64], JVM[Oracle Corporation/OpenJDK 64-Bit Server VM/1.8.0_121/25.121-b13]
[2017-05-06T04:53:33,293][INFO ][o.e.p.PluginsService     ] [p00ea4y] loaded module [aggs-matrix-stats]
[2017-05-06T04:53:33,293][INFO ][o.e.p.PluginsService     ] [p00ea4y] loaded module [ingest-common]
[2017-05-06T04:53:33,293][INFO ][o.e.p.PluginsService     ] [p00ea4y] loaded module [lang-expression]
[2017-05-06T04:53:33,294][INFO ][o.e.p.PluginsService     ] [p00ea4y] loaded module [lang-groovy]
[2017-05-06T04:53:33,294][INFO ][o.e.p.PluginsService     ] [p00ea4y] loaded module [lang-mustache]
[2017-05-06T04:53:33,294][INFO ][o.e.p.PluginsService     ] [p00ea4y] loaded module [lang-painless]
[2017-05-06T04:53:33,294][INFO ][o.e.p.PluginsService     ] [p00ea4y] loaded module [percolator]
[2017-05-06T04:53:33,294][INFO ][o.e.p.PluginsService     ] [p00ea4y] loaded module [reindex]
[2017-05-06T04:53:33,295][INFO ][o.e.p.PluginsService     ] [p00ea4y] loaded module [transport-netty3]
[2017-05-06T04:53:33,295][INFO ][o.e.p.PluginsService     ] [p00ea4y] loaded module [transport-netty4]
[2017-05-06T04:53:33,296][INFO ][o.e.p.PluginsService     ] [p00ea4y] loaded plugin [queryexpansion]    <-----
[2017-05-06T04:53:33,296][INFO ][o.e.p.PluginsService     ] [p00ea4y] loaded plugin [x-pack]
[2017-05-06T04:53:36,194][INFO ][o.n.e.q.RestQueryExpansionAction] Plugin loaded!                       <-----
[2017-05-06T04:53:36,216][INFO ][o.e.n.Node               ] initialized
[2017-05-06T04:53:36,216][INFO ][o.e.n.Node               ] [p00ea4y] starting ...
...
[2017-05-06T04:53:44,940][INFO ][o.n.e.q.RestQueryExpansionAction] Preparing request!                   <-----
[2017-05-06T04:53:44,945][INFO ][o.n.e.q.QueryExpansionTransportAction] Executing transport action!     <-----
[2017-05-06T04:53:44,949][INFO ][o.n.e.q.QueryExpansionResponse    ] Sending response: Hello Mike!      <-----
...
```

# TODO
* Figure out how to load the plugin after startup (it's inconvenient to restart to reload plugin)
* Write some unit / integration tests
* Publish release artifacts so users don't need to build the source by hand
