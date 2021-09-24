library(lubridate)
library(tidyverse)
library(dplyr)


trips <- read.csv2("[RUN_ID].output_trips.csv.gz")


walks <- trips %>% 
  filter(main_mode == "walk") %>% 
  mutate(trav_time = hms(trav_time)) %>% 
  mutate(trav_time_s = hour(trav_time) * 3600 + minute(trav_time) * 60 + second(trav_time) * 1) %>% 
  mutate(speed = traveled_distance / trav_time_s)

problematic <- walks %>% 
  filter(hour(trav_time) > 1)

walks_wo50km <- walks %>% 
  filter(traveled_distance <= 50 * 1000)

walks_wo20km <- walks %>% 
  filter(traveled_distance <= 20 * 1000)

boxplot(walks$trav_time_s, walks_wo50km$trav_time_s, walks_wo20km$trav_time_s)
max(walks_wo50km$trav_time_s) / 3600
max(walks_wo20km$trav_time_s) / 3600

boxplot(walks_wo20km$trav_time_s)
