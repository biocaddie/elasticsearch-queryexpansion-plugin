#!/bin/bash

if [ "$1" == "search" ]; then 
    curl -u elastic:changeme 'localhost:9200/biocaddie/dataset/_esearch?pretty&query=multiple+sclerosis'
    exit 0
fi

curl -u elastic:changeme 'localhost:9200/biocaddie/dataset/_expand?pretty&query=multiple+sclerosis'
