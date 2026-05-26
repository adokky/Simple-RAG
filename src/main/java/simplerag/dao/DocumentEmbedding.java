package simplerag.dao;

import dev.langchain4j.data.embedding.Embedding;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "document_embeddings")
public class DocumentEmbedding extends PanacheEntityBase {

    @EmbeddedId
    public DocumentEmbeddingId id;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    public String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, Object> metadata;

    @Column(name = "embedding", columnDefinition = "vector(384)")
    public float[] embedding;

    public static List<DocumentEmbedding> findClosest(Embedding queryEmbedding, int maxResults) {
        String sql = """
            SELECT url, section_name, content, metadata, NULL as embedding
            FROM document_embeddings
            ORDER BY embedding <=> CAST(?1 AS vector)
            """;

        EntityManager em = DocumentEmbedding.getEntityManager();

        return em.createNativeQuery(sql, DocumentEmbedding.class)
                .setParameter(1, queryEmbedding.vector())
                .setMaxResults(maxResults)
                .getResultList();
    }

    public static int delete(String baseUrl) {
        long deleted = DocumentEmbedding.delete("DELETE FROM DocumentEmbedding WHERE id.url=?1", baseUrl);
        return Math.toIntExact(deleted);
    }
}
