# All the functions which will interact with the DB will come here
# TABLENAMES:
# loginfo: For user login/register details
#   id : PRIMARY_KEY
#   username : VARCHAR
#   passwd : VARCHAR
#   firstlogin : INT(0/1)
# fixed_preferences: One time preferences which will be set at the time of register
#   user_id : FOREIGN_KEY AND PRIMARY_KEY
#   length   : INT       (Value between 0-1)
#   elevation : INT      same
#   ped_friend : INT     same
#   uniqueness : INT     same
#   nature     : INT     same
# flexible_preferences: To be used for updated recommendations
#   user_id : FOREIGN_KEY AND PRIMARY_KEY
#   length   : INT       (Value between 0-1)
#   elevation : INT      same
#   ped_friend : INT     same
#   uniqueness : INT     same
#   nature     : INT     same
#   avg_num    : INT     same       for continuous average calculation

import sqlhelper
import params
import traceback
import special_sql_queries
import badgeParams

import logging
logger = logging.getLogger(__name__)

def register(username,passwd):
    try:
        element_dict = {}
        element_dict['username'] = username

        user_info = sqlhelper.select_mulparams(params.DB_NAME_EXP,element_dict,params.LOGIN_INO)
        
        if user_info:
            return {'error': 'username taken'}

        element_dict['password'] = passwd
        element_dict['firstlogin'] = 0

        insert_dict = sqlhelper.insert("",params.DB_NAME_EXP,params.LOGIN_INO,element_dict)

        user_dict = {}
        user_dict['user_id'] = insert_dict['insert_id']
        #sqlhelper.insert("",params.FXD_PREFERENCES,user_dict)
        #sqlhelper.insert("",params.FLX_PREFERENCES,user_dict)

        return {'id':user_dict['user_id']}
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}

def login(username,passwd):
    try:
        user_dict = {}
        element_dict = {}
        element_dict['username'] = username
        element_dict['password'] = passwd
        login_info = sqlhelper.select_mulparams(params.DB_NAME_EXP,element_dict,params.LOGIN_INO)

        if len(login_info) == 0:
            return {'error': "username or password incorrect"}

        for row in login_info:
            user_dict['id'] = row[0]
            user_dict['firstlogin'] = row[3]
        
        if user_dict['firstlogin'] == 1:
            user_info = sqlhelper.select(user_dict['id'],'user_id',params.DB_NAME_EXP,params.USER_INFO)
            for row in user_info:
                user_dict['sex'] = row[1]
                user_dict['age'] = row[2]
                user_dict['height'] = row[3]
                user_dict['weight'] = row[4]
        
        return user_dict
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}


def get_preferences(user_id):
    try:
        user_dict = {}
        user_info = sqlhelper.select(user_id,'user_id',params.DB_NAME_EXP,params.FXD_PREFERENCES)
        for row in user_info:
            user_dict['length'] = row[1]
            user_dict['elevation'] = row[2]
            user_dict['ped_friend'] = row[3]
            user_dict['uniqueness'] = row[4]
            user_dict['nature'] = row[5]

        return user_dict
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}    
    
 #input:  dict of user preferences   
def set_preferences(user_id,preferences):
    try:
        element_dict = {}
        element_dict['id'] = user_id
        element_dict['firstlogin'] = 1
        sqlhelper.update('id',element_dict,params.DB_NAME_EXP,params.LOGIN_INO)
        
        element_dict = {}
        element_dict['user_id'] = user_id
        element_dict['length'] = preferences['length']
        element_dict['elevation'] = preferences['elevation']
        element_dict['ped_friend'] = preferences['ped_friend']
        element_dict['uniqueness'] = preferences['uniqueness']
        element_dict['nature'] = preferences['nature']
        sqlhelper.insert("",params.DB_NAME_EXP,params.FXD_PREFERENCES,element_dict)
        
        element_dict = {}
        element_dict['user_id'] = user_id
        element_dict['sex'] = preferences['sex']
        element_dict['age'] = preferences['age']
        element_dict['height'] = preferences['height']
        element_dict['weight'] = preferences['weight']
        user_dict = sqlhelper.insert("",params.DB_NAME_EXP, params.USER_INFO,element_dict)

        return user_dict
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}

