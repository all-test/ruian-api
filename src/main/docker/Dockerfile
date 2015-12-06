FROM java:8
ENV INSTALL_PATH /opt/ruian-api
RUN mkdir -p $INSTALL_PATH
VOLUME $INSTALL_PATH
WORKDIR $INSTALL_PATH
ADD ruian-api-0.0.1-jar-with-dependencies.jar $INSTALL_PATH/ruian-api.jar
RUN bash -c 'touch /app.jar'
CMD ["java","-jar","ruian-api.jar"]
EXPOSE 8080