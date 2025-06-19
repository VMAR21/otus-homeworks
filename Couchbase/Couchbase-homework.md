### Задание

- Развернуть кластер Couchbase
- Создать БД, наполнить небольшими тестовыми данными
- Проверить отказоустойчивость

### Решение

#### Развертка кластера

Для развертки используем docker-compose.yml:
```
version: '3.9'
services:
  couchbase1:
    image: couchbase/server
    container_name: couchbase1
    volumes:
      - ./node1:/opt/couchbase/var
    ports:
      - 8094:8091
  couchbase2:
    image: couchbase/server
    container_name: couchbase2
    volumes:
      - ./node2:/opt/couchbase/var
    ports:
      - 8095:8091
  couchbase3:
    image: couchbase/server
    container_name: couchbase3
    volumes:
      - ./node3:/opt/couchbase/var
    ports:
      - 8091:8091
      - 8092:8092
      - 8093:8093
      - 11210:11210

```
После запуска скрипта появились папки с 3 нодами:
- node1
- node2
- node3

Заходим в localhost:8091 и делаем первичную настройку кластера.
Далее аналогично подключаемся к порту 8094 и 8095 и включаем сервера в кластер, нажимая "Join existing cluster".

Теперь в servers мы видим три активные ноды в одной группе с разными настройками:

- 172.18.0.2 (query)
- 172.18.0.3 (data, query)
- 172.18.0.4 (data, query, index, ...)

#### Создание БД и наполнение данными

Здесь аналогично, через ui, мы можем настроить наши бакеты.
Создадим БД с юзерами:

1. Кликаем add bucket и создаем бакет accounts
2. Заходим в бакет и кликаем Add scope с названием main
3. Кликаем add collection и добавляем коллекцию юзеров users
4. Кликаем add documents и в новом окне добавляем необходимые данные


#### Проверка отказоустойчивости

Опускам любую ноду и в окне servers сразу видим сообщение:
```
Node unresponsive | Not available for traffic | FAILOVER to activate any available replicas
```

При обратном включении все ок, данные реплицировались.