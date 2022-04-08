library(tidyverse)
library(readr)
library(dplyr)

#define input 
runDir <- 'D:/ReallabHH/v3.0/25pct/hv3-25-7-4-2-newNode/'
runDir <- 'Z:/net/ils/schlenther/openHH-calibration/output/output-hv3-10-7-4-2-ff/ITERS/it.0/'
runID <- "hamburg-v3.0-10pct-base"
runID <- "hamburg-v3.0-10pct-base.0.trips.csv.gz"


#read trips table
#trips <- read_csv2(paste(runDir, runID, ".output_trips.csv.gz", sep = ""))
trips <- read_csv2(paste(runDir, runID, sep = ""))

# contains a set of person Ids whose trips are to be considered.
personHomeLocations <- read_tsv("D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v3/hamburg-v3.0-person2HomeLocation.tsv")


personsInCity <- personHomeLocations %>% 
  filter(area == "Hamburg_city")

personsInHVV <- personHomeLocations %>% 
  filter(area == "HVV_Umland")

personsInCityAndHVV <- personHomeLocations %>% 
  filter(area == "HVV_Umland" | area == "Hamburg_city")

## city -------------------------------
trips_city <- trips %>% 
  filter(person %in% personsInCity$person)

totalTrips_city <- count(trips_city)
  
city_mode_share <- trips_city %>%   
  group_by(main_mode) %>% 
  summarise(share_city = n() / totalTrips_city)

## hvv -------------------------------
trips_hvv <- trips %>% 
  filter(person %in% personsInHVV$person)

totalTrips_hvv <- count(trips_hvv)

hvv_mode_share <- trips_hvv %>%   
  group_by(main_mode) %>% 
  summarise(share_hvv = n() / totalTrips_hvv)

## city+hvv -------------------------------
trips_both <- trips %>% 
  filter(person %in% personsInCityAndHVV$person)

totalTrips_both <- count(trips_both)

both_mode_share <- trips_both %>%   
  group_by(main_mode) %>% 
  summarise(share_cityAndHVV = n() / totalTrips_both)


df <- data.frame(both_mode_share$main_mode, both_mode_share$share_cityAndHVV, city_mode_share$share_city, hvv_mode_share$share_hvv)
colnames(df) <-  c("mode", "cityAndHVV", "city", "hvv")

##-----------------------------------------------------------------------------------------------------------------
fileName <- paste(runDir, "analysis/", runID, ".mode-share", sep="")
print(paste("writing ouput to", fileName))

if(!file.exists(file.path(runDir, "analysis"))){
  print("creating analysis sub-directory")
  dir.create(file.path(runDir, "analysis"))  
}

#write.table(joined, file = paste(fileName, ".tsv", sep = ""), sep = '\t', dec = '.', row.names = FALSE, quote = FALSE, col.names = TRUE)
write_tsv(df, file = paste(fileName, ".tsv", sep = ""), quote = "none")

