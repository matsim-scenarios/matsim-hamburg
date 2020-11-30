# %%
import geopandas as gpd
import pandas as pd
from shapely.geometry import Point

trips_info_folder = '/Users/meng/work/realLabHH/calibrate/trips_info/'
scenario_scale = '1'
scenario_id = '19'


trips_ending = '.output_trips.csv.gz'
trips_raw_data = pd.read_csv(trips_info_folder + 'hh-' + scenario_scale + 'pct-' + scenario_id + trips_ending, sep=';')

person_to_home = pd.read_csv('/Users/meng/IdeaProjects/shared-svn/projects/matsim-hamburg/hamburg-v1.0'
                             '/person_specific_info/person2homeLocation.csv')
hamburg = gpd.read_file("/Users/meng/work/realLabHH/files/shapeFIle/hamburg_merge/hamburg.shp")
# %%
trips_raw_data = trips_raw_data.merge(right=person_to_home, left_on=['person'], right_on=['person'], how='left')

# %%
points = trips_raw_data.apply(lambda row: Point(row['home_x'], row['home_y']), axis=1)
points_geo = gpd.GeoDataFrame(trips_raw_data, geometry=points, crs=hamburg.crs)

# %%
home_in_hamburg = gpd.sjoin(points_geo, hamburg, how='inner', op='within')
# %%
mode_counts = home_in_hamburg.groupby(['AreaType'])['main_mode'].value_counts()
# %%
mode_counts = mode_counts.unstack(level=0)
# %%
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
# %%
mode_share.T.to_csv('../../results/' + 'hh-' + scenario_scale + 'pct-' + scenario_id + '-mode_share.csv')
