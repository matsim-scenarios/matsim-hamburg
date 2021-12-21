library(tidyverse)
library(dplyr)
library(lubridate)

#define input 
runDir <- "D:/ReallabHH/v2.2/2021-11-12/reallab2030-ff/"
runID <- "hamburg-v2.2-10pct-reallabHH2030"

#read legs table
legs <- read.csv2(paste(runDir, runID, ".output_legs.csv", sep = ""))

# contains a set of person Ids whose trips are to be considered.
#In this case here (HH), this corresponds to a spatial filter: Only inhabitants inside HVV-Area are considered.
personList <- read.csv2("D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v1/personHomeInHVV.csv")

legs_filtered <- legs %>% 
  filter(person %in% personList$person)

legs_modalDistances <- legs_filtered %>% 
  select(mode, distance) %>% 
  group_by(mode) %>% 
  summarise(totalDistance_km = sum(distance) / 1000) %>% 
  column_to_rownames(var = "mode")

legs_modalDistances <- as_tibble(t(legs_modalDistances)) 
#row.names(legs_modalDistances) -> "totalDistance_km"

fileName <- paste(runDir, runID, ".output_legs_km_per_mode", sep="")
print(paste("writing ouput to", fileName))
#write.csv2(legs_modalDistances, file = paste(fileName, ".csv", sep = ""), row.names = FALSE, quote = FALSE)
#write_tsv(legs_modalDistances, file = paste(fileName, ".tsv", sep = ""),  quote = FALSE)
#write_delim(legs_modalDistances, file = paste(fileName, ".tsv", sep = ""))
write.table(legs_modalDistances, file = paste(fileName, ".txt", sep = ""), sep = '\t', dec = '.', row.names = FALSE, quote = FALSE, col.names = TRUE)

legs_modalTravelTimes <- legs_filtered %>% 
  select(mode, trav_time) %>% 
  mutate(trav_time = hms(trav_time)) %>% 
  group_by(mode) %>%  
  summarise(totalTavelTime_h = sum(totalTavelTime_h = hour(trav_time) + minute(trav_time) /60 + second(trav_time) /3600)) %>% 
  column_to_rownames(var = "mode")

legs_modalTravelTimes <- as_tibble(t(legs_modalTravelTimes)) 
fileName <- paste(runDir, runID, ".output_legs_ph_per_mode", sep="")
print(paste("writing ouput to", fileName))
write.table(legs_modalTravelTimes, file = paste(fileName, ".txt", sep = ""), sep = '\t', dec = '.', row.names = FALSE, quote = FALSE, col.names = TRUE)
