Wildfly-redeployer watches the target directory of your project for a new or changes in war file and redeploys it through wildfly standalone deployments directory.
***
Make a build using Apache Maven:
    mvn clean package
***
Usage:
    project-directory> java -jar wildfly-redeployer-1.0.0.jar wildfly-directory
or
    any-directory> java -jar wildfly-redeployer-1.0.0.jar wildly-directory project-directory
