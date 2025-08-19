package ru.vladimir;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Main {
    private static Jedis jedis;
    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String jsonPath = "E:/users.json";
        String jsonString = new String(Files.readAllBytes(Paths.get(jsonPath)));
        List<Map<String, Object>> dataList = mapper.readValue(jsonString, new TypeReference<List<Map<String, Object>>>(){});

        jedis = new Jedis("localhost", 6379);
        jedis.flushAll();

        // Запускаем тесты
        testString(jsonString);
        testHash(dataList);
        testList(dataList);
        testZSet(dataList);

        jedis.close();
    }

    /**
     * Тест стрингового формата записи
     * @param jsonString
     */
    private static void testString(String jsonString) {
        Pipeline p = jedis.pipelined();
        long startTime = System.currentTimeMillis();

        // Команда помещается в пайплайн
        p.set("test_json_string", jsonString);

        // Отправка команды и ожидание ответа
        p.sync();
        long writeTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        String retrievedString = jedis.get("test_json_string");
        long readTime = System.currentTimeMillis() - startTime;

        System.out.println("\n--- String Test  ---");
        System.out.println("Write Time: " + writeTime + "ms");
        System.out.println("Read Time: " + readTime + "ms");
    }

    /**
     * Тест использования хэш-таблиц
     * @param dataList
     */
    private static void testHash(List<Map<String, Object>> dataList) {
        Pipeline p = jedis.pipelined();
        long startTime = System.currentTimeMillis();

        dataList.forEach(item -> {
            try {
                String key = item.get("_id").toString();
                p.hset("test_json_hash", key, mapper.writeValueAsString(item));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        p.sync();
        long writeTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        Map<String, String> retrievedHash = jedis.hgetAll("test_json_hash");
        long readTime = System.currentTimeMillis() - startTime;

        System.out.println("\n--- Hash Test  ---");
        System.out.println("Write Time: " + writeTime + "ms");
        System.out.println("Read Time: " + readTime + "ms");
    }

    /**
     * Тест использования связанных-списков
     * @param dataList
     */
    private static void testList(List<Map<String, Object>> dataList) {
        Pipeline p = jedis.pipelined();
        long startTime = System.currentTimeMillis();

        dataList.forEach(item -> {
            try {
                p.rpush("test_json_list", mapper.writeValueAsString(item));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        p.sync();
        long writeTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        List<String> retrievedList = jedis.lrange("test_json_list", 0, -1);
        long readTime = System.currentTimeMillis() - startTime;

        System.out.println("\n--- List Test  ---");
        System.out.println("Write Time: " + writeTime + "ms");
        System.out.println("Read Time: " + readTime + "ms");
    }

    /**
     * Тест структуры упорядоченные множества
      * @param dataList
     */
    private static void testZSet(List<Map<String, Object>> dataList) {
        Pipeline p = jedis.pipelined();
        long startTime = System.currentTimeMillis();

        dataList.forEach(item -> {
            try {
                String member = mapper.writeValueAsString(item);
                Double score = Double.parseDouble(item.get("score").toString()); // в данных как раз есть сгенерированное поле score
                p.zadd("test_json_zset", score, member);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        p.sync();
        long writeTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        List<String> retrievedZSet = jedis.zrange("test_json_zset", 0, -1);
        long readTime = System.currentTimeMillis() - startTime;

        System.out.println("\n--- ZSet Test  ---");
        System.out.println("Write Time: " + writeTime + "ms");
        System.out.println("Read Time: " + readTime + "ms");
    }
}