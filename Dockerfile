# Use official Maven image with Java 17
FROM maven:3.9-eclipse-temurin-17

# Install DuckDB CLI (supports both ARM64 and AMD64)
RUN apt-get update && \
    apt-get install -y wget unzip && \
    ARCH=$(dpkg --print-architecture) && \
    if [ "$ARCH" = "arm64" ]; then \
        wget https://github.com/duckdb/duckdb/releases/download/v1.1.3/duckdb_cli-linux-aarch64.zip && \
        unzip duckdb_cli-linux-aarch64.zip && \
        rm duckdb_cli-linux-aarch64.zip; \
    else \
        wget https://github.com/duckdb/duckdb/releases/download/v1.1.3/duckdb_cli-linux-amd64.zip && \
        unzip duckdb_cli-linux-amd64.zip && \
        rm duckdb_cli-linux-amd64.zip; \
    fi && \
    mv duckdb /usr/local/bin/ && \
    chmod +x /usr/local/bin/duckdb && \
    apt-get clean

# Set working directory
WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline

# Copy init.sql and populate the database
COPY init.sql .
RUN duckdb coffee.db < init.sql

# Copy the rest of the application
COPY src ./src

# Compile the application
RUN mvn compile

# Run the application
CMD ["mvn", "exec:java"]
