# Gradle Plugin

## Description
A Plugin for developing Lambda Java Projects on AWS. This project drastically speeds up the development time needed to develop Lambda applications on AWS by splitting up your project dependencies into layers, and your function code separately. This way when making edits to your code you will only need to deploy the function code as opposed to the entire dependencies of the project. This will mean after dependencies are uploaded it will take only a few seconds to deploy your code, eliminating a pain point when developing a compiled language such as Java with AWS.


### To Publish
To publish this plugin remotely:
```bash
./gradlew publishPlugins
```

To publish the plugin locally:
```bash
./gradlew publishToMavenLocal
```