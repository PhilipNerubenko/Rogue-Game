# src/Makefile

# Переменные
JAR_FILE = build/libs/Rogue1980-1.0-SNAPSHOT.jar
GRADLEW = ./gradlew
# Docker
IMAGE_NAME = rogue1980:1.0.0
CONTAINER_NAME = rogue-container

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

# Команда для удаленной отладки
debugger: clean build
	java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005 -jar ${JAR_FILE}

install-java:
	sudo apt update
	sudo apt install -y default-jdk

install-gradle:
	sudo apt install -y gradle

# Docker цели
docker-build:
	docker build -t $(IMAGE_NAME) .

docker-run:
	docker run -it --rm --name $(CONTAINER_NAME) $(IMAGE_NAME)

docker-stop:
	docker stop $(CONTAINER_NAME) 2>/dev/null || echo "Контейнер уже остановлен"

docker-rm:
	docker rm $(CONTAINER_NAME) 2>/dev/null || echo "Контейнер уже удалён"

docker-down: docker-stop docker-rm

docker-clean: docker-down
	docker rmi $(IMAGE_NAME) 2>/dev/null || echo "Образ уже удалён"

docker-logs:
	docker logs -f $(CONTAINER_NAME)

docker-ps:
	docker ps -a --filter name=$(CONTAINER_NAME)

docker-dockle:
	dockle $(IMAGE_NAME)

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
	@echo "  docker-build - сборка Docker-образа"
	@echo "  docker-run - запуск в Docker (с TTY)"
	@echo "  docker-stop - остановка контейнера"
	@echo "  docker-rm  - удаление контейнера"
	@echo "  docker-down - остановка + удаление"
	@echo "  docker-clean - полная очистка Docker"
	@echo "  docker-logs - логи контейнера"
	@echo "  docker-ps - проверка активный ли контейнер"
	@echo "  docker-dockle - анализ уязвимостей образа"

.PHONY: all build run clean wrapper install-java install-gradle test dependencies build-release version help \
        docker-build docker-run docker-stop docker-rm docker-down docker-clean docker-logs docker-ps docker-dockle
