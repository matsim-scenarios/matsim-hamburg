#%%
import geopandas as gpd
import pandas as pd
from shapely.geometry import Point
#%%

legs = pd.read_csv("/Users/meng/work/realLabHH_meng/calibrate/legs_info/hh-1pct-25.output_legs.csv.gz",sep=";")
links_to_park_pressure = pd.read_csv("/Users/meng/shared-svn/projects/matsim-hamburg/hamburg-v1.0"
                                     "/network_specific_info/link2parkpressure.csv")

data = [[1.,0],[0.85,720],[0.7,1200]]


#%%
car_legs = legs[legs['mode'] == 'car']

#%%
hamburg = gpd.read_file("/Users/meng/work/realLabHH_meng/files/shapeFIle/hamburg_merge/hamburg.shp")
points = car_legs.apply(lambda row: Point(row['end_x'], row['end_y']), axis=1)
points_geo = gpd.GeoDataFrame(car_legs, geometry=points, crs=hamburg.crs)

car_legs_in_hamburg = gpd.sjoin(points_geo, hamburg[hamburg['AreaType'] == 2], how='inner', op='within')

#%%
park_to_legs = pd.merge(left=car_legs_in_hamburg,
                        right=links_to_park_pressure,
                        left_on=['end_link'],
                        right_on=['link_id'],
                        how='left')
park_to_legs['Parkpl'] = park_to_legs['Parkpl'].fillna(1)
#%%

park_time = pd.DataFrame(data,columns=['park_pressure','park_time'])

park_to_legs = park_to_legs.merge(
                        right=park_time,
                        left_on=['Parkpl'],
                        right_on=['park_pressure'],
                        how='left')

#%%
park_to_legs['park_time'].describe()
