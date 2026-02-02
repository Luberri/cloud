import { useState } from "react";
import "./AddUserPage.css";

interface AddUserPageProps {
  onNavigate: (page: string) => void;
}

export default function AddUserPage({ onNavigate }: AddUserPageProps) {
  const [form, setForm] = useState({
    email: "",
    password: "",
    fullName: "",
  });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      const res = await fetch("http://localhost:8082/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: form.email,
          password: form.password,
          fullName: form.fullName,
        }),
      });
      if (!res.ok) throw new Error("Erreur lors de l'ajout");
      setSuccess("Utilisateur ajouté !");
      setForm({ email: "", password: "", fullName: "" });
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="add-user-wrapper">
      <button
        className="btn"
        style={{ marginBottom: "1rem" }}
        onClick={() => onNavigate('home')}
      >
        Retour à l'accueil
      </button>
      <div className="add-user-container">
        <h2>Ajouter un utilisateur</h2>
        <form onSubmit={handleSubmit}>
          <label>Nom complet</label>
          <input
            type="text"
            name="fullName"
            value={form.fullName}
            onChange={handleChange}
            required
          />
          <label>Email</label>
          <input
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
            required
          />
          <label>Mot de passe</label>
          <input
            type="password"
            name="password"
            value={form.password}
            onChange={handleChange}
            required
          />
          <button type="submit">Ajouter</button>
        </form>
        {error && <div className="error">{error}</div>}
        {success && <div className="success">{success}</div>}
      </div>
    </div>
  );
}
