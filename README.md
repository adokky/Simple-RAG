# Simple RAG Application

Простое RAG-приложение на Quarkus с использованием векторного поиска в PostgreSQL и генерации ответов через OpenAI.

1. *Retrieval*: Вопрос преобразуется в 384-мерный вектор с помощью локальной модели `BGE-small-en-v1.5`. Поиск топ-3 релевантных фрагментов в PostgreSQL (`pgvector`) по косинусному сходству.
2. *Augmentation*: Найденный контекст объединяется с вопросом в единый промпт.
3. *Generation*: Ответ генерируется через OpenAI через LangChain4j.

### Chunking

Приложение позволяет индексировать произвольные HTML-документы с помощью метода `POST ai/ingest`:

- Документ разбивается на чанки по `<h>`-тегам.
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

Ответ:

```text
If multiple beans declare the same type, exactly one bean must be assignable to an injection point for it to work correctly. If none of the beans are assignable, the build will fail with an `UnsatisfiedResolutionException`. If multiple beans are assignable, the build will fail with an `AmbiguousResolutionException`.
```