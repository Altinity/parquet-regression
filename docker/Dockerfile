# Use Ubuntu 24.04 as the base image
FROM ubuntu:24.04

# Set environment variables for non-interactive installs
ENV DEBIAN_FRONTEND=noninteractive

# Install system dependencies
# Install dependencies for building Thrift from source
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    maven \
    git \
    curl \
    wget \
    ca-certificates \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Set Java home
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# Set a working directory for where the repository will be mounted
WORKDIR /opt/parquet-regression

# Run Maven build to compile the project and generate the jar file for parquet-generator
# This will automatically build the jar as part of the container build process
CMD mvn clean package
