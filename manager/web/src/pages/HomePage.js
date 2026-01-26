import { Link } from "react-router-dom";
import "./HomePage.css";

export function HomePage() {
  return (
    <div className="container">

      {/* ===== HEADER ===== */}
      <div className="header">
        <h1>Bienvenue</h1>
        <p>
          Vous êtes connecté en tant que <strong>Manager</strong>
        </p>
      </div>

      {/* ===== ACTIONS ===== */}
      <div className="actions">

        <div className="card">
          <h3>Synchroniser les données</h3>
          <p>
            Synchroniser les signalements et utilisateurs
            avec la base centrale.
          </p>
          <button className="btn">Synchroniser</button>
        </div>

        <div className="card">
          <h3>Gestion des utilisateurs</h3>
          <p>
            Ajouter, bloquer ou débloquer les utilisateurs
            de la plateforme.
          </p>
          <Link to="/blocked-users">
            <button className="btn">Gérer les utilisateurs</button>
          </Link>
        </div>

        <div className="card">
          <h3>Gestion des signalements</h3>
          <p>
            Suivre, traiter et clôturer les signalements
            routiers.
          </p>
          <Link to="/issues">
            <button className="btn">Gérer les signalements</button>
          </Link>
        </div>

      </div>

    </div>
  );
}
