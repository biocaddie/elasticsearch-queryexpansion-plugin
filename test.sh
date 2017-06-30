#!/bin/bash

curl -u elastic:changeme 'localhost:9200/biocaddie/dataset/_expand?pretty&query=Hello'
