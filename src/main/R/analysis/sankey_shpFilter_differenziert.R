library(tidyverse)
library(tidyr)
library(dplyr)
library(networkD3)
library(sf) #=> geography

writeSankeyData <- function(tibbleBase, tibblePolicy, outputFile){
  # join tables. base tibble is spatially filtered, so we do a left join in order to take only the rows within base tibble
  joined <- left_join(tibbleBase, tibblePolicy, by = "trip_id", keep = TRUE, suffix = c(".base", ".policy")) %>% 
    rename(from = main_mode.base, to = main_mode.policy)
    
  # print total nr of trips
  summarise(joined, n = n())
  
  # produce sankey input data table with changers only
  result <- joined %>% 
    group_by(from, to) %>% 
    filter(!is.na(to), !is.na(from)) %>% 
    filter( from!=to) %>%                     # filter for trips where mode has changed
    tally()
  # print
  result
  
  result$from[result$from=="pt_w_drt_used"] <- "shtl + pt"
  result$to[result$to=="pt_w_drt_used"] <- "shtl + pt"
  result$from[result$from=="scar"] <- "car-sharing"
  result$to[result$to=="scar"] <- "car-sharing"
  result$from[result$from=="sbike"] <- "bike-sharing"
  result$to[result$to=="sbike"] <- "bike-sharing"
  
  #result$from[result$from=="commercial_Lfw"] <- "Lfw"
  #result$to[result$to=="commercial_Lfw"] <- "Lfw"
  #result$from[result$from=="commercial_Lkw-g"] <- "Lkw-g"
  #result$to[result$to=="commercial_Lkw-g"] <- "Lkw-g"
  #result$from[result$from=="commercial_Lkw-k"] <- "Lkw-k"
  #result$to[result$to=="commercial_Lkw-k"] <- "Lkw-k"
  #result$from[result$from=="commercial_Lkw-m"] <- "Lkw-m"
  #result$to[result$to=="commercial_Lkw-m"] <- "Lkw-m"
  #result$from[result$from=="commercial_Trans"] <- "Transporter"
  #result$to[result$to=="commercial_Trans"] <- "Transport"
  

  
  fileName <- paste(outputFile, "-changers.csv", sep="")

  print(paste("writing ouput to", fileName))
  write.csv2(result, file = fileName, sep = ';', dec = '.', row.names = FALSE, quote = FALSE)
  
  # produce sankey input data table with non-changers
  result <- joined %>% 
    group_by(from, to) %>%
    filter(!is.na(to), !is.na(from)) %>% 
    tally()
  
  result$from[result$from=="pt_w_drt_used"] <- "shtl + pt"
  result$to[result$to=="pt_w_drt_used"] <- "shtl + pt"
  result$from[result$from=="scar"] <- "car-sharing"
  result$to[result$to=="scar"] <- "car-sharing"
  result$from[result$from=="sbike"] <- "bike-sharing"
  result$to[result$to=="sbike"] <- "bike-sharing"
  
  result$from[result$from=="commercial_Lfw"] <- "commercial"
  result$to[result$to=="commercial_Lfw"] <- "commercial"
  result$from[result$from=="commercial_Lkw-g"] <- "commercial"
  result$to[result$to=="commercial_Lkw-g"] <- "commercial"
  result$from[result$from=="commercial_Lkw-k"] <- "commercial"
  result$to[result$to=="commercial_Lkw-k"] <- "commercial"
  result$from[result$from=="commercial_Lkw-m"] <- "commercial"
  result$to[result$to=="commercial_Lkw-m"] <- "commercial"
  result$from[result$from=="commercial_Trans"] <- "commercial"
  result$to[result$to=="commercial_Trans"] <- "commercial"
  
  ##i know it is ugly but a quick fix to do that twice
  
  result <- result %>% 
    group_by(from, to) %>%
    filter(!is.na(to), !is.na(from)) %>% 
    summarise(n = sum(n))
  
  
  # print
  result
  
  fileName <- paste(outputFile, ".csv", sep="")
  print(paste("writing ouput to", fileName))
  write.csv2(result, file = fileName, sep = ';', dec = '.', row.names = FALSE, quote = FALSE)
}

