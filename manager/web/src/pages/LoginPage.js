import { useState } from "react";
import { useNavigate } from "react-router-dom"; 
import "./LoginPage.css";

export function LoginPage() {
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };
  
  const navigate = useNavigate();
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const res = await fetch("/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: form.email,
          password: form.password,
        }),
      });
      if (!res.ok) throw new Error("Identifiants invalides");
      const token = await res.text();
      navigate("/accueil");
    } catch (err) {
      alert(err.message);
    }
  };

  return (
    <div className="login-wrapper">
      <div className="login-container">
        <h2>Connexion</h2>

        <form onSubmit={handleSubmit}>

          <label>Email</label>
          <input
            type="email"
            name="email"
            placeholder="exemple@email.com"
            value={form.email}
            onChange={handleChange}
          />

          <label>Mot de passe</label>
          <input
            type="password"
            name="password"
            placeholder="********"
            value={form.password}
            onChange={handleChange}
          />

          <button type="submit">Se connecter</button>
        </form>

        <div className="note">Accès sécurisé</div>
      </div>
    </div>
  );
}
