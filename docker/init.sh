#!/bin/bash

# --- IMPORTANT ---- #
# Only to be run, when backend folder is inside main-service folder

docker build -t mahtovivek741/xplorun-mysql ./mysql
docker build -t mahtovivek741/service-main ./service-main

# Creates a network which will bind these containers together, if network "xplo-network" already exists
# then throws the error, the error will cause no trouble to the flow
docker network create --driver bridge xplo-network

# Remove the containers, in case they were already created, if not created then it will give an error
# The error causes no issue
docker stop MYSQL PYTHON_ENDPOINT 
docker rm MYSQL PYTHON_ENDPOINT

docker run -u 0 -d --name MYSQL -p 3306:3306 --network xplo-network mahtovivek741/xplorun-mysql
docker run -u 0 -d --name PYTHON_ENDPOINT -p 5000:5000 --network xplo-network mahtovivek741/service-main