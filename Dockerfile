# Use official Maven image with Java 17
FROM maven:3.9-eclipse-temurin-17

# Set working directory
WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline

# Copy the rest of the application
COPY src ./src

# Compile the application
RUN mvn compile

EXPOSE 8080

# Run the application
CMD ["mvn", "exec:java"]
