import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./AddUser.css";

export function AddUserPage() {
  const [form, setForm] = useState({
    email: "",
    password: "",
    fullName: "",
  });
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      const res = await fetch("/auth/register", {
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
      // navigate("/accueil"); // décommente si tu veux rediriger
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="add-user-wrapper">
              <button
        className="btn"
        style={{ marginBottom: "1rem" }}
        onClick={() => navigate("/accueil")}
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
        {error && <div style={{ color: "red" }}>{error}</div>}
        {success && <div style={{ color: "green" }}>{success}</div>}
      </div>
    </div>
  );
}