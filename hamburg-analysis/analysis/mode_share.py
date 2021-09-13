# %%
import os

import geopandas as gpd
import pandas as pd
from shapely.geometry import Point

# give the folder path:
runId = "hamburg-v2.0-10pct-reallab2030"
trips_info_folder = 'D:/ReallabHH/output-' + runId + '/'
trips_info_folder = 'D:/ReallabHH/runs/reallabHH2030/r11/'
runId = "h-v2-10pct-sharing-c4-highAcc"
trips_info_folder = 'D:/ReallabHH/runs/testSharing/output-' + runId + '/'


runId = "bC-singleMC"
trips_info_folder = 'D:/ReallabHH/runs/baseCase/calibration/aaa_singleMC/output-' + runId + '/'



#version = 'v1.1'
#scenario_scale = '10'
#scenario_id = '13-3'
#runId = 'hamburg-' + version + "-" + scenario_scale + 'pct'


#output location
outputDir = trips_info_folder + 'analysis/'
outputFileCnt =  outputDir + runId + '.mode-count.csv'
outputFileShr =  outputDir + runId + '.mode-share.csv'

###read data
trips_ending = '.output_trips.csv.gz'
print("reading data from " + trips_info_folder + runId + trips_ending)
trips_raw_data = pd.read_csv(trips_info_folder + runId + trips_ending, sep=';')

# put the file here which contains information about person and their home coordinate. Download it from:
# shared-svn/projects/matsim-hamburg/hamburg-v1/person2homeCoord.csv
person_to_home = pd.read_csv('D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v1/person2homeCoord.csv')

# put the shapefile of hamburg here, Download it from:
# /shared-svn/projects/realLabHH/data/hamburg_shapeFile/hamburg_merge/hamburg.shp
hamburg = gpd.read_file("D:/svn/shared-svn/projects/realLabHH/data/hamburg_shapeFile/hamburg_merge/hamburg.shp")
print("...finished")

###process data
# %%
print("process data")
trips_raw_data = trips_raw_data.merge(right=person_to_home, left_on=['person'], right_on=['person'], how='left')

# %%
points = trips_raw_data.apply(lambda row: Point(row['home_x'], row['home_y']), axis=1)
points_geo = gpd.GeoDataFrame(trips_raw_data, geometry=points, crs=hamburg.crs)

home_in_hamburg = gpd.sjoin(points_geo, hamburg, how='inner', op='within')
# %%
home_in_hamburg =  home_in_hamburg.drop_duplicates(subset=['trip_id'])
mode_counts = home_in_hamburg.groupby(['AreaType'])['main_mode'].value_counts()
mode_counts = mode_counts.unstack(level=0)
mode_counts['Hamburg_city'] = mode_counts[2]
mode_counts['HVV_Umland'] = mode_counts[1]
mode_counts["HVV_gesamt"] = mode_counts[2] + mode_counts[1]


# %%
mode_counts.loc['total'] = mode_counts.sum()
# %%
mode_share = pd.DataFrame(index=mode_counts.index)
mode_share['Hamburg_city'] = mode_counts['Hamburg_city'] / mode_counts.loc['total', 'Hamburg_city']
mode_share['HVV_Umland'] = mode_counts['HVV_Umland'] / mode_counts.loc['total', 'HVV_Umland']
mode_share['HVV_gesamt'] = mode_counts['HVV_gesamt'] / mode_counts.loc['total', 'HVV_gesamt']
print("... finished")
# %%

###dump output
print("dump output to " + outputDir)
if not os.path.exists(outputDir):
    os.mkdir(outputDir)
mode_counts[['Hamburg_city','HVV_Umland','HVV_gesamt']].to_csv(outputFileCnt)
mode_share.T.to_csv(outputFileShr)