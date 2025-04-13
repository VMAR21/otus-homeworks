## 1. Инициализация монго и заполнение данными

Для установки mongo использовал docker-compose.yml:

```yml
version: '3.8'
services:
  mongodb:
    image: mongo 
    container_name: mongod 
    ports:
      - 27017:27017
    volumes:
      - ./init-mongodb:/docker-entrypoint-initdb.d

```

Инициализировал скриптом прямо внутри монго-контейнера (init-mongo.sh) с помощью утилиты mongoimport и базы sample_tratinig.

## 2. Запросы на поиск данных

1. Смотрим импортированные коллекции:

```bash
show dbs
use sample_trainig
show collections
```

2. Выбираем первые 5 студентов, у которых оценка за экзамен от 80 до 90 баллов:
```js
db.grades.find({
    scores: {
        $elemMatch: {
            type: "exam", 
            score: { $gt: 80, $lt: 90 }
        }
    }
}, 
{ 
    _id: 0 
}).limit(5)
```

Результат:
```js
[
  {
    student_id: 0,
    scores: [
      { type: 'exam', score: 85.08219987294274 },
      { type: 'quiz', score: 97.95868307329667 },
      { type: 'homework', score: 37.01609289831771 },
      { type: 'homework', score: 7.934055059943413 }
    ],
    class_id: 466
  },
  {
    student_id: 0,
    scores: [
      { type: 'exam', score: 84.72636832669608 },
      { type: 'quiz', score: 7.8865616909793435 },
      { type: 'homework', score: 22.860114572528147 },
      { type: 'homework', score: 80.85669686147487 }
    ],
    class_id: 149
  },
  {
    student_id: 2,
    scores: [
      { type: 'exam', score: 89.1838715782135 },
      { type: 'quiz', score: 60.78999591419918 },
      { type: 'homework', score: 80.1394331814776 },
      { type: 'homework', score: 87.07482638428962 }
    ],
    class_id: 452
  },
  {
    student_id: 3,
    scores: [
      { type: 'exam', score: 88.58869707977564 },
      { type: 'quiz', score: 15.320833625783425 },
      { type: 'homework', score: 22.603222285410663 },
      { type: 'homework', score: 20.473944912521148 }
    ],
    class_id: 311
  },
  {
    student_id: 3,
    scores: [
      { type: 'exam', score: 84.94169348862414 },
      { type: 'quiz', score: 44.16215616696122 },
      { type: 'homework', score: 2.0595339249265154 },
      { type: 'homework', score: 75.22138619912252 }
    ],
    class_id: 374
  }
]
```

3. Выбираем первые 5 студентов, у которых оценка за экзамен от 80 до 90 баллов, результат сортируем по 'scores.type': "exam" 
и 'scores.score' : asc

```js
db.grades.aggregate([
    // 1 шаг: выбираем выходные поля
  {
    $project: {
      _id: 0,
      student_id: 1,
      class_id: 1,
      
      examScore: {
        $first: {
          $filter: {
            input: "$scores",
            as: "score",
            cond: { $eq: ["$$score.type", "exam"] }
          }
        }
      }
    }
  },
  // 2 шаг: баллы экзамен (по новому полю) от 80 до 90
  {
    $match: {
      "examScore.score": { $gte: 80, $lte: 90 }
    }
  },
  // 3 шаг: сортируем по новому полю examScore.scpore
  {
    $sort: { "examScore.score": 1 }
  }
])
```

Результат:
```js
[
  {
    student_id: 7948,
    class_id: 140,
    examScore: { type: 'exam', score: 80.0009731161463 }
  },
  {
    student_id: 7697,
    class_id: 281,
    examScore: { type: 'exam', score: 80.0014948830213 }
  },
  {
    student_id: 4513,
    class_id: 56,
    examScore: { type: 'exam', score: 80.00173743198704 }
  },
  {
    student_id: 6740,
    class_id: 216,
    examScore: { type: 'exam', score: 80.00375775832785 }
  },
  {
    student_id: 7293,
    class_id: 125,
    examScore: { type: 'exam', score: 80.00517455428384 }
  },
  {
    student_id: 4446,
    class_id: 419,
    examScore: { type: 'exam', score: 80.00957623021205 }
  },
  {
    student_id: 6254,
    class_id: 394,
    examScore: { type: 'exam', score: 80.0101333125489 }
  },
...
]
```

