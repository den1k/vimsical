FROM alpine:3.4

RUN apk add openjdk8 --no-cache

# Leiningen
ENV LEIN_ROOT 1
RUN apk add --update wget ca-certificates bash && \
wget -q "https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein" \
-O /usr/local/bin/lein && \
chmod 0755 /usr/local/bin/lein && \
lein && \
apk del wget ca-certificates && \
rm -rf /tmp/* /var/cache/apk/*

# install git (for version tagging)
RUN apk add --no-cache git && \
rm -rf /tmp/* /var/cache/apk/*

# install om fork
RUN apk add git --no-cache && \
git clone https://github.com/vimsical/om.git && \
cd om && \
git checkout vimsical && \
lein install && \
cd .. && rm -rf om

# TODO Delete this once profiles have their own dependencies
ARG DATOMIC_LOGIN
ENV DATOMIC_LOGIN $DATOMIC_LOGIN
ARG DATOMIC_PASSWORD
ENV DATOMIC_PASSWORD $DATOMIC_PASSWORD

# Getting initial deps
ADD project.clj /usr/local/src/app/
WORKDIR /usr/local/src/app
RUN lein with-profiles backend-test,backend-dev deps
RUN rm -rf /usr/local/src/app
