import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { postJson } from "../api/client";
import "./LoginPage.css";

export function LoginPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    email: "",
    password: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const token = await postJson("/auth/login", {
        email: form.email,
        password: form.password,
      });
      
      localStorage.setItem("authToken", token);
      navigate("/home");
    } catch (err) {
      setError(err.message || "Erreur de connexion");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-wrapper">
      <div className="login-container">
        <h2>Connexion</h2>

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
            disabled={loading}
          />

          <label>Mot de passe</label>
          <input
            type="password"
            name="password"
            placeholder="********"
            value={form.password}
            onChange={handleChange}
            required
            disabled={loading}
          />

          <button type="submit" disabled={loading}>
            {loading ? "Connexion..." : "Se connecter"}
          </button>
        </form>

        <div className="note">Accès sécurisé</div>
      </div>
    </div>
  );
}
