FROM openjdk:26-ea-21-oraclelinux8

WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser appuser

RUN microdnf install -y ncurses-compat-libs ncurses-term && \
    microdnf clean all

RUN mkdir -p /app/native && \
    cd /app/native && \
    curl -L -o jcurses.tar.gz https://sourceforge.net/projects/javacurses/files/javacurses/0.9.5b/jcurses-linux-0.9.5b.tar.gz/download && \
    tar -xf jcurses.tar.gz --strip-components=2 "jcurses/lib/libjcurses64.so" && \
    mv libjcurses64.so libjcurses.so && \
    rm jcurses.tar.gz

RUN chown -R appuser:appuser /app

COPY build/libs/Rogue1980-1.0-SNAPSHOT.jar /app/Rogue1980.jar

ENV JAVA_OPTS="--enable-native-access=ALL-UNNAMED -Djava.library.path=/app/native"

USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD pgrep -f "Rogue1980.jar" > /dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/Rogue1980.jar"]
