#%%
import pandas as pd
import matplotlib.pyplot as plt

person_income = pd.read_csv('/Users/meng/IdeaProjects/shared-svn/projects/matsim-hamburg/hamburg-v1.0'
                            '/person_specific_info/person2income.csv')
person_to_home = pd.read_csv('/Users/meng/IdeaProjects/shared-svn/projects/matsim-hamburg/hamburg-v1.0'
                             '/person_specific_info/person2homeLocation.csv')
#%%
person = pd.merge(
    left=person_income,
    right=person_to_home,
    left_on='person',
    right_on='person',
    how='inner'
)

#%%
#color = [str(item/255.) for item in person['true_income']]
plt.scatter(x=person['home_x'], y=person['home_y'], s=0.01, c=person['true_income'], cmap='viridis')
plt.colorbar()
plt.show()
