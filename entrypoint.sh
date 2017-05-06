#!/bin/bash

# TODO: Start elasticsearch with the default command

# Load our custom plugin
bin/elasticsearch-plugin install file://${PLUGIN_PATH}
