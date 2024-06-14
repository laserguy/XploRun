import datalayer
import traceback
from statistics import mean

def create_challenge(user_id):
    try:
        dist_history, ele_history, time_history = datalayer.get_run_history(user_id)
        avg_dist, avg_ele, avg_time = mean(dist_history), mean(ele_history), mean(time_history)
        
        ### CHALLENGE ###
        # Increase distance by 10% of the average
        # Increase elevation by 10%
        # Decrease time by 10%
        
        challenged_dist, challenged_ele, challenged_time = avg_dist+0.1*avg_dist, avg_ele+0.1*avg_ele, avg_time-0.1*avg_time
        
    except Exception as e:
        print(traceback.print_exc())
        return {'error': str(e)}    