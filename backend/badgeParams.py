# Same as the DB table badge_info
NATURE = 1
FORREST_GUMP = 2
HIGH_INTENSITY = 3
MOUNTAINEER = 4
LAZY = 5
SURRENDER = 6
CRAWLER = 7
CHALLENGE_COMPLETED = 8


##################################################################
# Below vars to be used during the initialization od the database(setup)
# badges description

NATURE_DESC = 'Total distance(meters) covered in nature in a week'
FORREST_GUMP_DESC = 'Time spent(minutes) running in a week'
HIGH_INTENSITY_DESC = 'High average pace(km/h) reached in a week'
MOUNTAINEER_DESC = 'Elevation(meters) covered in a week'
LAZY_DESC = 'Very small distance covered in a week(meters), minimum_val means max here'
SURRENDER_DESC = 'Run not finished'
CRAWLER_DESC = 'Never touched the average running speed(km/h) in a week'
CHALLENGE_COMPLETED_DESC = 'Challenge of the week done'

# THRESHOLD VALUES FOR EACH BADGE

NATURE_THRESH = 10000
FORREST_GUMP_THRESH = 600
HIGH_INTENSITY_THRESH = 16.1
MOUNTAINEER_THRESH = 1000
LAZY_THRESH = 7000
SURRENDER_THRESH = -1   # Threshold doesn't make sense here
CRAWLER_THRESH = 12
CHALLENGE_COMPLETED_THRESH = -1  # Threshold doesn't make sense here
