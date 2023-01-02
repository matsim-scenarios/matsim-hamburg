library(gridExtra)
library(tidyverse)
library(lubridate)
library(patchwork)
library(viridis)
library(ggsci)
library(sf)

source("https://raw.githubusercontent.com/matsim-scenarios/matsim-duesseldorf/master/src/main/R/theme.R")

setwd("D:git/matsim-hamburg/src/main/R")

theme_set(theme_Publication(18))

# trip distance groups
levels = c("0 - 500", "500 - 1000", "1000 - 2000", "2000 - 5000", "5000 - 10000", "10000 - 20000", "20000 - 50000", "50000 - 100000", "100000+")
breaks = c(0, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000, Inf)

shape <- st_read("D:/svn/shared-svn/projects/realLabHH/data/hamburg_shapeFile/hamburg_merge", crs=25832)

#########
# Read simulation data
#########

sim_scale <- 4 # set to 4 for 25pct, 10 for 10pct, 100 for 1pct, ...
f <- "//sshfs.r/schlenther@cluster.math.tu-berlin.de/net/ils/matsim-hamburg/v4.0-calibration/runs/020" # set to run output directory

#homes <- read_csv("../../../scenarios/input/kelheim-v3.0-homes.csv", 
#                  col_types = cols(
#                    person = col_character()
#                  ))

homes <- read_tsv("D:/svn/shared-svn/projects/matsim-hamburg/hamburg-v3/hamburg-v3.0-person2HomeLocation.tsv",
                   col_types = cols( person = col_character())
                  )

persons <- read_delim(list.files(f, pattern = "*.output_persons.csv.gz", full.names = T, include.dirs = F), delim = ";", trim_ws = T, 
                     col_types = cols(
                       person = col_character(),
                       good_type = col_integer()
                     ))

persons <- persons %>%
          right_join(homes) %>%
          st_as_sf(coords = c("home_x", "home_y"), crs = 25832) %>%
#          st_as_sf(coords = c("first_act_x", "first_act_y"), crs = 25832) %>%
          st_filter(shape)

trips <- read_delim(list.files(f, pattern = "*.output_trips.csv.gz", full.names = T, include.dirs = F), delim = ";", trim_ws = T, 
                    col_types = cols(
                      person = col_character()
                    )) %>%
        filter(main_mode!="freight") %>%
        semi_join(persons) %>%
        mutate(dist_group = cut(traveled_distance, breaks=breaks, labels=levels, right = F)) %>%  # traveled_distance == 0 is considered
        filter(!is.na(dist_group))


sim <- trips %>%
  group_by(dist_group, main_mode) %>%
  summarise(trips=n()) %>%
  mutate(mode = fct_relevel(main_mode, "walk", "bike", "pt", "ride", "car")) %>%
  mutate(scaled_trips=sim_scale * trips) %>%
  mutate(source = "sim")

########
# Read survey data
########


#for scaling trips, see https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/realLabHH/data/MiD_Hamburg_2019/MiD-Hamburg.xlsx
# Hamburg Metropolregion has 16960000 trips (5.3*10^6 inhabitants * 3.2 trips per day)
srv <- read_csv("analysis/mid.csv") %>%
    mutate(main_mode=mode) %>%
    mutate(scaled_trips=16960000 * share) %>% ## number of person agents * avgTrips * mode-share
    mutate(source = "mid") %>%
    mutate(dist_group=fct_relevel(dist_group, levels)) %>%
    arrange(dist_group)

######
# Total modal split
#######

srv_aggr <- srv %>%
    group_by(mode) %>%
    summarise(share=sum(share)) %>%  # assume shares sum to 1
    mutate(mode=fct_relevel(mode, "walk", "bike", "pt", "ride", "car"))  
  
aggr <- sim %>%
    group_by(mode) %>%
    summarise(share=sum(trips) / sum(sim$trips)) %>%
    mutate(mode=fct_relevel(mode, "walk", "bike", "pt", "ride", "car"))

p1_aggr <- ggplot(data=srv_aggr, mapping =  aes(x=1, y=share, fill=mode)) +
  labs(subtitle = "Survey data") +
  geom_bar(position="fill", stat="identity") +
  coord_flip() +
  geom_text(aes(label=scales::percent(share, accuracy = 0.1)), size= 8, angle=90, color="white", position=position_fill(vjust=0.5)) +
  scale_fill_locuszoom() +
  theme_void() +
  theme(legend.position="none")

p2_aggr <- ggplot(data=aggr, mapping =  aes(x=1, y=share, fill=mode)) +
  labs(subtitle = "Simulation") +
  geom_bar(position="fill", stat="identity") +
  coord_flip() +
  geom_text(aes(label=scales::percent(share, accuracy = 0.1)), size= 8, angle=90, color="white", position=position_fill(vjust=0.5)) +
  scale_fill_locuszoom() +
  theme_void() +
  theme(legend.position = "bottom")

