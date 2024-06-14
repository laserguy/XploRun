import osmnx as ox 
import traceback


import helper_functions as hf
import params
import nature_component

import logging
logger = logging.getLogger(__name__)

#Elevation of the route calculation, overall elevation of the closed route will always be zero
#So calculating only uphill
def get_uphill(G, route):
    try:
        G = ox.elevation.add_node_elevations_google(G, api_key=params.GOOGLE_ELEVATION_API_KEY)
        G = ox.elevation.add_edge_grades(G)    
        uphill = 0
        flag = False
        
        for node in route:
            elevation = G.nodes[node]['elevation']
            
            if flag == False:
                prev_elevation = elevation  
                flag = True
            
            #Calculate the uphill elevation of the route
            elif flag == True and elevation > prev_elevation:
                uphill += elevation - prev_elevation 
            
        return uphill
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}
        
def get_bbox_route(G,route):
    try:
        # W LONG, S LAT,E LONG, N LAT
        # min long, min lat, max long, max lat
        bbox_cords = [999, 999, -999, -999]
        
        for node in route:
            lat = G.nodes[node]['y']
            long = G.nodes[node]['x']

            # Get the bounding box of the route, this area will be used to get the landcover
            if lat > bbox_cords[3]: # maximum latitude/ North latitude
                bbox_cords[3] = lat
            if lat < bbox_cords[1]: # minimum latitude/ South latitude
                bbox_cords[1] = lat
            if long > bbox_cords[2]: # maximum longitude/ East longitude
                bbox_cords[2] = long
            if long < bbox_cords[0]: # minimum longitude/ West longitude
                bbox_cords[0] = long
                
        return bbox_cords
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}

def get_uniqueness(route):
    try:
        node_repeat = {}
        route_len = len(route)
        
        for i in range(0,route_len-1):
            if route[i] in node_repeat:
                node_repeat[route[i]] +=1
            else:
                node_repeat[route[i]] = 1
        
        count = 0
        for key in node_repeat:
            if node_repeat[key] == 1:
                count += 1
          
        # % uniquness
        uniqueness =  100 - (((route_len-1) - count)/(route_len-1))*100
        return uniqueness/100
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}

def get_length(G,route):
    try:
        length = 0
        
        route_len = len(route)
        for i in range(0,route_len-1):
            # get_edge_data usually returns a dictionary with only one key 0 and that key contains the all information, but in some cases when there are more
            # edges between two nodes, the dictionary can contain more than one key with different key mostly 2 ans 3 and won't contain 0
            edge_info = G.get_edge_data(route[i+1],route[i])
            for key in edge_info:
                #if key != 0:
                    #print(edge_info)
                edge_info = edge_info[key]
                break
            length += edge_info['length']
            
        return length
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}

# Returns dictionary of edge_types
# Actual logic of %age friendliness calculation should be in ML
def pedistrian_friendliness(G,route):
    try:
        edges = len(route) - 1
        edge_types = {}
        for i in range(0,edges):
            edge_info = G.get_edge_data(route[i+1],route[i])
            for key in edge_info:
                edge_info = edge_info[key]
                break
            
            # Usually edge have only one edge type but in some cases they can have multiple edge types, in that case edge_type would be a list
            edge_type = edge_info['highway']
            if (type(edge_type) == type([])):
                edge_type = edge_type[0]
                
            if edge_type in edge_types:
                edge_types[edge_type] +=1
            else:
                edge_types[edge_type] =1
                
        return   edge_types
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}

# Returns dictionary of %age landuse
# ADVANTAGES:
# Gives the mapping of every entity
# DISADVANTAGES:
# Not 100% accurate, as it creates the polygons, but there could be some overlapping in them
# Takes a bit long time, gets data from the OSM database
def get_landuse_gdal(bbox):
    try:
        bbox_str = str(bbox[0])+','+str(bbox[1])+','+str(bbox[2])+','+str(bbox[3])
        
        from subprocess import Popen, PIPE
        
        args = ['ogrinfo', 'http://www.overpass-api.de/api/xapi?*[landuse=*][bbox='+bbox_str+']', '-dialect', 'sqlite', '-sql', 
                'select landuse, st_area(geometry) from multipolygons']
        process = Popen(args, stdout=PIPE, stderr=PIPE)

        stdout = process.communicate()[0].decode('utf-8').strip()
        
        land_cover = {}
        last_key = 'NULL'
        total_area = 0

        for line in stdout.splitlines():
            if(line.find('landuse (String)') != -1):
                key = line.split('=')[1].strip()
                last_key = key
            elif(line.find('st_area(geometry) (Real)') != -1):
                value = line.split('=')[1].strip()
                total_area += float(value)
                if last_key in land_cover:
                    land_cover[last_key] += float(value)
                else:
                    land_cover[last_key] = float(value)

        for key in land_cover:
            land_cover[key] = (land_cover[key]/total_area)*100
            
        return land_cover
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}


def get_landuse_osm(bbox):
    try:
        img = nature_component.get_bbox_image(bbox)

        temp_dict = {}

        for i in range(0,255):
            for j in range(0,255):
                rgb = img.getpixel((i, j))
                if rgb in temp_dict:
                    temp_dict[rgb] +=1
                else:
                    temp_dict[rgb] = 1

        sorted_dict = dict(sorted(temp_dict.items(), key=lambda item: item[1], reverse=True))
        
        landuse = {}

        for key in sorted_dict:
            color_name = hf.closest_colour(key)
            if color_name in landuse:
                landuse[color_name] += sorted_dict[key]
            else:
                landuse[color_name] = sorted_dict[key]
                
        return landuse
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}

# To be called from outside
# Return dict {route_id: route_info}
# route_ids will be created sequential
def extract_routes_features(G, routes):
    try:
        # Right now 
        # Route: One route may contain many paths which will be connected together to form loop,
        # Route: <list<list>>
        # Routes: Therefore list<Route>   =>  list<list<list>>
        route_id = 0
        routes_info = {}
        for route in routes:       
            #flat_route = [item for sublist in route for item in sublist]
            #flat_route = hf.create_route_array(route)
            flat_route = route
            
            bbox = get_bbox_route(G, flat_route)
            #land_cover = get_landuse_gdal(bbox)
            land_cover = get_landuse_osm(bbox)
            ped_friend = pedistrian_friendliness(G, flat_route)
            length = get_length(G,flat_route)
            uniqueness = get_uniqueness(flat_route)              # % value
            uphill = get_uphill(G, flat_route)
            
            route_info = {
                        'latlng_bounds_sw':[bbox[1],bbox[0]],
                        'latlng_bounds_ne':[bbox[3],bbox[2]],
                        'land_cover':land_cover,
                        'ped_friend': ped_friend,
                        'length':length,
                        'uniqueness':uniqueness,
                        'uphill':uphill
                        }
            routes_info[route_id] = route_info
            route_id +=1
            
        return routes_info
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}
    