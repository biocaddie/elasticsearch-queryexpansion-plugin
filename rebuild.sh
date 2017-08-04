#!/bin/bash

scripts/remove.sh; scripts/build.sh && scripts/install.sh && scripts/restart.sh && ./logs.sh
