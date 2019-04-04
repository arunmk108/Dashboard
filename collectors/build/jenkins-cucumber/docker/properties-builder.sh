#!/bin/bash

if [ "$SKIP_PROPERTIES_BUILDER" = true ]; then
  echo "Skipping properties builder"
  exit 0
fi
  
if [ "$MONGO_PORT" != "" ]; then
	# Sample: MONGO_PORT=tcp://172.17.0.20:27017
	MONGODB_HOST=`echo $MONGO_PORT|sed 's;.*://\([^:]*\):\(.*\);\1;'`
	MONGODB_PORT=`echo $MONGO_PORT|sed 's;.*://\([^:]*\):\(.*\);\2;'`
else
	env
	echo "ERROR: MONGO_PORT not defined"
	exit 1
fi

echo "MONGODB_HOST: $MONGODB_HOST"
echo "MONGODB_PORT: $MONGODB_PORT"


#update local host to bridge ip if used for a URL
DOCKER_LOCALHOST=
echo $JENKINS_MASTER|egrep localhost >>/dev/null
if [ $? -ne 1 ]
then
	#this seems to give a access to the VM of the dockermachine
	#LOCALHOST=`ip route|egrep '^default via'|cut -f3 -d' '`
	#see http://superuser.com/questions/144453/virtualbox-guest-os-accessing-local-server-on-host-os
	DOCKER_LOCALHOST=10.0.2.2
	MAPPED_URL=`echo "$JENKINS_MASTER"|sed "s|localhost|$DOCKER_LOCALHOST|"`
	echo "Mapping localhost -> $MAPPED_URL"
	JENKINS_MASTER=$MAPPED_URL	
fi

cat > $PROP_FILE <<EOF
#Database Name
dbname=${HYGIEIA_API_ENV_SPRING_DATA_MONGODB_DATABASE:-dashboarddb}

#Database HostName - default is localhost
dbhost=${MONGODB_HOST:-10.0.1.1}

#Database Port - default is 27017
dbport=${MONGODB_PORT:-27017}

#Database Username - default is blank
dbusername=${HYGIEIA_API_ENV_SPRING_DATA_MONGODB_USERNAME:-dashboarduser}

#Database Password - default is blank
dbpassword=${HYGIEIA_API_ENV_SPRING_DATA_MONGODB_PASSWORD:-dbpassword}

#Collector schedule (required)
jenkins-cucumber.cron=${JENKINS_CRON:-0 0/5 * * * *}

#pattern used for finding cucumber.json files 
# note: failed builds will not be picked up, the collector looks for lastSucessfulArtifacts

#Jenkins server (required) - Can provide multiple
jenkins-cucumber.servers[0]=${JENKINS_MASTER:-http://jenkins.company.com}

#If using username/token for api authentication (required for Cloudbees Jenkins Ops Center) see sample
#jenkins-cucumber.servers[1]=${JENKINS_OP_CENTER:-http://username:token@jenkins.company.com}
#jenkins-cucumber.servers[1]=${JENKINS_OP_CENTER}

#Another option: If using same username/password Jenkins auth - set username/apiKey to use HTTP Basic Auth (blank=no auth)
jenkins-cucumber.username=${JENKINS_USERNAME}
jenkins-cucumber.apiKey=${JENKINS_API_KEY}

#Determines if build console log is collected - defaults to false
jenkins-cucumber.saveLog=${JENKINS_SAVE_LOG:-true}

#pattern to find cucubmer reports
jenkins-cucumber.cucumberJsonRegex=${JENKINS_CUCUMBER_JSON_FILENAME:-cucumber.json}

#map the entry localhost so URLS in jenkins resolve properly
# Docker NATs the real host localhost to 10.0.2.2 when running in docker
# as localhost is stored in the JSON payload from jenkins we need
# this hack to fix the addresses
jenkins-cucumber.dockerLocalHostIP=${DOCKER_LOCALHOST}

EOF

echo "

===========================================
Properties file created `date`:  $PROP_FILE
Note: passwords & apiKey hidden
===========================================
`cat $PROP_FILE |egrep -vi 'password|apiKey'`
"

exit 0
