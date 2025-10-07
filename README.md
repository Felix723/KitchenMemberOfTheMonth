# How to Build and Start the Kitchen Project

To build and start the Coffee Shop web app, you need to have the following software 
installed: 
- Java 17
- Maven
- Git

We give installation instructions for Linux and the Windows for Linux Subsystem. 

## Step 1: Clone the repository

```bash
git clone git@github.com:ITU-DASYALab/introdb2025coffeeshop.git
cd introdb2025coffeeshop
```

## Step 2: Create the database file

Create a `coffee.db` file and execute the statements in `init.sql` using DuckDB:

```bash
duckdb coffee.db < init.sql
```

## Step 3: Compile the project via Maven
```bash
mvn compile
```

## Step 4: Start the project via Maven
```bash
mvn exec:java
```

## Step 5: Open the web app
Open your browser and navigate to:
[http://localhost:8080](http://localhost:8080)

## Notes:
If Java is not installed, install Java:

```bash
sudo apt install default-jdk -y
```

If Maven is not installed, install Maven:
```bash
sudo apt install maven
```

If git is not installed, install git:
```bash
sudo apt install git
```
