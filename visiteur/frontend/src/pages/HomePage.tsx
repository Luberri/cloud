import "./HomePage.css";

interface HomePageProps {
  onNavigate: (page: string) => void;
  onLogout: () => void;
}

export default function HomePage({ onNavigate, onLogout }: HomePageProps) {
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
          <h3>Ajouter un utilisateur</h3>
          <p>
            Créer un nouvel utilisateur sur la plateforme.
          </p>
          <button className="btn" onClick={() => onNavigate('add-user')}>
            Ajouter un utilisateur
          </button>
        </div>

        <div className="card">
          <h3>Liste de tous les utilisateurs</h3>
          <p>
            Voir tous les utilisateurs de la plateforme.
          </p>
          <button className="btn" onClick={() => onNavigate('all-users')}>
            Voir les utilisateurs
          </button>
        </div>

        <div className="card">
          <h3>Gestion des utilisateurs bloqués</h3>
          <p>
            Ajouter, bloquer ou débloquer les utilisateurs
            de la plateforme.
          </p>
          <button className="btn" onClick={() => onNavigate('blocked-users')}>
            Gérer les utilisateurs
          </button>
        </div>

        <div className="card">
          <h3>Gestion des signalements</h3>
          <p>
            Suivre, traiter et clôturer les signalements
            routiers.
          </p>
          <button className="btn" onClick={() => onNavigate('issues')}>
            Gérer les signalements
          </button>
        </div>

        <div className="card">
          <h3>Voir la carte</h3>
          <p>
            Afficher la carte des signalements routiers.
          </p>
          <button className="btn" onClick={() => onNavigate('map')}>
            Voir la carte
          </button>
        </div>

        <div className="card">
          <h3>Résumé visiteur</h3>
          <p>
            Voir le résumé public des signalements.
          </p>
          <button className="btn" onClick={() => onNavigate('summary')}>
            Voir le résumé
          </button>
        </div>

      </div>

    </div>
  );
}
