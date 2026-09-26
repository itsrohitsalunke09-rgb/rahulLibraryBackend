# Delete the corrupted file
del Dockerfile

# Create using cmd (not PowerShell) - no BOM
echo FROM maven:3.9-eclipse-temurin-17 AS build > Dockerfile
echo WORKDIR /app >> Dockerfile
echo COPY pom.xml . >> Dockerfile
echo COPY src ./src >> Dockerfile
echo RUN mvn clean package -DskipTests >> Dockerfile
echo. >> Dockerfile
echo FROM eclipse-temurin:17-jre >> Dockerfile
echo WORKDIR /app >> Dockerfile
echo COPY --from=build /app/target/*.jar app.jar >> Dockerfile
echo EXPOSE 8080 >> Dockerfile
echo ENTRYPOINT ["java", "-jar", "app.jar"] >> Dockerfile

# Verify content
type Dockerfile

# Push
git add Dockerfile
git commit -m "Fix Dockerfile"
git push origin main