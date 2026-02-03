import { useEffect, useState } from "react";
import { getJson } from "../api/client";
import { useNavigate } from "react-router-dom"; // Ajoute ceci
import "./BlockedUsersPage.css"; // Réutilise le style existant

export function AllUsersPage() {
    const navigate = useNavigate(); // Ajoute ceci
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    getJson("/users")
      .then(setUsers)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const getRoleLabel = (u) => {
    if (typeof u.role === "string") return u.role;
    if (u.role?.label) return u.role.label;
    if (u.role?.name) return u.role.name;
    return "Utilisateur";
  };

  const getRoleClass = (u) => {
    const role = getRoleLabel(u).toLowerCase();
    return role.includes("manager") ? "manager" : "user";
  };

  if (loading) return <p>Chargement...</p>;
  if (error) return <p>Erreur : {error}</p>;

  return (
    <div className="container">
    <button
        className="btn"
        style={{ marginBottom: "1rem" }}
        onClick={() => navigate("/accueil")}
      >
        Retour à l'accueil
      </button>
      <button
        className="btn"
        style={{ marginBottom: "1rem" }}
        onClick={() => navigate("/blocked-users")}
      >
        Bloqué
      </button>
      <div className="table-container">
        <h2>Tous les utilisateurs</h2>
        {!users.length ? (
          <p>Aucun utilisateur.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nom</th>
                <th>Email</th>
                <th>Rôle</th>
                <th>Statut</th>
                <th>Bloqué jusqu’au</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.fullName}</td>
                  <td>{u.email}</td>
                  <td>
                    <span className={`role ${getRoleClass(u)}`}>
                      {getRoleLabel(u)}
                    </span>
                  </td>
                  <td>{u.isActive ? "Actif" : "Inactif"}</td>
                  <td>{u.lockedUntil || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}