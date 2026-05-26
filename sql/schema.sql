CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS document_embeddings (
    url TEXT NOT NULL,
    section_name TEXT NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    -- bge-small-en-v1.5
    embedding vector(384),
    PRIMARY KEY(url, section_name)
);

CREATE INDEX ON document_embeddings USING hnsw (embedding vector_cosine_ops);