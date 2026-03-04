const $ = (id) => document.getElementById(id);

const state = {
  lastQueryLogId: null,
  lastRequestId: null,
  docs: [],
  lastProof: null,
  selectedDocIds: new Set(),
  healthPollTimer: null,
  healthPollAbort: null,
};

const views = {
  upload: { title: "Upload", sub: "Upload PDF/TXT/MD -> chunk -> store -> ready for RAG." },
  docs: { title: "Documents", sub: "Search, delete, inspect chunks." },
  ask: { title: "Ask (RAG)", sub: "Evidence-only answers: citations or \"I don't know.\"" },
  eval: { title: "Evaluation", sub: "Run eval cases and inspect recent runs." },
  audit: { title: "Audit", sub: "Review request trace and governance logs." },
  feedback: { title: "Feedback", sub: "See recent feedback events for improvement." },
  settings: { title: "Settings", sub: "Theme + quick info." },
};

function toast(msg, type = "info") {
  const t = $("toast");
  t.classList.remove("hidden");
  t.textContent = msg;
  t.style.borderColor =
    type === "good" ? "rgba(61,220,151,.45)" :
    type === "bad" ? "rgba(255,107,107,.45)" :
    "rgba(110,168,254,.28)";
  setTimeout(() => t.classList.add("hidden"), 2200);
}

function setLastRequestId(id) {
  if (!id) return;
  state.lastRequestId = id;
  const el = $("lastRequestId");
  if (el) {
    el.textContent = `Last Request ID: ${id}`;
    el.title = id;
  }
}

async function api(path, opts = {}) {
  const res = await fetch(path, opts);
  const requestId = res.headers.get("X-Request-Id") || "";
  setLastRequestId(requestId);

  const text = await res.text().catch(() => "");
  let data = text;
  try {
    if (text) {
      data = JSON.parse(text);
    }
  } catch (_) {
    data = text;
  }

  if (!res.ok) {
    throw new Error(`${res.status} ${res.statusText} ${typeof data === "string" ? data : JSON.stringify(data)}`.trim());
  }

  return { data, requestId };
}

async function refreshHealth() {
  const badge = $("healthBadge");
  if (state.healthPollAbort) {
    state.healthPollAbort.abort();
  }
  const controller = new AbortController();
  state.healthPollAbort = controller;
  try {
    const [live, ready] = await Promise.all([
      api("/actuator/health/liveness", { signal: controller.signal }),
      api("/actuator/health/readiness", { signal: controller.signal }),
    ]);
    const liveUp = live.data && live.data.status === "UP";
    const readyUp = ready.data && ready.data.status === "UP";

    if (liveUp && readyUp) {
      badge.textContent = "Healthy";
      badge.className = "badge badge-good";
      return { status: "Healthy", liveness: live.data, readiness: ready.data };
    }
    if (liveUp && !readyUp) {
      badge.textContent = "Degraded";
      badge.className = "badge badge-warn";
      return { status: "Degraded", liveness: live.data, readiness: ready.data };
    }
    badge.textContent = "Down";
    badge.className = "badge badge-bad";
    return { status: "Down", liveness: live.data, readiness: ready.data };
  } catch (_) {
    try {
      const [liveLegacy, readyLegacy] = await Promise.all([
        api("/liveness", { signal: controller.signal }),
        api("/readiness", { signal: controller.signal }),
      ]);
      const liveUp = liveLegacy.data && liveLegacy.data.status === "UP";
      const readyUp = readyLegacy.data && readyLegacy.data.status === "UP";
      if (liveUp && readyUp) {
        badge.textContent = "Healthy";
        badge.className = "badge badge-good";
        return { status: "Healthy", liveness: liveLegacy.data, readiness: readyLegacy.data };
      }
      badge.textContent = liveUp ? "Degraded" : "Down";
      badge.className = liveUp ? "badge badge-warn" : "badge badge-bad";
      return { status: liveUp ? "Degraded" : "Down", liveness: liveLegacy.data, readiness: readyLegacy.data };
    } catch (_) {
      badge.textContent = "Down";
      badge.className = "badge badge-bad";
      return { status: "Down" };
    } finally {
      state.healthPollAbort = null;
    }
  } finally {
    if (state.healthPollAbort === controller) {
      state.healthPollAbort = null;
    }
  }
}

