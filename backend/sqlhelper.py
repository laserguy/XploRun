import pymysql
import params

import logging
logger = logging.getLogger(__name__)

def connectDB(dbname):
    connection = pymysql.connect(host=params.DB_SERVER,user=params.USER_NAME,passwd=params.PASSWORD,database=dbname, port=params.PORT )
    return connection   

# creates a connection, if dbcon is not passed explicitly, therefore it commits and collects it by itself
# If it comes from outside then it does nothing on the dbcon object

## The element_dict will contain the mulitple params as dict
# In case of insert, it will be dict with key value pairs

def insert(dbcon, dbname, tablename, element_dict):
    try:
        closeConnection = False
        if dbcon == "":
            closeConnection = True
            dbcon = connectDB(dbname)

        query = "INSERT INTO {} ".format(tablename)
        val_names = "("
        values = " VALUES ("

        for key in element_dict:
            val_names += "{}, ".format(key)
            if key.find('id') != -1 or key == 'Id' or key == 'avg_num':
                values += "{}, ".format(element_dict[key])
            else:
                values += "'{}', ".format(element_dict[key])

        val_names = val_names[:-2]
        val_names += ")"
        values = values[:-2]
        values += ")"

        query = query + val_names + values

        cursor = dbcon.cursor()
        cursor.execute(query)
        insert_id = dbcon.insert_id()

        if closeConnection == True:
            dbcon.commit()
            dbcon.close()
        return {'insert_id': insert_id}
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}

# SELECT JUST BY PRIMARY KEY  
def select(id, primary_key, dbname, tablename):
    try:
        dbcon = connectDB(dbname)
        cursor = dbcon.cursor()
        query = "SELECT * FROM {} WHERE {} = {}".format(tablename, primary_key, id)
        cursor.execute(query)
        record = cursor.fetchall()

        dbcon.close()

        return record
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}

# SELECT ALL

def select_all(dbname,tablename):
    try:
        dbcon = connectDB(dbname)
        cursor = dbcon.cursor()
        query = "SELECT * FROM {}".format(tablename)
        cursor.execute(query)
        record = cursor.fetchall()

        dbcon.close()

        return record
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}

# SELECT by multiple parameters in the element_dict    
def select_mulparams(dbname, element_dict, tablename):
    try:
        dbcon = connectDB(dbname)
        cursor = dbcon.cursor()
        query = "SELECT * FROM {} ".format(tablename)
        where_clause = "WHERE"

        for key in element_dict:
            where_clause +=" {} = '{}' AND ".format(key,element_dict[key])

        where_clause = where_clause[:-5]
        query += where_clause

        cursor.execute(query)
        record = cursor.fetchall()

        dbcon.close()

        return record
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
# SELECT last few rows from the table ordered by DATE(Primary KEY)
def select_last_rows(dbname, tablename,row_count):
    try:
        dbcon = connectDB(dbname)
        cursor = dbcon.cursor()
        query = "SELECT * FROM {} ORDER BY date DESC LIMIT {}".format(tablename,row_count)
        cursor.execute(query)
        record = cursor.fetchall()

        dbcon.close()

        return record
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}


def update(primary_key, element_dict, dbname, tablename):
    try:
        dbcon = connectDB(dbname)
        cursor = dbcon.cursor()
        #query = "UPDATE {} SET {} = {} WHERE {} = {}".format(tablename, column, value, primary_key, id)
        query = "UPDATE {} ".format(tablename)
        set_clause = "SET "
        where_clause = "WHERE "
        for key in element_dict:
            if key == primary_key:
                where_clause += "{} = {}".format(key, element_dict[key])
            else:
                if key.find('id') != -1 or key == 'Id' or key == 'avg_num':
                    set_clause += "{} = {}, ".format(key, element_dict[key])
                else:
                    set_clause += "{} = '{}', ".format(key, element_dict[key])

        set_clause = set_clause[:-2]
        set_clause += " "
        query = query + set_clause + where_clause

        cursor.execute(query)

        dbcon.commit()
        dbcon.close()
        return {'success': True}
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}

def delete(id, primary_key, dbname, tablename):
    try:
        dbcon = connectDB(dbname)
        cursor = dbcon.cursor()
        query = "DELETE FROM {} WHERE {} = {}".format(tablename, primary_key, id)
        cursor.execute(query)

        dbcon.commit()
        dbcon.close()
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
def check_table_exists(dbname, tablename):
    try:
        dbcon = connectDB(dbname)
        cursor = dbcon.cursor()
        cursor.execute("""
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_name = '{0}'
            """.format(tablename.replace('\'', '\'\'')))
        if cursor.fetchone()[0] == 1:
            cursor.close()
            return True

        cursor.close()
        dbcon.close()
        return False
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}


def create_table(dbname, tablename, columns):
    try:
        dbcon = connectDB(dbname)
        cursor = dbcon.cursor()
        query = "CREATE TABLE {}({}) ".format(tablename,columns)
        cursor.execute(query)

        dbcon.commit()
        cursor.close()
        dbcon.close()
        return True
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}
    
    
def create_db(dbname):
    try:
        dbcon = pymysql.connect(host=params.DB_SERVER,user=params.USER_NAME,passwd=params.PASSWORD, port=params.PORT )
        cursor = dbcon.cursor()
        query = "CREATE DATABASE IF NOT EXISTS {}".format(dbname)
        cursor.execute(query)

        dbcon.commit()
        cursor.close()
        dbcon.close()
        return True
    except Exception as e:
        logger.error(str(e))
        return {'error': str(e)}