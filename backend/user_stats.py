import time
import badgeParams
import datalayer

import logging
logger = logging.getLogger(__name__)

# Distance(meters)
# elevation(meters)
# time (minutes)

def xp_calculation(distance, elevation, time):
    try:
        xp = distance + 2*elevation - time
        return 0 if xp < 0 else xp
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
# week_stats = > calcuated stats for the current week    
# badge_info => contains the generic information of badges from the table badge_info, all details except the badge description
# Badge SURRENDER and CHALLENGE_COMPLETED should be calculated at the frontend and passed to the backend
# Here we are not checking the already obtained badges(checking won't make any difference)
def badges_calculation(week_stats,badges_info, run_finished):
    try:
        badges_dict = {}
        if run_finished == False:
            badges_dict[badgeParams.SURRENDER] = int(time.time()) + 7*24*60*60
        if week_stats['elevation'] >= badges_info[badgeParams.MOUNTAINEER]['minimum_val']:
            badges_dict[badgeParams.MOUNTAINEER] = int(time.time()) + 7*24*60*60
        # Time taken in minutes
        time_taken = week_stats['time_taken']/60
        if time_taken >= badges_info[badgeParams.FORREST_GUMP]['minimum_val']:
            badges_dict[badgeParams.FORREST_GUMP] = int(time.time()) + 7*24*60*60
        # Avg speed in km/h
        max_avg_speed = week_stats['max_avg_speed'] * 3.6  # m/s to km/h
        if max_avg_speed >= badges_info[badgeParams.HIGH_INTENSITY]['minimum_val']:
            badges_dict[badgeParams.HIGH_INTENSITY] = int(time.time()) + 7*24*60*60        
        if week_stats['nature_D'] >= badges_info[badgeParams.NATURE]['minimum_val']:
            badges_dict[badgeParams.NATURE] = int(time.time()) + 7*24*60*60
            
        return badges_dict
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}


# To be called from the cron-server
def badges_calculation_cron(week_stats,badges_info):
    try:
        badges_dict = {}
        if week_stats['distance'] < badges_info[badgeParams.LAZY]['minimum_val']:
            badges_dict[badgeParams.LAZY] = int(time.time()) + 7*24*60*60
        if week_stats['elevation'] >= badges_info[badgeParams.MOUNTAINEER]['minimum_val']:
            badges_dict[badgeParams.MOUNTAINEER] = int(time.time()) + 7*24*60*60
        
        time_taken = week_stats['time_taken']/60
        if time_taken >= badges_info[badgeParams.FORREST_GUMP]['minimum_val']:
            badges_dict[badgeParams.FORREST_GUMP] = int(time.time()) + 7*24*60*60
            
        max_avg_speed = week_stats['max_avg_speed'] * 3.6
        if max_avg_speed >= badges_info[badgeParams.HIGH_INTENSITY]['minimum_val']:
            badges_dict[badgeParams.HIGH_INTENSITY] = int(time.time()) + 7*24*60*60
        if max_avg_speed < badges_info[badgeParams.CRAWLER]['minimum_val']:
            badges_dict[badgeParams.CRAWLER] = int(time.time()) + 7*24*60*60            
        if week_stats['nature_D'] >= badges_info[badgeParams.NATURE]['minimum_val']:
            badges_dict[badgeParams.NATURE] = int(time.time()) + 7*24*60*60
            
        return badges_dict
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
def get_nonexpired_badges(user_id):
    try:
        badges_dict = {}
        # This will contain all the badges which are even expired
        overall_badges = datalayer.get_user_badges(user_id)
        if overall_badges == None:
            return badges_dict
        count = 0
        
        # Get all badges info(overall), to add the badge description in the user badges
        badges_info = datalayer.get_badge_info()
        
        for key in overall_badges:
            expire_ts = overall_badges[key]
            if expire_ts > int(time.time()):
                temp_dict = {}
                temp_dict['badge_id'] = key
                temp_dict['expire_ts'] = expire_ts
                temp_dict['description'] = badges_info[key]['description']
                badges_dict[count] = temp_dict
                count += 1
        
        return badges_dict
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
# Returns the badges dict which is difference between the two badges_dict
# before_badges => Badges before the run
# after_badges => Badges after the run
def get_new_obtained_badges(before_badges,after_badges):
    try:
        old_badges = []
        for badge in before_badges:
            old_badges.append(before_badges[badge]['badge_id'])
        
        diff_badge_dict = {}
        count = 0
        
        for badge in after_badges:
            if after_badges[badge]['badge_id'] not in old_badges:
                diff_badge_dict[count] = after_badges[badge]
                count += 1
                
        return diff_badge_dict
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}

def collect_user_overall_stats(user_id):
    try:
        overall_stats = datalayer.get_user_stats(user_id)
        if not overall_stats:
            overall_stats['distance'] = 0
            overall_stats['nature_D'] = 0
            overall_stats['urb_D'] = 0
            overall_stats['kcal'] = 0
            overall_stats['xp'] = 0
            
        user_records = datalayer.get_user_records(user_id)
        if not user_records:
            overall_stats['longest_distance'] = 0
            overall_stats['best_pace'] = 0
        else:
            overall_stats['longest_distance'] = user_records['longest_distance']
            # Convert to km/h
            overall_stats['best_pace'] = user_records['best_pace']*3.6
        
        return overall_stats 
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
def collect_user_past7days_stats(user_id,week_stats):
    try:
        final_dict = {}
        final_dict['week_runs'] = week_stats
        
        week_distance, week_kcal, week_xp = 0,0,0
        for day in week_stats:
            week_distance += week_stats[day]['distance']
            week_kcal += week_stats[day]['kcal']
            week_xp += week_stats[day]['xp']
            
        temp_dict = {}
        temp_dict['week_distance'] = week_distance
        temp_dict['week_kcal'] = week_kcal
        temp_dict['week_xp'] = week_xp
        
        final_dict['week_stats'] = temp_dict
        final_dict['badges'] = get_nonexpired_badges(user_id)
        
        return final_dict
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}