# All calls from the endpoint should land here, no calls should go directly to datalayer or mllayer etc.

import datalayer
import home_route_gen
import mllayer
import helper_functions as hf
import feature_extraction
import params
import json_helper
import nature_component
import user_stats

import logging
logger = logging.getLogger(__name__)

def register(username,passwd):
    try:
        return datalayer.register(username,passwd)
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}

def login(username,passwd):
    try:
        user_info =  datalayer.login(username,passwd)
        if len(user_info) > 2:
            user_info['sex'] = 'M' if user_info['sex'] == 0 else 'F'
        return user_info
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
def set_preferences(user_id,preferences):
    try:
        preferences['length'] = mllayer.project_length(preferences['length'])
        preferences['elevation'] = mllayer.project_elevation(preferences['elevation'])
        preferences['sex'] = 0 if preferences['sex'] == 'M' else 1
        return datalayer.set_preferences(user_id,preferences)
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
def getRoutes(user_id, location):
    try:
        location = eval(location)
        # get user perferences
        
        user_pref = datalayer.get_preferences(user_id)
        length = hf.back_project_length(user_pref['length'])
        
        # routes here is a list of list of osmids(co-ordinates)
        routes, graph = home_route_gen.get_home_routes(location, length,'PREVIEW')
        routes_info = feature_extraction.extract_routes_features(graph,routes)
        
        best_route_dict = mllayer.get_favourable_routes(user_pref,routes_info,params.ROUTES_COUNT)
            
        geo_json = json_helper.geo_jsonify(graph,routes,best_route_dict)
        
        # Add nature information for each co-ordinate in the geo_json
        return nature_component.add_nature_info(geo_json)
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
def getCustomRoutes(location, preferences):
    try:
        location = eval(location)
        routes, graph = home_route_gen.get_home_routes(location,preferences['length'],'PREVIEW')
        routes_info = feature_extraction.extract_routes_features(graph,routes)
        
        best_route_dict = mllayer.get_favourable_routes(preferences,routes_info,params.ROUTES_COUNT)
            
        geo_json = json_helper.geo_jsonify(graph,routes,best_route_dict)
    
        # Add nature information for each co-ordinate in the geo_json
        return nature_component.add_nature_info(geo_json)
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}

    
def feedback(user_id,route_features):
    try:
        route_features['length'] = mllayer.project_length(route_features['length'])
        route_features['elevation'] = mllayer.project_elevation(route_features['elevation'])
        return datalayer.update_preferences(user_id, route_features)
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
    
###############################################################################
################    BIG MASTER PROJECT CHANGES   ##############################

# Gets the information of the current run
# And returns the badges obtained with their expiry dates

def dispatch_run_info(user_id,run_info):
    try:
        # Add the run to the run history
        run_info['xp'] = user_stats.xp_calculation(run_info['distance'],run_info['elevation'],run_info['time_taken'])
        datalayer.update_run_history(user_id,run_info)
        
        week_stats = datalayer.get_weekly_stats(user_id)
        badges_info = datalayer.get_badge_info()
        
        logger.info(run_info['run_finished'])
        badges_dict = user_stats.badges_calculation(week_stats,badges_info, run_info['run_finished'])
        
        # Update user overall stats
        datalayer.update_user_records(user_id,run_info)
        datalayer.update_user_stats(user_id,run_info)
        
        before_badges = user_stats.get_nonexpired_badges(user_id)
        datalayer.update_user_badges(user_id,badges_dict)
        after_badges = user_stats.get_nonexpired_badges(user_id)
        
        return user_stats.get_new_obtained_badges(before_badges,after_badges)
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
# Last week stats of the user    
def get_profile_stats(user_id):
    try:
        week_stats = datalayer.get_sum_run_history(user_id, params.PAST_DAYS)
        
        return user_stats.collect_user_past7days_stats(user_id,week_stats)
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}

   
def get_overall_stats(user_id):
    try:
        final_stats = {}
        overall_stats = user_stats.collect_user_overall_stats(user_id)
        final_stats['overall_stats'] = overall_stats
        
        final_stats['runs'] = datalayer.get_recent_runs(user_id,params.LAST_ROWS_COUNT)
        return final_stats
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
    
#################### INIT THE SETUP, DATABASE #########################

def init():
    try:
        return datalayer.init()
    except Exception as e:
        logger.error(str(e))
        return params.FAILURE