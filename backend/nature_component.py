from itsdangerous import json
import requests
from PIL import Image
from io import BytesIO
import traceback

import helper_functions as hf
import json_helper

import logging
logger = logging.getLogger(__name__)


#What percent of each landuse type can be considered as nature
map_nature_to_DB = {'urban_fabric':'URBAN',
                'arable_land':'NATURE',
                'forests':'NATURE',
                'ind_com':'URBAN',					
                'artif_non_agri_vegetated':'URBAN',
                'mine_dump_constr':'URBAN',			
                'pastures':'NATURE',
                'permanent_crops':'NATURE',
                'water_bodies':'NATURE',
                'open_spaces_no_veg':'NATURE',		
                'scrub_herbs':'NATURE',
                'wetlands':'NATURE',
                'coastal_wetlands':'NATURE',
                'unmapped area':'UNCLASSIFIED'}


# This takes the data from the https://osmlanduse.org/, using WMS service
# This outputs the dictionary of color:pixelcount
# How each color is mapped to a landuse will be in ML/nature calculation
# ADVANTAGE:
# Much faster compared to get_landuse_gdal, and data is accurate
# DISADVANTAGE:
# Color has to be mapped to appropriate entity
def get_bbox_image(bbox):
    try:
        # convert the bbox co-ords to
        # epsg3857/projected co-ords(These co-rds are used in the OSM)
        
        projected_coords = hf.geoCords2ProjCords(bbox)
        
        URL = "https://maps.heigit.org/osmlanduse/service"
        
        PARAMS = {'SERVICE':'WMS',
                    'VERSION':'1.3.0',
                    'REQUEST':'GetMap',
                    'FORMAT':'image/png',
                    'TRANSPARENT':'true',
                    'LAYERS':'osmlanduse:osm_lulc_combined_osm4eo',
                    'BUFFER':25,
                    'WIDTH':256,
                    'HEIGHT':256,
                    'CRS':'EPSG:3857',
                    'STYLES':'',
                    'BBOX':projected_coords}

        response = requests.get(url = URL, params=PARAMS)
        #print(response.url)

        # The above service only failed once
        img = Image.open(BytesIO(response.content)).convert('RGB')
        
        return img
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}
    
'''
    coords -> Geo co-ordinates of the route
'''
def get_route_nature(coords,lat_bounds):
    try:
        # NE -> NORTH LATTITUDE, EAST LONGITUDE  latlng_bounds_ne[0], latlng_bounds_ne[1]
        # SW -> SOUTH LATTITUDE, WEST LONGITUDE  latlng_bounds_sw[0], latlng_bounds_sw[1]
        # bbox = [WEST LONGITUDE, SOUTH LATTITUDE, EAST LONGITUDE, NORTH LATTITUDE]
        latlng_bounds_ne, latlng_bounds_sw = lat_bounds[0],lat_bounds[1]
        bbox = [latlng_bounds_sw[1],latlng_bounds_sw[0],latlng_bounds_ne[1],latlng_bounds_ne[0]]
        img = get_bbox_image(bbox)
        
        length = latlng_bounds_ne[0] - latlng_bounds_sw[0] #lattitude
        step_length = length/256
        width = latlng_bounds_ne[1] - latlng_bounds_sw[1]  #longitude
        step_width = width/256
        
        '''
            For nature calculation of each point, we will consider SW as (0,0) and NE (1,1)
        '''
        for coord in coords:
            lat = coord[1]
            long = coord[0]
            # Lattitude and Longitude difference with the SW co-ords
            lat_diff = lat - latlng_bounds_sw[0]
            long_diff = long - latlng_bounds_sw[1]
            # Find where the current point will lie in the image, we have to get (x,y) value where 0 <= x,y <= 255
            y = int(lat_diff/step_length)-1
            x = int(long_diff/step_width)-1
            rgb = img.getpixel((x, y))
            color_name = hf.closest_colour(rgb)
            # All the colors should be mapped to 3 distinct parts to be stored in the DB
            # The color code means the nature here
            color_code = map_nature_to_DB[color_name]
            
            # TODO: Right now appending the color_code in the list for each co-ordinate, may change later
            coord.append(color_code)
            
        
    except Exception as e:
        print(x,y)
        print('latlng_bounds_ne = ',latlng_bounds_ne)
        print('latlng_bounds_sw = ',latlng_bounds_sw)
        print('coord = ',coord)
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}
    
    
'''
    Adds the nature info in the geo_json for each co-ordinate
'''
def add_nature_info(geo_json):
    try:
        for id in geo_json:
            route_coords = json_helper.get_coords_from_geojson(id,geo_json)
            latlng_bounds_ne, latlng_bounds_sw = json_helper.get_bbox_from_geojson(id,geo_json)
            lat_bounds = (latlng_bounds_ne,latlng_bounds_sw)
            
            # This will inplace add the nature part for each coordinate in geo_json
            get_route_nature(route_coords,lat_bounds)
        return geo_json
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}