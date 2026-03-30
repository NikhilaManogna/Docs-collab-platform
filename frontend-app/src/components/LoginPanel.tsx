import { useState, type FormEvent } from "react";

type Props = {
  loading: boolean;
  onLogin: (username: string, password: string) => Promise<void>;
  onRegister: (username: string, email: string, password: string) => Promise<void>;
  onForgotPassword: (email: string) => Promise<{ expiresAt: string; inboxUrl: string }>;
  onResetPassword: (token: string, newPassword: string) => Promise<void>;
};

export function LoginPanel({ loading, onLogin, onRegister, onForgotPassword, onResetPassword }: Props) {
  const [mode, setMode] = useState<"login" | "register" | "forgot" | "reset">("login");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [resetToken, setResetToken] = useState("");
  const [resetExpiresAt, setResetExpiresAt] = useState("");
  const [inboxUrl, setInboxUrl] = useState("");

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (mode === "login") {
      await onLogin(username, password);
      return;
    }
    if (mode === "forgot") {
      const result = await onForgotPassword(email);
      setResetExpiresAt(result.expiresAt);
      setInboxUrl(result.inboxUrl);
      setPassword("");
      setMode("reset");
      return;
    }
    if (mode === "reset") {
      await onResetPassword(resetToken, password);
      setMode("login");
      setPassword("");
      return;
    }
    await onRegister(username, email, password);
  };

  const switchMode = (nextMode: "login" | "register" | "forgot" | "reset") => {
    setMode(nextMode);
    setUsername("");
    setEmail("");
    setPassword("");
    if (nextMode !== "reset") {
      setResetToken("");
      setResetExpiresAt("");
      setInboxUrl("");
    }
  };

  return (
    <section className="card auth-card">
      <div className="auth-header">
        <h1>{mode === "login" ? "Sign in" : mode === "register" ? "Create account" : mode === "forgot" ? "Forgot password" : "Reset password"}</h1>
        <p className="muted">
          {mode === "login"
            ? "Access your collaborative workspace."
            : mode === "register"
              ? "Create an account to start managing documents."
              : mode === "forgot"
                ? "Enter your email and we will send a password reset email."
                : "Paste the reset token from your email to set a new password."}
        </p>
      </div>
      <form onSubmit={submit} className="stack">
        {mode === "login" || mode === "register" ? (
          <label>
            Username
            <input value={username} onChange={(event) => setUsername(event.target.value)} placeholder="Enter username" />
          </label>
        ) : null}
        {mode === "register" || mode === "forgot" ? (
          <label>
            Email
            <input value={email} onChange={(event) => setEmail(event.target.value)} placeholder="Enter email" />
          </label>
        ) : null}
        {mode === "reset" ? (
          <label>
            Reset token
            <input value={resetToken} onChange={(event) => setResetToken(event.target.value)} placeholder="Paste reset token" />
          </label>
        ) : null}
        {mode !== "forgot" ? (
          <label>
            {mode === "reset" ? "New password" : "Password"}
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder={mode === "reset" ? "Enter new password" : "Enter password"}
            />
          </label>
        ) : null}
        {mode === "reset" && resetExpiresAt ? <p className="muted">Token expires at: {new Date(resetExpiresAt).toLocaleString()}</p> : null}
        {mode === "reset" && inboxUrl ? <p className="muted">Check your inbox here: {inboxUrl}</p> : null}
        {mode === "reset" && !inboxUrl ? <p className="muted">Check your email provider inbox for the reset token.</p> : null}
        <button type="submit" disabled={loading}>
          {loading
            ? "Please wait..."
            : mode === "login"
              ? "Login"
              : mode === "register"
                ? "Register"
                : mode === "forgot"
                  ? "Send reset email"
                  : "Reset password"}
        </button>
      </form>
      {mode === "login" ? (
        <p className="auth-switch">
          Need an account?{" "}
          <button className="text-button" onClick={() => switchMode("register")} type="button">
            Register here
          </button>{" "}
          or{" "}
          <button className="text-button" onClick={() => switchMode("forgot")} type="button">
            Forgot password
          </button>
        </p>
      ) : null}
      {mode === "register" ? (
        <p className="auth-switch">
          Already have an account?{" "}
          <button className="text-button" onClick={() => switchMode("login")} type="button">
            Login here
          </button>
        </p>
      ) : null}
      {mode === "forgot" ? (
        <p className="auth-switch">
          Remembered it?{" "}
          <button className="text-button" onClick={() => switchMode("login")} type="button">
            Back to login
          </button>
        </p>
      ) : null}
      {mode === "reset" ? (
        <p className="auth-switch">
          Need another token?{" "}
          <button className="text-button" onClick={() => switchMode("forgot")} type="button">
            Request again
          </button>
        </p>
      ) : null}
    </section>
  );
}
