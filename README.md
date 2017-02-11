# Yet another UI for BigClown project
The purpose of this project is to create an alternative UI for BigClown and any other IoT project that is based on the mqtt.

Currently home_center is able to visualize several basic BigClown sensors such as temperature and humidity which are connected to a hub by the BridgeModule.

This project is written in Scala and is based on Play framework. For the data persistence H2 embeded sql database is used. This db is backed by the files. Frontend is based on React.js. Twitter boot and Graph.js are used as graphics components.
