const API_BASE = "http://localhost:8082";

export async function getJson(path) {
  const res = await fetch(`${API_BASE}${path.startsWith("/") ? path : `/${path}`}`);
  if (!res.ok) {
    throw new Error(`Erreur API ${res.status}`);
  }
  return res.json();
}

export async function postJson(path, body) {
  const res = await fetch(`${API_BASE}${path.startsWith("/") ? path : `/${path}`}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(`Erreur API ${res.status}: ${errorText}`);
  }
  return res.text();
}
