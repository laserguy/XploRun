import webcolors
from pyproj import Proj, transform

import logging
logger = logging.getLogger(__name__)

# Name for each color
map_legend = {'#e6004d':'urban_fabric',
            '#ffffa8':'arable_land',
            '#4dff00':'forests',
            '#cc4df2':'ind_com',                  # industrial, commercial and transport units
            '#ffa6ff':'artif_non_agri_vegetated', # artifical, non agricultural vegetated units
            '#a600cc':'mine_dump_constr',         # mine, dump and construction sites
            '#e6e64d':'pastures',
            '#e68000':'permanent_crops',
            '#00ccf2':'water_bodies',
            '#e6e6e6':'open_spaces_no_veg',       # open spaces with little or no vegetation
            '#ccf24d':'scrub_herbs',
            '#a6a6ff':'wetlands',
            '#e6e6ff':'coastal_wetlands',
            '#ffffff':'unmapped area'
}

#What percent of each landuse type can be considered as nature
nature_percent = {'urban_fabric':0.1,
                'arable_land':0.9,
                'forests':1,
                'ind_com':0,					
                'artif_non_agri_vegetated':0.2,
                'mine_dump_constr':0,			
                'pastures':0.9,
                'permanent_crops':0.8,
                'water_bodies':1,
                'open_spaces_no_veg':0.8,		
                'scrub_herbs':1,
                'wetlands':1,
                'coastal_wetlands':1,
                'unmapped area':0}

# pedistrian edge types

ped_edge_types = ['footway',
                  'bridleway',
                  'steps',
                  #'corridor',
                  'path',
                  'track',
                  'pedestrian',
                  'living_street',
                  'residential']

# min and max values for each preference
# Ranges here are in meters, min and max values cannot be assigned for nature, pedistrian friendliness and uniquness

preference_ranges = {
                    'length':{'min':2000,'max':20000},
                     'elevation':{'min':1,'max':1000}
                     }      

# project length back from value 0-1.0 to actual length in meters
def back_project_length(projected_length):
    try:
        min_length = preference_ranges['length']['min']
        max_length = preference_ranges['length']['max']
        length = projected_length * (max_length-min_length) + min_length
        return length
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}

def create_route_array(route):
    flat_route = []
    route_len = len(route)
    for i in range(0,route_len):
        sub_route_len = len(route[i])
        for j in range(0,sub_route_len-1):
            flat_route.append(route[i][j])
            
    flat_route.append(route[route_len-1][sub_route_len-1])
    
    return flat_route

def hex2name(c):
    h_color = '#{:02x}{:02x}{:02x}{:02x}'.format(int(c[0]), int(c[1]), int(c[2]))
    return h_color

def closest_colour(requested_colour):
    min_colours = {}
    for key, name in map_legend.items():
        r_c, g_c, b_c = webcolors.hex_to_rgb(key)
        rd = (r_c - requested_colour[0]) ** 2
        gd = (g_c - requested_colour[1]) ** 2
        bd = (b_c - requested_colour[2]) ** 2
        min_colours[(rd + gd + bd)] = name
    return min_colours[min(min_colours.keys())]

# Pass latitude and longitude
def geoCords2ProjCords(bbox):
    projected_coords = ''

    inProj = Proj('epsg:4326')                          # Geographical co-ordinates lattitude and longitude
    outProj = Proj('epsg:3857')                         # Projected co-ordinates
    x,y = transform(inProj,outProj,bbox[1],bbox[0])
    projected_coords += str(x) + ',' + str(y) + ','
    x,y = transform(inProj,outProj,bbox[3],bbox[2])
    projected_coords += str(x) + ',' + str(y)
    return projected_coords