import { useEffect, useState } from "react";
import { getJson } from "../api/client";

export function IssuesPage() {
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

      if (!res.ok) {
        throw new Error(`Erreur API ${res.status}`);
      }

      const updated = await res.json();
      setIssues((prev) => prev.map((i) => (i.id === updated.id ? updated : i)));
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
    <div style={{ padding: "2rem", fontFamily: "sans-serif" }}>
      <h1>Signalements routiers</h1>
      <table border="1" cellPadding="8">
        <thead>
          <tr>
            <th>Titre</th>
            <th>Description</th>
            <th>Surface (m²)</th>
            <th>Budget</th>
            <th>Statut</th>
            <th>Créé le</th>
            <th>Actions</th>
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
                <button onClick={() => startEdit(i)}>Modifier</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {editingIssue && (
        <div style={{ marginTop: "2rem" }}>
          <h2>Modifier le signalement</h2>
          {formError && <p style={{ color: "red" }}>Erreur : {formError}</p>}
          <form onSubmit={handleSubmit}>
            <div style={{ marginBottom: "0.5rem" }}>
              <label>
                Titre :
                <input
                  type="text"
                  name="title"
                  value={form.title}
                  onChange={handleChange}
                  style={{ marginLeft: "0.5rem", width: "300px" }}
                />
              </label>
            </div>
            <div style={{ marginBottom: "0.5rem" }}>
              <label>
                Description :
                <textarea
                  name="description"
                  value={form.description}
                  onChange={handleChange}
                  rows={3}
                  style={{ marginLeft: "0.5rem", width: "300px" }}
                />
              </label>
            </div>
            <div style={{ marginBottom: "0.5rem" }}>
              <label>
                Surface (m²) :
                <input
                  type="number"
                  step="0.01"
                  name="surfaceM2"
                  value={form.surfaceM2}
                  onChange={handleChange}
                  style={{ marginLeft: "0.5rem", width: "150px" }}
                />
              </label>
            </div>
            <div style={{ marginBottom: "0.5rem" }}>
              <label>
                Budget :
                <input
                  type="number"
                  step="0.01"
                  name="budget"
                  value={form.budget}
                  onChange={handleChange}
                  style={{ marginLeft: "0.5rem", width: "150px" }}
                />
              </label>
            </div>
            <div style={{ marginBottom: "0.5rem" }}>
              <label>
                Statut (id) :
                <input
                  type="number"
                  name="statusId"
                  value={form.statusId}
                  onChange={handleChange}
                  style={{ marginLeft: "0.5rem", width: "100px" }}
                />
              </label>
            </div>
            <button type="submit" disabled={saving}>
              {saving ? "Enregistrement..." : "Enregistrer"}
            </button>
            <button
              type="button"
              onClick={() => setEditingIssue(null)}
              style={{ marginLeft: "1rem" }}
            >
              Annuler
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
