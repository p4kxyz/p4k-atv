const API_BASE = "/v1/search-history";

export default {
  async fetch(request, env) {
    try {
      if (request.method === "OPTIONS") {
        return new Response(null, { status: 204, headers: corsHeaders(env) });
      }

      const url = new URL(request.url);
      if (url.pathname === "/health") {
        return json({ ok: true, service: "search-history-worker" }, 200, env);
      }

      if (!url.pathname.startsWith(API_BASE)) {
        return json({ error: "Not found" }, 404, env);
      }

      const auth = await authenticate(request, env);
      if (!auth.ok) {
        return json({ error: auth.error }, 401, env);
      }

      const userId = auth.user.userId;
      const key = historyKey(userId);

      if (request.method === "GET" && url.pathname === API_BASE) {
        const items = await readHistory(env, key);
        return json({ userId, items, count: items.length }, 200, env);
      }

      if (request.method === "POST" && url.pathname === API_BASE) {
        const payload = await safeJson(request);
        const query = normalizeQuery(payload?.query);
        if (!query) {
          return json({ error: "query is required" }, 400, env);
        }

        const maxItems = toPositiveInt(env.MAX_HISTORY_ITEMS, 10);
        const items = await readHistory(env, key);
        const lower = query.toLowerCase();

        const deduped = items.filter((it) => (it.query || "").toLowerCase() !== lower);
        deduped.unshift({ id: crypto.randomUUID(), query, timestamp: Date.now() });
        const trimmed = deduped.slice(0, maxItems);

        await env.SEARCH_HISTORY_KV.put(key, JSON.stringify(trimmed));
        return json({ ok: true, items: trimmed, count: trimmed.length }, 200, env);
      }

      if (request.method === "DELETE" && url.pathname === API_BASE) {
        await env.SEARCH_HISTORY_KV.put(key, JSON.stringify([]));
        return json({ ok: true, cleared: true }, 200, env);
      }

      if (request.method === "DELETE" && url.pathname.startsWith(`${API_BASE}/`)) {
        const entryId = decodeURIComponent(url.pathname.slice((`${API_BASE}/`).length));
        if (!entryId) {
          return json({ error: "entryId is required" }, 400, env);
        }

        const items = await readHistory(env, key);
        const next = items.filter((it) => it.id !== entryId);
        await env.SEARCH_HISTORY_KV.put(key, JSON.stringify(next));

        return json({ ok: true, items: next, count: next.length }, 200, env);
      }

      return json({ error: "Method not allowed" }, 405, env);
    } catch (error) {
      return json({ error: "Internal error", detail: String(error?.message || error) }, 500, env);
    }
  },
};

async function readHistory(env, key) {
  const raw = await env.SEARCH_HISTORY_KV.get(key);
  if (!raw) return [];

  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function historyKey(userId) {
  return `search_history:${userId}`;
}

function toPositiveInt(value, fallback) {
  const n = Number.parseInt(value, 10);
  return Number.isFinite(n) && n > 0 ? n : fallback;
}

function normalizeQuery(query) {
  if (typeof query !== "string") return "";
  return query.trim().replace(/\s+/g, " ").slice(0, 120);
}

function json(data, status, env) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      ...corsHeaders(env),
    },
  });
}

function corsHeaders(env) {
  const origin = env.ALLOWED_ORIGIN || "*";
  return {
    "access-control-allow-origin": origin,
    "access-control-allow-methods": "GET,POST,DELETE,OPTIONS",
    "access-control-allow-headers": "authorization,content-type",
  };
}

async function safeJson(request) {
  try {
    return await request.json();
  } catch {
    return {};
  }
}

async function authenticate(request, env) {
  const authHeader = request.headers.get("authorization") || "";
  const [scheme, token] = authHeader.split(" ");

  if (scheme !== "Bearer" || !token) {
    return { ok: false, error: "Missing bearer token" };
  }

  if (!env.JWT_SECRET) {
    return { ok: false, error: "JWT_SECRET is not configured" };
  }

  const payload = await verifyJwtHs256(token, env.JWT_SECRET);
  if (!payload) {
    return { ok: false, error: "Invalid token" };
  }

  const exp = Number(payload.exp || 0);
  if (exp > 0 && Math.floor(Date.now() / 1000) >= exp) {
    return { ok: false, error: "Token expired" };
  }

  const userId = String(payload.sub || payload.userId || "").trim();
  if (!userId) {
    return { ok: false, error: "Token missing user id" };
  }

  return {
    ok: true,
    user: {
      userId,
      email: payload.email || null,
    },
  };
}

async function verifyJwtHs256(token, secret) {
  const parts = token.split(".");
  if (parts.length !== 3) return null;

  const [headerB64, payloadB64, signatureB64] = parts;
  let header;
  let payload;

  try {
    header = JSON.parse(base64UrlDecodeToString(headerB64));
    payload = JSON.parse(base64UrlDecodeToString(payloadB64));
  } catch {
    return null;
  }

  if (header?.alg !== "HS256") return null;

  const data = `${headerB64}.${payloadB64}`;
  const expectedSig = await hmacSha256Base64Url(secret, data);
  if (!timingSafeEqual(expectedSig, signatureB64)) {
    return null;
  }

  return payload;
}

async function hmacSha256Base64Url(secret, data) {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(data));
  return base64UrlEncode(new Uint8Array(sig));
}

function base64UrlDecodeToString(input) {
  const base64 = input.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((input.length + 3) % 4);
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return new TextDecoder().decode(bytes);
}

function base64UrlEncode(bytes) {
  let binary = "";
  for (let i = 0; i < bytes.length; i += 1) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function timingSafeEqual(a, b) {
  if (typeof a !== "string" || typeof b !== "string") return false;
  if (a.length !== b.length) return false;

  let diff = 0;
  for (let i = 0; i < a.length; i += 1) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return diff === 0;
}
