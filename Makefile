
JAR := matsim-hamburg-*.jar
V := v3.0
CRS := EPSG:25832

export SUMO_HOME := $(abspath ../../sumo-1.8.0/)
osmosis := osmosis\bin\osmosis

.PHONY: prepare

$(JAR):
	mvn package

# Required files
scenarios/input/network.osm.pbf:
	curl -L https://download.geofabrik.de/europe/germany-latest.osm.pbf\
	  -o $@

scenarios/input/network.osm: scenarios/input/network.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction,residential,unclassified,living_street\
	 --bounding-box top=54.10 left=8.80 bottom=52.90 right=11.30\
	 --used-node --wb network-detailed.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction\
	 --bounding-box top=54.70 left=8.08 bottom=52.41 right=12.35\
	 --used-node --wb network-coarse.osm.pbf

	$(osmosis) --rb file=network-coarse.osm.pbf --rb file=network-detailed.osm.pbf\
  	 --merge --wx $@

	rm network-detailed.osm.pbf
	rm network-coarse.osm.pbf


scenarios/input/hamburg-sumo.net.xml: scenarios/input/network.osm

	$(SUMO_HOME)/bin/netconvert --geometry.remove --ramps.guess\
	 --type-files $(SUMO_HOME)/data/typemap/osmNetconvert.typ.xml,$(SUMO_HOME)/data/typemap/osmNetconvertUrbanDe.typ.xml\
	 --tls.guess-signals true --tls.discard-simple --tls.join --tls.default-type actuated\
	 --junctions.join --junctions.corner-detail 5\
	 --roundabouts.guess --remove-edges.isolated\
	 --no-internal-links --keep-edges.by-vclass passenger --remove-edges.by-type highway.track,highway.services,highway.unsurfaced\
	 --remove-edges.by-vclass hov,tram,rail,rail_urban,rail_fast,pedestrian\
	 --output.original-names --output.street-names\
	 --proj "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"\
	 --ignore-errors.connections\
	 --osm-files $< -o=$@


scenarios/input/hamburg-$V-network.xml.gz: scenarios/input/hamburg-sumo.net.xml
	java -cp $(JAR) org.matsim.prepare.network.CreateNetwork $<\
	 --capacities ../public-svn/matsim/scenarios/countries/de/hamburg/hamburg-v1/hamburg-v1.2/RLHH_analyze_Q_at_LSA_all.csv\
	 --output $@

scenarios/input/hamburg-$V-network-with-pt.xml.gz: scenarios/input/hamburg-$V-network.xml.gz
	java -cp $(JAR) org.matsim.application.prepare.pt.CreateTransitScheduleFromGtfs ../shared-svn/projects/realLabHH/data/gtfs_2019/Upload__HVV_Rohdaten_GTFS_Fpl_20200810.zip\
	 --network $<\
	 --name hamburg-$V --date "2020-09-09" --target-crs $(CRS)

# Aggregated target
prepare: scenarios/input/hamburg-$V-network-with-pt.xml.gz
	echo "Done"