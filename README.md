# ruian-api
Služba na vyhledávaní obce v databazi RUIAN


# build
mvn package docker:build

# docker
docker login
docker push dracul/ruian-api

docker pull dracul/ruian-api
docker run --name="ruian-api" -p 8080:8080 -it dracul/ruian-api
docker start ruian-api
docker stop ruian-api

docker run --name="ruian-api" -p 8080:8080 -d --restart=always dracul/ruian-api