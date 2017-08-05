#!/bin/bash

mvn clean package && exit 0 \
    || echo "WARNING: No native Maven installed - using Docker instead" \
        && docker exec -it $(pwd):/workspace -w /workspace maven:3 mvn clean package && exit 0

exit 1
