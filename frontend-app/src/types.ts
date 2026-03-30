export type AuthResponse = {
  userId: string;
  username: string;
  role: string;
  accessToken: string;
};

export type ForgotPasswordResponse = {
  message: string;
  expiresAt: string;
  inboxUrl: string;
};

export type UserSummary = {
  userId: string;
  username: string;
  email: string;
};

export type DocumentSummary = {
  id: string;
  title: string;
  currentVersion: number;
  role: "OWNER" | "EDITOR" | "VIEWER";
  updatedAt: string;
};

export type DocumentResponse = {
  id: string;
  title: string;
  content: string;
  ownerId: string;
  currentVersion: number;
  role: "OWNER" | "EDITOR" | "VIEWER";
  createdAt: string;
  updatedAt: string;
};

export type MembershipResponse = {
  userId: string;
  role: "OWNER" | "EDITOR" | "VIEWER";
};

export type VersionResponse = {
  id: number;
  versionNumber: number;
  title: string;
  content: string;
  createdBy: string;
  eventType: string;
  createdAt: string;
};

export type PresenceUser = {
  userId: string;
  username: string;
};

export type CollaborationState = {
  documentId: string;
  title: string;
  content: string;
  version: number;
  role: "OWNER" | "EDITOR" | "VIEWER";
  activeUsers: PresenceUser[];
};

export type OperationBroadcast = {
  documentId: string;
  version: number;
  type: "INSERT" | "DELETE" | "REPLACE";
  index: number;
  value?: string;
  length?: number;
  actorId: string;
  actorUsername?: string;
  content: string;
  requestId?: string;
  occurredAt: string;
};

export type EditorActivity = {
  username: string;
  occurredAt: string;
};

export type PresenceEvent = {
  documentId: string;
  action: string;
  activeUsers: PresenceUser[];
  occurredAt: string;
};
