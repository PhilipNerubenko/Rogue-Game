# src/Makefile
# ./gradlew clean
# ./gradlew jar
# ./gradlew runGame
# java -jar build/libs/Rogue1980-1.0-SNAPSHOT.jar


all: clean build run


build:
	./gradlew jar

run:
	java -jar build/libs/Rogue1980-1.0-SNAPSHOT.jar



#build:
	#java -Djava.library.path=libs -jar build/libs/Rogue1980-1.0-SNAPSHOT.jar
	## ./gradlew jar --stacktrace --info

clean:
	./gradlew clean

install:
	./gradlew install

wrapper:
	gradle wrapper --gradle-version 8.5

# Установка инструментов (опционально)
install-java:
	sudo apt update
	sudo apt install default-jdk

install-gradle:
	sudo apt install gradle



.PHONY: all build run clean install
