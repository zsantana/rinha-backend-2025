#!/bin/bash

./services.sh stop
./clean_docker_rinha.sh
./build_docker.sh
./services.sh start
./services.sh logs