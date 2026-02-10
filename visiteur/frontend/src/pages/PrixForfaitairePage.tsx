import { useEffect, useState } from "react";
import { getJson, putJson } from "../api/client";
import "./PrixForfaitairePage.css";

interface PrixForfaitaire {
  id: number;
  prix: number;
  updatedAt: string;
}

interface PrixForfaitairePageProps {
  onNavigate: (page: string) => void;
}

export default function PrixForfaitairePage({ onNavigate }: PrixForfaitairePageProps) {
  const [prixForfaitaire, setPrixForfaitaire] = useState<PrixForfaitaire | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [newPrix, setNewPrix] = useState("");
  const [saving, setSaving] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    loadPrix();
  }, []);

  const loadPrix = async () => {
    try {
      setLoading(true);
      const data = await getJson<PrixForfaitaire>("/api/prix-forfaitaire");
      setPrixForfaitaire(data);
      setNewPrix(data.prix.toString());
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!newPrix || isNaN(Number(newPrix)) || Number(newPrix) <= 0) {
      setError("Veuillez entrer un prix valide supérieur à 0");
      return;
    }

    try {
      setSaving(true);
      setError(null);
      const updated = await putJson<PrixForfaitaire>("/api/prix-forfaitaire", {
        prix: Number(newPrix)
      });
      setPrixForfaitaire(updated);
      setSuccessMessage("Prix mis à jour avec succès !");
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('fr-MG', {
      style: 'currency',
      currency: 'MGA',
      maximumFractionDigits: 0
    }).format(value);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="prix-page">
        <p>Chargement...</p>
      </div>
    );
  }

  return (
    <div className="prix-page">
      <div className="header-actions">
        <button className="btn" onClick={() => onNavigate('home')}>
          ← Retour à l'accueil
        </button>
      </div>

      <h1>💰 Prix Forfaitaire par m²</h1>
      <p className="subtitle">
        Définissez le prix unitaire utilisé pour calculer le budget des réparations routières.
      </p>

      {error && <div className="error-message">❌ {error}</div>}
      {successMessage && <div className="success-message">✅ {successMessage}</div>}

      {prixForfaitaire && (
        <div className="prix-card">
          <div className="prix-header">
            <h2>Prix Actuel</h2>
            <span className="last-update">
              Dernière mise à jour : {formatDate(prixForfaitaire.updatedAt)}
            </span>
          </div>

          <div className="current-prix">
            <div className="prix-display-large">
              <span className="amount">{formatCurrency(prixForfaitaire.prix)}</span>
              <span className="unit">/ m²</span>
            </div>
          </div>

          <form onSubmit={handleUpdate} className="prix-form">
            <div className="input-group">
              <label>Nouveau prix (Ariary / m²)</label>
              <input
                type="number"
                value={newPrix}
                onChange={(e) => setNewPrix(e.target.value)}
                min="0"
                step="1000"
                placeholder="Ex: 50000"
                required
              />
            </div>
            
            <button 
              type="submit"
              className="btn save-btn" 
              disabled={saving}
            >
              {saving ? "Enregistrement..." : "💾 Mettre à jour le prix"}
            </button>
          </form>
        </div>
      )}
    </div>
  );
}