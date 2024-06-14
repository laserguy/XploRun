import osmnx as ox 
import numpy as np 
import networkx as nx
import math
from copy import deepcopy
import multiprocessing as mp
import geopy.distance as dis
import time
import os
import json
import helper_functions as hf

import logging
logger = logging.getLogger(__name__)

# help function for list flattening
def flatten_list(list):
    f_list = [val for sublist in list for val in sublist]
    return f_list


# generate the osmnx graph based on users location and convert MultiDiGraph to MultiGraph
# 
# u_location: (lat,long) of users gps location 
# distance: distance of bounding box used to collect street data
# 
# returns: NetworkX MultiGraph containing map information
def generate_graph(u_location,distance):
    try:
        G = ox.graph_from_point(u_location, dist=distance, dist_type="bbox", network_type="walk", simplify=True)
        G2 = ox.get_undirected(G)

    except Exception as e:
        return None

    return G2


# method for graph segmentation used for equally distributed start point generation
# Can be utilized by multiprocessing
#
# G: NetworkX graph containing the street information as edges/nodes
# center_point: (lat,long) of the users loation point (used as central point in graph generation)
# distance: distance of bounding box used for the full graph
# segments: number of segments the graph shall be segmented in                  3 | 4 
# current_seg: index of the segment in the grid (starts bottom left to right -> 1 | 2 )
#  
# returns: NetworkX graph-segment
def segment_graph_parallel(G,segments,center_point,distance,current_seg):

    km = distance/1000
    north1 = dis.distance(kilometers=km).destination((center_point),bearing=0).latitude
    west1 = dis.distance(kilometers=km).destination((center_point),bearing=270).longitude
    south2 = dis.distance(kilometers=km).destination((center_point),bearing=180).latitude

    dist = ((2*math.fabs(center_point[0]-north1)/math.sqrt(segments)),(2*math.fabs(center_point[1]-west1))/math.sqrt(segments))
    bottom_left =(south2,west1)

    # row and column in grid for indexed calculations
    row = current_seg%(math.sqrt(segments))
    column = math.ceil(current_seg/math.sqrt(segments))-1

    if row == 0:
        offset = math.sqrt(segments)
    else:
        offset = math.sqrt(segments)-(math.sqrt(segments)-row)

    north = bottom_left[0] + dist[0] + (column * dist[0])
    south = bottom_left[0] + (column * dist[0])
    east = bottom_left[1] + (offset) * dist[1]
    west = bottom_left[1] + (offset -1) * dist[1]

    try:
        segment = ox.truncate.truncate_graph_bbox(G,north,south,east,west,retain_all=True)
    except ValueError as e:
        segment = None

    return segment


# truncate graph with smaller bbox around users home location (for startpoint choosing)
def create_home_bbox(G,distance,user_location):

    km = distance/1000
    north = dis.distance(kilometers=km).destination((user_location),bearing=0).latitude
    east = dis.distance(kilometers=km).destination((user_location),bearing=90).longitude
    south = dis.distance(kilometers=km).destination((user_location),bearing=180).latitude
    west = dis.distance(kilometers=km).destination((user_location),bearing=270).longitude

    try:
        graph = ox.truncate.truncate_graph_bbox(G,north,south,east,west,retain_all=True)
    except ValueError as e:
        graph = None

    return graph