4. Выбираем первые 5 перелетов, в которых: 
    - пункт назначения НЕ "DME" 
    - имя авиакомпании: S7 Airlines 

```js
db.routes.find({
    $and: [
        {dst_airport: {$ne: "DME"}}, 
        {'airline.name': 'S7 Airlines'}
    ]}, 
    {
        _id: 0, 
        stops: 0, 
        codeshare: 0}
).limit(5)
```

Результат: 
```js
[
  {
    airline: { id: 4329, name: 'S7 Airlines', alias: 'S7', iata: 'SBI' },
    src_airport: 'ALA',
    dst_airport: 'OVB',
    airplane: 320
  },
  {
    airline: { id: 4329, name: 'S7 Airlines', alias: 'S7', iata: 'SBI' },
    src_airport: 'CEK',
    dst_airport: 'LBD',
    airplane: 319
  },
  {
    airline: { id: 4329, name: 'S7 Airlines', alias: 'S7', iata: 'SBI' },
    src_airport: 'BKK',
    dst_airport: 'OVB',
    airplane: 763
  },
  {
    airline: { id: 4329, name: 'S7 Airlines', alias: 'S7', iata: 'SBI' },
    src_airport: 'CIT',
    dst_airport: 'OVB',
    airplane: 320
  },
  {
    airline: { id: 4329, name: 'S7 Airlines', alias: 'S7', iata: 'SBI' },
    src_airport: 'DME',
    dst_airport: 'AAQ',
    airplane: 320
  }
]
```

## 3. Запросы на обновление данных

1. Пусть необходимо обновить алиас для компаний S7 airlines на S7AIR

```js
db.routes.updateMany(
    {'airline.name': 'S7 Airlines'}, 
    { $set: {'airline.alias': 'S7AIR'}}
)
```

Результаты:
```js
{
  acknowledged: true,
  insertedId: null,
  matchedCount: 281,
  modifiedCount: 281,
  upsertedCount: 0
}
```
Новые результаты выборки:
```js
[
  {
    airline: { id: 4329, name: 'S7 Airlines', alias: 'S7AIR', iata: 'SBI' },
    src_airport: 'ALA',
    dst_airport: 'OVB',
    airplane: 320
  },
  {
    airline: { id: 4329, name: 'S7 Airlines', alias: 'S7AIR', iata: 'SBI' },
    src_airport: 'CEK',
    dst_airport: 'LBD',
    airplane: 319
  },
  {
    airline: { id: 4329, name: 'S7 Airlines', alias: 'S7AIR', iata: 'SBI' },
    src_airport: 'BKK',
    dst_airport: 'OVB',
    airplane: 763
  },
  {
    airline: { id: 4329, name: 'S7 Airlines', alias: 'S7AIR', iata: 'SBI' },
    src_airport: 'CIT',
    dst_airport: 'OVB',
    airplane: 320
  },
  {
    airline: { id: 4329, name: 'S7 Airlines', alias: 'S7AIR', iata: 'SBI' },
    src_airport: 'DME',
    dst_airport: 'AAQ',
    airplane: 320
  }
]
```


## 4. Задача на создание индекса

Пусть выберем все полеты авиакомпании Aerocondor('2B'):

