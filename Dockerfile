# Use OpenJDK image with Maven pre-installed
FROM maven:3.9.6-eclipse-temurin-17

# Set working directory
WORKDIR /src

# Copy source code
COPY . .

# Build the application
RUN mvn clean package

# Run the application
CMD ["mvn", "exec:java"]
