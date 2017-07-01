#!/bin/bash
set -e

# Change this to match the path to your (unzipped) biocaddie benchmark dataset
dataset_directory=$HOME/update_json_folder 

echo 'Started indexing!'
for docid in {1..790000}
do
    if [ "$1" == "-vvvv" ]; then  
        echo "Indexing document: $docid"
    elif [ "$1" == "-vvv" -a "$(expr $docid % 10)" == "0" ]; then
        echo "Indexing document: $docid"
    elif [ "$1" == "-vv" -a "$(expr $docid % 100)" == "0" ]; then
        echo "Indexing document: $docid"
    elif [ "$1" == "-v" -a "$(expr $docid % 1000)" == "0" ]; then
        echo "Indexing document: $docid"
    elif [ "$1" != "-q" -a "$$(expr $docid % 100000)" == "0" ]; then
        echo "Indexing document: $docid"
    fi

    curl --silent -u elastic:changeme -XPUT --header 'Content-Type: application/json' localhost:9200/biocaddie/dataset/$docid?pretty -d@$HOME/update_json_folder/$docid.json > /dev/null
done

echo 'Indexing complete!'
