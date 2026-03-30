import type {
  AuthResponse,
  CollaborationState,
  DocumentResponse,
  DocumentSummary,
  ForgotPasswordResponse,
  MembershipResponse,
  UserSummary,
  VersionResponse
} from "../types";

const browserHost = typeof window !== "undefined" ? window.location.hostname : "localhost";
const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "http://localhost:8080";
const WS_BASE = (import.meta.env.VITE_WS_URL as string | undefined) ?? `http://${browserHost}:8082/ws/collaboration`;

async function request<T>(path: string, init?: RequestInit, token?: string): Promise<T> {
  const headers = new Headers(init?.headers ?? {});
  headers.set("Content-Type", "application/json");
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers
  });

  if (!response.ok) {
    const text = await response.text();
    let parsed: { error?: string; message?: string } | null = null;
    try {
      parsed = JSON.parse(text) as { error?: string; message?: string };
    } catch {
      parsed = null;
    }

    const message = parsed?.message ?? parsed?.error ?? text ?? `Request failed with ${response.status}`;
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const api = {
  login(username: string, password: string) {
    return request<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password })
    });
  },
  register(username: string, email: string, password: string) {
    return request<AuthResponse>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ username, email, password })
    });
  },
  forgotPassword(email: string) {
    return request<ForgotPasswordResponse>("/api/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify({ email })
    });
  },
  resetPassword(token: string, newPassword: string) {
    return request<void>("/api/auth/reset-password", {
      method: "POST",
      body: JSON.stringify({ token, newPassword })
    });
  },
  listUsers(query = "") {
    const search = new URLSearchParams({ query });
    return request<UserSummary[]>(`/api/auth/users?${search.toString()}`);
  },
  listDocuments(token: string) {
    return request<DocumentSummary[]>("/api/documents", undefined, token);
  },
  createDocument(token: string, title: string, content: string) {
    return request<DocumentResponse>("/api/documents", {
      method: "POST",
      body: JSON.stringify({ title, content })
    }, token);
  },
  getDocument(token: string, documentId: string) {
    return request<DocumentResponse>(`/api/documents/${documentId}`, undefined, token);
  },
  updateDocument(token: string, documentId: string, title: string, content: string) {
    return request<DocumentResponse>(`/api/documents/${documentId}`, {
      method: "PUT",
      body: JSON.stringify({ title, content })
    }, token);
  },
  listVersions(token: string, documentId: string) {
    return request<VersionResponse[]>(`/api/documents/${documentId}/versions`, undefined, token);
  },
  rollback(token: string, documentId: string, versionId: number) {
    return request<DocumentResponse>(`/api/documents/${documentId}/rollback/${versionId}`, {
      method: "POST"
    }, token);
  },
  listMembers(token: string, documentId: string) {
    return request<MembershipResponse[]>(`/api/documents/${documentId}/members`, undefined, token);
  },
  grantAccess(token: string, documentId: string, userId: string, role: "EDITOR" | "VIEWER") {
    return request<MembershipResponse>(`/api/documents/${documentId}/members`, {
      method: "POST",
      body: JSON.stringify({ userId, role })
    }, token);
  },
  collaborationState(token: string, documentId: string) {
    return request<CollaborationState>(`/api/collaboration/documents/${documentId}/state`, undefined, token);
  }
};

export const config = {
  apiBaseUrl: API_BASE,
  wsUrl: WS_BASE.replace(/^http:/, "ws:").replace(/^https:/, "wss:"),
  wsHttpUrl: WS_BASE
    .replace(/^ws:/, "http:")
    .replace(/^wss:/, "https:")
};
