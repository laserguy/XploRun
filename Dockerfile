# This Dockerfile is created to create service-main container especially for the Kubernetes
# !!! VERY IMPORTANT !!!
# Not to be used on the host machine
# Only use it for the local Kubernets Cluster or for the GCP
# Don't use it for the local docker deployments, for that use docker folder(check init.py script)

# The whole purpose of creating separate dockers for k8s is because I was not able to map relative paths in kubernetes confi files
# Therefore I'm copying the whole code in the docker

FROM gboeing/osmnx:latest

COPY ./docker/service-main/requirements.txt /app/requirements.txt
WORKDIR "/app"

COPY ./backend ./

RUN pip install --upgrade pip
RUN pip install -r requirements.txt

CMD ["python","endpoint.py"]
