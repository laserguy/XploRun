from re import A
import sqlhelper
import time
from datetime import datetime

import logging
logger = logging.getLogger(__name__)

# Runs the query similar to below
# SELECT DATE(from_unixtime(date)) as day, SUM(distance) as distance FROM 1_run_history
# TO BE USED WHEN:
# elements = list of columns on which you want to perform GROUP BY SUM operations
# The table should contain a column 'date'
# past_days = For how many previous days you want the data
# Function returns the SUM on given columns grouped by days
def select_group_rows(dbname,tablename,elements,past_days=0):
    try:
        dbcon = sqlhelper.connectDB(dbname)
        cursor = dbcon.cursor()
        #query = "SELECT DATE(from_unixtime({})), SUM({}),SUM({})..... FROM {} WHERE user_id = {}"
        # .format(tablename, column, value, primary_key, id)
        query = "SELECT DATE(from_unixtime({})), COUNT(*)".format('date')
        
        for element in elements:
            query += ', SUM({})'.format(element)
            
        query += ' FROM {} '.format(tablename)
        
        if past_days != 0:
            now = datetime.now()
            seconds_since_midnight = (now - now.replace(hour=0, minute=0, second=0, microsecond=0)).total_seconds()
            seconds = (past_days-1)*24*60*60 + seconds_since_midnight
            duration = int(time.time()) - seconds
            query += 'WHERE {} >= {}'.format('date', duration)
            query += ' GROUP BY 1'
            query += ' LIMIT {}'.format(past_days)
        
        cursor.execute(query)
        record = cursor.fetchall()

        dbcon.close()

        return record
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
    
    
def stats_summation(dbname,tablename,elements,past_days=0):
    try:
        dbcon = sqlhelper.connectDB(dbname)
        cursor = dbcon.cursor()
        query = "SELECT "
        
        for ele in elements:
            # We are passing avg_speed as the last parameter, so no comma needed after MIN(ele)
            if ele == 'avg_speed':
                query += 'MAX({}) '.format(ele, ele)
            else:
                query += 'SUM({}) ,'.format(ele)
            
        
        query += ' FROM {} '.format(tablename)
        
        if past_days != 0:
            now = datetime.now()
            seconds_since_midnight = (now - now.replace(hour=0, minute=0, second=0, microsecond=0)).total_seconds()
            seconds = (past_days-1)*24*60*60 + seconds_since_midnight
            duration = int(time.time()) - seconds
            query += 'WHERE {} >= {}'.format('date', duration)
            
        cursor.execute(query)
        record = cursor.fetchall()
        dbcon.close()
        return record
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}