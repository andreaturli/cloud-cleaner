Cloud Cleanre
===========

Utility to clean up long running instances on AWS EC2 and Rackspace Cloud Server (Next Generation) UK.

## Building

Build the program using Maven:

    % mvn clean install

This will create an executable Jar file named `cloudcleaner-0.1.0-SNAPSHOT-jar-with-dependencies.jar` containing the code and all its dependencies.

## Usage

Use the program as follows, substituting your EC2 access key and secret key for the `aws-ec2.identity`, `aws-ec2.credential`, `rackspace-cloudservers-uk.identity`, `rackspace-cloudservers-uk.credential` properties:

    % java -jar -Daws-ec2.identity=XXXXXXXX -Daws-ec2.credential=XXXXXXXX
                    -Drackspace-cloudservers-uk.identity=XXXXXXXX -Drackspace-cloudservers-uk.credential=XXXXXXXX 
                    target/cloud-cleaner-jar-with-dependencies.jar LIST

    [INFO] List all nodes and their tags
    [INFO] ==================================================================================================
    [INFO]   PROVIDER 'aws-ec2'
    [INFO] ==================================================================================================
    [INFO] --------------------------------------------------------------------------------------------------
    [INFO]   Region:'sa-east-1'
    [INFO] ---------------------------------------------------------------------------------------------------
    [INFO] --------------------------------------------------------------------------------------------------
    [INFO]   Region:'ap-northeast-1'
    [INFO] ---------------------------------------------------------------------------------------------------
    [INFO] --------------------------------------------------------------------------------------------------
    [INFO]   Region:'eu-west-1'
    [INFO] ---------------------------------------------------------------------------------------------------
    [INFO] 	* Instance 'i-1e8e4954' [type:'t1.micro', status:'stopped', keyName:'adk'] has an uptime of 84 days (2028 hours)
    [INFO] 		> Tag:Name=adk-ganglia-test
    [INFO] 	* Instance 'i-1c8e4956' [type:'t1.micro', status:'stopped', keyName:'adk'] has an uptime of 84 days (2028 hours)
    [INFO] 		> Tag:Name=adk-ganglia-test
    [INFO] 	* Instance 'i-128e4958' [type:'t1.micro', status:'stopped', keyName:'adk'] has an uptime of 84 days (2028 hours)
    [INFO] 		> Tag:Name=adk-ganglia-test
    [INFO] 	* Instance 'i-e64216ac' [type:'m1.xlarge', status:'running', keyName:'jenkins'] has an uptime of 1 days (37 hours)
    [INFO] 		> Tag:Name=Jenkins Brooklyn
    [INFO] 	* Instance 'i-0e83b344' [type:'m1.large', status:'running', keyName:'jenkins'] has an uptime of 11 days (282 hours)
    [INFO] 		> Tag:Name=Jenkins Cloudsoft
    ...