#input: features of the route along with the user_id
def update_preferences(user_id,features):
    try:
        length = 0
        elevation = 0
        ped_friend = 0
        uniqueness = 0
        nature = 0
        avg_num = 0
        
        user_pref = sqlhelper.select(user_id,'user_id',params.DB_NAME_EXP,params.FXD_PREFERENCES)
        for row in user_pref:
            length = row[1]
            elevation = row[2]
            ped_friend = row[3]
            uniqueness = row[4]
            nature = row[5]
            avg_num = row[6]
            
        preferences = {}
        preferences['user_id'] = user_id
        preferences['length'] = (length*avg_num + features['length'])/(avg_num+1)
        preferences['elevation'] = (elevation*avg_num + features['elevation'])/(avg_num+1)
        preferences['ped_friend'] = (ped_friend*avg_num + features['ped_friend'])/(avg_num+1)
        preferences['uniqueness'] = (uniqueness*avg_num + features['uniqueness'])/(avg_num+1)
        preferences['nature'] = (nature*avg_num + features['nature'])/(avg_num+1)
        preferences['avg_num'] = avg_num + 1
        
      
        user_dict = sqlhelper.update('user_id',preferences,params.DB_NAME_EXP,params.FXD_PREFERENCES)

        return user_dict
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}
    
    
###############################################################################
################    BIG MASTER PROJECT CHANGES   ##############################

def update_run_history(user_id,run_info):
    try:
        table_name = str(user_id)+'_run_history'
        if sqlhelper.check_table_exists(params.DB_NAME_RUN,table_name) == False:
            # nature_S_D, res_S_D, urb_S_D  ===> Nature, residential and urban speed and distance respectively
            columns = 'date INT, distance FLOAT,elevation FLOAT, avg_speed FLOAT, max_speed FLOAT, time_taken FLOAT, nature_D FLOAT, urb_D FLOAT, kcal FLOAT, xp FLOAT'
            sqlhelper.create_table(params.DB_NAME_RUN,table_name,columns)
        
        element_dict = {}
        import time    
        element_dict['date'] = int(time.time())
        element_dict['distance'] = run_info['distance']
        element_dict['elevation'] = run_info['elevation']
        element_dict['avg_speed'] = run_info['avg_speed']
        element_dict['max_speed'] = run_info['max_speed']
        element_dict['time_taken'] = run_info['time_taken']
        # TODO: Seaggregate the speed and distance distributions
        element_dict['nature_D'] = run_info['nature_D']
        element_dict['urb_D'] = run_info['urb_D']
        element_dict['kcal'] = run_info['kcal']
        element_dict['xp'] = run_info['xp']
        user_dict = sqlhelper.insert("",params.DB_NAME_RUN,table_name,element_dict)
        return user_dict
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}
 
# This is for the overall stats where we don't do group by
# This will return last few rows of the run_history table
def get_recent_runs(user_id, count):
    try:
        table_name = str(user_id)+'_run_history'
        run_history = sqlhelper.select_last_rows(params.DB_NAME_RUN,table_name,count)
        
        runs = {}
        counter = 0
        for row in run_history:
            temp_dict = {}
            temp_dict['date'] = row[0]
            temp_dict['distance'] = row[1]
            temp_dict['elevation'] = row[2]
            temp_dict['avg_speed'] = row[3]
            temp_dict['max_speed'] = row[4]
            temp_dict['time_taken'] = row[5]
            temp_dict['nature_D'] = row[6]
            temp_dict['urb_D'] = row[7]
            temp_dict['kcal'] = row[8]
            temp_dict['xp'] = row[9]
            
            runs[counter] = temp_dict
            counter += 1
            
        return runs
    except Exception as e:
        print(traceback.print_exc())
        logger.error(str(e))
        return {'error': str(e)}

# Returns the summation of stats of the present week    
def get_weekly_stats(user_id):
    try:
        table_name = str(user_id)+'_run_history'
        if sqlhelper.check_table_exists(params.DB_NAME_RUN,table_name) ==  False:
            return {}
        elements = ['distance','elevation','time_taken','nature_D','kcal','xp','avg_speed']
        
        import datetime
        # Monday is the starting day of the week, next monday everything will refresh
        # Get the day number of the week starting from Monday
        # Monday is 0, therefore (0-6)
        days_from_monday = datetime.datetime.today().weekday()
        
        # We are adding 1 to days_from_monday, as the function needs value to be more than 0
        week_info = special_sql_queries.stats_summation(params.DB_NAME_RUN,table_name,elements,days_from_monday+1)
        
        week_stats = {}
        for row in week_info:
            week_stats['distance'] = row[0]
            week_stats['elevation'] = row[1]
            week_stats['time_taken'] = row[2]
            week_stats['nature_D'] = row[3]
            week_stats['kcal'] = row[4]
            week_stats['xp'] = row[5]
            week_stats['max_avg_speed'] = row[6]
        
        return week_stats
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
    
