import { useEffect, useState } from "react";
import { getJson } from "../api/client";
import { useNavigate } from "react-router-dom"; // Ajoute ceci

import "./IssuesPage.css";

export function IssuesPage() {
    
  const navigate = useNavigate(); // Ajoute ceci
  const [issues, setIssues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [editingIssue, setEditingIssue] = useState(null);

  const [form, setForm] = useState({
    title: "",
    description: "",
    surfaceM2: "",
    budget: "",
    statusId: "",
  });

  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState(null);

  useEffect(() => {
    getJson("/issues")
      .then(setIssues)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const startEdit = (issue) => {
    setEditingIssue(issue);
    setForm({
      title: issue.title || "",
      description: issue.description || "",
      surfaceM2: issue.surfaceM2 ?? "",
      budget: issue.budget ?? "",
      statusId: issue.statusId ?? "",
    });
    setFormError(null);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!editingIssue) return;

    try {
      setSaving(true);
      setFormError(null);

      const payload = {
        title: form.title,
        description: form.description,
        surfaceM2: form.surfaceM2 === "" ? null : Number(form.surfaceM2),
        budget: form.budget === "" ? null : Number(form.budget),
        statusId: form.statusId === "" ? null : Number(form.statusId),
      };

      const res = await fetch(`/issues/${editingIssue.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!res.ok) throw new Error(`Erreur API ${res.status}`);

      const updated = await res.json();

      setIssues((prev) =>
        prev.map((i) => (i.id === updated.id ? updated : i))
      );

      setEditingIssue(null);
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <p>Chargement...</p>;
  if (error) return <p>Erreur : {error}</p>;
  if (!issues.length) return <p>Aucun signalement.</p>;

  return (
    <div className="issues-page">
      <button
        className="btn"
        style={{ marginBottom: "1rem" }}
        onClick={() => navigate("/accueil")}
      >
        Retour à l'accueil
      </button>

      <h1>Signalements routiers</h1>

      <table className="issues-table">
        <thead>
          <tr>
            <th>Titre</th>
            <th>Description</th>
            <th>Surface</th>
            <th>Budget</th>
            <th>Statut</th>
            <th>Date</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {issues.map((i) => (
            <tr key={i.id}>
              <td>{i.title}</td>
              <td>{i.description}</td>
              <td>{i.surfaceM2}</td>
              <td>{i.budget}</td>
              <td>{i.status?.label || i.statusId}</td>
              <td>{i.reportedAt}</td>
              <td>
                <button className="small-btn" onClick={() => startEdit(i)}>
                  Modifier
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {editingIssue && (
        <div className="form-wrapper">

          <div className="form-container">
            <h2>Modifier le signalement</h2>

            {formError && <p className="error">Erreur : {formError}</p>}

            <form onSubmit={handleSubmit}>
              <label>Titre</label>
              <input
                type="text"
                name="title"
                value={form.title}
                onChange={handleChange}
              />

              <label>Description</label>
              <input
                type="text"
                name="description"
                value={form.description}
                onChange={handleChange}
              />

              <label>Surface (m²)</label>
              <input
                type="number"
                step="0.01"
                name="surfaceM2"
                value={form.surfaceM2}
                onChange={handleChange}
              />

              <label>Budget</label>
              <input
                type="number"
                step="0.01"
                name="budget"
                value={form.budget}
                onChange={handleChange}
              />

              <label>Statut</label>
              <select
                name="statusId"
                value={form.statusId}
                onChange={handleChange}
              >
                <option value="">-- Choisir --</option>
                <option value="1">Nouveau</option>
                <option value="2">En cours</option>
                <option value="3">Terminé</option>
              </select>

              <button type="submit" disabled={saving}>
                {saving ? "Enregistrement..." : "Enregistrer les modifications"}
              </button>

              <button
                type="button"
                className="cancel-btn"
                onClick={() => setEditingIssue(null)}
              >
                Annuler
              </button>
            </form>
          </div>

        </div>
      )}

    </div>
  );
}
