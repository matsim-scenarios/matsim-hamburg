## inputs
fileLocation <- dirname(rstudioapi::getSourceEditorContext()$path)
inputRoutes <- paste(fileLocation,"/routes.txt", sep = "")
zipName <- "Ready4matsim_HVB-VEP_Szenario-1_20210720_GTFS_210826"
#outputName <- "../../../Prognose2030-4_U5_HVV_VEP GTFS_210521/routes_mod.txt"
folderName <- paste(fileLocation,"/", zipName, sep = "")
outputRoutes <- paste(folderName,"/routes.txt", sep = "")

iterateU <- 5
iterateS <- 40
iterateA <- 3
iterateR <- 90
iterateF <- 79
remove_LongDistTrains <- FALSE

## Laden der notwendigen Packages

if(!require(install.load)){
  install.packages("install.load")
  library(install.load)
}

install_load(
  "readr",
  "dplyr",
  "tidyverse"
)

routes <- read_csv(inputRoutes)

linesU <- "U1"
for(i in c(2:iterateU)){
  linesU <- c(linesU, paste("U", i, sep = ""))
}
linesS <- "S1"
for(i in c(2:iterateS)){
  linesS <- c(linesS, paste("S", i, sep = ""))
}
linesA <- "A1"
for(i in c(2:iterateA)){
  linesA <- c(linesA, paste("A", i, sep = ""))
}
linesR <- c("RE1", "RB1")
for(i in c(2:iterateR)){
  linesR <- c(linesR, paste("RB", i, sep = ""))
  linesR <- c(linesR, paste("RE", i, sep = ""))
}
linesF <- c("HBEL", "HBL")
for(i in c(60:iterateF)){
  linesF <- c(linesF, paste(i, " ", sep = ""))
}


route_type_1 <- c(linesU, linesS)
route_type_2 <- c(linesA, linesR)
route_type_4 <- linesF

routes_edit <- routes
{
  routes_edit$route_type <- 3
  if(remove_LongDistTrains){
    routes_edit$route_type <- ifelse(grepl("IC", substr(routes_edit$route_id, 1, 2), fixed = TRUE), 0, routes_edit$route_type)
    routes_edit$route_type <- ifelse(grepl("EC", substr(routes_edit$route_id, 1, 2), fixed = TRUE), 0, routes_edit$route_type)
    routes_edit$route_type <- ifelse(grepl("THA", substr(routes_edit$route_id, 1, 3), fixed = TRUE), 0, routes_edit$route_type)
  }else{
    routes_edit$route_type <- ifelse(grepl("IC", substr(routes_edit$route_id, 1, 2), fixed = TRUE), 2, routes_edit$route_type)
    routes_edit$route_type <- ifelse(grepl("EC", substr(routes_edit$route_id, 1, 2), fixed = TRUE), 2, routes_edit$route_type)
    routes_edit$route_type <- ifelse(grepl("THA", substr(routes_edit$route_id, 1, 3), fixed = TRUE), 2, routes_edit$route_type)
  }
  
  for(line in route_type_1){
    routes_edit$route_type <- ifelse(grepl(line, routes_edit$route_id, fixed = TRUE), 1, routes_edit$route_type)
  }
  for(line in route_type_2){
    routes_edit$route_type <- ifelse(grepl(line, routes_edit$route_id, fixed = TRUE), 2, routes_edit$route_type)
  }
  for(line in route_type_4){
    routes_edit$route_type <- ifelse(grepl(line, substr(routes_edit$route_id, 1, 3), fixed = TRUE), 4, routes_edit$route_type)
  }
  routes_edit$route_type <- ifelse(grepl("HBL", substr(routes_edit$route_id, 1, 3), fixed = TRUE), 4, routes_edit$route_type)
  routes_edit$route_type <- ifelse(grepl("HBEL", substr(routes_edit$route_id, 1, 4), fixed = TRUE), 4, routes_edit$route_type)
  routes_edit$route_type <- ifelse(grepl("RS", substr(routes_edit$route_id, 1, 2), fixed = TRUE), 2, routes_edit$route_type)
}

routes_edit <- subset(routes_edit, routes_edit$route_type != 0)


if(!dir.exists(folderName)){
  dir.create(folderName)
}

gtfs_files <- list.files(path = fileLocation, pattern = "txt$")
setwd(fileLocation)
file.copy(gtfs_files, folderName, overwrite = TRUE)
write.csv(routes_edit, outputRoutes, row.names = FALSE)

setwd(folderName)
zip(zipfile =  zipName, files = gtfs_files)
file.copy(paste(zipName,".zip", sep = ""), "../..", overwrite = TRUE)
file.remove(paste(zipName,".zip", sep = ""))