# Gets the data for last one week from the run_history of the user
# This will do the summation of user stats grouped by day
# For how many past days you want the data
def get_sum_run_history(user_id, past_days):
    try:
        table_name = str(user_id)+'_run_history'
        elements = ['distance','kcal','xp']
        sum_run_history = special_sql_queries.select_group_rows(params.DB_NAME_RUN,table_name,elements,past_days)
        
        week_stats = {}
        
        for row in sum_run_history:
            temp_dict = {}
            temp_dict['runs'] = row[1]
            temp_dict['distance'] = row[2]
            temp_dict['kcal'] = row[3]
            temp_dict['xp'] = row[4]
            week_stats[str(row[0])] = temp_dict
        
        return week_stats
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
def get_user_stats(user_id):
    try:
        run_info = sqlhelper.select(user_id,'user_id',params.DB_NAME_EXP,params.RUN_INFO)
        run_stats = {}
        # If no entry present then empty dict will be returned  
        if len(run_info) == 0:
            return run_stats
        
        for row in run_info:
            run_stats['distance'] = row[1]
            run_stats['nature_D'] = row[2]
            run_stats['urb_D'] = row[3]
            run_stats['kcal'] = row[4]
            run_stats['xp'] = row[5]
        return run_stats
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
  
# user_run_stats here is the calculated value(adding the current run)  , we just have to update this in the DB
def update_user_stats(user_id,current_run):
    try:
        entry_exists = 1
        user_run_stats = get_user_stats(user_id)
        if len(user_run_stats) == 0:
            entry_exists = 0  # Entry doesn't exists, therefore we have to insert it
            user_run_stats['distance'] = 0
            user_run_stats['nature_D'] = 0
            user_run_stats['urb_D'] = 0
            user_run_stats['kcal'] = 0
            user_run_stats['xp'] = 0
        
        user_run_stats['user_id'] = user_id
        user_run_stats['distance'] += current_run['distance']
        user_run_stats['nature_D'] += current_run['nature_D']
        user_run_stats['urb_D'] += current_run['urb_D']
        user_run_stats['kcal'] += current_run['kcal']
        user_run_stats['xp'] += current_run['xp']
        
        if entry_exists:
            user_dict = sqlhelper.update('user_id',user_run_stats,params.DB_NAME_EXP,params.RUN_INFO)
        else:
            user_dict = sqlhelper.insert("",params.DB_NAME_EXP, params.RUN_INFO,user_run_stats)
            
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}  

# Gives badges general info   
def get_badge_info():
    try:
        badges = sqlhelper.select_all(params.DB_NAME_EXP, params.BADGE_INFO)
        
        badge_info = {}
        for row in badges:
            temp_dict = {}
            temp_dict['name'] = row[1]
            temp_dict['description'] = row[2]
            temp_dict['minimum_val'] = row[3]
            badge_info[row[0]] = temp_dict
        return badge_info
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}

# Get badges for the, user
def get_user_badges(user_id):
    try:
        table_name = str(user_id)+'_badges'
        if sqlhelper.check_table_exists(params.DB_NAME_BADGE,table_name) == False:
            return None
        badges = sqlhelper.select_all(params.DB_NAME_BADGE, table_name)
        
        badge_dict = {}
        for row in badges:
            badge_dict[row[0]] = row[1]
        return badge_dict
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
  
