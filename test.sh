#!/bin/bash

# Point to a specific instance of elasticsearch (defaults to Docker instance)
TEST_HOST="localhost"
TEST_PORT="9200"
TEST_USERNAME="elastic"
TEST_PASSWORD="changeme"

# Specify expansion / search parameters
SEARCH_INDEX="biocaddie"
SEARCH_TYPE="dataset"
TEST_QUERY="multiple+sclerosis"
EXPAND_ON="biocaddie"
STOP_LIST="a+an+the+and+or+of+from+on+was+to+is+-+were+at+as+we"

# Override additional parameters here
ADDITIONAL_ARGS="&fbTerms=20&fbDocs=50"

# If search was specified, use the _esearch endpoint
if [ "$1" == "search" ]; then 
    curl -u "${TEST_USERNAME}:${TEST_PASSWORD}" ${TEST_HOST}:${TEST_PORT}/${SEARCH_INDEX}/${SEARCH_TYPE}/_esearch'?pretty&query='${TEST_QUERY}${ADDITIONAL_ARGS}'&stoplist='${STOP_LIST}'&expandOn='${EXPAND_ON}
    exit 0
fi

# Otherwise, just run Rocchio and return the expanded query
curl -u "${TEST_USERNAME}:${TEST_PASSWORD}" ${TEST_HOST}:${TEST_PORT}/${EXPAND_ON}/${SEARCH_TYPE}/_expand'?pretty'${ADDITIONAL_ARGS}'&query='${TEST_QUERY}
