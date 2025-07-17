#!/bin/bash

./services.sh stop
./build_docker.sh
./services.sh start
./services.sh logs