import helper_functions as hf
import heapq
import traceback
import math

import logging
logger = logging.getLogger(__name__)

# ALL projection functions here will map the values between 0-1

def project_length(length):
    try:
        min_length = hf.preference_ranges['length']['min']
        max_length = hf.preference_ranges['length']['max']
        if length > max_length:
            return 1
        if length < min_length:
            return 0
        return (length - min_length)/(max_length - min_length)
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}
    
def project_elevation(elevation):
    try:
        min_elevation = hf.preference_ranges['elevation']['min']
        max_elevation = hf.preference_ranges['elevation']['max']
        if elevation > max_elevation:
            return 1
        if elevation < min_elevation:
            return 0
        return (elevation - min_elevation)/(max_elevation - min_elevation)
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}
    
def project_ped_friendliness(ped_friend):
    try:
        edges = 0
        ped_friend_edges = 0
        for key in ped_friend:
            edges += ped_friend[key]
            if key in hf.ped_edge_types:
                ped_friend_edges += ped_friend[key]
                
        return ped_friend_edges/edges
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}
    
def project_nature_osm(landuse):
    try:
        total = 0
        nature = 0
        for key in landuse:
            nature += landuse[key]*hf.nature_percent[key]
            total += landuse[key]
        
        return nature/total
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}
 
# Smaller the rank, closer they are    
def similarity(pref1,pref2):
    rank = 0
    for key in pref1:
        rank += abs(pref1[key] - pref2[key])
        
    return rank

def cosine_similarity(dic1,dic2):
    numerator = 0
    dena = 0
    for key1,val1 in dic1.items():
        numerator += val1*dic2.get(key1,0.0)
        dena += val1*val1
    denb = 0
    for val2 in dic2.values():
        denb += val2*val2
    return numerator/math.sqrt(dena*denb)

# Returns the route_ids of the most favourable routes
#  user_pref: Dictionary which contains the user preferences
#  routes: Routes dictionary which contains the extracted features of the route
#  count: Pass the no. of routes required
def get_favourable_routes(user_pref, routes, count):
    try:
        #print(user_pref)
        best_routes = {}
        for route_id in routes:
            route_features = {}
            route_info = routes[route_id]
            route_features['length'] = project_length(route_info['length'])
            route_features['uniqueness'] = route_info['uniqueness']                              #Already between 0-1
            route_features['elevation'] = project_elevation(route_info['uphill'])
            route_features['ped_friend'] = project_ped_friendliness(route_info['ped_friend'])    #dict
            route_features['nature'] = project_nature_osm(route_info['land_cover'])          #dict
            
            try:
                #best_routes[route_id] = similarity(user_pref,route_features)
                best_routes[route_id] = cosine_similarity(user_pref,route_features)
            except Exception as e:
                print(route_info)
                return {'error': str(e)}
            
        # This returns a list of routes_id with highest similarity in the dictionary
        recommendations =  heapq.nsmallest(count, best_routes, key=best_routes.get)
        
        # Create a dictionary to return more information about the routes
        # Creates a dictionary in this format
        # { <route_id>: { 
        #               'length':2000,
        #               'uniqueness':0.34,
        #               'elevation':100,
        #               'ped_friend':0.65,
        #               'nature':0.78
        #               }}
        recommended_routes = {}
        for route_id in recommendations:
            route_info = routes[route_id]
            route_details = {
                'latlng_bounds_sw':route_info['latlng_bounds_sw'],
                'latlng_bounds_ne':route_info['latlng_bounds_ne'],
                'length':route_info['length'],
                'uniqueness':route_info['uniqueness'],
                'elevation':route_info['uphill'],
                'ped_friend':project_ped_friendliness(route_info['ped_friend']),
                'nature':project_nature_osm(route_info['land_cover'])
                }
            
            #print(route_details)
            recommended_routes[route_id] = route_details
        
        return recommended_routes
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}