def update_user_badges(user_id,badges_dict):
    try:
        table_name = str(user_id)+'_badges'
        if sqlhelper.check_table_exists(params.DB_NAME_BADGE,table_name) == False:
            # nature_S_D, res_S_D, urb_S_D  ===> Nature, residential and urban speed and distance respectively
            columns = 'badge_id INT, expiry_date INT'
            sqlhelper.create_table(params.DB_NAME_BADGE,table_name,columns)
        
        acquired_badges = get_user_badges(user_id)
        success_dict = {}
        
        for key in badges_dict:
            element_dict = {}
            element_dict['badge_id'] = key
            element_dict['expiry_date'] = badges_dict[key]
            if key in acquired_badges:
                # If the acquired badges haven't expired now, then don't update the expiry timestamp
                # Only update the timestamp, if badges are expired
                import time
                if acquired_badges[key] < int(time.time()):
                    success_dict = sqlhelper.update('badge_id',element_dict,params.DB_NAME_BADGE,table_name)
            else:
                success_dict = sqlhelper.insert("",params.DB_NAME_BADGE, table_name,element_dict)
        
        return success_dict
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    

# Update the records on the basis of user current run_info
def update_user_records(user_id, run_info):
    try:
        user_records = get_user_records(user_id)
        
        # Update if the records exists else insert the entry
        if user_records:
            if run_info['distance'] > user_records['longest_distance']:
                user_records['longest_distance'] = run_info['distance']
            if run_info['avg_speed'] > user_records['best_pace']:
                user_records['best_pace'] = run_info['avg_speed']
            success_dict = sqlhelper.update('user_id',user_records,params.DB_NAME_EXP,params.USER_RECORDS)
        else:
            user_records['user_id'] = user_id
            user_records['longest_distance'] = run_info['distance']
            user_records['best_pace'] = run_info['avg_speed']
            success_dict = sqlhelper.insert("",params.DB_NAME_EXP, params.USER_RECORDS,user_records)
                 
        return success_dict
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}


def get_user_records(user_id):
    try:
        records = sqlhelper.select(user_id,'user_id',params.DB_NAME_EXP,params.USER_RECORDS)
        
        user_records = {}
        for row in records:
            user_records['user_id'] = row[0]
            user_records['longest_distance'] = row[1]
            user_records['best_pace'] = row[2]
        return user_records
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
      
##### FOR CRON-SERVER ###########

# Returns the user ids
def get_all_users():
    try:
        users = sqlhelper.select_all(params.DB_NAME_EXP, params.USER_INFO)
        
        user_ids = []
        for row in users:
            user_ids.append(row[0])
        return user_ids
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
    
##### INIT SCRIPT ##############
# To setup the initial tables in the DB

def init_badge_setup():
    try:
        badge_dict = {}
        badge_dict['id'], badge_dict['name'], badge_dict['description'], badge_dict['minimum_val'] = badgeParams.NATURE, 'NATURE', badgeParams.NATURE_DESC, badgeParams.NATURE_THRESH
        sqlhelper.insert("",params.DB_NAME_EXP, params.BADGE_INFO,badge_dict)
        badge_dict['id'], badge_dict['name'], badge_dict['description'], badge_dict['minimum_val'] = badgeParams.FORREST_GUMP, 'FORREST_GUMP', badgeParams.FORREST_GUMP_DESC, badgeParams.FORREST_GUMP_THRESH
        sqlhelper.insert("",params.DB_NAME_EXP, params.BADGE_INFO,badge_dict)
        badge_dict['id'], badge_dict['name'], badge_dict['description'], badge_dict['minimum_val'] = badgeParams.HIGH_INTENSITY, 'HIGH_INTENSITY', badgeParams.HIGH_INTENSITY_DESC, badgeParams.HIGH_INTENSITY_THRESH
        sqlhelper.insert("",params.DB_NAME_EXP, params.BADGE_INFO,badge_dict)
        badge_dict['id'], badge_dict['name'], badge_dict['description'], badge_dict['minimum_val'] = badgeParams.MOUNTAINEER, 'MOUNTAINEER', badgeParams.MOUNTAINEER_DESC, badgeParams.MOUNTAINEER_THRESH
        sqlhelper.insert("",params.DB_NAME_EXP, params.BADGE_INFO,badge_dict)
        badge_dict['id'], badge_dict['name'], badge_dict['description'], badge_dict['minimum_val'] = badgeParams.LAZY, 'LAZY', badgeParams.LAZY_DESC, badgeParams.LAZY_THRESH
        sqlhelper.insert("",params.DB_NAME_EXP, params.BADGE_INFO,badge_dict)
        badge_dict['id'], badge_dict['name'], badge_dict['description'], badge_dict['minimum_val'] = badgeParams.SURRENDER, 'SURRENDER', badgeParams.SURRENDER_DESC, badgeParams.SURRENDER_THRESH
        sqlhelper.insert("",params.DB_NAME_EXP, params.BADGE_INFO,badge_dict)
        badge_dict['id'], badge_dict['name'], badge_dict['description'], badge_dict['minimum_val'] = badgeParams.CRAWLER, 'CRAWLER', badgeParams.CRAWLER_DESC, badgeParams.CRAWLER_THRESH
        sqlhelper.insert("",params.DB_NAME_EXP, params.BADGE_INFO,badge_dict)
        badge_dict['id'], badge_dict['name'], badge_dict['description'], badge_dict['minimum_val'] = badgeParams.CHALLENGE_COMPLETED, 'CHALLENGE_COMPLETED', badgeParams.CHALLENGE_COMPLETED_DESC, badgeParams.CHALLENGE_COMPLETED_THRESH
        sqlhelper.insert("",params.DB_NAME_EXP, params.BADGE_INFO,badge_dict)
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}

