#%%
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
#%%
trips_info_folder = '/Users/meng/work/realLabHH/calibrate/trips_info/'
scenario_scale = '1'
scenario_id_1 = '15'
scenario_id_2 = '16'


trips_ending = '.output_trips.csv.gz'
trips_raw_data_1 = pd.read_csv(trips_info_folder + 'hh-' + scenario_scale + 'pct-' + scenario_id_1 + trips_ending, sep=';')
trips_raw_data_1 = trips_raw_data_1.set_index('trip_id')

trips_raw_data_2 = pd.read_csv(trips_info_folder + 'hh-' + scenario_scale + 'pct-' + scenario_id_2 + trips_ending, sep=';')
trips_raw_data_2 = trips_raw_data_2.set_index('trip_id')

trips_raw_data_1 = trips_raw_data_1.loc[:,('person','main_mode')]
trips_raw_data_1['new mode'] = trips_raw_data_2['main_mode']

#%% income based mode switch analysis
person_income = pd.read_csv('/Users/meng/IdeaProjects/shared-svn/projects/matsim-hamburg/hamburg-v1.0'
                            '/person_specific_info/person2income.csv')
trips_raw_data_1 = trips_raw_data_1.merge(right=person_income, left_on=['person'], right_on= ['person'], how= 'left')
# income_based_mode_switch = trips_raw_data_1.groupby(by = ['main_mode','mode_change']).apply(lambda x : (
# x.drop_duplicates(subset=['person']))['true_income'].describe())
#%%
data = trips_raw_data_1.groupby(by = ['main_mode','new mode']).apply(lambda x : ( x.drop_duplicates(subset=['person']))['true_income'])

data = data.reset_index()
#%%
# Draw Plot
sns.boxplot(x='main_mode', y='true_income', data=data.loc[data['main_mode'] != 'ride'], hue='new mode')
plt.xlabel('previous mode')

for i in range(len(data['main_mode'].unique())-2):
    plt.vlines(i+.5, 10, 160000, linestyles='solid', colors='gray', alpha=0.2)

plt.show()