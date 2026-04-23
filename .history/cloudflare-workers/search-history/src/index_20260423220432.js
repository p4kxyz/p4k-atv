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

      const email = auth.user.email;
      const key = historyKey(email);

      if (request.method === "GET" && url.pathname === API_BASE) {
        const items = await readHistory(env, key);
        return json({ email, items, count: items.length }, 200, env);
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
  return `search_history:${normalizeEmail(userId)}`;
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
  const emailHeader = request.headers.get("x-user-email") || "";
  const keyHeader = request.headers.get("x-list-key") || "";

  const body = await safeJson(request.clone());
  const emailBody = typeof body?.email === "string" ? body.email : "";
  const keyBody = typeof body?.listKey === "string" ? body.listKey : "";

  const email = normalizeEmail(emailHeader || emailBody);
  const listKey = (keyHeader || keyBody || "").trim();

  if (!email) {
    return { ok: false, error: "Missing user email" };
  }

  if (!env.APP_LIST_KEY) {
    return { ok: false, error: "APP_LIST_KEY is not configured" };
  }

  if (!listKey) {
    return { ok: false, error: "Missing list key" };
  }

  if (!timingSafeEqual(listKey, String(env.APP_LIST_KEY))) {
    return { ok: false, error: "Invalid list key" };
  }

  return {
    ok: true,
    user: {
      email,
    },
  };
}

function normalizeEmail(email) {
  if (typeof email !== "string") return "";
  return email.trim().toLowerCase();
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
