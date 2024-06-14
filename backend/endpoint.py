from flask.wrappers import Response
from flask import Flask, request, jsonify, make_response
import orchestrator
import json
import logging.config

app = Flask(__name__)
logging.basicConfig(format='%(asctime)s %(message)s',
                        datefmt='%d/%m/%Y %I:%M:%S %p',
                        filename='main-service.log',
                        filemode='w',
                        level=logging.DEBUG)

# This variable is being used for assistance in cry for help.
# The Database init did not work (orchestrator.init()) at this line
# Neither did it work in __main__, just before app.run()
# At above places we get the error of MYSQL, "connection refused"
# Only happens with the docker, without docker it works fine
SETUP_DONE = False

# Just parsing the info here, and pass the info the next layer which will do the job

# password sent here should be in hash
@app.route("/api/users/register", methods=['POST'])
def register():
    # The weird part is, it somehow works here, so as a workaround I have kept init part here for now.
    global SETUP_DONE
    if SETUP_DONE == False and orchestrator.init():
        SETUP_DONE = True
    data = request.get_json()
    u = orchestrator.register(data["username"], data["password"])
    if "error" in u:
        if u["error"] == "username taken":
            return make_response(jsonify(u), 409)
        return make_response(jsonify(u), 503)
    return jsonify(u)


@app.route("/api/users/login", methods=["POST"])
def login():
    data = request.get_json()
    u = orchestrator.login(data["username"], data["password"])
    # if u['id'] == 'e':
    #     return make_response(jsonify(u), 402)
    return jsonify(u) if u else make_response(jsonify(u), 500)


# FOr passing the user preferences to the backend, after user logs in for the first time
@app.route("/api/users/set_preferences", methods=["POST"])
def set_preferences():
    data = request.get_json()
    user_id = data['user_id']
    preferences = {}
    preferences['length'] = data['length']
    preferences['uniqueness'] = data['uniqueness']
    preferences['elevation'] = data['elevation']
    preferences['ped_friend'] = data['ped_friend']
    preferences['nature'] = data['nature']
    preferences['sex'] = data['sex']
    preferences['age'] = data['age']
    preferences['height'] = data['height']
    preferences['weight'] = data['weight']
    
    u = orchestrator.set_preferences(user_id,preferences)
    return jsonify(u) if u else make_response(jsonify(u), 404)


@app.route("/api/users/getRoutes", methods=["GET"])
def getRoutes():
    user_id = request.args.get("user_id")
    location = request.args.get("location")
    
    u = orchestrator.getRoutes(user_id,location)
    return u if u else make_response(jsonify(u), 404)


@app.route("/api/users/getCustomRoutes", methods=["POST"])
def getCustomRoutes():
    data = request.get_json()

    preferences = {}
    # The features will be in the request body
    preferences['length'] = data['length']
    preferences['uniqueness'] = data['uniqueness']
    preferences['elevation'] = data['elevation']
    preferences['ped_friend'] = data['ped_friend']
    preferences['nature'] = data['nature']
    
    # Location will be request params
    location = request.args.get("location")
    
    u = orchestrator.getCustomRoutes(location,preferences)
    return u if u else make_response(jsonify(u), 404)


# For now, pass the route which user thumbs up to
@app.route("/api/users/feedback", methods=["POST"])
def feedback():
    data = request.get_json()
    user_id = data['user_id']
    route_features = {}
    route_features['length'] = data['length']
    route_features['uniqueness'] = data['uniqueness']
    route_features['elevation'] = data['elevation']
    route_features['ped_friend'] = data['ped_friend']
    route_features['nature'] = data['nature']
    
    u = orchestrator.feedback(user_id,route_features)
    return jsonify(u) if u else make_response(jsonify(u), 404)

###############################################################################
################    BIG MASTER PROJECT CHANGES   ##############################

@app.route("/api/users/dispatch_run_info", methods=["POST"])
def dispatch_run_info():
    data = request.get_json()
    user_id = data['user_id']
    run_info = {}
    run_info['run_finished'] = data['run_finished']
    run_info['distance'] = data['distance']
    run_info['elevation'] = data['elevation']
    run_info['avg_speed'] = data['avg_speed']
    run_info['max_speed'] = data['max_speed']
    run_info['time_taken'] = data['time_taken']
    # TODO: distribution should be dictionary, containing the distance and speed distribution of nature part.
    # All distribution should be divided into NATURE,RESIDENTIAL AND URBAN_FABRIC
    run_info['nature_D'] = data['nature_D']
    run_info['urb_D'] = data['urb_D']
    run_info['kcal'] = data['kcal']
    
    u = orchestrator.dispatch_run_info(user_id,run_info)
    return jsonify(u)

@app.route("/api/users/get_profile_stats", methods=["GET"])
# The profile stats will give the weekly stats for a user
def get_profile_stats():
    user_id = request.args.get("user_id")
    
    u = orchestrator.get_profile_stats(user_id)
    return jsonify(u) if u else make_response(jsonify(u), 404)


@app.route("/api/users/get_overall_stats", methods=["GET"])
# The profile stats will give the weekly stats for a user
def get_overall_stats():
    user_id = request.args.get("user_id")
    
    u = orchestrator.get_overall_stats(user_id)
    return jsonify(u) if u else make_response(jsonify(u), 404)

##############################################################################

# Sample getRoutes()
@app.route("/api/users/getRoutesTest", methods=["GET"])
def getRoutesTest():
    f = open("./test_data/getRoutes.json")
    route = json.load(f)
    return route

# Sample getCustomRoutes()
@app.route("/api/users/getCustomRoutesTest", methods=["POST"])
def getCustomRoutesTest():
    f = open("./test_data/getCustomRoutes.json")
    route = json.load(f)
    return route


if __name__ == "__main__":
    app.run(debug=True, host='0.0.0.0')  # run our Flask app