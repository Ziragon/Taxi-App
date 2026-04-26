FROM osrm/osrm-backend

COPY novosibirsk.osm.pbf /data/novosibirsk.osm.pbf

RUN osrm-extract -p /opt/car.lua /data/novosibirsk.osm.pbf && \
    osrm-partition /data/novosibirsk.osrm && \
    osrm-customize /data/novosibirsk.osrm

EXPOSE 5000

ENTRYPOINT ["osrm-routed", "--algorithm", "mld", "/data/novosibirsk.osrm"]