writeModalSplit <- function(tibbleBase, tibblePolicy, outputFile){
  # join tables. base tibble is spatially filtered, so we do a left join in order to take only the rows within base tibble
  joined <- left_join(tibbleBase, tibblePolicy, by = "trip_id", keep = TRUE, suffix = c(".base", ".policy"))   %>%  rename(from = main_mode.base, mode = main_mode.policy)
  # print total nr of trips
  summe <- summarise(joined, n = n())[1,1]
  
  # summary
  result <- joined %>% 
    group_by(mode) %>%
    filter(!is.na(mode), !is.na(from)) %>% 
    summarise(modeShare = n()/summe) %>% 
    column_to_rownames(var ="mode") 
  
  #rownames(result)[rownames(result) == "drt"] <- "robotaxi"  
  #rownames(result)[rownames(result) == "drt"] <- "pooling"
  
  
  #rownames(result)[rownames(result) == "drt2"] <- "pooling"  
  #rownames(result)[rownames(result) == "ride"] <- "car-ride"
  #rownames(result)[rownames(result) == "car_w_drt_used"] <- "car+drt"
  
  result <- as_tibble(t(result))
  
  print(paste("writing ouput to", outputFile))
  
  write.table(result, file = outputFile, sep = '\t', dec = '.', row.names = FALSE, quote = FALSE, col.names = TRUE)
}

processRun <- function(policy_scenario, policy_runID){
  
  #policy_runID <- policyRunID
  #policy_scenario <- policyScenario
  
  #read raw data
  fName <- paste(policy_scenario, "/", policy_runID, ".output_trips.csv", sep = "")
  print(paste("trying to read from ", fName))
  policy_rawData <- read.csv2(fName)
  
  policy <- policy_rawData %>%
    select(trip_id, main_mode)
  
  
  ################# QUELL- + ZIEL- + BINNENSVERKEHR ###################################
  outputFile <- paste(policy_scenario, "/", policy_runID, ".sankey-data-", areaName, sep = "")
  writeSankeyData(base, policy, outputFile)
  
  outputFile <- paste(policy_scenario, "/", policy_runID, ".modeStats-", areaName, ".txt", sep = "")
  writeModalSplit(base, policy, outputFile)
  
  ################# BINNENVERKEHR ###################################
  outputFile <- paste(policy_scenario, "/", policy_runID, ".sankey-data-", areaName, "-binnenVerkehr", sep = "")
  writeSankeyData(base_binnenV, policy, outputFile)
  
  outputFile <- paste(policy_scenario, "/", policy_runID, ".modestats-", areaName, "-binnenVerkehr.txt", sep = "")
  writeModalSplit(base_binnenV, policy, outputFile)
  
  ################# QUELLVERKEHR ###################################
  outputFile <- paste(policy_scenario, "/", policy_runID, ".sankey-data-", areaName, "-quellVerkehr", sep = "")
  writeSankeyData(base_quellV, policy, outputFile)
  
  outputFile <- paste(policy_scenario, "/", policy_runID, ".modestats-", areaName, "-quellVerkehr.txt", sep = "")
  writeModalSplit(base_quellV, policy, outputFile)
  
  ################# ZIELVERKEHR ###################################
  outputFile <- paste(policy_scenario, "/", policy_runID, ".sankey-data-", areaName, "-zielVerkehr", sep = "")
  writeSankeyData(base_zielV, policy, outputFile)
  
  outputFile <- paste(policy_scenario, "/", policy_runID, ".modestats-", areaName, "-zielVerkehr.txt", sep = "")
  writeModalSplit(base_zielV, policy, outputFile)
}







#############################################################
## INPUT DEFINITION ##

#set working directory
setwd("D:/ReallabHH/v2.2/p20x")

# shapeFile for filtering. if you modify the shp you should also modify the areaName! be aware that the shpFile must consist of exactly 1 geometry! (can be one geometry object with multiple polygons)
shpFileName <- "../../../svn/shared-svn/projects/reallabHH/data/hamburg_shapeFile/hamburg_hvv/hamburg_hvv_one_geom.shp"
areaName <- "hvvArea"

# run-identifiers
base_Folder <- "output-p208"
base_runID <- "hamburg-v2.2-baseCase"


# determines whether spatial filtering is to be conducted on the base case trips.
#if TRUE, trips from base case are differentiated as quellverkehr, zielverkehr, binnenverkehr and the combination of all. takes some time as spatial computations are needed.
#output csv are dumped out such that 
#if FALSE, these csvs can be read in and one saves the computation time.
filterBase = FALSE


