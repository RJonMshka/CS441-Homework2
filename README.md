# CS-441 Cloud Computing Objects
# Homework 2

## By: Rajat Kumar (UIN: 653922910)

---


## Introduction
This homework is focused towards processing large log files using Optimal Algorithm using Serverless Compute Service
provided by Amazon AWS called AWS Lambda function. EC2 instance has been used to upload generated logs to Amazon S3 bucket.
gRPC and REST client-server programs are used to interact with Lambda function using AWS API Gateway's REST API. gRPC and REST client programs
request the servers to process logs and gets the result back in timely manner.

The whole project is developed using Scala programming language and some of the AWS services including Amazon EC2, Amazon S3, Amazon Lambda, Amazon API Gateway.
More information abot implementation, structure, and deployment is discussed in later sections of this documentation.

---

## GitHub Repositories
1. Main Project - gRPC client and Server Programs (this repo) - https://github.com/RJonMshka/CS441-Homework2
2. Log Generation Project and uploading to S3 - https://github.com/RJonMshka/LogFileGenerator
3. AWS Lambda Function Implementation Project - https://github.com/RJonMshka/CS441-HW2-AWSLambda

---
## How to run the application/project

1. Download IntelliJ or your favourite IDE. The application is developed using IntelliJ IDE, and it is highly recommended to use it for various reasons.
2. Make sure you have Java SDK version 11 (at least version 8) installed on your machine.
3. Also, it is assumed that your machine have git (version control) installed on your machine. If not, please do.
4. Clone this repository from GitHub (repo 1 in GitHub repos section) and switch to main branch. This is where the latest code is located.
5. Open IntelliJ, and open up this project in the IDE environment. Or you can do New - Project from Version Control and then enter the GitHub URL of this repository to load in directly into your system if not cloned already.
6. The application's code is written using Scala programming language. The version used for Scala is 2.13.10.
7. For building the project, sbt (Scala's Simple Build Tool) is used. Version 1.7.2 is used for sbt.
8. All the dependencies and build setting can be found in build.sbt file.
9. Once intelliJ is successfully detected the Scala, sbt, and right versions for both of them, the project is ready to be compiled/built.
10. Go to terminal at the bottom of IntelliJ, or open any terminal with path being set to the project's root directory.
11. Enter sbt clean compile, the project will start building.
12. The compilation step will also generate stubs from Protobuf file (.proto). These stubs can be imported into client and server programs which is done in this project.
13. The protobuf file is in `src/main/protobuf/LogProcessor.proto` location. 
14. Test cases are written using a library called `ScalaTest`.
15. Test cases are part of two repos. In this repo, test cases are for testing methods in client and server programs which are present in file `src/test/scala/GRPCAndRestClientTest.scala`.
16. The rest of the test cases are in 3rd repo (aws lambda repo) in file `src/test/scala/LambdaTest.scala`. These tests will be testing method responsible for processing logs.
17. Run `sbt test` on both repos to run these test cases.
18. The scala version used for writing AWS lambda logic is also `2.13.10` and the same sbt version is used for compiling.
19. For producing a jar file of lambda function, use `sbt assembly` which will generate a jar file in `target/scala-2.13/LogProcessorLambda-assembly-0.1.jar` location.
20. That jar file can be uploaded to S3 bucket and then can be used as a source of code for Lambda function. This step is present in the video demonstration.
21. Now, there is another repo (2nd repo in github repos section above) that deals will log generation and uploading it to S3. 
22. The scala version used for that repo is `3.0.2`.
23. For uploading the generated logs to S3, you can download `aws cli` and configure aws credentials using `aws configure` command using the cli.
24. Then you just need to compile the log file generator project and run it on your machine and it will upload logs to a S3 bucket whose name is mentioned in the application.conf file (make sure the S3 bucket is in US EAST 2 region or you have to update it in source code).
25. Then create an EC2 instance on AWS console, give it permissions to put object in S3, and also connect will it using ssh from your local machine.
26. You can then use `scp -i ec2-key-val-pair-location.pem -r "/path-to-log-generator-project-folder/* ec2-instance-user@ec2-instance-public-dns-address:/path-to-folder-on-ec2/` to copy the content from your local machine to ec2 machine. This step is present in the video demonstration.
27. Update the ec2 instance, install java (version 11 recommended), scala and sbt on it. You can find resources on the internet on how to do it. For example, update can be done using `sudo apt-get update` and java can be installed using `sudo apt install default-jre`.
28. Once, done installing necessary dependencies, you can go to the log file generator folder on ec2 instance, and perform `sbt clean compile` and then `sbt run`.
29. Once, the program finishes running, check S3 bucket, and you will the new files pop up there. If they are not there, there is some issue with s3 region, or bucket name or with ec2 permissions.
30. There is also steps to create AWS API Gateway REST API in the video which can act as trigger to the lambda function.
31. Once everything is done, update the aws api gateway uri in application.conf file of this repo.
32. Then execute `sbt clean compile` to compile programs in this repo.
33. To start gRPC Server, execute command `sbt "runMain GrpcProject.LogProcessorGRPCServer"` in the terminal.
34. To run gRPC Client program, execute command `sbt "runMain GrpcProject.LogProcessorGRPCClient"` in the terminal.
35. To start the REST Server, execute command `sbt "runMain RestProject.LogProcessorRestServer"` in the terminal.
36. To run the REST client program, execute command `sbt "runMain RestProject.LogProcessorRestClient"` in the terminal.
37. That is it for the running the project, please keep experimenting and have fun.

---
### Deployment on AWS (YouTube Video)
The project demonstration is thoroughly presenting in this [youtube](https://youtu.be/0zpjS9sm-n0) video.
I have added chapters in the video itself to segregate it into parts.

---
### Implementation Details
The whole project is diving into 3 repositories. First repo is this one, which includes implementation of Client and Server programs for gRPC as well as REST.
When the project is compiled, the sbt compiler calles `protoc` compiler which is used to convert protobuf definition into scala code. The protoc compiler used is a library named `scalapb`.
Once compilation is done, the stubs and request and response objects are produced in `target/scala-2.13/classes/com/grpcLogProcessor/protos/LogProcessor` location.
Now, when the gRPC server is started, it keeps on listening to incoming gRPC request on a specific port. Similarly, REST server listens to incoming http request on another specific port.
Both the gRPC server and REST server interacts will AWS API gateway, by making an HTTP call to it, passing the parameters from its request to REST APIs exposed by API Gateway. 
Now, this API gateway is added as a trigger for AWS Lambda function responsible for processing log files.

**gRPC Setup**
`src/main/scala/GrpcProject/LogProcessorGRPCServer.scala` starts the gRPC server.
`src/main/scala/GrpcProject/LogProcessorGRPCClient.scala` is the gRPC client program that performs an RPC. The server listens to it and when the request is received, it is passed to Akka HTTP client.
The Akka HTTP client that interacts with AWS API Gateway is `src/main/scala/GrpcProject/LogProcessorGRPCRestClient.scala`. Once, gRPC server receives a request, the server calls this client which in turn makes an HTTP call to AWS API Gateway.
The response from API Gateway is received by Akka HTTP client which gives it back to gRPC server. 
gRPC servers returns this response to RPC client using Reply generated from protobuf.

Protobuf Response and Reply looks like this:
```
message LogProcessorRequest {
  string date = 1;
  string time = 2;
  int32 interval = 3;
}

message LogProcessorReply {
  string response = 1;
  int32 statusCode = 2;
}
```

The LogProcessorRequest (request) consists of date, time and interval. The LogProcessorReply(response) consists of response string and a status code.


**REST Setup**
`src/main/scala/RestProject/LogProcessorRestServer.scala` starts the REST server.
`src/main/scala/RestProject/LogProcessorRestClient.scala` is the REST client program that performs an HTTP call to REST Server. The server listens to it and when the request is received, it creates another HTTP request to API gateway.
The response from API Gateway is received by REST Server which gives it back to REST client using response.
The REST server can also be tested using http clients like `Postman` using a call such as this `localhost:8080/processLogs?date=2022-10-30&time=17:58:52.345&interval=10`.

**LogFileGenerator**
The usual log file generator, after successfully generating the log files, it makes a `hashtable` file in the below format.
```
2022-10-28->14:27:37.234|14:27:40.031|log/LogFileGenerator.2022-10-28.1.log,14:27:22.436|14:27:37.189|log/LogFileGenerator.2022-10-28.0.log,
2022-10-30->17:59:58.316|18:00:23.244|log/LogFileGenerator.2022-10-30.5.log,17:59:31.364|17:59:58.299|log/LogFileGenerator.2022-10-30.4.log,17:57:42.071|17:58:09.983|log/LogFileGenerator.2022-10-30.0.log,17:58:37.108|17:59:03.746|log/LogFileGenerator.2022-10-30.2.log,17:59:03.793|17:59:31.323|log/LogFileGenerator.2022-10-30.3.log,17:58:10.027|17:58:37.072|log/LogFileGenerator.2022-10-30.1.log,
```

In the above example, each line signifies all log files belonging to same date.

The rolling policy used for generating log is `SizeAndTimeBasedRollingPolicy` with a max file size of 100KB. So, each log file cannot exceed 100KB and it generate multiple file for each date.

After `->`, the entry is `14:27:37.234|14:27:40.031|log/LogFileGenerator.2022-10-28.1.log,14:27:22.436|14:27:37.189|log/LogFileGenerator.2022-10-28.0.log,`.

This means for a single date `2022-10-28`, there are two log files:
1. `log/LogFileGenerator.2022-10-28.0.log` with start time `14:27:22.436` and end time `14:27:37.189`.
2. `log/LogFileGenerator.2022-10-28.1.log` with start time `14:27:37.234` and end time `14:27:40.031`.

Same goes for other lines as well.

This hashtable file along with the log files in uploaded to Amazon S3 bucket.


** AWS Lambda**
The AWS lambda code is present in this `https://github.com/RJonMshka/CS441-HW2-AWSLambda` repository.

The lambda function is responsible for processing log data in S3 bucket.
This is how, it performs log processing:
1. First it validates event input (API Gateway proxy event). Validates if date and time are in right format.
2. Then it reads the 'hashtable' file from S3 and converts the raw text data in hashtable file into a scala hashMap data structure.
3. Then it filters out the entry in hashMap using the date passed to its input.
4. If date is found it continues, otherwise responds with a 404.
5. It checks all the log files belonging to the input date and filers out one file by checking its start time and end time and comparing it with the input time and interval passed.
6. If the time passed and time interval is within the start and end time of a particular log file, that log file is checked for log messages matching a particular regex pattern which is also used to generate these log messages.
7. If no file is matched, result is returned with 404 otherwise program continues.
8. To find the range within the log messages that closely match the input time and time interval, the lambda function performs a Binary search and finds start index and end index in log file for that range in O(logN) time complexity.
9. After finding the start and end index of the range using a Binary Search algorithm, it then checks each log within that range to match that log's message with the pattern.
10. Based on the filtering the logs based on pattern, it converts these filtered messages into string data and hashed then to a fixed length hash generated using MD5 hashing algorithm.
11. If no log is found after filtering, MD5 hash is not done, but instead another 404 message is passed back to source of request.
12. The successful result hashed with MD5 is then returned with 200 status code.

The code is pretty clean and self-explanatory, however I have added useful comments to make it even more clear and readable.
That's it for this project. Have fun.