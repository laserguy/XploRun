import schedule
import time
import datalayer
import user_stats

import logging.config
logging.basicConfig(format='%(asctime)s %(message)s',
                    datefmt='%d/%m/%Y %I:%M:%S %p',
                    filename='cron-server.log',
                    filemode='w',
                    level=logging.DEBUG)

# Update the badges of all the users before the next cycle starts
def update_badges():
    try:
        import logging
        logger = logging.getLogger(__name__)
        logger.info("CRON STARTED")
        
        user_ids = datalayer.get_all_users()
        badges_info = datalayer.get_badge_info()
        
        for user_id in user_ids:
            try:
                week_stats = datalayer.get_weekly_stats(user_id)
                if not week_stats:  # If no stats are available
                    continue
                
                badges_dict = user_stats.badges_calculation_cron(week_stats,badges_info)
                datalayer.update_user_badges(user_id,badges_dict)
            except Exception as e:
                print("Cron failed for User: "+ str(user_id))
                logger.error("Cron failed for user: "+str(user_id) )
        logger.info("CRON ENDED")
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}

# GMT TIME    
schedule.every().sunday.at("23:55").do(update_badges)
#schedule.every().tuesday.at("10:41").do(update_badges)  # FOR TESTTING

while True:
    schedule.run_pending()
    time.sleep(1)   