ALTER TABLE chunks ADD COLUMN parent_id BIGINT;
ALTER TABLE chunks ADD COLUMN tokens TEXT;

ALTER TABLE chunks
    ADD CONSTRAINT fk_chunks_parent
        FOREIGN KEY (parent_id) REFERENCES chunks(id) ON DELETE CASCADE;

CREATE INDEX idx_chunks_doc_idx ON chunks(document_id, chunk_index);
CREATE INDEX idx_chunks_parent_idx ON chunks(parent_id);
