#!/bin/bash

docker run --name=elastic-qe-5.3.2 -it -d -p 9200:9200 -v /Users/lambert8/workspace/queryexpansion/:/plugin-src/ -v $HOME/es-5.3.2-data:/usr/share/elasticsearch/data -e "http.host=0.0.0.0" -e "transport.host=127.0.0.1" docker.elastic.co/elasticsearch/elasticsearch:5.3.2
