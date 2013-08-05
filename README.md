Cloud Cleaner
===========

Utility to clean up long running instances on a number of different cloud providers.

The following cloud providers are supported:
    
    * AWS EC2 (aws-ec2)
    * Rackspace Cloud Server (Next Generation) UK (rackspace-cloudservers-uk)
    * HpCloud Compute (hpcloud-compute)
    * Softlayer Cloudlayer (softlayer)

## Building

Build the program using Maven:

    % mvn clean install

This will create an executable Jar file named `cloudcleaner-0.1.0-SNAPSHOT-jar-with-dependencies.jar` containing the code and all its dependencies.

## Usage

Use the program as follows, 

    % java -jar target/cloud-cleaner-jar-with-dependencies.jar LIST 
    		-Daws-ec2.identity=XXXXXXXX 
    		-Daws-ec2.credential=XXXXXXXX 
            -Drackspace-cloudservers-uk.identity=XXXXXXXX 
            -Drackspace cloudservers-uk.credential=XXXXXXXX
            -Dhpcloud-compute.identity=XXXXXXXX 
            -Dhpcloud-compute.credential=XXXXXXXX
            -Dsoftlayer.identity=XXXXXXXX 
            -Dsoftlayer.credential=XXXXXXXX                        

or, for example,

     % java -jar target/cloud-cleaner-jar-with-dependencies.jar LIST aws-ec2     
     			-Daws-ec2.identity=XXXXXXXX 
     			-Daws-ec2.credential=XXXXXXXX

if you want to list only the instances on AWS EC2.

This is an example of the output of the `LIST` command:

    [INFO] List all nodes and their tags
    [INFO] ==================================================================================================
    [INFO]   PROVIDER 'softlayer'
    [INFO] ==================================================================================================
    [INFO] Instance{id=26558908-cdb5-0779-4217-78ba9a475509, provider=softlayer, region=Dallas 1, status=PowerState{keyName=Running}, uptime (in hours)=0, tags=null, type=Cloud Compute Instance, keyName=278918}
    [INFO] Instance{id=ff814d9c-86ff-0376-cafc-adc57b14435e, provider=softlayer, region=Dallas 1, status=PowerState{keyName=Running}, uptime (in hours)=0, tags=null, type=Cloud Compute Instance, keyName=278918}
    [INFO] Instance{id=d1b36b44-f4f1-11bb-55da-d315e4a69295, provider=softlayer, region=Dallas 1, status=PowerState{keyName=Running}, uptime (in hours)=0, tags=null, type=Cloud Compute Instance, keyName=278918}
    [INFO] Instance{id=da064dea-3a93-c67d-3919-f82e35074641, provider=softlayer, region=Dallas 1, status=PowerState{keyName=Running}, uptime (in hours)=0, tags=null, type=Cloud Compute Instance, keyName=278918}
    [INFO] Instance{id=1322ca82-db96-84fe-f33f-7e2749a13ac4, provider=softlayer, region=Dallas 1, status=PowerState{keyName=Running}, uptime (in hours)=0, tags=null, type=Cloud Compute Instance, keyName=278918}
    [INFO] Instance{id=88b56e6f-c259-e341-64ec-395080acfc04, provider=softlayer, region=Dallas 1, status=PowerState{keyName=Running}, uptime (in hours)=0, tags=null, type=Cloud Compute Instance, keyName=278918}
    [INFO] ==================================================================================================
    [INFO]   PROVIDER 'aws-ec2'
    [INFO] ==================================================================================================
    [INFO] Instance{id=i-e64216ac, provider=aws-ec2, region=eu-west-1, status=running, uptime (in hours)=2557, tags={Name=Jenkins Brooklyn}, type=m1.xlarge, keyName=jenkins}
    [INFO] Instance{id=i-0e83b344, provider=aws-ec2, region=eu-west-1, status=running, uptime (in hours)=2802, tags={Name=Jenkins Cloudsoft}, type=m1.large, keyName=jenkins}
    [INFO] Instance{id=i-1e686853, provider=aws-ec2, region=eu-west-1, status=running, uptime (in hours)=582, tags={Name=brooklyn-mja3-alex-simple-wordpress-qxxr-mysql-odae-1e686853}, type=t1.micro, keyName=jclouds#brooklyn-mja3-alex-simple-wordpress-qxxr-mysql-odae#47}
    [INFO] Instance{id=i-2e686863, provider=aws-ec2, region=eu-west-1, status=running, uptime (in hours)=582, tags={Name=brooklyn-loqo-alex-simple-qxxr-wordpress-httpd-xutq-2e686863}, type=t1.micro, keyName=jclouds#brooklyn-loqo-alex-simple-qxxr-wordpress-httpd-xutq#ee0}
    [INFO] ==================================================================================================
    [INFO]   PROVIDER 'rackspace-cloudservers-uk'
    [INFO] ==================================================================================================
    [INFO] ==================================================================================================
    [INFO]   PROVIDER 'hpcloud-compute'
    [INFO] ==================================================================================================
    [INFO] Instance{id=2032715, provider=hpcloud-compute, region=az-1.region-a.geo-1, status=ACTIVE, uptime (in hours)=2, tags={tags=LONG-RUNNING}, type=100, keyName=jclouds-brooklyn-pifb-adk-opengam-nvte-opengamma-server-odeu-aba}
`