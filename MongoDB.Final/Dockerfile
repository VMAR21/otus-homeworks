FROM mongo:7

RUN mkdir -p /etc/mongo/
COPY ./mongo-keyfile/mongo.key /etc/mongo/mongo.key
RUN chmod 400 /etc/mongo/mongo.key && chown mongodb:mongodb /etc/mongo/mongo.key

CMD ["mongod"]