import { useState } from "react";
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

  const handleSubmit = (e) => {
    e.preventDefault();
    console.log("Login data :", form);
    alert("Formulaire prêt — branche ton API ici 🔐");
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
