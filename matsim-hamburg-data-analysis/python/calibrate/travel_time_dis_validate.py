#%%
import pandas as pd
import matplotlib.pyplot as plt

validated_trips = pd.read_csv("/Users/meng/work/realLabHH/calibrate/HERE/here_validation_freeFlow_1,7_2020-10-15_00:00:00-36:00:00/validated_trips.csv", sep=";")
time_bin = 6
#%%
time = range(0,36,time_bin)
time_string = []
for i in time:
    time_string.append(str(i) + '-' + str(i+time_bin))

#%%
validated_trips['time'] = validated_trips['departureTime'].apply(lambda x: time_string[int(x/3600/time_bin)])
validated_trips = validated_trips.set_index('time')

#%%
fig = plt.figure()
n = 1
for time in time_string:
    if(time in validated_trips.index):
        ax = plt.subplot(2,3,n)
        n = n+1
        x=range(0,8000,1000)
        ax.plot(x,x,c='r')
        ax.scatter(x = validated_trips.loc[time,'traveltimeActual'], y = validated_trips.loc[time,'traveltimeValidated'], s=1)
        ax.set_xlim(0,8000)
        ax.set_ylim(0,8000)
        ax.set_xlabel(time)
        # ax.set_ylabel('traveltimeValidated')
plt.show()