#############################################################
### SCRIPT START ##

if(filterBase){
  ##########################################################################################################################################
  ## this is how to filter the base case data for trips within the shape file. You can instead read in the filtered data, once generated for the first time
  # filter for trips with origin AND destination within shape. Select columns that are necessary
    shpFile <- st_read(shpFileName)
    base_rawData <- read.csv2(paste(base_Folder, "/", base_runID, ".output_trips.csv", sep = ""))  
    
    (baseGeoms <-base_rawData
      %>% mutate(wkt = paste("MULTIPOINT((", start_x, " ", start_y, "),(", end_x, " ", end_y, "))", sep =""))
      %>% st_as_sf(wkt = "wkt", crs= st_crs(shpFile))
    )
    
    (base <- baseGeoms
      %>% filter(st_intersects(shpFile, ., sparse=FALSE))
    )
    base <- as_tibble(base) %>% select(person,trip_id, main_mode)
    
    (base_binnenV <- baseGeoms
    %>% filter(st_contains(shpFile, ., sparse=FALSE))
    )
    base_binnenV <- as_tibble(base_binnenV) %>% select(person,trip_id, main_mode)
    
    (base_startInside <- base_rawData
      %>% mutate(wkt = paste("MULTIPOINT((", start_x, " ", start_y, "))", sep =""))
      %>% st_as_sf(wkt = "wkt", crs= st_crs(shpFile))
      %>% filter(st_contains(shpFile, ., sparse=FALSE))
    )
    base_startInside <- as_tibble(base_startInside) %>% select(person, trip_id, main_mode)
    
    (base_endInside <- base_rawData
      %>% mutate(wkt = paste("MULTIPOINT((", end_x, " ", end_y, "))", sep =""))
      %>% st_as_sf(wkt = "wkt", crs= st_crs(shpFile))
      %>% filter(st_contains(shpFile, ., sparse=FALSE))
    )
    base_endInside <- as_tibble(base_endInside) %>% select(person, trip_id, main_mode)
    
    base_quellV <- anti_join(base_startInside, base_binnenV, by = "trip_id")
    base_zielV <- anti_join(base_endInside, base_binnenV, by = "trip_id")
    
    write.csv2(base, file=paste(base_Folder, "/", base_runID, ".output_trips-", areaName, ".csv", sep = ""), row.names = FALSE)
    write.csv2(base_binnenV, file=paste(base_Folder, "/", base_runID, ".output_trips-", areaName, "-binnenVerkehr.csv", sep = ""), row.names = FALSE)
    write.csv2(base_quellV, file=paste(base_Folder, "/", base_runID, ".output_trips-", areaName, "-quellVerkehr.csv", sep = ""), row.names = FALSE)
    write.csv2(base_zielV, file=paste(base_Folder, "/", base_runID, ".output_trips-", areaName, "-zielVerkehr.csv", sep = ""), row.names = FALSE)
    
  ##########################################################################################################################################
} else {
    base <- read.csv2(paste(base_Folder, "/", base_runID, ".output_trips-", areaName, ".csv", sep = ""), sep = ";", dec = ",") %>%
      select(trip_id, main_mode)
    base_binnenV <- read.csv2(paste(base_Folder, "/", base_runID, ".output_trips-", areaName, "-binnenVerkehr.csv", sep = ""), sep = ";", dec = ",") %>%
      select(trip_id, main_mode)
    base_quellV <- read.csv2(paste(base_Folder, "/", base_runID, ".output_trips-", areaName, "-quellVerkehr.csv", sep = ""), sep = ";", dec = ",") %>%
      select(trip_id, main_mode)
    base_zielV <- read.csv2(paste(base_Folder, "/", base_runID, ".output_trips-", areaName, "-zielVerkehr.csv", sep = ""), sep = ";", dec = ",") %>%
      select(trip_id, main_mode)
}


#####################################################
### LOOP


##########



policyScenario <- "output-p208-reallab2030plus"
policyRunID <- "hamburg-v2.2-10pct-reallab2030plus"
processRun(policyScenario, policyRunID)

policyScenario <- "output-p208-reallab2030"
policyRunID <- "hamburg-v2.2-10pct-reallab2030"
processRun(policyScenario, policyRunID)


#####################################################

print("######################## FINISHED #######################################")
























