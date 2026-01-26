import { useEffect, useState } from "react";
import { getJson } from "../api/client";

export function BlockedUsersPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [unlockingId, setUnlockingId] = useState(null);

  useEffect(() => {
    getJson("/auth/blocked")
      .then(setUsers)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const handleUnlock = async (id) => {
    try {
      setUnlockingId(id);
      const res = await fetch(`/auth/unlock/id/${id}`, {
        method: "POST",
      });
      if (!res.ok) {
        throw new Error(`Erreur API ${res.status}`);
      }
      // On retire l'utilisateur débloqué de la liste locale
      setUsers((prev) => prev.filter((u) => u.id !== id));
    } catch (e) {
      setError(e.message);
    } finally {
      setUnlockingId(null);
    }
  };

  if (loading) return <p>Chargement...</p>;
  if (error) return <p>Erreur : {error}</p>;
  if (!users.length) return <p>Aucun utilisateur bloqué.</p>;

  return (
    <div style={{ padding: "2rem", fontFamily: "sans-serif" }}>
      <h1>Utilisateurs bloqués</h1>
      <table border="1" cellPadding="8">
        <thead>
          <tr>
            <th>Email</th>
            <th>Nom</th>
            <th>Tentatives échouées</th>
            <th>Bloqué jusqu&apos;au</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td>{u.email}</td>
              <td>{u.fullName}</td>
              <td>{u.failedLoginAttempts ?? 0}</td>
              <td>{u.lockedUntil}</td>
              <td>
                <button
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
    </div>
  );
}