# find n routes of approx. lenght from start point, allowing +/- 30% difference from ideal length 
# UTILIZED BY MULTIPROCESSING
#
# start_node: node id of the start point
# total_length: approx. length of the route
# G: NetworkX graph containing the street information as edges/nodes
# n_count: number of different routes to generate for this point (default = 2)
#
# returns: a list[list] containing the nodes of the possible routes
def find_paths(start_node, total_length, G, n_count=2):
    paths = []
    length = math.floor(total_length/3)
    l_thresh = math.floor(length *0.3)
    start_dist = nx.shortest_path_length(G, source=start_node, weight='length')
    filtered_dist = {k: v for k,v in start_dist.items() if v < length+l_thresh and v > length-l_thresh}
    nodes_u = list(filtered_dist.keys())
    candidates = []

    # hardcoded limit of iterations (at least 2*number of generated routes, at most 24):
    LIMIT = min(24,n_count*2)
    limited_nodes = np.random.choice(nodes_u, LIMIT, replace=False)
    for u in limited_nodes:
        u_dist = nx.shortest_path_length(G, source=u, weight='length')   
        filtered_u = {k: v for k,v in u_dist.items() if v < length+l_thresh and v > length-l_thresh}
        candidates.append(set.intersection(set(filtered_u.keys()), set(nodes_u)))

    # select possible nodes to generate n_count paths (if possible)
    max_n = min(len(candidates),n_count)
    for i in range(max_n):
        u = nodes_u[i]
        if len(candidates[i])>0:
            v = candidates[i].pop()
            su = nx.shortest_path(G, source=start_node, target=u, weight='length')
            uv = nx.shortest_path(G, source=u, target=v, weight='length')
            vs = nx.shortest_path(G, source=v, target=start_node, weight='length')
            path = [su,uv,vs]
            paths.append(path)

    return paths


# find and eliminate duplicate paths (two paths that share to many similar streets)
# 
# paths: all possible paths for a starting point list[list] 
# elim_percentage: value for maximum similarity; eliminate paths that are above this value (default=0.8)
#
# returns: filtered paths without duplicates (list[list])
def eliminate_dup(paths,elim_percentage=0.8):
    elim_paths = deepcopy(paths)

    for p in paths:
        for p2 in elim_paths:
            if (p != p2):
                # flatten lists
                fp = flatten_list(p)
                fp2 = flatten_list(p2)
                perc = len(set.intersection(set(fp),set(fp2)))/len(set(fp))
                if (perc > elim_percentage):
                    elim_paths.remove(p2)

    return elim_paths


# favour paths without reusing many of the same streets (but leave at least minimum amount of paths)
# 
# paths: all possible paths for a starting point list[list]
# minimum_paths: min amount of paths to be kept even if not unique for the starting point (default=0)
# cut_perc: value for minimum uniqueness : closer to 1.0 -> less duplicate nodes (default=0.6)
#
# returns: list[list] of paths that are unique enough
def unique_streets(paths,minimum_paths=0,cut_perc=0.6):
    unique_paths = []

    if len(paths) <= minimum_paths:
        return paths

    for p in paths:
        # flatten list
        fp = flatten_list(p)
        total_nodes = len(fp)
        unique_nodes = len(set(fp))
        if (unique_nodes/total_nodes < cut_perc):
            continue
        unique_paths.append(p)

    return unique_paths


# find p random startpoints on the graph segment
# UTILIZED BY MULTIPROCESSING
#
# G: NetworkX graph containing street information as edges/nodes
# p: number of startpoints to randomly generate
#
# returns: list of possible start points on the graph
def find_sub_startpoints(G,p):
    start_points = set()
    for i in range(p):
        try:
            rand_num = np.random.randint(0,len(list(G.nodes(data=True))))
            start = list(G.nodes(data=True))[rand_num][0]
            start_points.add(start)
        except AttributeError as e:
            continue
    return list(start_points)


# get startpoints from all subgraphs
# UTILIZES MULTIPROCESSING
#
# n: number of startpoints to generate in each segment
# g_segments: number of segments to divide the graph into (has to be a square number, e.g. 4,9,16,25 etc.)
#
# returns: list of all starpoints (node ids)
def get_startpoints(n,g_segments):
    start_points = []

    # multiprocessing for startpoint finding (mp may not be necessary)
    pool = mp.Pool(processes=mp.cpu_count())
    try:
        start_points.append(pool.starmap(find_sub_startpoints,((g_segments[i],n)for i in range(len(g_segments)))))
    except ValueError as e:
        start_points.append(None)
    pool.close()
    pool.join()

    start_points = flatten_list(start_points)
    return start_points


