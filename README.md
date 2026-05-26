# Simple RAG Application

Простое RAG-приложение на Quarkus с использованием векторного поиска в PostgreSQL и генерации ответов через OpenAI.

1. *Retrieval*: Вопрос преобразуется в 384-мерный вектор с помощью локальной модели `BGE-small-en-v1.5`. Поиск топ-3 релевантных фрагментов в PostgreSQL (`pgvector`) по косинусному сходству.
2. *Augmentation*: Найденный контекст объединяется с вопросом в единый промпт.
3. *Generation*: Ответ генерируется через OpenAI через LangChain4j.

### Chunking

Приложение позволяет индексировать произвольные HTML-документы с помощью метода `POST ai/ingest`:

- Документ разбивается на чанки по `<h>`-тегам.
- Если для раздела задан якорь (anchor), он сохраняется в метаданных для выдачи более точных URL.
- Нерелевантные разделы фильтруются.
- Для каждого чанка сохраняется путь из `<h>`-тегов.
- HTML каждого чанка преобразуется в Markdown.
- Путь из `<h>`-тегов векторизуется вместе с Markdown-версией чанка. Это позволяет сохранить контекст расположения раздела в структуре исходной страницы.


## Переменные окружения

Все переменные обязательны.

```env
OPENAI_API_KEY=<API key>
OPENAI_BASE_URI=https://api.example.com
OPENAI_MODEL_NAME=model-name

DB_USER=username
DB_PASSWORD=12345678
DB_NAME=simplerag
```

## Запуск

```shell
./mvnw quarkus:dev
```

БД должна автоматически подниматься через Docker Compose. 

## Использование

### Проиндексировать страницу
```bash
curl -X POST http://localhost:8080/ai/ingest --url-query "url=https://quarkus.io/guides/cdi"
```

### Задать вопрос
```bash
curl -X POST http://localhost:8080/ai/ask --url-query "q=What if multiple beans declare same type"
```

Сырой ответ:

```json
{
  "response": "If multiple beans declare the same type, the CDI container will fail the build if this causes ambiguity at any injection point. Specifically:\n\n- If **none** of the beans is assignable to the injection point, the build fails with `UnsatisfiedResolutionException`.\n- If **multiple** beans are assignable, the build fails with `AmbiguousResolutionException`.\n\nThis behavior is intentional because it helps identify and resolve unresolved dependencies early, allowing the application to fail fast.\n\nIn case of ambiguity, you can use programmatic lookup via `jakarta.enterprise.inject.Instance<T>` at runtime to iterate over all beans implementing the required type and resolve ambiguities.\n\nFor example:\n\n```java\n@Inject\nInstance<Dictionary> dictionaries; // non-ambiguous by design\n\nString translate(String sentence) {\n    for (Dictionary dict : dictionaries) {\n        // Use the selected Dictionary implementation as needed\n    }\n}\n```",
  "links":
  [
    "https://quarkus.io/guides/cdi#hm-wait-a-minute-what-happens-if-multiple-beans-declare-the-same-type",
    "https://quarkus.io/guides/cdi#ok-you-said-that-there-are-several-kinds-of-beans",
    "https://quarkus.io/guides/cdi#typesafe_resolution"
  ]
}
```

Форматированный ответ:

If multiple beans declare the same type, the CDI container will fail the build if this causes ambiguity at any injection point. Specifically:

- If **none** of the beans is assignable to the injection point, the build fails with `UnsatisfiedResolutionException`.
- If **multiple** beans are assignable, the build fails with `AmbiguousResolutionException`.

This behavior is intentional because it helps identify and resolve unresolved dependencies early, allowing the application to fail fast.

In case of ambiguity, you can use programmatic lookup via `jakarta.enterprise.inject.Instance<T>` at runtime to iterate over all beans implementing the required type and resolve ambiguities.

For example:

```java
@Inject
Instance<Dictionary> dictionaries; // non-ambiguous by design

String translate(String sentence) {
    for (Dictionary dict : dictionaries) {
        // Use the selected Dictionary implementation as needed
    }
}
```