#!/bin/bash

# Point to a specific instance of elasticsearch (defaults to Docker instance)
TEST_HOST="localhost"
TEST_PORT="9200"
TEST_USERNAME="elastic"
TEST_PASSWORD="changeme"

# Specify expansion / search parameters
TEST_INDEX="biocaddie"
SEARCH_TYPE="dataset"
TEST_QUERY="multiple+sclerosis"
STOP_LIST="a+an+the+and+or+of+from+on+was+to+is+-+were+at+as+we"

# Override additional parameters here
ADDITIONAL_ARGS="&fbTerms=20&fbDocs=50"

# Otherwise, just run Rocchio and return the expanded query
curl -u "${TEST_USERNAME}:${TEST_PASSWORD}" ${TEST_HOST}:${TEST_PORT}/${TEST_INDEX}/${SEARCH_TYPE}/_expand'?pretty'${ADDITIONAL_ARGS}'&query='${TEST_QUERY}
