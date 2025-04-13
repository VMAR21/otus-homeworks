#!/bin/bash

echo "===> Запуск инициализации MongoDB"

for directory in /docker-entrypoint-initdb.d/*; do
    if [ -d "${directory}" ]; then
        db_name=$(basename "$directory")
        echo "Импорт базы: $db_name"

        for data_file in "$directory"/*.json; do
            collection_name=$(basename "$data_file" .json)
            echo "Импорт коллекции: $collection_name ..."

            mongoimport --drop --host localhost --port 27017 \
              --db "$db_name" \
              --collection "$collection_name" \
              --file "$data_file"
            
            echo "Импорт коллекции: $collection_name успешно завершен"
        done
    fi
done

echo "Инициализация завершена"