function startHealthPolling() {
  if (state.healthPollTimer) {
    return;
  }
  const tick = async () => {
    if (document.visibilityState !== "visible") {
      return;
    }
    await refreshHealth().catch(() => {});
  };
  tick();
  state.healthPollTimer = setInterval(tick, 5000);
}

function stopHealthPolling() {
  if (state.healthPollTimer) {
    clearInterval(state.healthPollTimer);
    state.healthPollTimer = null;
  }
  if (state.healthPollAbort) {
    state.healthPollAbort.abort();
    state.healthPollAbort = null;
  }
}

function setTheme(theme) {
  document.documentElement.setAttribute("data-theme", theme);
  localStorage.setItem("rag_theme", theme);
  $("themeLabel").textContent = theme;
}

function toggleTheme() {
  const cur = document.documentElement.getAttribute("data-theme") || "dark";
  setTheme(cur === "dark" ? "light" : "dark");
}

function switchView(name) {
  Object.keys(views).forEach((v) => {
    const el = $("view-" + v);
    if (!el) return;
    el.classList.toggle("hidden", v !== name);
  });

  document.querySelectorAll(".nav-item").forEach((b) => {
    b.classList.toggle("active", b.getAttribute("data-view") === name);
  });

  $("pageTitle").textContent = views[name].title;
  $("pageSub").textContent = views[name].sub;
  localStorage.setItem("rag_view", name);
}

function setUploadProgress(pct) {
  const wrap = $("uploadProgress");
  const bar = $("uploadBar");
  wrap.classList.remove("hidden");
  bar.style.width = `${Math.max(5, Math.min(100, pct))}%`;
}

function clearUploadProgress() {
  $("uploadProgress").classList.add("hidden");
  $("uploadBar").style.width = "0%";
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, (m) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;" }[m]));
}

function renderDocFilter() {
  const list = $("docFilterList");
  if (!list) return;
  list.innerHTML = "";
  if (!state.docs.length) {
    list.innerHTML = '<span class="muted small">Load documents to select filters.</span>';
    return;
  }

  for (const doc of state.docs) {
    const label = document.createElement("label");
    label.className = "chip";
    const checked = state.selectedDocIds.has(doc.id) ? "checked" : "";
    label.innerHTML = `<input type="checkbox" data-doc-id="${doc.id}" ${checked}/> ${escapeHtml(doc.name || `doc-${doc.id}`)}`;
    list.appendChild(label);
  }

  list.querySelectorAll("input[type=checkbox]").forEach((cb) => {
    cb.addEventListener("change", () => {
      const id = Number(cb.getAttribute("data-doc-id"));
      if (cb.checked) state.selectedDocIds.add(id);
      else state.selectedDocIds.delete(id);
    });
  });
}

async function loadDocs() {
  const res = await api("/v1/documents");
  state.docs = Array.isArray(res.data) ? res.data : [];
  renderDocs();
  renderDocFilter();
  return res;
}

