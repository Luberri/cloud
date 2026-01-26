import { Link } from "react-router-dom";

export function HomePage() {
  return (
    <div style={{ padding: "2rem", fontFamily: "sans-serif" }}>
      <h1>Accueil Web</h1>
      <p>Projet Cloud – partie Web React.</p>
      <ul>
        <li>
          <Link to="/blocked-users">Liste des utilisateurs bloqués</Link>
        </li>
        <li>
          <Link to="/issues">Liste des signalements routiers</Link>
        </li>
      </ul>
    </div>
  );
}
