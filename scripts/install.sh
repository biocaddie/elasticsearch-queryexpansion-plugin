#!/bin/bash

docker exec -it elastic-qe-5.3.2 bin/elasticsearch-plugin install file:///plugin-src/target/releases/queryexpansion-5.3.2-SNAPSHOT.zip
