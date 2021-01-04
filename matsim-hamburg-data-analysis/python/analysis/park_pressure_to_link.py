#%%
import geopandas as gpd
import matsim
from shapely.geometry import Point

#%%
net = matsim.read_network("/Users/meng/public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1.0-1pct/input"
                          "/hamburg-v1.0-network-with-pt.xml.gz")

park_pressure = gpd.read_file("/Users/meng/shared-svn/projects/realLabHH/data/Parkdruckdaten_HamburgerHochbahnAG"
                              "/Parkplatzverfuegbarkeit_20180712/Parkpl_U5.shp")

#%%
links = net.links
links = links.merge(net.nodes,
       left_on='from_node',
       right_on='node_id')
links = links.merge(net.nodes,
       left_on='to_node',
       right_on='node_id',
       suffixes=('_from_node', '_to_node'))

#%%
links['x_centroid'] = links.apply(lambda row: (row.x_from_node + row.x_to_node) * 0.5, axis = 1 )
links['y_centroid'] = links.apply(lambda row: (row.y_from_node + row.y_to_node) * 0.5, axis = 1 )
#%%
points = links.apply(lambda row: Point(row['x_centroid'], row['y_centroid']), axis=1)
points_geo = gpd.GeoDataFrame(links, geometry=points, crs="epsg:25832")

#%%
park_pressure = park_pressure.to_crs(points_geo.crs)
#%%
link_with_park_pressure = gpd.sjoin(points_geo, park_pressure, how='inner', op='within')
#%%
link_with_park_pressure = link_with_park_pressure[link_with_park_pressure['Parkpl'] < 1]
#%%
link_with_park_pressure[['link_id','Parkpl']].to_csv('link2parkpressure.csv', index=False)