# find the routes on the graph for a starting point
#
# G: NetworkX MultiGraph containing street information
# start_point: node id of starting point
# distance: approx total distance of route
# minimum_paths: keep at least this many rotues (default=0)
# n_routes: ideal number of routes to generate for the starting point (default=4)
#
# returns a list[list] of possible paths for the starting point
def find_routes(G,start_point,distance,minimum_paths=0,n_routes=4):

    try:
        p_a = find_paths(start_point,distance,G,n_routes)

    # if the graph doesn't contain any nodes, return an empty list[list]
    except Exception as e:
        return [[]]
    p_b = eliminate_dup(p_a)
    pa = unique_streets(p_b,minimum_paths)

    return pa


# main method for route generation
# UTILIZES MULTIPROCESSING
#
# graph: MultiGraph of the total area
# p_distance: approx. path distance for the routes
# start_points: all the generated starting points for the routes
# min_routes: min number of routes to keep for each starting point (if any are found)
# n_routes: total number of routes to generate for each starting point
# mode: 'single' or 'multi': using single or multiprocessing (only useful for large amount of starting points / routes) 
#
# returns a list of all generated routes (a list of lists of nodes)
def generate_routes(graph,p_distance,start_points,min_routes,n_routes,mode='single'):
    routes = []

    # multiprocessing for route_generating (can be heavy on the CPU and RAM)
    # USEFUL ONLY FOR LARGE NO OF ROUTES AND BIG GRAPHS
    if mode == 'multi':
        pool = mp.Pool(processes=math.floor(mp.cpu_count()/2))
        try:
            routes.append(pool.starmap(find_routes,((graph,start_points[i],p_distance,min_routes,n_routes)for i in range(len(start_points)))))
        except Exception as e:
            routes.append(None)
        pool.close()
        pool.join()

        # flatten twice and filter list of routes (remove empty lists []):
        routes = flatten_list(routes)
        routes = list(filter(None, routes))
        routes = flatten_list(routes)

    # generate routes sequentially (used for smaller graphs and routes)
    elif mode == 'single':
        for p in start_points:
            routes.append(find_routes(graph,p,p_distance,min_routes,n_routes))

         # flatten and filter list of routes (remove empty lists []):
        routes = flatten_list(routes)
        routes = list(filter(None, routes))
    

    return routes

# Post processing the generated routes
# Route contains a lot of noise points/nodes outliers, which doesn't create ideal routes for running
# Post processing gets rids of all such unnecessary nodes
def route_post_processing(unfurnshied_route):
    L = hf.create_route_array(unfurnshied_route)
    D = {}    # Dictionary D[<osm_nodeid>] = [<list>]  # List of postitions where the nodeid is repeated
    pos = 0
    
    for point in L:
        if point in D:
            D[point].append(pos)
        else:
            D[point] = [pos]
        pos += 1
    
    prev = False
    del_range = []      # The position ranges to be deleted from the unfurnished route
    for key in D:
        rep_list = D[key]
        # The assumption here is no point will be repeated more than once
        # On a very rare ocassion, a route is seen which has a node with more than 2 repeations.
        # Therefore more than 2 is not handeled
        if len(rep_list) == 2:
            if prev == True:
                continue        
            point_sepeartion = rep_list[1] - rep_list[0]
            # no. of nodes after which a node is repeated, allowed is more than 20 here
            # If maximum separation between two repeations is less than 20, then all the nodes between that repeations well be deleted
            # 20, seems to be right, less than 20 lets there to be some nosiy points.
            # This number can be modified.
            if point_sepeartion < 20:    
                del_range.append(D[key])
            prev = True

        else:
            prev = False
        
    points_to_eliminate = []    
    for del_ele in del_range:
        points_to_eliminate.extend(range(del_ele[0]+1, del_ele[1]+1))

    clean_route = []
    for i in range(len(L)):
        if i not in points_to_eliminate:
            clean_route.append(L[i])
    
    return clean_route


