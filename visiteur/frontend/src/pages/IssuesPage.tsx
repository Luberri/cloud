import { useEffect, useState } from "react";
import { getJson, putJson } from "../api/client";
import "./IssuesPage.css";

interface RoadIssue {
  id: string;
  title: string;
  description: string;
  surfaceM2: number | null;
  budget: number | null;
  statusId: number | null;
  status?: { label: string };
  reportedAt: string;
}

interface IssuesPageProps {
  onNavigate: (page: string) => void;
}

export default function IssuesPage({ onNavigate }: IssuesPageProps) {
  const [issues, setIssues] = useState<RoadIssue[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<number | null>(null);

  const [editingIssue, setEditingIssue] = useState<RoadIssue | null>(null);

  const [form, setForm] = useState({
    title: "",
    description: "",
    surfaceM2: "",
    budget: "",
    statusId: "",
  });

  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    getJson<RoadIssue[]>("/issues")
      .then(setIssues)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const startEdit = (issue: RoadIssue) => {
    setEditingIssue(issue);
    setForm({
      title: issue.title || "",
      description: issue.description || "",
      surfaceM2: issue.surfaceM2?.toString() ?? "",
      budget: issue.budget?.toString() ?? "",
      statusId: issue.statusId?.toString() ?? "",
    });
    setFormError(null);
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
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

      const updated = await putJson<RoadIssue>(`/issues/${editingIssue.id}`, payload);

      setIssues((prev) =>
        prev.map((i) => (i.id === updated.id ? updated : i))
      );

      setEditingIssue(null);
    } catch (err: any) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <p>Chargement...</p>;
  if (error) return <p>Erreur : {error}</p>;

  const filteredIssues = statusFilter === null 
    ? issues 
    : issues.filter(i => i.statusId === statusFilter);

  return (
    <div className="issues-page">
      <div style={{ marginBottom: "1rem" }}>
        <button
          className="btn"
          onClick={() => onNavigate('home')}
        >
          Retour à l'accueil
        </button>
        <button
          className="btn"
          onClick={() => onNavigate('statistics')}
        >
          Voir les statistiques
        </button>
      </div>

      <h1>Signalements routiers</h1>

      {!issues.length ? (
        <p>Aucun signalement pour le moment.</p>
      ) : (
        <>
          <div className="filter-container">
            <button
              className={`filter-btn ${statusFilter === null ? 'active' : ''}`}
              onClick={() => setStatusFilter(null)}
            >
              Tous ({issues.length})
            </button>
            <button
              className={`filter-btn ${statusFilter === 1 ? 'active' : ''}`}
              onClick={() => setStatusFilter(1)}
            >
              Nouveau ({issues.filter(i => i.statusId === 1).length})
            </button>
            <button
              className={`filter-btn ${statusFilter === 2 ? 'active' : ''}`}
              onClick={() => setStatusFilter(2)}
            >
              En cours ({issues.filter(i => i.statusId === 2).length})
            </button>
            <button
              className={`filter-btn ${statusFilter === 3 ? 'active' : ''}`}
              onClick={() => setStatusFilter(3)}
            >
              Terminé ({issues.filter(i => i.statusId === 3).length})
            </button>
          </div>

          <table className="issues-table">
            <thead>
              <tr>
                <th>Titre</th>
                <th>Description</th>
                <th>Surface</th>
                <th>Budget</th>
                <th>Avancement</th>
                <th>Date</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredIssues.map((i) => (
                <tr key={i.id}>
                  <td>{i.title}</td>
                  <td>{i.description}</td>
                  <td>{i.surfaceM2}</td>
                  <td>{i.budget}</td>
                  <td>
                    <div className="progress-bar-container">
                      <div
                        className="progress-bar-fill"
                        style={{ width: `${i.statusId === 3 ? 100 : i.statusId === 2 ? 50 : 0}%` }}
                      />
                    </div>
                    <span className="progress-label">{i.statusId === 3 ? 100 : i.statusId === 2 ? 50 : 0}%</span>
                  </td>
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
        </>
      )}

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
