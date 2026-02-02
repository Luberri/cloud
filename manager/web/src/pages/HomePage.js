import { Link } from "react-router-dom";
import { useState } from "react";
import "./HomePage.css";

export function HomePage() {
  const [syncMsg, setSyncMsg] = useState("");

  const handleSync = async () => {
    setSyncMsg("Synchronisation en cours...");
    try {
      const res = await fetch("/sync/all", { method: "POST" });
      const data = await res.json();
      setSyncMsg(`Synchronisation terminée : ${data.roadIssues} signalements et ${data.users} utilisateurs synchronisés.`);
    } catch (e) {
      setSyncMsg("Erreur de synchronisation");
    }
  };

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
          <h3>Ajouter un utilisateur</h3>
          <p>
            Créer un nouvel utilisateur sur la plateforme.
          </p>
          <Link to="/add-user">
            <button className="btn">Ajouter un utilisateur</button>
          </Link>
        </div>

        <div className="card">
          <h3>Liste de tous les utilisateurs</h3>
          <p>
            Voir tous les utilisateurs de la plateforme.
          </p>
          <Link to="/all-users">
            <button className="btn">Voir les utilisateurs</button>
          </Link>
        </div>

        <div className="card">
          <h3>Gestion des utilisateurs bloqués</h3>
          <p>
            Ajouter, bloquer ou débloquer les utilisateurs
            de la plateforme.
          </p>
          <Link to="/blocked-users">
            <button className="btn">Gérer les utilisateurs</button>
          </Link>
        </div>
        
        <div className="card">
          <h3>Synchroniser les données</h3>
          <p>
            Synchroniser les signalements et utilisateurs
            avec la base centrale.
          </p>
          <button className="btn" onClick={handleSync}>Synchroniser</button>
          {syncMsg && <p>{syncMsg}</p>}
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
