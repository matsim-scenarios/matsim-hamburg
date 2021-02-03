
JAR := matsim-hamburg-*.jar
V := v1.0

export SUMO_HOME := $(abspath ../../sumo-1.6.0/)
osmosis := osmosis\bin\osmosis

.PHONY: prepare

$(JAR):
	mvn package

# Required files
scenarios/input/network.osm.pbf:
	curl https://download.geofabrik.de/europe/germany-latest.osm.pbf\
	  -o $@

scenarios/input/network.osm: scenarios/input/network.osm.pbf

	$(osmosis) --rb file=$<\
	 --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction,residential,unclassified,living_street\
	 --bounding-box top=54.10 left=8.80 bottom=52.90 right=11.30\
	 --used-node --wx $@

	#$(osmosis) --rb file=$<\
	# --tf accept-ways highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary_link,secondary,tertiary,motorway_junction\
	# --bounding-box top=51.46 left=6.60 bottom=50.98 right=7.03\
	# --used-node --wb network-coarse.osm.pbf

	#$(osmosis) --rb file=network-detailed.osm.pbf\
  	# --merge --wx $@

	#rm network-detailed.osm.pbf
	#rm network-coarse.osm.pbf


scenarios/input/sumo.net.xml: scenarios/input/network.osm

	$(SUMO_HOME)/bin/netconvert --geometry.remove --junctions.join --tls.discard-simple --tls.join\
	 --type-files $(SUMO_HOME)/data/typemap/osmNetconvert.typ.xml,$(SUMO_HOME)/data/typemap/osmNetconvertUrbanDe.typ.xml\
	 --roundabouts.guess --remove-edges.isolated\
	 --no-internal-links --keep-edges.by-vclass passenger --remove-edges.by-type highway.track,highway.services,highway.unsurfaced\
	 --remove-edges.by-vclass hov,tram,rail,rail_urban,rail_fast,pedestrian\
	 --output.original-names --output.street-names\
	 --proj "+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs"\
	 --ignore-errors.connections\
	 --osm-files $< -o=$@


scenarios/input/duesseldorf-$V-network.xml.gz: scenarios/input/sumo.net.xml
	java -cp $(JAR) org.matsim.prepare.CreateNetwork $<
	 --output $@

# Aggregated target
prepare: scenarios/input/duesseldorf-$V-network.xml.gz
	echo "Done"