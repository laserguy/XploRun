import helper_functions as hf
import traceback

import logging
logger = logging.getLogger(__name__)

def get_coords_from_geojson(id,geo_json):
    return geo_json[id]['geometry']['coordinates'][0]

def get_bbox_from_geojson(id,geo_json):
    latlng_bounds_ne = geo_json[id]['latlng_bounds_ne']
    latlng_bounds_sw = geo_json[id]['latlng_bounds_sw']
    
    return latlng_bounds_ne, latlng_bounds_sw

def create_geo_json(G,route,route_detail):
    data = route_detail
    data['type'] = 'Feature'
    data['geometry'] = {
                        'type':'Polygon',
                        'coordinates':[[]]}

    for node in route:
        lat = G.nodes[node]['y']
        long = G.nodes[node]['x']

        data['geometry']['coordinates'][0].append([long,lat])
    
    return data

# Needs Graph G and route(nodeids) as input
def geo_jsonify(G,routes,route_details):
    try:
        routes_json = {}
        count = 0
        for route_id in route_details:
            #route = hf.create_route_array(routes[route_id])
            route = routes[route_id]
            route_detail = route_details[route_id]
            data = create_geo_json(G,route,route_detail)
            
            routes_json[count] = data
            count +=1
            
        return routes_json
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}