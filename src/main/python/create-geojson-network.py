# matsim network converter
try:
    import sys, json, gzip
    import matsim
    from dfply import *
    from pyproj import Transformer
except:
    print("OOPS! Error importing required libraries.")
    print('try "pip3 install matsim-tools dfply pyproj"')

if len(sys.argv) != 3:
    print(
        "USAGE:  python create-geojson-network.py  [network]  [coord-system]"
    )
    sys.exit(1)


p_network = sys.argv[1]
p_coords = sys.argv[2]
out_file = p_network.replace(".xml.", ".geo.json.")

coord_transformer = Transformer.from_crs(p_coords, "EPSG:4326")

print("reading network:", p_network)
network = matsim.read_network(p_network)

# Build link x/y lookup
nodes = network.nodes >> mutate(to_node=X.node_id, from_node=X.node_id)
links = (
    network.links
    >> inner_join(nodes, by="from_node")
    >> select(X.link_id, X.from_node, X.to_node_x, X.x, X.y)
    >> mutate(x0=X.x, y0=X.y, to_node=X.to_node_x)
    >> inner_join(nodes, by="to_node")
    >> select(X.link_id, X.x0, X.y0, X.x_y, X.y_y)
)

# convert coords to lat/lon
print("reprojecting coordinates")
converted_links = {}
for link in links.values:
    coords = []

    fromY, fromX = coord_transformer.transform(link[1],link[2])
    coords.extend([round(fromX, 5), round(fromY, 5)])
    toY, toX = coord_transformer.transform(link[3], link[4])
    coords.extend([round(toX, 5), round(toY, 5)])

    converted_links[link[0]] = coords

# convert to geojson
print("creating geojson")
geojson = {
    "type": "FeatureCollection",
    "features": []
}

for link_id in converted_links:
    points = converted_links[link_id]
    geojson["features"].append({
        "type": "Feature",
        "geometry": {
            "type": "LineString",
            "coordinates": [ [points[0],points[1]], [points[2],points[3]]]
        },
        "properties": {
            "id": link_id
        }
    })

# write it out
print("writing:", out_file)
#links_bytes = json.dumps(geojson, separators=(',',":")).encode("utf-8")
links_bytes = json.dumps(geojson).encode("utf-8")

with gzip.open(out_file, "wb") as f:
    f.write(links_bytes)

print(len(converted_links), "links written.")
