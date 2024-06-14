# XploRun

## Instructions to run the project:

### Backend

- To run the backend locally
- Have the docker setup ready on the machine
- Please obtain your `google_api_key` and enable elevation apis, then put that in placeholder `GOOGLE_ELEVATION_API_KEY` in `backend/params.py`
- Run file docker/init.py `python init.py`
- It will start the containers.
- backend endpoints will be accessible at `127.0.0.1:5000`

### Frontend

- Follow the osrm_setup.txt guide to setup an instance of the osrm.

- file Params contains default values for our running instance of the backend

- if running locally, set the values:
  - `EXPLORUN_URL` to url of running XploRun backend
  - `OSRM_URL` to url of instance of OSRM


## Sources

- https://openrouteservice.org/
- https://github.com/gboeing/osmnx
- https://osmlanduse.org/
- https://maps.heigit.org/osmlanduse/service
- http://www.overpass-api.de
