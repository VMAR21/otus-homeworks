## Задание

1. построить шардированный кластер из 3 кластерных нод( по 3 инстанса с репликацией) и с кластером конфига(3 инстанса);
2. добавить балансировку, нагрузить данными, выбрать хороший ключ шардирования, посмотреть как данные перебалансируются между шардами;
3. поронять разные инстансы, посмотреть, что будет происходить, поднять обратно. Описать что произошло.
4. настроить аутентификацию и многоролевой доступ;

## Решение

### 1. Строим ширдированный кластер

Сначала поднимаем все шарды и реплики из docker-compose.yml

Далее инициализируем первую конфиг реплику:

```js
// заходим в mongosh: docker exec -it mongo-configsvr-1 mongosh --port 40001

rs.initiate({ 
  "_id": "config-replica-set", 
  members : [ 
    { "_id": 0, "host": "mongo-configsvr-1:40001" }, 
    { "_id": 1, "host": "mongo-configsvr-2:40002" }, 
    { "_id": 2, "host": "mongo-configsvr-3:40003" } 
  ] 
});

```

Ждем, чтобы одна реплика стала PRIMARY (через команду rs.status()).

Далее инициализируем реплики в шардах:

```js
// заходим в mongosh: docker exec -it mongo-shard-1-rs-1 mongosh --port 40011

rs.initiate({ 
  "_id" : "shard-replica-set-1", 
  members : [ 
    {"_id" : 0, host : "mongo-shard-1-rs-1:40011"}, 
    {"_id" : 1, host : "mongo-shard-1-rs-2:40012"}, 
    {"_id" : 2, host : "mongo-shard-1-rs-3:40013" } 
  ] 
});

// Ожидаем, что какая-нибудь реплика станет PRIMARY
//...
// exit

// Заходим во второй шард: docker exec -it mongo-shard-2-rs-1 mongosh --port 40021
rs.initiate({ 
  "_id" : "shard-replica-set-2", 
  members : [ 
    {"_id" : 0, host : "mongo-shard-2-rs-1:40021"}, 
    {"_id" : 1, host : "mongo-shard-2-rs-2:40022"}, 
    {"_id" : 2, host : "mongo-shard-2-rs-3:40023" } 
  ] 
});
```

Теперь можно добавить наши шарды:

```js
// docker exec -it mongos-shard mongosh

sh.addShard("shard-replica-set-1/mongo-shard-1-rs-1:40011,mongo-shard-1-rs-2:40012,mongo-shard-1-rs-3:40013")
sh.addShard("shard-replica-set-2/mongo-shard-2-rs-1:40021,mongo-shard-2-rs-2:40022,mongo-shard-2-rs-3:40023")
```

Запускаем `sh.status` и проверяем, что все ок.
Инициализация завершена.

### 2. Создать шард с данными

- Установим node.js
- Запустим скрипт insert-shard.js, заполняющий данными

В результате проверяем, что скрипт отработал успешно:
```js
show dbs // должна появится db bank
use bank
show collections // должна появиться коллекция users
db.users.countDocuments() // должно вывести 1_000_000 элементов
```

Каждый объект users имеет следующую структуру: 

```js
{
  "userId" : 0,
  "name" : "User-1",
  "email" : "user1@mail.com"
}
```

### 2.1 Выбираем ключ шардирования и добавляем балансировку

Для ключа шардирования выберем name (т.к. в нашем случае в вакууме здесь будут лежать уникальные значения -> хорошая селективность).
```js
// обязательно создаем индекс, т.к. без него не создастся шард
use bank
db.users.createIndex({name: 1})

// включаем шардинг в админе
use admin
db.runCommand({shardCollection: "bank.users", key: {name: 1}})

// добавляем балансировку
sh.balancerCollectionStatus("bank.users")

//смотрим распределение
db.tickets.getShardDistribution()
```

Получаем следющее распределение:
```js
Totals
{
  data: '80.26MiB',
  docs: 1493264,
  chunks: 40,
  'Shard shard-replica-set-2': [
    '49.1 % data',
    '33.03 % docs in cluster',
    '83B avg obj size on shard'
  ],
  'Shard shard-replica-set-1': [
    '50.89 % data',
    '66.96 % docs in cluster',
    '83B avg obj size on shard'
  ]
}
```

Как мы видим, MongoDB шардинг делит данные по чанкам по памяти, а не по количеству документов.

### 3. Роняем инстансы, проверяем отказоустойчивость

- Пусть отключим mongo-shard-2-rs-1.
- Заходим в mongo-shard-2-rs-2 и смотрим rs.status():
```js
members: [
    {
      _id: 0,
      name: 'mongo-shard-2-rs-1:40021',
      health: 0,
      state: 8,
      stateStr: '(not reachable/healthy)',
    },
    {
      _id: 1,
      name: 'mongo-shard-2-rs-2:40022',
      health: 1,
      state: 1,
      stateStr: 'PRIMARY',
    },
    {
      _id: 2,
      name: 'mongo-shard-2-rs-3:40023',
      health: 1,
      state: 2,
      stateStr: 'SECONDARY',
    }
  ],

```

