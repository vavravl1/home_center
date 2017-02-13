# Yet another UI for BigClown project
The purpose of this project is to create an alternative UI for BigClown and any other IoT project that is based on the mqtt.

Currently home_center is able to visualize several basic BigClown sensors such as temperature and humidity which are connected to a hub by the BridgeModule.

This project is written in Scala and is based on Play framework. For the data persistence H2 embeded sql database is used. This db is backed by the files. Frontend is based on React.js. Twitter boot and Graph.js are used as graphics components.

## How to build
### Necessary softwate
java sdk 8, scala 2.11.8, sbt 0.13.13 and node.js v7.4.0
### How to install
To build the frontend change the current dir to the project root and then run `npm install` and then `npm run watch`. The first command installs all necessary fronetend modules. The later step will transforms the code to vanilla javascript and will watch for all subsequent changes to the UI code.

Configure setting of your envorinment, namely location of the mqtt broker. Open the file `<root>/conf/application.conf` and change `url` and possibly `clientId` in the section `home_center.mqtt`.

To build the scala code change the current dir to the project root and run `sbt`. This will start the sbt console and in that console execute `run`. This will start the application. Then in your browser open the url `http://localhost:9000`
