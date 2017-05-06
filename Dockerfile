FROM docker.elastic.co/elasticsearch/elasticsearch:5.3.2

ENV PLUGIN_PATH /plugins/plugin.zip
COPY Dockerfile entrypoint.sh .

CMD [ "./entrypoint.sh" ]
