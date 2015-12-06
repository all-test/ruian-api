FROM java:8
ENV INSTALL_PATH /opt/ruian-api
RUN mkdir -p $INSTALL_PATH
VOLUME $INSTALL_PATH
WORKDIR $INSTALL_PATH
ADD ruian-api-0.0.1-jar-with-dependencies.jar $INSTALL_PATH/ruian-api.jar
RUN apt-get update && apt-get -y install socat

#CMD ["java","-jar","ruian-api.jar"]
CMD (/usr/bin/socat tcp6-listen:80,fork,su=nobody tcp4:localhost:8080) && /usr/bin/java -jar ruian-api.jar
EXPOSE 8080
