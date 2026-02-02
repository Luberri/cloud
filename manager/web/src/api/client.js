//const API_BASE = ""; // on utilise le proxy CRA vers http://localhost:8080

export async function getJson(path) {
  const res = await fetch(path.startsWith("/") ? path : `/${path}`);
  if (!res.ok) {
    throw new Error(`Erreur API ${res.status}`);
  }
  return res.json();
}
