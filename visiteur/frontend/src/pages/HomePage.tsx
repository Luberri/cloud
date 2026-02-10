import "./HomePage.css";
import { useState } from "react";

interface HomePageProps {
  onNavigate: (page: string) => void;
  onLogout: () => void;
}

export default function HomePage({ onNavigate, onLogout }: HomePageProps) {
  const [syncLoading, setSyncLoading] = useState(false);
  const [syncResult, setSyncResult] = useState<any>(null);
  const [syncError, setSyncError] = useState<string | null>(null);

  // Utilise VITE_API_URL si défini, sinon localhost:8082
  const API_BASE_URL = (import.meta as any).env?.VITE_API_URL ?? "http://localhost:8082";

  const handleSync = async () => {
    setSyncLoading(true);
    setSyncError(null);
    setSyncResult(null);

    try {
      const res = await fetch(`${API_BASE_URL}/sync/all`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
      });

      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || `HTTP ${res.status}`);
      }

      const data = await res.json();
      setSyncResult(data);
    } catch (e: any) {
      setSyncError(e?.message ?? "Erreur inconnue");
    } finally {
      setSyncLoading(false);
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
        <button className="btn logout-btn" onClick={onLogout}>
          Déconnexion
        </button>
      </div>

      {/* ===== ACTIONS ===== */}
      <div className="actions">
        <div className="card">
          <h3>Synchroniser</h3>
          <p>Synchroniser la base locale avec Firebase (users + signalements).</p>

          <button className="btn" onClick={handleSync} disabled={syncLoading}>
            {syncLoading ? "Synchronisation..." : "Synchroniser"}
          </button>

          {syncError && (
            <p style={{ marginTop: 10, color: "crimson" }}>
              Erreur: {syncError}
            </p>
          )}

          {syncResult && (
            <pre style={{ marginTop: 10, whiteSpace: "pre-wrap" }}>
              {JSON.stringify(syncResult, null, 2)}
            </pre>
          )}
        </div>

        <div className="card">
          <h3>Ajouter un utilisateur</h3>
          <p>Créer un nouvel utilisateur sur la plateforme.</p>
          <button className="btn" onClick={() => onNavigate("add-user")}>
            Ajouter un utilisateur
          </button>
        </div>

        <div className="card">
          <h3>Liste de tous les utilisateurs</h3>
          <p>Voir tous les utilisateurs de la plateforme.</p>
          <button className="btn" onClick={() => onNavigate("all-users")}>
            Voir les utilisateurs
          </button>
        </div>

        <div className="card">
          <h3>Gestion des utilisateurs bloqués</h3>
          <p>
            Ajouter, bloquer ou débloquer les utilisateurs
            de la plateforme.
          </p>
          <button className="btn" onClick={() => onNavigate("blocked-users")}>
            Gérer les utilisateurs
          </button>
        </div>

        <div className="card">
          <h3>Gestion des signalements</h3>
          <p>
            Suivre, traiter et clôturer les signalements
            routiers.
          </p>
          <button className="btn" onClick={() => onNavigate("issues")}>
            Gérer les signalements
          </button>
        </div>

        <div className="card">
          <h3>Statistiques des travaux</h3>
          <p>Tableau de statistiques : avancement et délais de traitement.</p>
          <button className="btn" onClick={() => onNavigate("statistics")}>
            Voir les statistiques
          </button>
        </div>

        <div className="card">
          <h3>Voir la carte</h3>
          <p>Afficher la carte des signalements routiers.</p>
          <button className="btn" onClick={() => onNavigate("map")}>
            Voir la carte
          </button>
        </div>

        <div className="card">
          <h3>Résumé visiteur</h3>
          <p>Voir le résumé public des signalements.</p>
          <button className="btn" onClick={() => onNavigate("summary")}>
            Voir le résumé
          </button>
        </div>
      </div>
    </div>
  );
}
