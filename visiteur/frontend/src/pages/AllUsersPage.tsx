import { useEffect, useState } from "react";
import { getJson } from "../api/client";
import "./BlockedUsersPage.css";

interface User {
  id: string;
  fullName: string;
  email: string;
  role: { label?: string; name?: string } | string;
  lockedUntil: string | null;
  isActive: boolean;
}

interface AllUsersPageProps {
  onNavigate: (page: string) => void;
}

export default function AllUsersPage({ onNavigate }: AllUsersPageProps) {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<boolean | null>(null);

  useEffect(() => {
    getJson<User[]>("/users")
      .then(setUsers)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const getRoleLabel = (u: User): string => {
    if (typeof u.role === "string") return u.role;
    if (u.role?.label) return u.role.label;
    if (u.role?.name) return u.role.name;
    return "Utilisateur";
  };

  const getRoleClass = (u: User): string => {
    const role = getRoleLabel(u).toLowerCase();
    return role.includes("manager") ? "manager" : "user";
  };

  if (loading) return <p>Chargement...</p>;
  if (error) return <p>Erreur : {error}</p>;

  const filteredUsers = statusFilter === null
    ? users
    : users.filter(u => u.isActive === statusFilter);

  return (
    <div className="container">
      <button
        className="btn"
        style={{ marginBottom: "1rem" }}
        onClick={() => onNavigate('home')}
      >
        Retour à l'accueil
      </button>
      <button
        className="btn"
        style={{ marginBottom: "1rem" }}
        onClick={() => onNavigate('blocked-users')}
      >
        Bloqués
      </button>

      <div className="filter-container" style={{ marginBottom: "1rem" }}>
        <button
          className={`filter-btn ${statusFilter === null ? 'active' : ''}`}
          onClick={() => setStatusFilter(null)}
        >
          Tous ({users.length})
        </button>
        <button
          className={`filter-btn ${statusFilter === true ? 'active' : ''}`}
          onClick={() => setStatusFilter(true)}
        >
          Actif ({users.filter(u => u.isActive).length})
        </button>
        <button
          className={`filter-btn ${statusFilter === false ? 'active' : ''}`}
          onClick={() => setStatusFilter(false)}
        >
          Inactif ({users.filter(u => !u.isActive).length})
        </button>
      </div>

      <div className="table-container">
        <h2>Tous les utilisateurs</h2>
        {!filteredUsers.length ? (
          <p>Aucun utilisateur.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nom</th>
                <th>Email</th>
                <th>Rôle</th>
                <th>Statut</th>
                {/* <th>Bloqué jusqu'au</th> */}
              </tr>
            </thead>
            <tbody>
              {filteredUsers.map((u) => (
                <tr key={u.id}>
                  <td>{u.fullName}</td>
                  <td>{u.email}</td>
                  <td>
                    <span className={`role ${getRoleClass(u)}`}>
                      {getRoleLabel(u)}
                    </span>
                  </td>
                  <td>{u.isActive ? "Actif" : "Inactif"}</td>
                  {/* <td>{u.lockedUntil || "-"}</td> */}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
