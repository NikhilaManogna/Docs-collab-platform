import { useEffect, useMemo, useState } from "react";
import type { ChangeEvent } from "react";
import type {
  CollaborationState,
  EditorActivity,
  MembershipResponse,
  PresenceUser,
  UserSummary,
  VersionResponse
} from "../types";

type Props = {
  documentId: string | null;
  state: CollaborationState | null;
  content: string;
  title: string;
  currentUserId: string;
  currentUsername: string;
  users: UserSummary[];
  lastActivity: EditorActivity | null;
  typingActivity: EditorActivity | null;
  saving: boolean;
  members: MembershipResponse[];
  versions: VersionResponse[];
  onTitleChange: (title: string) => void;
  onContentChange: (event: ChangeEvent<HTMLTextAreaElement>) => void;
  onPersistTitle: () => Promise<void>;
  onShare: (userId: string, role: "EDITOR" | "VIEWER") => Promise<void>;
  onRollback: (versionId: number) => Promise<void>;
};

function renderPresence(activeUsers: PresenceUser[]) {
  if (activeUsers.length === 0) {
    return <p className="muted">No active collaborators yet.</p>;
  }

  return (
    <div className="presence-list">
      {activeUsers.map((user) => (
        <span key={user.userId} className="presence-pill">
          {user.username}
        </span>
      ))}
    </div>
  );
}

export function EditorWorkspace({
  documentId,
  state,
  content,
  title,
  currentUserId,
  currentUsername,
  users,
  lastActivity,
  typingActivity,
  saving,
  members,
  versions,
  onTitleChange,
  onContentChange,
  onPersistTitle,
  onShare,
  onRollback
}: Props) {
  const [shareUserId, setShareUserId] = useState("");
  const [shareRole, setShareRole] = useState<"EDITOR" | "VIEWER">("EDITOR");

  const shareableUsers = useMemo(
    () => users.filter((user) => user.userId !== currentUserId),
    [currentUserId, users]
  );

  const userLookup = useMemo(() => {
    const lookup = new Map<string, string>();
    lookup.set(currentUserId, currentUsername);
    users.forEach((user) => lookup.set(user.userId, user.username));
    return lookup;
  }, [currentUserId, currentUsername, users]);

  useEffect(() => {
    if (!shareUserId && shareableUsers.length > 0) {
      setShareUserId(shareableUsers[0].userId);
    }
    if (shareableUsers.length === 0) {
      setShareUserId("");
    }
  }, [shareUserId, shareableUsers]);

  const shareDocument = async () => {
    if (!shareUserId) {
      throw new Error("Create another account first, then select it here to share access.");
    }
    await onShare(shareUserId, shareRole);
    setShareRole("EDITOR");
  };

  if (!documentId || !state) {
    return (
      <section className="card editor-empty">
        <h2>Select a document to start collaborating.</h2>
      </section>
    );
  }

  return (
    <section className="editor-layout">
      <div className="card editor-card">
        <div className="editor-toolbar">
          <div>
            <input
              className="title-input"
              value={title}
              onChange={(event) => onTitleChange(event.target.value)}
              disabled={state.role === "VIEWER"}
            />
          </div>
          <div className="toolbar-actions">
            <button onClick={() => void onPersistTitle()} disabled={saving || state.role === "VIEWER"}>
              {saving ? "Saving..." : "Save title"}
            </button>
          </div>
        </div>
        <textarea
          className="editor-textarea"
          value={content}
          onChange={onContentChange}
          disabled={state.role === "VIEWER"}
          placeholder="Start typing..."
        />
        <div className="editor-meta">
          <span>Document ID: {documentId}</span>
          <span>Version: {state.version}</span>
          <span>Role: {state.role}</span>
          <span>Signed in as: {currentUsername}</span>
          {typingActivity ? <span>Currently editing: {typingActivity.username}</span> : null}
          {lastActivity ? <span>Last updated by: {lastActivity.username}</span> : null}
        </div>
      </div>

      <div className="side-panels">
        <section className="card side-card">
          <h3>Active collaborators</h3>
          {renderPresence(state.activeUsers)}
        </section>

        <section className="card side-card">
          <h3>Access control</h3>
          {state.role === "OWNER" ? (
            <div className="share-form">
              <select
                value={shareUserId}
                onChange={(event) => setShareUserId(event.target.value)}
                disabled={shareableUsers.length === 0}
              >
                {shareableUsers.length === 0 ? <option value="">No other users available yet</option> : null}
                {shareableUsers.map((user) => (
                  <option key={user.userId} value={user.userId}>
                    {user.username} ({user.email})
                  </option>
                ))}
              </select>
              <select value={shareRole} onChange={(event) => setShareRole(event.target.value as "EDITOR" | "VIEWER")}>
                <option value="EDITOR">EDITOR</option>
                <option value="VIEWER">VIEWER</option>
              </select>
              <button onClick={() => void shareDocument()} disabled={shareableUsers.length === 0} type="button">
                Share access
              </button>
            </div>
          ) : null}
          <div className="info-list">
            {members.map((member) => (
              <div key={member.userId} className="info-row">
                <span>{userLookup.get(member.userId) ?? member.userId}</span>
                <strong>{member.role}</strong>
              </div>
            ))}
          </div>
        </section>

        <section className="card side-card">
          <h3>Rollback versions</h3>
          <div className="info-list">
            {versions.map((version) => (
              <button
                key={version.id}
                className="version-item"
                onClick={() => void onRollback(version.id)}
                type="button"
              >
                <strong>v{version.versionNumber}</strong>
                <span>{version.eventType}</span>
              </button>
            ))}
          </div>
        </section>
      </div>
    </section>
  );
}
