FROM codingtony/java

MAINTAINER tony.bussieres@ticksmith.com

RUN apt-get update
RUN apt-get upgrade -y
RUN apt-get install unzip -y
RUN wget --content-disposition http://dl.bintray.com/jfrog/artifactory/artifactory-3.7.0.zip
RUN cd /opt && unzip ~/artifactory-3.7.0.zip
RUN mv /opt/artifactory-3.7.0 /opt/artifactory
RUN rm ~/artifactory-3.7.0.zip
RUN mkdir /opt/artifactory/data

EXPOSE 8081

CMD [ "/opt/artifactory/bin/artifactory.sh" ]
