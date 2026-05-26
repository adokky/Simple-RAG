package simplerag.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DocumentEmbeddingId implements Serializable {

    @Column(name = "url", nullable = false)
    public String url;

    @Column(name = "section_name", nullable = false)
    public String sectionName;

    public DocumentEmbeddingId() {}

    public DocumentEmbeddingId(String url, String sectionName) {
        this.url = url;
        this.sectionName = sectionName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentEmbeddingId i = (DocumentEmbeddingId) o;
        return Objects.equals(url, i.url) && Objects.equals(sectionName, i.sectionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, sectionName);
    }
}
