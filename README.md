Cloud Cleaner
===========

Utility to clean up long running instances on a number of different cloud providers.

The following cloud providers are supported:
    
    * AWS EC2 (aws-ec2)
    * Rackspace Cloud Server (Next Generation) UK (rackspace-cloudservers-uk)
    * Rackspace Cloud Server (Next Generation) US (rackspace-cloudservers-us)
    * HpCloud Compute (hpcloud-compute)
    * Softlayer Cloudlayer (softlayer)
    * Interoute (abiquo) 
    * IBM Smart Cloud Enterprise 
    * IBM SoftLayer,
    * Google Compute Engine

## Building

Build the program using Maven:

    % mvn clean install

This will create an executable Jar file named `cloudcleaner-0.2.0-SNAPSHOT-jar-with-dependencies.jar` containing the code and all its dependencies.

## Usage

Use the program as follows, 

    % java -jar target/cloud-cleaner-jar-with-dependencies.jar

to have the usage of the program:

    % usage: cloud-cleaner <command> [<args>]
    
    The most commonly used cloud-cleaner commands are:
    	cleanup          Clean up the given cloud (nodes, firewalls, networks)
    	deletenetworks   Destroy all networks in the clouds matching a given prefix
    	deletenodes      Destroy all instances in the clouds matching a given prefix
    	help             Display help information
    	list             List all instances running on the clouds

	See 'cloud-cleaner help <command>' for more information on a specific command.

## Examples

This is an example of the output of the `java -jar target/cloud-cleaner-jar-with-dependencies.jar list softlayer -s` command:

    Cloud providers to be searched: [softlayer]
    CloudCleaner will search the following providers: [softlayer]
    Found credentials for provider(softlayer) - identity(andrea.turli), credential(xxxxxxxxxxxxxxxxxxxxxxxx)

    List all instances of provider(s): [softlayer] and their tags

    ================================================================================

      PROVIDER 'softlayer' - 121 instances running

    ==================================================================================================
    Instance{id=3f05f077-5762-dd48-6bde-238c3da424fe, name=null, provider=softlayer, region=San Jose 1, type=Cloud Compute Instance, status=PowerState{keyName=Running}, keyName=278184, uptime=959 hour(s), groupName=null, tags=null}
    Instance{id=ff0f77fb-474f-ee09-0cf1-2898d1e9b704, name=null, provider=softlayer, region=Dallas 5, type=Cloud Compute Instance, status=PowerState{keyName=Running}, keyName=278184, uptime=2203 hour(s), groupName=null, tags=null}
    Instance{id=852ef0c8-78cd-1ea8-4f46-5a1728e3bbc7, name=null, provider=softlayer, region=Dallas 5, type=Cloud Compute Instance, status=PowerState{keyName=Running}, keyName=278184, uptime=879 hour(s), groupName=null, tags=null}
    Instance{id=38a99e89-e3c1-1843-3854-d7d34a44fe4c, name=null, provider=softlayer, region=San Jose 1, type=Cloud Compute Instance, status=PowerState{keyName=Running}, keyName=278184, uptime=393 hour(s), groupName=null, tags=null}
    Instance{id=90758e2e-cdfc-aecf-ac74-c67215d8c9c9, name=null, provider=softlayer, region=San Jose 1, type=Cloud Compute Instance, status=PowerState{keyName=Running}, keyName=278184, uptime=536 hour(s), groupName=null, tags=null}
    ...