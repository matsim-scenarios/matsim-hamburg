import os

import geopandas as gpd
import pandas as pd
from shapely.geometry import Point

# %%
# give the folder path:

# give the folder path:
runId = "hamburg-v2.0-10pct-reallab2030"
trips_info_folder = 'D:/ReallabHH/output-' + runId + '/'
trips_info_folder = 'D:/ReallabHH/runs/reallabHH2030/r11/'

#version = 'v1.1'
#scenario_scale = '10'
#scenario_id = ''
#runId = 'hamburg-' + version + "-" + scenario_scale + 'pct'

#output file path
outputDir = trips_info_folder + 'analysis/'
outputFile =  outputDir + runId + '.distance_distribution_per_mode.csv'

print("reading output trips and person2homeCoord csv files")
#trips_ending = 'pct.output_trips.csv.gz'
trips_ending = '.output_trips.csv.gz'

print("will read from " + trips_info_folder +  runId + trips_ending)
trips = pd.read_csv(trips_info_folder +  runId + trips_ending, sep=';')


# put the file here which contains information about person and their home coordinate. Download it from:
# shared-svn/projects/matsim-hamburg/hamburg-v1/person2homeCoord.csv

person_home_location = pd.read_csv('D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v1/person2homeCoord.csv')
print("... finished")

# %%
# ---------------------------- geo process
trips = trips.merge(right=person_home_location, how='left', left_on=['person'], right_on=['person'])
person_home_location = trips[['person', 'home_x', 'home_y']]
trips_start_location = trips[['person', 'start_x', 'start_y']]
trips_end_location = trips[['person', 'end_x', 'end_y']]
# %%
# put the shapefile of hamburg here, Download it from:
# /shared-svn/projects/realLabHH/data/hamburg_shapeFile/hamburg_merge/hamburg.shp
shapeFile = "D:/svn/shared-svn/projects/realLabHH/data/hamburg_shapeFile/hamburg_merge/hamburg.shp"

print("reading shape file")
hamburg_map = gpd.read_file(shapeFile)
print("... finished")

# %%
def geo_merge(df, name):
    points = df.apply(lambda row: Point(row[name + '_x'], row[name + '_y']), axis=1)
    points_geo = gpd.GeoDataFrame(df, geometry=points, crs=hamburg_map.crs)
    df_shape = gpd.sjoin(points_geo, hamburg_map, how='inner', op='within')
    df_shape = pd.DataFrame(df_shape)
    df_shape = df_shape[~df_shape.index.duplicated()]
    df_shape = pd.concat([trips, df_shape], axis=1)
    trips[name + '_area'] = df_shape['AreaType']

print("perform geo merge(s)")
geo_merge(person_home_location, 'home')
geo_merge(trips_start_location, 'start')
geo_merge(trips_end_location, 'end')
print(".... done")

# %%
## distance distrbution
def get_trip_dis_bin(x):
    if x < 500:
        return "0.5"
    elif x < 1000:
        return "1"
    elif x < 2000:
        return "2"
    elif x < 5000:
        return "5"
    elif x < 10000:
        return "10"
    elif x < 20000:
        return "20"
    elif x < 50000:
        return "50"
    elif x < 100000:
        return "100"
    else:
        return "100+"

print("apply distance distribution")
trips['dis_bins_rou'] = trips['traveled_distance'].apply(lambda x: get_trip_dis_bin(x))
trips['dis_bins_euc'] = trips['euclidean_distance'].apply(lambda x: get_trip_dis_bin(x))


# %%

def get_trip_dis_dis(trips_dataframe):
    rou_distance_dist = trips_dataframe.groupby(['main_mode', 'dis_bins_rou'])['person'].count()
    rou_distance_dist = rou_distance_dist.unstack().fillna(0)
    rou_distance_dist = rou_distance_dist[['0.5', '1', '2', '5', '10', '20', '50', '100', '100+']]
    rou_distance_dist['dis_type'] = 'routed'

    euc_distance_dist = trips_dataframe.groupby(['main_mode', 'dis_bins_euc'])['person'].count()
    euc_distance_dist = euc_distance_dist.unstack().fillna(0)
    euc_distance_dist = euc_distance_dist[['0.5', '1', '2', '5', '10', '20', '50', '100', '100+']]
    euc_distance_dist['dis_type'] = 'euclidean'

    data = pd.concat([rou_distance_dist, euc_distance_dist])
    return data


# %%
out_dis_dist = get_trip_dis_dis(trips[trips['home_area'] == 0])
out_dis_dist['home_area'] = 'outer'
hvv_dis_dist = get_trip_dis_dis(trips[trips['home_area'] == 1])
hvv_dis_dist['home_area'] = 'hvv-umland'
hamburg_dis_dist = get_trip_dis_dis(trips[trips['home_area'] == 2])
hamburg_dis_dist['home_area'] = 'city'
metro_dis_dist = get_trip_dis_dis(trips)
metro_dis_dist['home_area'] = 'metropolregion'
print("... done")

print("write output to " + outputFile)
if not os.path.exists(outputDir):
    os.mkdir(trips_info_folder + 'analysis/')
pd.concat([metro_dis_dist, hamburg_dis_dist, hvv_dis_dist, out_dis_dist]).to_csv(
    outputFile, sep=";")
print("...done")
print("FINISHED")
