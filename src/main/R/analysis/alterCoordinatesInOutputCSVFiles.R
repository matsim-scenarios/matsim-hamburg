# Title     : TODO
# Objective : TODO
# Created by: tschlenther
# Created on: 22.09.2021
library(tidyverse)
library(plyr)
library(dplyr)


runDir = "D:/ReallabHH/v2.2/basierendAufP2-3-5/output-speeded-sharing10pct-hamburg-v2.2-reallabHH2030/"
runId = "hamburg-v2.0-10pct-reallab2030"
inputTripsFile <- paste(runDir, runId, ".output_trips.csv.gz", sep = "")
inputPersonsFile <- paste(runDir, runId, ".output_persons.csv.gz", sep = "")
outputTripsFile =  paste(runDir, runId, ".output_trips_modified.csv", sep = "")
outputPersonsFile =  paste(runDir, runId, ".output_persons_modified.csv", sep = "")


trips <- read.csv2(inputTripsFile)
persons <- read.csv2(inputPersonsFile)

trips_modified <- trips %>%
  mutate (start_x = round_any(as.numeric(start_x),100),
          start_y = round_any(as.numeric(start_y),100),
          end_x = round_any(as.numeric(end_x),100),
          end_y = round_any(as.numeric(end_y),100))

persons_modified <- persons %>% 
  mutate (first_act_x = round_any(as.numeric(first_act_x),100),
          first_act_y = round_any(as.numeric(first_act_y),100))

##trips
#first dump out
write.csv2(trips_modified, file = outputTripsFile, sep = ";", quote = FALSE)
#then gzip
gzip(filename = outputTripsFile, destname = paste(outputTripsFile, ".gz", sep = ""), overwrite = TRUE, remove = TRUE)

##persons
#first dump out
write.csv2(persons_modified, file = outputPersonsFile, sep = ";", quote = FALSE)
#then gzip
gzip(filename = outputPersonsFile, destname = paste(outputPersonsFile, ".gz", sep = ""), overwrite = TRUE, remove = TRUE)