"""NEW APPROACH, USER PICKS 'HOME' LOCATION"""
# returns the full set of possible routes in the big area around the user. if set to 'PREVIEW' use different parameters with less resources 
def get_home_routes(user_location, length, mode='PREVIEW'):

    if mode == 'PREVIEW':
        USER_BBOX_DISTANCE = 100
        # use log2 function to find appropriate bbox size for graph generation, depending on the desired route length
        value = np.round(math.log2(length/1000),1)
        # set bbox accordingly, but choose at least a size of 1000m for decent results
        TOTAL_BBOX_DISTANCE = max(np.rint(value*1000),1000)
        # no of starting points for routes, number of routes to generate per starting point (if possible)
        NUMBER_STARTING_POINTS = 8
        NUMBER_TOTAL_ROUTES = 8
        # try to keep at least this many routes, even if they are not very unique 
        MINIMUM_ROUTES = 4
    
    # generate routes of "all" different lengths within the specified range
    else:
        USER_BBOX_DISTANCE = 200
        TOTAL_BBOX_DISTANCE = 5000
        NUMBER_STARTING_POINTS = 16
        NUMBER_TOTAL_ROUTES = 6
        MINIMUM_ROUTES = 2
        #try to find routes for each approx. length (1000m, 2000m, ... 20000m)
        ROUTE_LENGHTS = [x*1000 for x in list(range(1,21))]

    
    # generate graphs and starting points
    map_graph = generate_graph(user_location,TOTAL_BBOX_DISTANCE)
    start_graph = create_home_bbox(map_graph,USER_BBOX_DISTANCE,user_location)
    start_points = find_sub_startpoints(start_graph,NUMBER_STARTING_POINTS)

    # generate routes for all possible lengths
    if mode == 'PREVIEW':
        routes = generate_routes(map_graph,length,start_points,MINIMUM_ROUTES,NUMBER_TOTAL_ROUTES)
    else:
        routes = []
        for length in ROUTE_LENGHTS:
            routes.append(generate_routes(map_graph,length,start_points,MINIMUM_ROUTES,NUMBER_TOTAL_ROUTES,mode='multi'))
        # flatten list to remove outmost unncecessary brackets
        routes = flatten_list(routes)

    # return at most 25 routes for further processing
    ret_routes = min(len(routes),25)
    
    furnished_routes = []
    for route in routes[:ret_routes]:
        furnished_routes.append(route_post_processing(route))

    return furnished_routes, map_graph



# save routes as .JSON and graph as .GRAPHML files
def save_routes(routes, G):

    CUR_DIR = os.getcwd()
    SAVE_PATH = os.path.join(CUR_DIR,"route_data/route_graph.GRAPHML")

    # save graph as .GRAPHML file
    ox.io.save_graphml(G,filepath=SAVE_PATH,gephi=False, encoding='utf-8')

    # save routes as .JSON file
    with open('route_data/routes.json', 'w') as f:
        json.dump(routes, f, ensure_ascii=False, indent=4)


# load graph and routes from directories
def load_routes_graph(route_dir,graph_dir):
    G = ox.io.load_graphml(graph_dir, node_dtypes=None, edge_dtypes=None, graph_dtypes=None)

    with open(route_dir, 'r') as f:
        routes = json.load(f)

    return routes,G



"""ONLY FOR TESTING PURPOSES"""
def testing():
    import matplotlib.pyplot as plt

    #ox.config(use_cache=True, log_console=True)
    print("TEST")

    user_location = (49.92700656238504, 11.58513832111843)
    routes, graph = get_home_routes(user_location, 4000, 'PREVIEW')

    print(len(routes))

    ox.plot_graph_routes(graph, routes[-1], node_color='b', node_size=25, show=False, close=False)
    plt.show()


if __name__ == '__main__':
    testing()