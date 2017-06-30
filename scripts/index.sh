#!/bin/bash

curl -u elastic:changeme -XPUT --header 'Content-Type: application/json' localhost:9200/biocaddie?pretty -d@biocaddie.json