Видим, что первая реплика пишется, как недоступная, при этом вторая реплика стала PRIMARY.
Данные вставляются как надо, кластер работает.

При отключении еще одной реплики появляется следующая информация:

```js
  members: [
    {
      _id: 0,
      name: 'mongo-shard-2-rs-1:40021',
      health: 0,
      state: 8,
      stateStr: '(not reachable/healthy)',
    },
    {
      _id: 1,
      name: 'mongo-shard-2-rs-2:40022',
      health: 1,
      state: 2,
      stateStr: 'SECONDARY',
    },
    {
      _id: 2,
      name: 'mongo-shard-2-rs-3:40023',
      health: 0,
      state: 8,
      stateStr: '(not reachable/healthy)',
    }
  ],
```

Как видим, второй узел стал SECONDARY. 
Соответственно, кластер стал нерабочий, данные не принимаются: `[NotWritablePrimary]: not primary`

После поднятия реплик обратно, последняя живая нода стала PRIMARY, другие SECONDARY.
Запись проходит успешно, кластер ожил.


### 4. Настраиваем ролевку

Проверяем, что у нас нет пользователей:

```js
use admin
db.getUsers(); // результат: users: []
```

Создадим root пользователя:

```js
// root пользователь
db.createUser({ 
  user: "root", 
  pwd: "strictRootPassword", 
  roles: [ { role: "root", db: "admin" } ] 
})
```

Проверяем, что пользователи добавлен `db.getUsers():
```js
users: [
    {
      _id: 'admin.root',
      userId: UUID('01fd6146-e370-40b0-8dfd-aaf16cd93041'),
      user: 'root',
      db: 'admin',
      roles: [ { role: 'root', db: 'admin' } ],
      mechanisms: [ 'SCRAM-SHA-1', 'SCRAM-SHA-256' ]
    }
  ]
```

Теперь, согласно официальной документации, мы должны добавить keyfile для нашего кластера:

`При аутентификации по keyFile mongod каждый или mongos экземпляры в кластере используют содержимое ключевого файла в качестве общего пароля для аутентификации других участников развертывания. Только mongod или mongos экземпляры с правильным keyFile могут присоединиться к сегментированному кластеру.`

Поэтому создаем такой файла командой :
```
openssl rand -base64 756 > ./mongo-keyfile/mongo.key
```
Поскольку довольно сложно и муторно менять права на файл в системе Windows, решено это делать внутри самого контейнера. 
Поэтому создаем Dockerfile:
```yaml
FROM mongo:7

RUN mkdir -p /etc/mongo/
COPY ./mongo-keyfile/mongo.key /etc/mongo/mongo.key
RUN chmod 400 /etc/mongo/mongo.key && chown mongodb:mongodb /etc/mongo/mongo.key

CMD ["mongod"]
```

В Dockerfile мы используем тот же образ, но меняем ему права на чтение и сразу задаем оунера:

`RUN chmod 400 /etc/mongo/mongo.key && chown mongodb:mongodb /etc/mongo/mongo.key`

Теперь нам необходимо немного исправить наш docker-compose.yml. Везде дописываем:

`"--keyFile", "/etc/mongo/mongo.key"`

Перезапускаем наш docker-compose и заново иницализируем реплики и шарды.

Заходим в mongos-shard и добавляем двух пользователей для дальнейших тестов:

```js
// зададим пользователя с правами только на чтение
db.createUser({ user: "readUser", pwd: "strictReadPassword", roles: [ { role: "read", db: "bank" } ] })

// зададим пользователя с правами на чтени / запись
db.createUser({ user: "readWriteBankUser", pwd: "strictReadWritePassword", roles: [ { role: "readWrite", db: "bank" } ] })

```

С командой `exit` выходим из mongos-shard и заходим с пользаком  readUser и смотрим результаты:

```js
// заходим с пользователем на чтение
docker exec -it mongos-shard mongosh -u readUser -p
Enter password: ...

// смотрим базы
show dbs // показывает только bank

use bank
db.users.insertOne(
  {
    userId: 1,
    name: "User-1",
    email: "user1@mail.com"
  }
)

// Вывод
Uncaught:
MongoBulkWriteError[Unauthorized]: not authorized on bank to execute command


// выходим из пользователя и заходим с правами на чтение / запись
docker exec -it mongos-shard mongosh -u readWriteBankUser -p
Enter password: ...

show dbs // только bank
use bank
db.users.insertOne(
  {
    userId: 6,
    name: "User-6",
    email: "user6@mail.com"
  }
)

// Вывод
{
  acknowledged: true,
  insertedId: ObjectId('68546d53da091c220169e328')
}
```