def init():
    try:
        # Create the 3 databases required
        sqlhelper.create_db(params.DB_NAME_EXP)
        sqlhelper.create_db(params.DB_NAME_RUN)
        sqlhelper.create_db(params.DB_NAME_BADGE)
        
        # Create the required tables
        if sqlhelper.check_table_exists(params.DB_NAME_EXP,params.LOGIN_INO) == False:
            columns = "id INT NOT NULL AUTO_INCREMENT, username VARCHAR(2000) NOT NULL, password VARCHAR(2000) NOT NULL, firstlogin INT(1) DEFAULT '0', PRIMARY KEY (id)"
            sqlhelper.create_table(params.DB_NAME_EXP,params.LOGIN_INO,columns)
        else:
            logger.info("All tables already exists, exiting init phase")
            return params.SUCCESS
        
        if sqlhelper.check_table_exists(params.DB_NAME_EXP,params.USER_INFO) == False:
            columns = "user_id INT(10) NOT NULL, sex TINYINT(1) NOT NULL DEFAULT '0', age INT(3) NOT NULL DEFAULT '25', height FLOAT NOT NULL DEFAULT '160', weight FLOAT NOT NULL DEFAULT '70', PRIMARY KEY (user_id)"
            sqlhelper.create_table(params.DB_NAME_EXP,params.USER_INFO,columns)
            
        if sqlhelper.check_table_exists(params.DB_NAME_EXP,params.USER_RECORDS) == False:
            columns = "user_id INT(10) NOT NULL, longest_distance FLOAT NOT NULL, best_pace FLOAT NOT NULL, PRIMARY KEY (user_id)"
            sqlhelper.create_table(params.DB_NAME_EXP,params.USER_RECORDS,columns)
            
        if sqlhelper.check_table_exists(params.DB_NAME_EXP,params.RUN_INFO) == False:
            columns = "user_id INT(10) NOT NULL, distance INT(10) DEFAULT NULL, nature_D INT(10) DEFAULT NULL, urb_D INT(10) DEFAULT NULL, kcal FLOAT NOT NULL DEFAULT '0', xp FLOAT NOT NULL DEFAULT '0', PRIMARY KEY (user_id)"
            sqlhelper.create_table(params.DB_NAME_EXP,params.RUN_INFO,columns)
            
        if sqlhelper.check_table_exists(params.DB_NAME_EXP,params.FXD_PREFERENCES) == False:
            columns = "user_id INT(10) NOT NULL, length FLOAT NOT NULL, elevation FLOAT NOT NULL, ped_friend FLOAT NOT NULL, uniqueness FLOAT NOT NULL, nature FLOAT NOT NULL, avg_num INT(10) NOT NULL DEFAULT '1', PRIMARY KEY (user_id)"
            sqlhelper.create_table(params.DB_NAME_EXP,params.FXD_PREFERENCES,columns)
            
        if sqlhelper.check_table_exists(params.DB_NAME_EXP,params.BADGE_INFO) == False:
            columns = "id INT(10) NOT NULL, name VARCHAR(200) NOT NULL, description VARCHAR(2000) NOT NULL, minimum_val FLOAT NOT NULL, PRIMARY KEY (id)"
            sqlhelper.create_table(params.DB_NAME_EXP,params.BADGE_INFO,columns)
            init_badge_setup()
        logger.info("INIT SUCCESS")
        return params.SUCCESS  
    except Exception as e:
        logger.error(str(e))
        return params.FAILURE