"""
rag.py — document ingestion + retrieval for Resonant
Supports PDF, DOCX, and plain text files.
Uses ChromaDB (local, no server needed) + sentence-transformers for embeddings.
"""

import os
import hashlib
import logging
from pathlib import Path

import chromadb
from chromadb.config import Settings
from sentence_transformers import SentenceTransformer

logger = logging.getLogger("rag")

VECTORSTORE_DIR = os.path.join(os.path.dirname(__file__), "vectorstore")
EMBED_MODEL = "all-MiniLM-L6-v2"  # 80MB, fast, runs on CPU fine
CHUNK_SIZE = 400        # characters per chunk
CHUNK_OVERLAP = 80


class RAGStore:
    def __init__(self):
        self.embedder = SentenceTransformer(EMBED_MODEL)
        self.client = chromadb.PersistentClient(
            path=VECTORSTORE_DIR,
            settings=Settings(anonymized_telemetry=False),
        )
        self.collection = self.client.get_or_create_collection(
            name="resonant_docs",
            metadata={"hnsw:space": "cosine"},
        )
        logger.info(f"RAG store ready. {self.collection.count()} chunks loaded.")

    # ------------------------------------------------------------------ #
    #  Ingestion                                                           #
    # ------------------------------------------------------------------ #

    def ingest_file(self, filepath: str, session_id: str) -> int:
        """Parse a file, chunk it, embed it, store it. Returns chunk count."""
        text = self._extract_text(filepath)
        if not text.strip():
            logger.warning(f"No text extracted from {filepath}")
            return 0

        chunks = self._chunk(text)
        ids, embeddings, metadatas, documents = [], [], [], []

        for i, chunk in enumerate(chunks):
            chunk_id = hashlib.md5(f"{session_id}:{filepath}:{i}:{chunk}".encode()).hexdigest()
            ids.append(chunk_id)
            embeddings.append(self.embedder.encode(chunk).tolist())
            metadatas.append({
                "session_id": session_id,
                "source": os.path.basename(filepath),
                "chunk_index": i,
            })
            documents.append(chunk)

        self.collection.upsert(
            ids=ids,
            embeddings=embeddings,
            metadatas=metadatas,
            documents=documents,
        )
        logger.info(f"Ingested {len(chunks)} chunks from {filepath}")
        return len(chunks)

    def delete_session(self, session_id: str):
        """Remove all chunks for a given session."""
        results = self.collection.get(where={"session_id": session_id})
        if results["ids"]:
            self.collection.delete(ids=results["ids"])
            logger.info(f"Deleted {len(results['ids'])} chunks for session {session_id}")

    # ------------------------------------------------------------------ #
    #  Retrieval                                                           #
    # ------------------------------------------------------------------ #

    def retrieve(self, query: str, session_id: str, top_k: int = 4) -> list[str]:
        """Return the top_k most relevant chunks for the query."""
        if self.collection.count() == 0:
            return []

        query_embedding = self.embedder.encode(query).tolist()

        # filter by session so students only see their own uploads
        results = self.collection.query(
            query_embeddings=[query_embedding],
            n_results=min(top_k, self.collection.count()),
            where={"session_id": session_id},
            include=["documents", "distances"],
        )

        chunks = results["documents"][0] if results["documents"] else []
        distances = results["distances"][0] if results["distances"] else []

        # only return chunks that are actually relevant (cosine distance < 0.6)
        relevant = [c for c, d in zip(chunks, distances) if d < 0.6]
        return relevant

    def has_documents(self, session_id: str) -> bool:
        results = self.collection.get(where={"session_id": session_id}, limit=1)
        return len(results["ids"]) > 0

    # ------------------------------------------------------------------ #
    #  Helpers                                                             #
    # ------------------------------------------------------------------ #

    def _extract_text(self, filepath: str) -> str:
        ext = Path(filepath).suffix.lower()
        try:
            if ext == ".pdf":
                return self._read_pdf(filepath)
            elif ext == ".docx":
                return self._read_docx(filepath)
            else:
                with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
                    return f.read()
        except Exception as e:
            logger.error(f"Failed to extract text from {filepath}: {e}")
            return ""

    def _read_pdf(self, filepath: str) -> str:
        import fitz  # PyMuPDF
        doc = fitz.open(filepath)
        return "\n".join(page.get_text() for page in doc)

    def _read_docx(self, filepath: str) -> str:
        from docx import Document
        doc = Document(filepath)
        return "\n".join(p.text for p in doc.paragraphs)

    def _chunk(self, text: str) -> list[str]:
        """Simple sliding window chunker."""
        chunks = []
        start = 0
        while start < len(text):
            end = start + CHUNK_SIZE
            chunks.append(text[start:end].strip())
            start += CHUNK_SIZE - CHUNK_OVERLAP
        return [c for c in chunks if len(c) > 40]  # skip tiny trailing chunks
