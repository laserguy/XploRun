# Running this file might give some errors
# Just run the command 'docker ps' after running this script to check if containers were created
# Check docker logs PYTHON_ENDPOINT/MYSQL/CRON_SERVER (one of these 3), in case of any errors

from os.path import dirname, abspath
import platform
import subprocess

parent_directory = dirname(dirname(abspath(__file__)))

if platform.system() == 'Windows':
    backend_directory = parent_directory + '\\backend'
    sql_data_dir = parent_directory +'\docker\mysql\sql_data'
else:
    backend_directory = parent_directory + '/backend'
    sql_data_dir = parent_directory +'/docker/mysql/sql_data'

print(backend_directory)

# Enter your docker_id if you want to push the docker to the docker hub, else don't change
DOCKER_ID = 'mahtovivek741'

# BUILD MYSQL IMAGE
# All the space characters are important here, !!! DON'T REMOVE THEM
MYSQL_TAG_NAME = DOCKER_ID + '/xplorun-mysql'
DOCKER_CONTEXT = ' ./mysql'
command = 'docker build -t ' + MYSQL_TAG_NAME + DOCKER_CONTEXT
print(command)
subprocess.run(command)

# BUILD PYTHON ENDPOINT IMAGE
MAIN_TAG_NAME = DOCKER_ID + '/service-main'
DOCKER_CONTEXT = ' ./service-main'
command = 'docker build -t ' + MAIN_TAG_NAME + DOCKER_CONTEXT
print(command)
subprocess.run(command)

# BUILD CRON SERVER IMAGE
CRON_TAG_NAME = DOCKER_ID + '/cron-server'
DOCKER_CONTEXT = ' ./cron-server'
command = 'docker build -t ' + CRON_TAG_NAME + DOCKER_CONTEXT
print(command)
subprocess.run(command)

# Creates a network which will bind these containers together, if network "xplo-network" already exists
# then throws the error, the error will cause no trouble to the flow
DOCKER_NETWORK = 'xplo-network'
command = 'docker network create --driver bridge ' + DOCKER_NETWORK
print(command)
subprocess.run(command)

# This command might throw error if containers not exists already, but not to worry
command = 'docker stop MYSQL PYTHON_ENDPOINT CRON_SERVER'
print(command)
subprocess.run(command)

# Remove the containers, in case they were already created, if not created then it will give an error
# The error causes no issue
command = 'docker rm MYSQL PYTHON_ENDPOINT CRON_SERVER'
print(command)
subprocess.run(command)

# RUN MYSQL CONTAINER
# Don't change the DOCKER_NAME, as that is being used here for the connection between containers
ROOT_PRIVILIGES = '0'    #starts docker as a root user (Important)
DOCKER_NAME = 'MYSQL'
PORT_MAPPING = '3306:3306'   #<machine_port>:<container_port>
VOLUME_MAPPING = sql_data_dir + ':/var/lib/mysql'
command = 'docker run -u ' + ROOT_PRIVILIGES + ' -d --name ' + DOCKER_NAME + ' -p ' + PORT_MAPPING + ' -v ' + VOLUME_MAPPING +  ' --network ' + DOCKER_NETWORK + ' ' + MYSQL_TAG_NAME
print(command)
subprocess.run(command)

# RUN PYTHON_ENDPOINT CONTAINER
DOCKER_NAME = 'PYTHON_ENDPOINT'
PORT_MAPPING = '5000:5000'
VOLUME_MAPPING = backend_directory + ':/app'
command = 'docker run -u ' + ROOT_PRIVILIGES + ' -d --name ' + DOCKER_NAME + ' -p ' + PORT_MAPPING + ' -v ' + VOLUME_MAPPING + ' --network ' + DOCKER_NETWORK + ' ' + MAIN_TAG_NAME
print(command)
subprocess.run(command)

# CRON_SERVER CONTAINER
DOCKER_NAME = 'CRON_SERVER'
VOLUME_MAPPING = backend_directory + ':/app'
command = 'docker run -u ' + ROOT_PRIVILIGES + ' -d --name ' + DOCKER_NAME + ' -v ' + VOLUME_MAPPING + ' --network ' + DOCKER_NETWORK + ' ' + CRON_TAG_NAME
print(command)
subprocess.run(command)



