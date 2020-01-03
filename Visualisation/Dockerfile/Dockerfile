FROM grafana/grafana:6.5.2
ENV GF_PATHS_PLUGINS=/var/lib/plugins
USER root
RUN mkdir /var/lib/plugins/
RUN mkdir /var/lib/plugins/SensorMapPlugin
RUN chown -R grafana:grafana /var/lib/plugins/
USER grafana
ADD SensorMapPlugin /var/lib/plugins/SensorMapPlugin
