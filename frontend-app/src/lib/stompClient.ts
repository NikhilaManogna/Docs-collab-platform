import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { config } from "./api";
import type { OperationBroadcast, PresenceEvent } from "../types";

type Handlers = {
  onSnapshot: (payload: unknown) => void;
  onOperation: (payload: OperationBroadcast) => void;
  onPresence: (payload: PresenceEvent) => void;
};

export class CollaborationSocket {
  private client: Client | null = null;
  private subscriptions: StompSubscription[] = [];

  connect(documentId: string, token: string, handlers: Handlers) {
    this.disconnect();
    const socketUrl = `${config.wsHttpUrl}${config.wsHttpUrl.includes("?") ? "&" : "?"}access_token=${encodeURIComponent(token)}`;

    this.client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 3000,
      debug: () => undefined
    });

    this.client.onConnect = () => {
      this.subscriptions.push(
        this.client!.subscribe(`/topic/documents.${documentId}.operations`, (message: IMessage) => {
          handlers.onOperation(JSON.parse(message.body) as OperationBroadcast);
        })
      );
      this.subscriptions.push(
        this.client!.subscribe(`/topic/documents.${documentId}.presence`, (message: IMessage) => {
          handlers.onPresence(JSON.parse(message.body) as PresenceEvent);
        })
      );
      this.subscriptions.push(
        this.client!.subscribe(`/user/queue/documents.${documentId}.snapshot`, (message: IMessage) => {
          handlers.onSnapshot(JSON.parse(message.body));
        })
      );

      this.client!.publish({
        destination: `/app/documents/${documentId}/join`,
        body: "{}"
      });
    };

    this.client.activate();
  }

  sendOperation(
    documentId: string,
    operation:
      | { type: "INSERT"; index: number; value: string }
      | { type: "DELETE"; index: number; length: number }
      | { type: "REPLACE"; index: number; value: string }
  ) {
    if (!this.client?.connected) {
      return;
    }

    this.client.publish({
      destination: `/app/documents/${documentId}/operations`,
      body: JSON.stringify({
        ...operation,
        requestId: `req-${Date.now()}-${Math.random().toString(16).slice(2)}`
      })
    });
  }

  disconnect() {
    this.subscriptions.forEach((subscription) => subscription.unsubscribe());
    this.subscriptions = [];
    this.client?.deactivate();
    this.client = null;
  }
}
