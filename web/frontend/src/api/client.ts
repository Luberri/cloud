const API_BASE = "http://localhost:8082";

export async function getJson<T>(path: string): Promise<T> {
  const res = await fetch(`${API_BASE}${path.startsWith("/") ? path : `/${path}`}`);
  if (!res.ok) {
    throw new Error(`Erreur API ${res.status}`);
  }
  return res.json();
}

export async function postJson(path: string, body: object): Promise<string> {
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

export async function putJson<T>(path: string, body: object): Promise<T> {
  const res = await fetch(`${API_BASE}${path.startsWith("/") ? path : `/${path}`}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(`Erreur API ${res.status}: ${errorText}`);
  }
  return res.json();
}
