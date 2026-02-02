import { useState } from "react";
import "./LoginPage.css";

interface LoginPageProps {
  onLoginSuccess: () => void;
}

export default function LoginPage({ onLoginSuccess }: LoginPageProps) {
  const [form, setForm] = useState({
    email: "",
    password: "",
  });
  const [error, setError] = useState("");

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      const res = await fetch("http://localhost:8082/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: form.email,
          password: form.password,
        }),
      });
      if (!res.ok) throw new Error("Identifiants invalides");
      const token = await res.text();
      localStorage.setItem("authToken", token);
      onLoginSuccess();
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="login-wrapper">
      <div className="login-container">
        <h2>Connexion Manager</h2>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit}>
          <label>Email</label>
          <input
            type="email"
            name="email"
            placeholder="exemple@email.com"
            value={form.email}
            onChange={handleChange}
            required
          />

          <label>Mot de passe</label>
          <input
            type="password"
            name="password"
            placeholder="********"
            value={form.password}
            onChange={handleChange}
            required
          />

          <button type="submit">Se connecter</button>
        </form>

        <div className="note">Accès sécurisé - Espace Manager</div>
      </div>
    </div>
  );
}