```js
// Запрос
db.routes.find({'airline.alias': '2B'}).explain('executionStats')

// Результат
{
  explainVersion: '1',
  queryPlanner: {
    namespace: 'sample_training.routes',
    parsedQuery: { 'airline.alias': { '$eq': '2B' } },
    indexFilterSet: false,
    queryHash: '1D533821',
    planCacheShapeHash: '1D533821',
    planCacheKey: 'A7AC2195',
    optimizationTimeMillis: 0,
    maxIndexedOrSolutionsReached: false,
    maxIndexedAndSolutionsReached: false,
    maxScansToExplodeReached: false,
    prunedSimilarIndexes: false,
    winningPlan: {
      isCached: false,
      stage: 'COLLSCAN',
      filter: { 'airline.alias': { '$eq': '2B' } },
      direction: 'forward'
    },
    rejectedPlans: []
  },
  executionStats: {
    executionSuccess: true,
    nReturned: 42,
    executionTimeMillis: 28,
    totalKeysExamined: 0,
    totalDocsExamined: 66985,
    executionStages: {
      isCached: false,
      stage: 'COLLSCAN',
      filter: { 'airline.alias': { '$eq': '2B' } },
      nReturned: 42,
      executionTimeMillisEstimate: 19,
      works: 66986,
      advanced: 42,
      needTime: 66943,
      needYield: 0,
      saveState: 1,
      restoreState: 1,
      isEOF: 1,
      direction: 'forward',
      docsExamined: 66985
    }
  },
  ...
  command: {
    find: 'routes',
    filter: { 'airline.alias': '2B' },
    '$db': 'sample_training'
  },
  ...
  ok: 1
}
```

Ключевое здесь:
```js
stage: 'COLLSCAN'
executionTimeMillis: 28,
```

Теперь создадим индекс на это поле и проверим результат:

```js
// Запрос на создание индекса
db.routes.createIndex({ "airline.alias": 1 })

// Результат
db.routes.find({'airline.alias': '2B'}).explain('executionStats')
{
  explainVersion: '1',
  queryPlanner: {
    namespace: 'sample_training.routes',
    parsedQuery: { 'airline.alias': { '$eq': '2B' } },
    indexFilterSet: false,
    queryHash: '1D533821',
    planCacheShapeHash: '1D533821',
    planCacheKey: '8A85532B',
    optimizationTimeMillis: 0,
    maxIndexedOrSolutionsReached: false,
    maxIndexedAndSolutionsReached: false,
    maxScansToExplodeReached: false,
    prunedSimilarIndexes: false,
    winningPlan: {
      isCached: false,
      stage: 'FETCH',
      inputStage: {
        stage: 'IXSCAN',
        keyPattern: { 'airline.alias': 1 },
        indexName: 'airline.alias_1',
        isMultiKey: false,
        multiKeyPaths: { 'airline.alias': [] },
        isUnique: false,
        isSparse: false,
        isPartial: false,
        indexVersion: 2,
        direction: 'forward',
        indexBounds: { 'airline.alias': [ '["2B", "2B"]' ] }
      }
    },
    rejectedPlans: []
  },
  executionStats: {
    executionSuccess: true,
    nReturned: 42,
    executionTimeMillis: 2,
    totalKeysExamined: 42,
    totalDocsExamined: 42,
    executionStages: {
      isCached: false,
      stage: 'FETCH',
      nReturned: 42,
      executionTimeMillisEstimate: 0,
      works: 43,
      advanced: 42,
      needTime: 0,
      needYield: 0,
      saveState: 0,
      restoreState: 0,
      isEOF: 1,
      docsExamined: 42,
      alreadyHasObj: 0,
      inputStage: {
        stage: 'IXSCAN',
        nReturned: 42,
        executionTimeMillisEstimate: 0,
        works: 43,
        advanced: 42,
        needTime: 0,
        needYield: 0,
        saveState: 0,
        restoreState: 0,
        isEOF: 1,
        keyPattern: { 'airline.alias': 1 },
        indexName: 'airline.alias_1',
        isMultiKey: false,
        multiKeyPaths: { 'airline.alias': [] },
        isUnique: false,
        isSparse: false,
        isPartial: false,
        indexVersion: 2,
        direction: 'forward',
        indexBounds: { 'airline.alias': [ '["2B", "2B"]' ] },
        keysExamined: 42,
        seeks: 1,
        dupsTested: 0,
        dupsDropped: 0
      }
    }
  },
  ...
  command: {
    find: 'routes',
    filter: { 'airline.alias': '2B' },
    '$db': 'sample_training'
  },
  ...
  ok: 1
}
```

Ключевое здесь:
```js
stage: 'IXSCAN', // индексное сканирование
executionTimeMillis: 2 // время выполнения даже здесь уменьшилось в 10 раз
```