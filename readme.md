## Getting Started

To bootstrap with an eclipse project:

    # setup eclipse project
    ./gradlew eclipse

Then, from within Eclipse, you can first import that project into your workspace and apply 
the Gradle nature:

 * Right-click on the project
 * Select **Configure** > **Convert to Gradle Project**

Project should build and library references and such updated.

### Note

Gradle project handling within Eclipse requires the [Gradle plugin]()

### Note

You may also directly import the Gradle project from **File** > **Import** > **Gradle** > **Gradle Project**

## Demo 

An instance of this application is currently deployed over Amazon Beanstalk (http://rivr-voicemail.elasticbeanstalk.com/dialogue/) 
and reacheable from the Voxeo's Hosted Platform

    skype: 990009369991420387
    pstn: +1-438-800-1164  

With the following voicemail login details:

    Mailbox: 4069 
    Password: 6522
