# src/Makefile

# Переменные
JAR_FILE = build/libs/Rogue1980-1.0-SNAPSHOT.jar
GRADLEW = ./gradlew

# Основные цели
all: clean build run

build:
	$(GRADLEW) jar

run: $(JAR_FILE)
	java -jar $(JAR_FILE)

clean:
	$(GRADLEW) clean

# Проверка существования JAR файла перед запуском
$(JAR_FILE): build

# Установочные цели
wrapper:
	gradle wrapper --gradle-version 8.5

install-java:
	sudo apt update
	sudo apt install -y default-jdk

install-gradle:
	sudo apt install -y gradle

# Дополнительные полезные цели
test:
	$(GRADLEW) test

dependencies:
	$(GRADLEW) dependencies

build-release:
	$(GRADLEW) build

# Информационные цели
version:
	$(GRADLEW) --version

help:
	@echo "Доступные цели:"
	@echo "  all        - очистка, сборка и запуск"
	@echo "  build      - сборка проекта"
	@echo "  run        - запуск приложения"
	@echo "  clean      - очистка проекта"
	@echo "  test       - запуск тестов"
	@echo "  build-release - полная сборка"
	@echo "  install-*  - установка зависимостей"

.PHONY: all build run clean wrapper install-java install-gradle test dependencies build-release version help
