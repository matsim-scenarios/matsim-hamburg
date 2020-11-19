# %%
import geopandas as gpd
import pandas as pd
from shapely.geometry import Point
import random

# %%
hamburg = gpd.read_file("/Users/meng/work/realLabHH/svn/hamburg/hamburg-v1.0-1pct/original-data/hamburg_shapeFIle"
                        "/hamburg_merge/hamburg.shp")
income = pd.read_csv("data/spatially_avg_income_2017.csv")
person_to_home = pd.read_csv('/Users/meng/IdeaProjects/shared-svn/projects/matsim-hamburg/hamburg-v1.0'
                             '/person_specific_info/person2homeLocation.csv')
# %% split hamburg to hamburg city and HVV umland, then assign the income to each area of them. The reason to do this
# is because there are Harburg in both datasets.

hvv_umland = hamburg[hamburg['AreaType'] < 2]
hamburg_city = hamburg[hamburg['AreaType'] == 2]

hvv_umland_income = income[income['type'].isnull()]
hamburg_city_income = income[income['type'] == 2]

# %% assign income to hvv_umland
hvv_umland = hvv_umland.set_index("name")
hvv_umland_income = hvv_umland_income.set_index("Region")
hvv_umland["income"] = hvv_umland_income["Income"]

# %% assign income to hamburg city
hamburg_city = hamburg_city.set_index("name")
hamburg_city_income = hamburg_city_income.set_index("Region")
hamburg_city["income"] = hamburg_city_income["Income"]
hamburg_city.loc[hamburg_city['income'].isnull(), ['income']] = 24404

# merge the hamburg city and hvv umland together
hvv = hamburg_city.append(hvv_umland)
hvv = hvv.reset_index()
# %%
# give person a income, based on persons home location
# covert person_to_home to geoDataframe and then find the home area in HVV
points = person_to_home.apply(lambda row: Point(row['home_x'], row['home_y']), axis=1)
points_geo = gpd.GeoDataFrame(person_to_home, geometry=points, crs=hvv.crs)
home_in_hamburg = gpd.sjoin(points_geo, hvv, how='inner', op='within')
# %%
# https://de.wikipedia.org/wiki/Einkommensverteilung_in_Deutschland
# Anteil der Personen (%) an allen Personen	10	20	30	40	50	60	70	80	90	100
# Nettoäquivalenzeinkommen(€)	826	1.142	1.399	1.630	1.847	2.070	2.332	2.659	3.156	4.329

average_income = 1957
income_pct_ile = pd.Series([826, 1142, 1399, 1630, 1847, 2070, 2332, 2659, 3156, 4329])
income_factor = income_pct_ile / average_income
# %%
df = home_in_hamburg.loc[:, ['person', 'name','income']]
df = df.reset_index().drop("index", axis=1)


# %%
def func(x):
    x['true_income'] = x['income'].apply(lambda a: a * income_factor.iloc[random.randint(0, 9)])
    return x


data = df.groupby(['name']).apply(func)
# %%
data.loc[:, ['person', 'true_income']].to_csv('../../results/person2income.csv')