function renderDocs() {
  const q = ($("docSearch").value || "").toLowerCase().trim();
  const rows = (state.docs || []).filter((d) => {
    const name = (d.name || "").toLowerCase();
    return !q || name.includes(q) || String(d.id).includes(q);
  });

  const tb = $("docsTbody");
  tb.innerHTML = "";
  if (!rows.length) {
    tb.innerHTML = `<tr><td colspan="5" class="muted">No documents found.</td></tr>`;
    return;
  }

  for (const d of rows) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${d.id ?? ""}</td>
      <td>${escapeHtml(d.name ?? "")}</td>
      <td>${escapeHtml(d.mimeType ?? "")}</td>
      <td>${escapeHtml((d.createdAt ?? "").toString())}</td>
      <td class="right">
        <button class="btn btn-ghost" data-act="chunks" data-id="${d.id}">Chunks</button>
        <button class="btn btn-ghost" data-act="delete" data-id="${d.id}">Delete</button>
      </td>
    `;
    tb.appendChild(tr);
  }

  tb.querySelectorAll("button").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const id = Number(btn.getAttribute("data-id"));
      const act = btn.getAttribute("data-act");
      try {
        if (act === "delete") {
          await api(`/v1/documents/${id}`, { method: "DELETE" });
          state.selectedDocIds.delete(id);
          toast(`Deleted document ${id}`, "good");
          await loadDocs();
        } else if (act === "chunks") {
          const data = await api(`/v1/documents/${id}/chunks?limit=50`);
          $("chunksJson").textContent = JSON.stringify(data.data, null, 2);
          $("chunksPanel").open = true;
        }
      } catch (e) {
        toast(e.message, "bad");
      }
    });
  });
}

async function uploadSelected(file) {
  const fd = new FormData();
  fd.append("file", file);
  $("btnUpload").disabled = true;
  setUploadProgress(18);

  try {
    const res = await api("/v1/documents/upload", { method: "POST", body: fd });
    setUploadProgress(100);
    toast(`Uploaded: ${res.data.name} (chunks=${res.data.chunkCount})`, "good");
    $("quickStatus").textContent = `Upload request_id=${res.requestId || "-"} docId=${res.data.documentId} chunks=${res.data.chunkCount}`;
    await loadDocs();
    return res;
  } catch (e) {
    toast(e.message, "bad");
    throw e;
  } finally {
    setTimeout(clearUploadProgress, 500);
    $("btnUpload").disabled = false;
  }
}

function renderCitations(cites) {
  const box = $("citations");
  box.innerHTML = "";
  if (!cites || !cites.length) {
    box.classList.add("muted");
    box.textContent = "No citations.";
    return;
  }
  box.classList.remove("muted");

  for (const c of cites) {
    const supportSpan = c.supportSpan ?? c.support_span ?? c.snippet ?? "";
    const rerank = c.scoreRerank ?? c.rerank_score;
    const rerankPart = (rerank == null) ? "" : ` | rerank ${Number(rerank).toFixed(3)}`;
    const div = document.createElement("div");
    div.className = "cite";
    div.innerHTML = `
      <div class="top">
        <div class="meta">
          doc <span class="kbd">${c.documentId ?? c.doc_id ?? "-"}</span> | parent <span class="kbd">${c.parentId ?? c.parent_id ?? "-"}</span> | chunk <span class="kbd">${c.chunkId ?? c.chunk_id ?? "-"}</span> | idx <span class="kbd">${c.chunkIndex ?? c.chunk_index ?? "-"}</span>
        </div>
        <div class="badge badge-good">final ${(c.score ?? c.final_score ?? 0).toFixed(3)}</div>
      </div>
      <div class="snip">${escapeHtml(supportSpan)}</div>
      <div class="scores">vec ${(c.scoreVec ?? c.vec_score ?? 0).toFixed(3)} | kw ${(c.scoreKw ?? c.kw_score ?? 0).toFixed(3)}${rerankPart}</div>
    `;
    box.appendChild(div);
  }
}

function buildAskPayload(question, topK, minScore) {
  const restrict = $("restrictDocs").checked;
  const allowedDocIds = restrict ? Array.from(state.selectedDocIds.values()) : null;
  return {
    question,
    topK,
    minScore,
    allowedDocIds,
    redactPii: true,
  };
}

async function askAndRender(question, topK, minScore) {
  const payload = buildAskPayload(question, topK, minScore);
  const res = await api("/v1/chat/ask", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  const body = res.data;
  state.lastQueryLogId = body.queryLogId ?? null;
  $("answerBox").textContent = body.answer ?? "";
  $("answerBox").classList.remove("muted");
  renderCitations(body.citations || []);
  $("metaLine").textContent =
    `bestScore=${(body.bestScore ?? 0).toFixed(3)} | latency=${body.latencyMs ?? 0}ms` +
    (res.requestId ? ` | request_id=${res.requestId}` : "");

  const hasLog = !!state.lastQueryLogId;
  $("btnCopyAnswer").disabled = !body.answer;
  $("btnThumbUp").disabled = !hasLog;
  $("btnThumbDown").disabled = !hasLog;
  $("btnSendFeedback").disabled = !hasLog;

  return { request: payload, response: body, requestId: res.requestId };
}

async function ask() {
  const q = $("qInput").value.trim();
  if (!q) {
    toast("Enter a question.", "bad");
    return;
  }
  const topK = Math.min(5, Math.max(1, Number($("topK").value || 3)));
  const minScore = Number($("minScore").value || 0.2);

  $("btnAsk").disabled = true;
  $("answerBox").textContent = "Thinking...";
  $("metaLine").textContent = "";

  try {
    const out = await askAndRender(q, topK, minScore);
    $("quickStatus").textContent = `Ask request_id=${out.requestId || "-"} mode=${out.response.mode || "-"}`;
    toast("Answer ready.", "good");
  } catch (e) {
    $("answerBox").textContent = "Request failed.";
    $("answerBox").classList.add("muted");
    renderCitations([]);
    toast(e.message, "bad");
  } finally {
    $("btnAsk").disabled = false;
  }
}

async function sendFeedback(helpful) {
  if (!state.lastQueryLogId) {
    toast("No queryLogId yet.", "bad");
    return;
  }
  const comment = $("fbComment").value || "";
  $("btnSendFeedback").disabled = true;
  try {
    const res = await api("/v1/feedback", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ queryLogId: state.lastQueryLogId, helpful, comment }),
    });
    $("quickStatus").textContent = `Feedback request_id=${res.requestId || "-"}`;
    toast("Feedback stored.", "good");
    $("fbComment").value = "";
  } catch (e) {
    toast(e.message, "bad");
  } finally {
    $("btnSendFeedback").disabled = false;
  }
}

async function runEval() {
  const topK = Math.min(5, Math.max(1, Number($("evalTopK").value || 3)));
  const minScore = Number($("evalMinScore").value || 0.2);
  $("btnRunEval").disabled = true;
  $("evalResult").textContent = "Running...";
  $("evalResult").className = "badge badge-warn";

  try {
    const res = await api(`/v1/eval/run?topK=${encodeURIComponent(topK)}&minScore=${encodeURIComponent(minScore)}`, { method: "POST" });
    const body = res.data;
    $("evalResult").textContent = `Score ${(body.score ?? 0).toFixed(2)} (${body.passed}/${body.total})`;
    $("evalResult").className = (body.score >= 0.7) ? "badge badge-good" : "badge badge-warn";
    $("quickStatus").textContent = `Eval request_id=${res.requestId || "-"} runId=${body.runId} score=${body.score}`;
    toast("Eval completed.", "good");
    await loadEvalRuns();
    return { response: body, requestId: res.requestId };
  } catch (e) {
    $("evalResult").textContent = "Eval failed";
    $("evalResult").className = "badge badge-bad";
    toast(e.message, "bad");
    throw e;
  } finally {
    $("btnRunEval").disabled = false;
  }
}

async function loadEvalRuns() {
  try {
    const runs = await api("/v1/eval/runs?limit=10");
    const tb = $("evalTbody");
    tb.innerHTML = "";
    const rows = Array.isArray(runs.data) ? runs.data : [];
    if (!rows.length) {
      tb.innerHTML = `<tr><td colspan="5" class="muted">No runs.</td></tr>`;
      return;
    }

    for (const r of rows) {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${r.id}</td>
        <td>${r.totalCases ?? ""}</td>
        <td>${r.passedCases ?? ""}</td>
        <td>${(r.score ?? 0).toFixed(3)}</td>
        <td class="right"><button class="btn btn-ghost" data-id="${r.id}">Details</button></td>
      `;
      tb.appendChild(tr);
    }

    tb.querySelectorAll("button").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const id = btn.getAttribute("data-id");
        const data = await api(`/v1/eval/runs/${id}`);
        $("evalJson").textContent = JSON.stringify(data.data, null, 2);
      });
    });
  } catch (e) {
    toast(e.message, "bad");
  }
}

