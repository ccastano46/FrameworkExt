#!/bin/bash
sudo yum update -y
sudo yum install -y java-25-amazon-corretto
sudo yum install -y unzip

mkdir -p /home/ec2-user/webframework

aws s3 cp s3://apps-902353451847-us-east-1-an/java/artifacts/webframework/WebFramework-1.0-SNAPSHOT.jar /home/ec2-user/webframework/ --no-sign-request
aws s3 cp s3://apps-902353451847-us-east-1-an/java/artifacts/webframework/public.zip /home/ec2-user/webframework/ --no-sign-request


unzip /home/ec2-user/webframework/public.zip -d /home/ec2-user/webframework/ -x "__MACOSX/*"
rm /home/ec2-user/webframework/public.zip


export APP_ENV=production
export STATIC_PATH=webframework/public

cd /home/ec2-user
java -jar webframework/WebFramework-1.0-SNAPSHOT.jar > /home/ec2-user/server.log 2>&1 &