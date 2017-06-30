#!/bin/bash

docker exec -it elastic-qe-5.3.2 bin/elasticsearch-plugin remove queryexpansion
