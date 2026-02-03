import { useEffect, useState } from "react";
import { getJson } from "../api/client";
import { useNavigate } from "react-router-dom"; // Ajoute ceci
import "./BlockedUsersPage.css";

export function BlockedUsersPage() {
  const navigate = useNavigate(); // Ajoute ceci
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [unlockingId, setUnlockingId] = useState(null);

  const [newUser, setNewUser] = useState({
    fullName: "",
    email: "",
    password: "",
  });

  useEffect(() => {
    getJson("/auth/blocked")
      .then(setUsers)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const handleUnlock = async (id) => {
    try {
      setUnlockingId(id);
      const res = await fetch(`/auth/unlock/id/${id}`, { method: "POST" });
      if (!res.ok) throw new Error(`Erreur API ${res.status}`);

      setUsers((prev) => prev.filter((u) => u.id !== id));
    } catch (e) {
      setError(e.message);
    } finally {
      setUnlockingId(null);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setNewUser((prev) => ({ ...prev, [name]: value }));
  };

  const handleAddUser = (e) => {
    e.preventDefault();
    alert("Branche l’API ici quand tu veux 👍");
    setNewUser({ fullName: "", email: "", password: "" });
  };

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
        Tout les utilisateurs
      </button>
      {/* TABLE */}
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
                <th>Bloqué jusqu’au</th>
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
