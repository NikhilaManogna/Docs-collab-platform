import { useEffect, useRef, useState, type ChangeEvent } from "react";
import { LoginPanel } from "./components/LoginPanel";
import { DocumentSidebar } from "./components/DocumentSidebar";
import { EditorWorkspace } from "./components/EditorWorkspace";
import { api } from "./lib/api";
import { CollaborationSocket } from "./lib/stompClient";
import type {
  AuthResponse,
  CollaborationState,
  DocumentSummary,
  EditorActivity,
  MembershipResponse,
  OperationBroadcast,
  PresenceEvent,
  UserSummary,
  VersionResponse
} from "./types";

type SessionState = AuthResponse | null;

export default function App() {
  const [session, setSession] = useState<SessionState>(() => {
    const raw = window.localStorage.getItem("docs-collab-session");
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  });
  const [documents, setDocuments] = useState<DocumentSummary[]>([]);
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState<string | null>(null);
  const [collaborationState, setCollaborationState] = useState<CollaborationState | null>(null);
  const [editorTitle, setEditorTitle] = useState("");
  const [editorContent, setEditorContent] = useState("");
  const [members, setMembers] = useState<MembershipResponse[]>([]);
  const [versions, setVersions] = useState<VersionResponse[]>([]);
  const [lastActivity, setLastActivity] = useState<EditorActivity | null>(null);
  const [typingActivity, setTypingActivity] = useState<EditorActivity | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const socketRef = useRef(new CollaborationSocket());
  const suppressDiffRef = useRef(false);
  const typingTimeoutRef = useRef<number | null>(null);
  const userLookupRef = useRef<Map<string, string>>(new Map());

  useEffect(() => {
    const nextLookup = new Map<string, string>();
    if (session) {
      nextLookup.set(session.userId, session.username);
    }
    users.forEach((user) => nextLookup.set(user.userId, user.username));
    userLookupRef.current = nextLookup;
  }, [session, users]);

  useEffect(() => {
    if (session) {
      window.localStorage.setItem("docs-collab-session", JSON.stringify(session));
      void Promise.all([loadDocuments(session.accessToken), loadUsers()]);
    } else {
      window.localStorage.removeItem("docs-collab-session");
      setUsers([]);
    }
  }, [session]);

  useEffect(() => {
    if (!session) {
      return;
    }

    const timer = window.setInterval(() => {
      void Promise.all([loadDocuments(session.accessToken), loadUsers()]);
    }, 5000);

    return () => window.clearInterval(timer);
  }, [session, selectedDocumentId]);

  useEffect(() => {
    if (!message) {
      return;
    }
    const timer = window.setTimeout(() => setMessage(null), 6000);
    return () => window.clearTimeout(timer);
  }, [message]);

  useEffect(() => {
    if (!session || !selectedDocumentId) {
      socketRef.current.disconnect();
      return;
    }

    void openDocument(selectedDocumentId, session.accessToken);

    socketRef.current.connect(selectedDocumentId, session.accessToken, {
      onSnapshot: (payload) => {
        const snapshot = payload as CollaborationState;
        suppressDiffRef.current = true;
        setCollaborationState(snapshot);
        setEditorTitle(snapshot.title);
        setEditorContent(snapshot.content);
      },
      onOperation: (payload: OperationBroadcast) => {
        suppressDiffRef.current = true;
        setCollaborationState((current) => current ? { ...current, version: payload.version, content: payload.content } : current);
        setEditorContent(payload.content);
        const actor = payload.actorUsername ?? userLookupRef.current.get(payload.actorId) ?? payload.actorId;
        const activity = { username: actor, occurredAt: payload.occurredAt };
        setLastActivity(activity);
        setTypingActivity(activity);
        if (typingTimeoutRef.current) {
          window.clearTimeout(typingTimeoutRef.current);
        }
        typingTimeoutRef.current = window.setTimeout(() => setTypingActivity(null), 1800);
      },
      onPresence: (payload: PresenceEvent) => {
        setCollaborationState((current) => current ? { ...current, activeUsers: payload.activeUsers } : current);
      }
    });

    return () => {
      socketRef.current.disconnect();
    };
  }, [selectedDocumentId, session]);

  async function loadDocuments(token: string) {
    const result = await api.listDocuments(token);
    setDocuments(result);
    if (result.length === 0) {
      setSelectedDocumentId(null);
      setCollaborationState(null);
      return;
    }
    if (!selectedDocumentId || !result.some((document) => document.id === selectedDocumentId)) {
      setSelectedDocumentId(result[0].id);
    }
  }

  async function loadUsers() {
    const result = await api.listUsers();
    setUsers(result);
  }

  async function openDocument(documentId: string, token: string) {
    const [state, memberList, versionList] = await Promise.all([
      api.collaborationState(token, documentId),
      api.listMembers(token, documentId),
      api.listVersions(token, documentId)
    ]);
    setCollaborationState(state);
    setEditorTitle(state.title);
    setEditorContent(state.content);
    setMembers(memberList);
    setVersions(versionList);
    if (versionList.length > 0) {
      const latest = versionList[0];
      const actor = userLookupRef.current.get(latest.createdBy) ?? latest.createdBy;
      setLastActivity({ username: actor, occurredAt: latest.createdAt });
    } else {
      setLastActivity(null);
    }
    setTypingActivity(null);
  }

  async function handleLogin(username: string, password: string) {
    setBusy(true);
    try {
      const auth = await api.login(username, password);
      setSession(auth);
      setMessage(`Logged in as ${auth.username}`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Login failed");
    } finally {
      setBusy(false);
    }
  }

  async function handleRegister(username: string, email: string, password: string) {
    setBusy(true);
    try {
      const auth = await api.register(username, email, password);
      setSession(auth);
      setMessage(`Registered and logged in as ${auth.username}`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Registration failed");
    } finally {
      setBusy(false);
    }
  }

  async function handleForgotPassword(email: string) {
    setBusy(true);
    try {
      const response = await api.forgotPassword(email);
      setMessage(response.message);
      return { expiresAt: response.expiresAt, inboxUrl: response.inboxUrl };
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Password reset email could not be sent");
      throw error;
    } finally {
      setBusy(false);
    }
  }

  async function handleResetPassword(token: string, newPassword: string) {
    setBusy(true);
    try {
      await api.resetPassword(token, newPassword);
      setMessage("Password reset successful. Please sign in with the new password.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Password reset failed");
      throw error;
    } finally {
      setBusy(false);
    }
  }

  async function handleCreate(title: string, content: string) {
    if (!session) {
      return;
    }
    setBusy(true);
    try {
      const created = await api.createDocument(session.accessToken, title, content);
      await loadDocuments(session.accessToken);
      setSelectedDocumentId(created.id);
      await openDocument(created.id, session.accessToken);
      setMessage(`Created document ${created.title}`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Document creation failed");
    } finally {
      setBusy(false);
    }
  }

  async function handlePersistTitle() {
    if (!session || !selectedDocumentId || !collaborationState || collaborationState.role === "VIEWER") {
      return;
    }
    setBusy(true);
    try {
      await api.updateDocument(session.accessToken, selectedDocumentId, editorTitle, editorContent);
      await openDocument(selectedDocumentId, session.accessToken);
      await loadDocuments(session.accessToken);
      setMessage("Title saved");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Save failed");
    } finally {
      setBusy(false);
    }
  }

  async function handleShare(userId: string, role: "EDITOR" | "VIEWER") {
    if (!session || !selectedDocumentId) {
      return;
    }
    setBusy(true);
    try {
      await api.grantAccess(session.accessToken, selectedDocumentId, userId, role);
      setMembers(await api.listMembers(session.accessToken, selectedDocumentId));
      await loadUsers();
      const recipient = users.find((user) => user.userId === userId);
      setMessage(`Granted ${role} access to ${recipient?.username ?? "selected user"}`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Share failed");
    } finally {
      setBusy(false);
    }
  }

  async function handleRollback(versionId: number) {
    if (!session || !selectedDocumentId) {
      return;
    }
    setBusy(true);
    try {
      await api.rollback(session.accessToken, selectedDocumentId, versionId);
      await openDocument(selectedDocumentId, session.accessToken);
      await loadDocuments(session.accessToken);
      setMessage(`Rolled back using version ${versionId}`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Rollback failed");
    } finally {
      setBusy(false);
    }
  }

  function handleContentChange(event: ChangeEvent<HTMLTextAreaElement>) {
    const nextValue = event.target.value;

    if (!selectedDocumentId || !collaborationState) {
      setEditorContent(nextValue);
      return;
    }

    if (collaborationState.role === "VIEWER") {
      return;
    }

    if (suppressDiffRef.current) {
      suppressDiffRef.current = false;
      setEditorContent(nextValue);
      return;
    }

    setEditorContent(nextValue);
    socketRef.current.sendOperation(selectedDocumentId, {
      type: "REPLACE",
      index: 0,
      value: nextValue
    });
  }

  const logout = () => {
    socketRef.current.disconnect();
    setSession(null);
    setDocuments([]);
    setSelectedDocumentId(null);
    setCollaborationState(null);
    setEditorTitle("");
    setEditorContent("");
    setMembers([]);
    setVersions([]);
    setLastActivity(null);
    setTypingActivity(null);
  };

  return (
    <div className="app-shell">
      {session ? (
        <header className="topbar minimal-topbar">
          <div className="topbar-actions">
            <span>{session.username}</span>
            <button onClick={logout}>Logout</button>
          </div>
        </header>
      ) : null}

      {message ? <div className="banner">{message}</div> : null}

      {!session ? (
        <LoginPanel
          loading={busy}
          onLogin={handleLogin}
          onRegister={handleRegister}
          onForgotPassword={handleForgotPassword}
          onResetPassword={handleResetPassword}
        />
      ) : (
        <main className="workspace">
          <DocumentSidebar
            documents={documents}
            selectedDocumentId={selectedDocumentId}
            creating={busy}
            onSelect={setSelectedDocumentId}
            onCreate={handleCreate}
          />
          <EditorWorkspace
            documentId={selectedDocumentId}
            state={collaborationState}
            content={editorContent}
            title={editorTitle}
            currentUserId={session.userId}
            currentUsername={session.username}
            users={users}
            lastActivity={lastActivity}
            typingActivity={typingActivity}
            saving={busy}
            members={members}
            versions={versions}
            onTitleChange={setEditorTitle}
            onContentChange={handleContentChange}
            onPersistTitle={handlePersistTitle}
            onShare={handleShare}
            onRollback={handleRollback}
          />
        </main>
      )}
    </div>
  );
}
