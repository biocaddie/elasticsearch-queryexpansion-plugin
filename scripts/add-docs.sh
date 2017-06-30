#!/bin/bash

curl -u elastic:changeme -XPUT --header 'Content-Type: application/json' localhost:9200/biocaddie/dataset/1?pretty -d@1.json && \
curl -u elastic:changeme -XPUT --header 'Content-Type: application/json' localhost:9200/biocaddie/dataset/2?pretty -d@2.json
