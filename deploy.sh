#!/bin/bash

npm run pack
sbt dist
scp ./target/universal/home_center-1.0.0.zip raspberry:
ssh raspberry "cp /home/vlvavra/home_center-1.0.0.zip /opt/home_center/"
ssh raspberry "cd /opt/home_center; unzip -o home_center-1.0.0.zip"
ssh raspberry "/etc/init.d/home_center stop"
ssh raspberry "cp /home/vlvavra/home_center-1.0.0/conf/application.conf /opt/home_center/home_center-1.0.0/conf/"
ssh raspberry "cp /home/vlvavra/home_center-1.0.0/conf/logback.xml /opt/home_center/home_center-1.0.0/conf/"
ssh raspberry "/etc/init.d/home_center start"