combined <- p1_aggr / p2_aggr
combined + plot_layout(guides = "collect")

g <- arrangeGrob(p1_aggr, p2_aggr, ncol = 2)
g
out <- file.path(f, "analysis-mode-choice")
if(!file.exists(out)){
  print("creating analysis sub-directory")
  dir.create(out)  
}

ggsave(filename = "modal-split.png", path = out, g,
       width = 12, height = 2, device='png', dpi=300)

#########
# Combined plot by distance
##########

total <- bind_rows(srv, sim) %>%
    mutate(mode=fct_relevel(mode, "walk", "bike", "pt", "ride", "car"))

# Maps left overgroups
dist_order <- factor(total$dist_group, level = levels)
dist_order <- fct_explicit_na(dist_order, "100000+")

g <- ggplot(total, aes(fill=mode, y=scaled_trips, x=source)) +
  labs(subtitle = paste("Hamburg scenario", substring(f, 52)), x="distance [m]", y="trips") +
  geom_bar(position="stack", stat="identity", width = 0.5) +
  facet_wrap(dist_order, nrow = 1) +
  scale_y_continuous(labels = scales::number_format(suffix = " K", scale = 1e-3)) +
  scale_fill_locuszoom() +
  theme_minimal()

g

ggsave(filename = "modal-distance-distribution.png", path = out, g,
       width = 12, height = 10, device='png', dpi=300)

###############################################################################
##
#        !!! CODE AND VALUES FROM KELHEIM !!!
##
##############################################################################
# Needed for adding short distance trips

sim_sum <- sum(sim$trips)
sim_aggr <- sim %>%
  group_by(dist_group) %>%
  summarise(share=sum(trips) / sim_sum)

# Needed share of trips
tripShare <- 0.19
shortDistance <- sum(filter(sim, dist_group=="0 - 1000")$trips)
numTrips = (shortDistance - sim_sum * tripShare) / (tripShare - 1)


##########################
# Distance distributions based on RegioStar data
##########################

levels = c("0 - 500", "500 - 1000", "1000 - 2000", "2000 - 5000", "5000 - 10000", 
           "10000 - 20000", "20000 - 50000", "50000 - 100000", "100000+")

breaks = c(0, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000, Inf)

trips2 <- trips %>%
  mutate(dist_group = cut(traveled_distance, breaks=breaks, labels=levels, right = F)) %>%
  mutate(mode = fct_relevel(main_mode, "walk", "bike", "pt", "ride", "car"))

rs <- read_csv("tidied-mode-share-per-distance.csv") %>%
  mutate(source="rs")

sim <- trips2 %>%
  group_by(dist_group) %>%
  summarise(trips=n()) %>%
  mutate(source="sim")

sim <- mutate(sim, share=trips/sum(sim$trips))

total_distance_dist <- bind_rows(filter(rs, mode=="total_distance_distribution"), sim)

dist_order <- factor(total_distance_dist$dist_group, level = levels)
dist_order <- fct_explicit_na(dist_order, "100000+")


g <- ggplot(total_distance_dist, aes(y=share, x=source, fill=source)) +
  labs(subtitle = paste("Kelheim scenario", substring(f, 52)), x="distance [m]", y="share") +
  geom_bar(position="stack", stat="identity", width = 0.5) +
  facet_wrap(dist_order, nrow = 1) +
  scale_fill_viridis_d() +
  theme_minimal()
g

ggsave(filename = "distance-distribution.png", path = out, g,
       width = 12, height = 10, device='png', dpi=300)

##################################

sim <- trips2 %>%
  group_by(dist_group) %>%
  mutate(n=n()) %>%
  group_by(mode, dist_group) %>%
  summarise(share=n()/first(n)) %>%
  mutate(source="sim")

by_distance <- bind_rows(filter(rs, mode!="total_distance_distribution"), sim) %>%
  mutate(mode=fct_relevel(mode, "walk", "bike", "pt", "ride", "car"))

dist_order <- factor(by_distance$dist_group, level = levels)
dist_order <- fct_explicit_na(dist_order, "100000+")

g <- ggplot(by_distance, aes(y=share, x=source, fill=mode)) +
  labs(subtitle = paste("Hamburg scenario", substring(f, 52)), x="distance [m]", y="share") +
  geom_bar(position="stack", stat="identity", width = 0.5) +
  facet_wrap(dist_order, nrow = 1) +
  scale_fill_locuszoom() +
  theme_minimal()
g

ggsave(filename = "modal-distance-distribution-relative.png", path = out, g,
       width = 12, height = 10, device='png', dpi=300)

