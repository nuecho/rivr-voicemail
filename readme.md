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
