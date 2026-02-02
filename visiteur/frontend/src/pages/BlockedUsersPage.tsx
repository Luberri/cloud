import { useEffect, useState } from "react";
import { getJson } from "../api/client";
import "./BlockedUsersPage.css";

interface User {
  id: string;
  fullName: string;
  email: string;
  role: { label?: string; name?: string } | string;
  lockedUntil: string;
  isActive: boolean;
}

interface BlockedUsersPageProps {
  onNavigate: (page: string) => void;
}

export default function BlockedUsersPage({ onNavigate }: BlockedUsersPageProps) {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [unlockingId, setUnlockingId] = useState<string | null>(null);

  useEffect(() => {
    getJson<User[]>("/auth/blocked")
      .then(setUsers)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const handleUnlock = async (id: string) => {
    try {
      setUnlockingId(id);
      const res = await fetch(`http://localhost:8082/auth/unlock/id/${id}`, { method: "POST" });
      if (!res.ok) throw new Error(`Erreur API ${res.status}`);

      setUsers((prev) => prev.filter((u) => u.id !== id));
    } catch (e: any) {
      setError(e.message);
    } finally {
      setUnlockingId(null);
    }
  };

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
        onClick={() => onNavigate('all-users')}
      >
        Tous les utilisateurs
      </button>

      <div className="table-container">
        <h2>Utilisateurs bloqués</h2>

        {!users.length ? (
          <p>Aucun utilisateur bloqué.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nom</th>
                <th>Email</th>
                <th>Rôle</th>
                <th>Bloqué jusqu'au</th>
                <th>Action</th>
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
                  <td>{u.lockedUntil}</td>
                  <td>
                    <button
                      className="btn btn-unlock"
                      onClick={() => handleUnlock(u.id)}
                      disabled={unlockingId === u.id}
                    >
                      {unlockingId === u.id ? "Déblocage..." : "Débloquer"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

    </div>
  );
}
