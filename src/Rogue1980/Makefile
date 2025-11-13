# src/Makefile
# ./gradlew clean
# ./gradlew jar
# ./gradlew runGame


all: clean build run


#build:
#	./gradlew build

run:
	./gradlew runGame

runG:
	@STTY_SAVE=$$(stty -g); stty raw -echo; java -Djava.library.path=libs -jar build/libs/Rogue1980-1.0-SNAPSHOT.jar; stty $$STTY_SAVE

start:
	java -Djava.library.path=libs -jar build/libs/Rogue1980-1.0-SNAPSHOT.jar

build:
	java -Djava.library.path=libs -jar build/libs/Rogue1980-1.0-SNAPSHOT.jar
	# ./gradlew jar --stacktrace --info

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