async function loadAudit() {
  try {
    const out = await api("/v1/audit?limit=50");
    const rows = Array.isArray(out.data) ? out.data : [];
    const tb = $("auditTbody");
    tb.innerHTML = "";
    if (!rows.length) {
      tb.innerHTML = `<tr><td colspan="6" class="muted">No audit logs.</td></tr>`;
      return rows;
    }

    for (const row of rows) {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${escapeHtml((row.createdAt || "").toString())}</td>
        <td>${escapeHtml(row.action || "")}</td>
        <td>${row.blocked ? "yes" : "no"}</td>
        <td>${row.bestScore == null ? "-" : Number(row.bestScore).toFixed(3)}</td>
        <td>${escapeHtml(row.docIds || "-")}</td>
        <td>${escapeHtml(row.requestId || "-")}</td>
      `;
      tb.appendChild(tr);
    }
    return rows;
  } catch (e) {
    toast(e.message, "bad");
    return [];
  }
}

async function loadFeedback() {
  try {
    const out = await api("/v1/feedback?limit=50");
    $("feedbackJson").textContent = JSON.stringify(out.data, null, 2);
    toast("Loaded feedback.", "good");
  } catch (e) {
    $("feedbackJson").textContent = "{}";
    toast(e.message, "bad");
  }
}

async function runDemo() {
  const btn = $("btnRunDemo");
  btn.disabled = true;
  $("quickStatus").textContent = "Demo: checking health...";

  try {
    const healthSummary = await refreshHealth();
    const healthRaw = await api("/actuator/health");

    const sampleText = "RAG means retrieval augmented generation. This system returns citations. It refuses to answer if not in docs.";
    const sampleBlob = new Blob([sampleText], { type: "text/plain" });
    const sampleFile = new File([sampleBlob], "demo-sample.txt", { type: "text/plain" });

    $("quickStatus").textContent = "Demo: uploading sample...";
    const upload = await uploadSelected(sampleFile);

    $("qInput").value = "What does RAG mean?";
    $("topK").value = "3";
    $("minScore").value = "0.2";
    $("quickStatus").textContent = "Demo: ask in-scope...";
    const inScope = await askAndRender("What does RAG mean?", 3, 0.2);
    if (!inScope.response.citations || inScope.response.citations.length === 0) {
      throw new Error("In-scope response missing citations");
    }

    $("quickStatus").textContent = "Demo: ask out-of-scope...";
    const outScope = await askAndRender("What is the capital of Mars?", 3, 0.8);
    if ((outScope.response.answer || "") !== "I don't know.") {
      throw new Error("Out-of-scope response did not return 'I don't know.'");
    }

    $("evalTopK").value = "3";
    $("evalMinScore").value = "0.2";
    $("quickStatus").textContent = "Demo: run eval...";
    const evalRun = await runEval();

    $("quickStatus").textContent = "Demo: load audit...";
    const auditRows = await loadAudit();

    const checks = [
      { name: "health_up", ok: healthRaw.data && healthRaw.data.status === "UP" },
      { name: "upload_ok", ok: !!upload.data.documentId && Number(upload.data.chunkCount) > 0 },
      { name: "in_scope_has_citations", ok: (inScope.response.citations || []).length > 0 },
      { name: "out_scope_idk", ok: (outScope.response.answer || "") === "I don't know." },
      { name: "eval_has_score", ok: evalRun.response && evalRun.response.score != null },
      { name: "audit_loaded", ok: auditRows.length > 0 },
    ];

    state.lastProof = {
      createdAt: new Date().toISOString(),
      healthSummary,
      health: { requestId: healthRaw.requestId, response: healthRaw.data },
      upload: { requestId: upload.requestId, response: upload.data },
      askInScope: inScope,
      askOutOfScope: outScope,
      eval: evalRun,
      audit: auditRows.slice(0, 10),
      checks,
    };

    $("quickStatus").textContent = "Demo complete. Download PROOF.md.";
    toast("Run Demo completed.", "good");
  } catch (e) {
    $("quickStatus").textContent = `Demo failed: ${e.message}`;
    toast(`Demo failed: ${e.message}`, "bad");
  } finally {
    btn.disabled = false;
  }
}

function downloadProof() {
  if (!state.lastProof) {
    toast("Run Demo first.", "bad");
    return;
  }
  const p = state.lastProof;
  const checklist = p.checks.map((c) => `- [${c.ok ? "x" : " "}] ${c.name}`).join("\n");
  const markdown =
    `# PROOF\n\n` +
    `Generated: ${p.createdAt}\n\n` +
    `## Health Summary\n\n` +
    `- Badge State: ${p.healthSummary.status}\n` +
    `- Request ID: ${p.health.requestId || "-"}\n\n` +
    `\`\`\`json\n${JSON.stringify(p.health.response, null, 2)}\n\`\`\`\n\n` +
    `## Upload\n\n` +
    `- Request ID: ${p.upload.requestId || "-"}\n\n` +
    `\`\`\`json\n${JSON.stringify(p.upload.response, null, 2)}\n\`\`\`\n\n` +
    `## Ask In Scope\n\n` +
    `- Request ID: ${p.askInScope.requestId || "-"}\n` +
    `- Request: \`${JSON.stringify(p.askInScope.request)}\`\n\n` +
    `\`\`\`json\n${JSON.stringify(p.askInScope.response, null, 2)}\n\`\`\`\n\n` +
    `## Ask Out Of Scope\n\n` +
    `- Request ID: ${p.askOutOfScope.requestId || "-"}\n` +
    `- Request: \`${JSON.stringify(p.askOutOfScope.request)}\`\n\n` +
    `\`\`\`json\n${JSON.stringify(p.askOutOfScope.response, null, 2)}\n\`\`\`\n\n` +
    `## Eval\n\n` +
    `- Request ID: ${p.eval.requestId || "-"}\n\n` +
    `\`\`\`json\n${JSON.stringify(p.eval.response, null, 2)}\n\`\`\`\n\n` +
    `## Audit (latest 10)\n\n` +
    `\`\`\`json\n${JSON.stringify(p.audit, null, 2)}\n\`\`\`\n\n` +
    `## Checklist\n\n${checklist}\n`;

  const blob = new Blob([markdown], { type: "text/markdown;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "PROOF.md";
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
  toast("Downloaded PROOF.md", "good");
}

function wireUpload() {
  const dz = $("dropzone");
  const input = $("fileInput");
  let selected = null;

  function setSelected(file) {
    selected = file;
    $("btnUpload").disabled = !selected;
    $("uploadHint").textContent = selected ? `${selected.name} (${Math.round(selected.size / 1024)} KB)` : "";
  }

  input.addEventListener("change", () => {
    if (input.files && input.files[0]) setSelected(input.files[0]);
  });

  dz.addEventListener("dragover", (e) => { e.preventDefault(); dz.classList.add("drag"); });
  dz.addEventListener("dragleave", () => dz.classList.remove("drag"));
  dz.addEventListener("drop", (e) => {
    e.preventDefault();
    dz.classList.remove("drag");
    const f = e.dataTransfer.files && e.dataTransfer.files[0];
    if (f) setSelected(f);
  });

  $("btnUpload").addEventListener("click", async () => {
    if (!selected) return;
    await uploadSelected(selected);
    setSelected(null);
    input.value = "";
  });
}

function wireNav() {
  document.querySelectorAll(".nav-item").forEach((btn) => {
    btn.addEventListener("click", () => switchView(btn.getAttribute("data-view")));
  });
}

function wire() {
  wireNav();
  wireUpload();

  $("btnLoadDocsQuick").addEventListener("click", async () => { await loadDocs(); toast("Docs loaded.", "good"); });
  $("btnLoadEvalRunsQuick").addEventListener("click", async () => { await loadEvalRuns(); toast("Eval runs loaded.", "good"); });
  $("btnRunDemo").addEventListener("click", runDemo);
  $("btnDownloadProof").addEventListener("click", downloadProof);

  $("btnLoadDocs").addEventListener("click", loadDocs);
  $("docSearch").addEventListener("input", renderDocs);

  $("btnAsk").addEventListener("click", ask);
  $("qInput").addEventListener("keydown", (e) => { if (e.key === "Enter") ask(); });

  $("btnCopyAnswer").addEventListener("click", async () => {
    const txt = $("answerBox").textContent || "";
    await navigator.clipboard.writeText(txt);
    toast("Copied.", "good");
  });

  $("btnThumbUp").addEventListener("click", () => sendFeedback(true));
  $("btnThumbDown").addEventListener("click", () => sendFeedback(false));
  $("btnSendFeedback").addEventListener("click", () => sendFeedback(true));

  $("btnRunEval").addEventListener("click", runEval);
  $("btnLoadEvalRuns").addEventListener("click", loadEvalRuns);

  $("btnLoadAudit").addEventListener("click", loadAudit);
  $("btnLoadFeedback").addEventListener("click", loadFeedback);

  $("btnTheme").addEventListener("click", toggleTheme);
  $("btnTheme2").addEventListener("click", toggleTheme);

  $("btnQuickRefresh").addEventListener("click", async () => {
    await refreshHealth();
    await loadDocs().catch(() => {});
    await loadEvalRuns().catch(() => {});
    await loadAudit().catch(() => {});
    toast("Refreshed.", "good");
  });

  window.addEventListener("keydown", (e) => {
    if (e.ctrlKey && e.key.toLowerCase() === "k") {
      e.preventDefault();
      switchView("ask");
      $("qInput").focus();
    }
  });
}

(async function init() {
  const savedTheme = localStorage.getItem("rag_theme") || "dark";
  setTheme(savedTheme);

  wire();

  const savedView = localStorage.getItem("rag_view") || "upload";
  switchView(savedView);

  startHealthPolling();
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") {
      startHealthPolling();
      refreshHealth().catch(() => {});
    }
  });
  window.addEventListener("beforeunload", stopHealthPolling);

  await loadDocs().catch(() => {});
  await loadEvalRuns().catch(() => {});
  await loadAudit().catch(() => {});
})();
