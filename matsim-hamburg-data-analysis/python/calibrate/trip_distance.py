# %%
import geopandas as gpd
import pandas as pd
from shapely.geometry import Point

trips_info_folder = '/Users/meng/work/realLabHH_meng/calibrate/trips_info/'
scenario_scale = '25'
scenario_id = '1'

trips_ending = '.output_trips.csv.gz'
trips_raw_data = pd.read_csv(trips_info_folder + 'hh-' + scenario_scale + 'pct-' + scenario_id + trips_ending, sep=';')

person_to_home = pd.read_csv('/Users/meng/shared-svn/projects/matsim-hamburg/hamburg-v1.0/person_specific_info'
                             '/person2homeLocation.csv')
hamburg = gpd.read_file("/Users/meng/work/realLabHH_meng/files/shapeFIle/hamburg_merge/hamburg.shp")

trips_raw_data = trips_raw_data.merge(right=person_to_home, left_on=['person'], right_on=['person'], how='left')


points = trips_raw_data.apply(lambda row: Point(row['home_x'], row['home_y']), axis=1)
points_geo = gpd.GeoDataFrame(trips_raw_data, geometry=points, crs=hamburg.crs)
# %%
person_home = gpd.GeoDataFrame(person_to_home, geometry=points, crs=hamburg.crs)
# %%
person_in_hamburg = gpd.sjoin(person_home, hamburg, how='inner', op='within')
# %%
person_in_hamburg['AreaType'].value_counts()
# %%
home_in_hamburg = gpd.sjoin(points_geo, hamburg, how='inner', op='within')

trips_in_hamburg = home_in_hamburg[home_in_hamburg['AreaType'] > 0]

# %%
def get_trip_dis_bin(x):
    if x < 500:
        return "0-500m"
    elif x < 1000:
        return "500-1000m"
    elif x < 2000:
        return "1-2km"
    elif x < 5000:
        return "2-5km"
    elif x < 10000:
        return "5-10km"
    elif x < 20000:
        return "10-20km"
    elif x < 50000:
        return "20-50km"
    elif x < 100000:
        return "50-100km"
    else:
        return "100km+"


trips_in_hamburg['dis_bins'] = trips_in_hamburg['traveled_distance'].apply(lambda x: get_trip_dis_bin(x))

# %%
trips_in_hamburg['dis_bins'].value_counts().sort_index()

# %%
con = (trips_raw_data['traveled_distance'] < 500) & (trips_raw_data['traveled_distance'] > 0)

trips_raw_data.loc[con, 'main_mode'].value_counts()

# %%
con = (trips_raw_data['traveled_distance'] < 1000) & (trips_raw_data['traveled_distance'] > 500)

trips_raw_data.loc[con, 'main_mode'].value_counts()
# %%
con = (trips_raw_data['traveled_distance'] < 2000) & (trips_raw_data['traveled_distance'] > 1000)

trips_raw_data.loc[con, 'main_mode'].value_counts()
# %%
con = (trips_raw_data['traveled_distance'] < 5000) & (trips_raw_data['traveled_distance'] > 2000)

trips_raw_data.loc[con, 'main_mode'].value_counts()
# %%
con = (trips_raw_data['traveled_distance'] < 10000) & (trips_raw_data['traveled_distance'] > 5000)

trips_raw_data.loc[con, 'main_mode'].value_counts()
# %%
con = (trips_raw_data['traveled_distance'] < 20000) & (trips_raw_data['traveled_distance'] > 10000)

trips_raw_data.loc[con, 'main_mode'].value_counts()
# %%
con = (trips_raw_data['traveled_distance'] < 50000) & (trips_raw_data['traveled_distance'] > 20000)

trips_raw_data.loc[con, 'main_mode'].value_counts()
# %%
con = (trips_raw_data['traveled_distance'] < 100000) & (trips_raw_data['traveled_distance'] > 50000)

trips_raw_data.loc[con, 'main_mode'].value_counts()
# %%
con = (trips_raw_data['traveled_distance'] > 100000)

trips_raw_data.loc[con, 'main_mode'].value_counts()
