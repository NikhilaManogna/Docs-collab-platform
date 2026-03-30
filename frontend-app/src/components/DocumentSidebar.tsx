import { useState } from "react";
import type { DocumentSummary } from "../types";

type Props = {
  documents: DocumentSummary[];
  selectedDocumentId: string | null;
  creating: boolean;
  onSelect: (documentId: string) => void;
  onCreate: (title: string, content: string) => Promise<void>;
};

export function DocumentSidebar({ documents, selectedDocumentId, creating, onSelect, onCreate }: Props) {
  const [draftTitle, setDraftTitle] = useState("");

  const createDocument = async () => {
    const normalized = draftTitle.trim();
    if (!normalized) {
      return;
    }
    await onCreate(normalized, "");
    setDraftTitle("");
  };

  return (
    <aside className="sidebar card">
      <div className="sidebar-header">
        <div>
          <h2>Documents</h2>
        </div>
      </div>
      <div className="create-row">
        <input
          value={draftTitle}
          onChange={(event) => setDraftTitle(event.target.value)}
          placeholder="New document title"
        />
        <button onClick={createDocument} disabled={creating || !draftTitle.trim()}>
          {creating ? "Creating..." : "Create"}
        </button>
      </div>
      <div className="document-list">
        {documents.length === 0 ? <p className="muted">No documents yet. Create your first one.</p> : null}
        {documents.map((document) => (
          <button
            key={document.id}
            className={`document-item ${selectedDocumentId === document.id ? "selected" : ""}`}
            onClick={() => onSelect(document.id)}
            type="button"
          >
            <strong>{document.title}</strong>
            <span>{document.role}</span>
            <small>v{document.currentVersion}</small>
          </button>
        ))}
      </div>
    </aside>
  );
}
