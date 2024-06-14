import os

# API keys
GOOGLE_ELEVATION_API_KEY = "AIzaSyBsxi5wccU1DoJ-flGxWQxj47Q9oWLvADU"

# DB
DB_SERVER = os.environ.get('DB_SERVER')
PORT = int(os.environ.get("DB_PORT"))
DB_NAME_EXP = os.environ.get("DB_NAME")
USER_NAME = os.environ.get("DB_USER_NAME")
PASSWORD = os.environ.get("DB_PASSWORD")

# Backend
LOGIN_INO = 'loginfo'
FXD_PREFERENCES = 'fixed_preferences'
USER_INFO = 'user_info'
USER_RECORDS = 'user_records'

# Run
RUN_INFO = 'run_info'
DB_NAME_RUN = 'run_history'
ROUTES_COUNT = 8
LAST_ROWS_COUNT = 10

# Badges
BADGE_INFO = 'badge_info'   #table for generic info of badges
DB_NAME_BADGE = 'user_badgeDB'

# For stats
PAST_DAYS = 7

# Return statement
SUCCESS = 1
FAILURE = 0

