/* Inspection Management – single-page frontend for the Spring Boot API.
   Served from src/main/resources/static, so all API calls are same-origin. */

const STORAGE_KEY = "im.session";

const state = {
    session: loadSession(),   // { token, id, name, email, role }
};

/* ------------------------------------------------------------------ */
/* Helpers                                                             */
/* ------------------------------------------------------------------ */

const $ = (sel, root = document) => root.querySelector(sel);

function esc(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

function badge(status) {
    if (!status) return "";
    return `<span class="badge status-${esc(status)}">${esc(status.replace("_", " "))}</span>`;
}

function loadSession() {
    try { return JSON.parse(localStorage.getItem(STORAGE_KEY)); } catch { return null; }
}

function saveSession(session) {
    state.session = session;
    try {
        if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
        else localStorage.removeItem(STORAGE_KEY);
    } catch { /* storage unavailable – session lives in memory only */ }
}

let toastTimer;
function toast(message, isError = false) {
    const el = $("#toast");
    el.textContent = message;
    el.className = "toast" + (isError ? " error" : "");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.add("hidden"), 3500);
}

function hasRole(...roles) {
    return state.session && roles.includes(state.session.role);
}

/* ------------------------------------------------------------------ */
/* API client                                                          */
/* ------------------------------------------------------------------ */

async function api(path, { method = "GET", body } = {}) {
    const headers = {};
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (state.session?.token) headers["Authorization"] = "Bearer " + state.session.token;

    const res = await fetch(path, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });

    if (res.status === 204) return null;

    const text = await res.text();
    let data = text;
    try { data = text ? JSON.parse(text) : null; } catch { /* plain-text error body */ }

    if (!res.ok) {
        if (res.status === 401 && state.session && !path.startsWith("/api/users/login")) {
            saveSession(null);
            render();
            throw new Error("Your session has expired. Please sign in again.");
        }
        let message = typeof data === "string" && data ? data : null;
        if (!message && data && typeof data === "object") message = data.message || data.error;
        if (!message) {
            message = {
                400: "Invalid request.",
                401: "Invalid email or password.",
                403: "You don't have permission to do that.",
                404: "Not found.",
                409: "That already exists.",
            }[res.status] || `Request failed (${res.status}).`;
        }
        throw new Error(message);
    }
    return data;
}

/* ------------------------------------------------------------------ */
/* Modal                                                               */
/* ------------------------------------------------------------------ */

function openModal(title, html, onSubmit) {
    $("#modal-title").textContent = title;
    $("#modal-body").innerHTML = html;
    $("#modal").classList.remove("hidden");

    const form = $("#modal-body form");
    if (form && onSubmit) {
        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            const btn = form.querySelector("[type=submit]");
            btn.disabled = true;
            try {
                await onSubmit(Object.fromEntries(new FormData(form)));
                closeModal();
            } catch (err) {
                toast(err.message, true);
            } finally {
                btn.disabled = false;
            }
        });
        form.querySelector("input, select, textarea")?.focus();
    }
}

function closeModal() {
    $("#modal").classList.add("hidden");
    $("#modal-body").innerHTML = "";
}

document.addEventListener("click", (e) => {
    if (e.target.matches("[data-close-modal]") || e.target.id === "modal") closeModal();
});
document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeModal();
});

/* ------------------------------------------------------------------ */
/* Pages                                                               */
/* ------------------------------------------------------------------ */

const PAGES = {
    dashboard:   { title: "Dashboard",       roles: ["ADMIN", "MANAGER"],  render: renderDashboard },
    assets:      { title: "Assets",          roles: ["ADMIN", "MANAGER"],  render: renderAssets },
    inspections: { title: "Inspections",     roles: ["ADMIN", "MANAGER"],  render: renderInspections },
    maintenance: { title: "Maintenance",     roles: ["ADMIN", "MANAGER"],  render: renderMaintenance },
    users:       { title: "Users",           roles: ["ADMIN"],             render: renderUsers },
    mine:        { title: "My inspections",  roles: ["INSPECTOR"],         render: renderMyInspections },
};

function allowedPages() {
    return Object.entries(PAGES).filter(([, p]) => hasRole(...p.roles));
}

function currentPage() {
    const key = location.hash.replace("#", "");
    const allowed = allowedPages();
    if (allowed.some(([k]) => k === key)) return key;
    return allowed[0]?.[0];
}

function setActions(html) {
    $("#page-actions").innerHTML = html;
}

function table(headers, rows, emptyText) {
    if (!rows.length) return `<div class="card"><div class="empty">${esc(emptyText)}</div></div>`;
    return `
        <div class="card table-wrap">
            <table>
                <thead><tr>${headers.map(h => `<th>${h}</th>`).join("")}</tr></thead>
                <tbody>${rows.join("")}</tbody>
            </table>
        </div>`;
}

/* ---------- Dashboard ---------- */

async function renderDashboard(view) {
    setActions("");
    const r = await api("/api/reports/summary");
    const stats = [
        ["Total assets", r.totalAssets],
        ["Total inspections", r.totalInspections],
        ["Pending inspections", r.pendingInspections],
        ["Completed", r.completedInspections],
        ["Failed", r.failedInspections],
        ["Open maintenance", r.openMaintenanceRequests],
    ];
    view.innerHTML = `
        <div class="stats">
            ${stats.map(([label, value]) => `
                <div class="card stat">
                    <div class="label">${label}</div>
                    <div class="value">${value}</div>
                </div>`).join("")}
        </div>
        <p class="muted">Pending = scheduled + in-progress inspections.</p>`;
}

/* ---------- Assets ---------- */

const ASSET_STATUSES = ["ACTIVE", "INACTIVE", "UNDER_MAINTENANCE", "RETIRED"];

function assetForm(asset = {}) {
    const statuses = asset.status && !ASSET_STATUSES.includes(asset.status)
        ? [asset.status, ...ASSET_STATUSES] : ASSET_STATUSES;
    return `
        <form class="stack">
            <label>Name <input name="name" required value="${esc(asset.name)}"></label>
            <label>Serial number <input name="serialNumber" required value="${esc(asset.serialNumber)}"></label>
            <label>Location <input name="location" required value="${esc(asset.location)}"></label>
            <label>Status
                <select name="status">
                    ${statuses.map(s => `<option ${s === asset.status ? "selected" : ""}>${s}</option>`).join("")}
                </select>
            </label>
            <div class="modal-actions">
                <button type="button" class="btn" data-close-modal>Cancel</button>
                <button type="submit" class="btn primary">Save</button>
            </div>
        </form>`;
}

async function renderAssets(view) {
    setActions(`<button class="btn primary" id="new-asset">New asset</button>`);
    const assets = await api("/api/assets");

    view.innerHTML = table(
        ["ID", "Name", "Serial", "Location", "Status", ""],
        assets.map(a => `
            <tr>
                <td class="muted">${a.id}</td>
                <td>${esc(a.name)}</td>
                <td>${esc(a.serialNumber)}</td>
                <td>${esc(a.location)}</td>
                <td>${badge(a.status)}</td>
                <td class="actions">
                    <button class="btn small" data-history="${a.id}">History</button>
                    <button class="btn small" data-edit="${a.id}">Edit</button>
                    ${hasRole("ADMIN") ? `<button class="btn small danger" data-delete="${a.id}">Delete</button>` : ""}
                </td>
            </tr>`),
        "No assets yet."
    );

    $("#new-asset").onclick = () =>
        openModal("New asset", assetForm({ status: "ACTIVE" }), async (data) => {
            await api("/api/assets", { method: "POST", body: data });
            toast("Asset created");
            route();
        });

    view.onclick = async (e) => {
        const id = e.target.dataset.edit || e.target.dataset.delete || e.target.dataset.history;
        if (!id) return;
        const asset = assets.find(a => String(a.id) === id);

        if (e.target.dataset.edit) {
            openModal("Edit asset", assetForm(asset), async (data) => {
                await api(`/api/assets/${id}`, { method: "PUT", body: data });
                toast("Asset updated");
                route();
            });
        } else if (e.target.dataset.delete) {
            if (!confirm(`Delete asset "${asset.name}"?`)) return;
            try {
                await api(`/api/assets/${id}`, { method: "DELETE" });
                toast("Asset deleted");
                route();
            } catch (err) { toast(err.message, true); }
        } else {
            try {
                const history = await api(`/api/reports/assets/${id}/inspections`);
                openModal(`Inspection history – ${asset.name}`, history.length
                    ? `<div class="table-wrap"><table>
                        <thead><tr><th>Date</th><th>Inspector</th><th>Status</th><th>Findings</th></tr></thead>
                        <tbody>${history.map(i => `
                            <tr><td>${esc(i.scheduledDate)}</td><td>${esc(i.inspectorName)}</td>
                            <td>${badge(i.status)}</td><td>${esc(i.findings) || '<span class="muted">—</span>'}</td></tr>`).join("")}
                        </tbody></table></div>`
                    : `<div class="empty">No inspections for this asset.</div>`);
            } catch (err) { toast(err.message, true); }
        }
    };
}

/* ---------- Inspections (admin / manager) ---------- */

async function renderInspections(view) {
    setActions(`<button class="btn primary" id="new-inspection">Schedule inspection</button>`);
    const inspections = await api("/api/inspections");

    view.innerHTML = table(
        ["ID", "Asset", "Inspector", "Scheduled", "Status", "Findings"],
        inspections.map(i => `
            <tr>
                <td class="muted">${i.id}</td>
                <td>${esc(i.assetName)}</td>
                <td>${esc(i.inspectorName)}</td>
                <td>${esc(i.scheduledDate)}</td>
                <td>${badge(i.status)}</td>
                <td>${esc(i.findings) || '<span class="muted">—</span>'}</td>
            </tr>`),
        "No inspections yet."
    );

    $("#new-inspection").onclick = async () => {
        let assets = [];
        let inspectors = null;
        try {
            assets = await api("/api/assets");
            // Only ADMIN can list users; managers type the inspector ID instead.
            if (hasRole("ADMIN")) {
                inspectors = (await api("/api/users")).filter(u => u.role === "INSPECTOR");
            }
        } catch (err) { return toast(err.message, true); }

        if (!assets.length) return toast("Create an asset first.", true);

        const inspectorField = inspectors
            ? `<label>Inspector
                   <select name="inspectorId" required>
                       ${inspectors.map(u => `<option value="${u.id}">${esc(u.name)} (${esc(u.email)})</option>`).join("")}
                   </select>
               </label>`
            : `<label>Inspector ID <input name="inspectorId" type="number" min="1" required></label>
               <p class="hint">Managers can't list users; ask an admin for the inspector's user ID.</p>`;

        const today = new Date().toISOString().slice(0, 10);
        openModal("Schedule inspection", `
            <form class="stack">
                <label>Asset
                    <select name="assetId" required>
                        ${assets.map(a => `<option value="${a.id}">${esc(a.name)} – ${esc(a.serialNumber)}</option>`).join("")}
                    </select>
                </label>
                ${inspectorField}
                <label>Scheduled date <input name="scheduledDate" type="date" required value="${today}"></label>
                <label>Notes <textarea name="findings" placeholder="Optional"></textarea></label>
                <div class="modal-actions">
                    <button type="button" class="btn" data-close-modal>Cancel</button>
                    <button type="submit" class="btn primary">Schedule</button>
                </div>
            </form>`,
            async (data) => {
                await api("/api/inspections", {
                    method: "POST",
                    body: {
                        assetId: Number(data.assetId),
                        inspectorId: Number(data.inspectorId),
                        scheduledDate: data.scheduledDate,
                        findings: data.findings || null,
                    },
                });
                toast("Inspection scheduled");
                route();
            });
    };
}

/* ---------- My inspections (inspector) ---------- */

const NEXT_STATUSES = {
    SCHEDULED: ["IN_PROGRESS"],
    IN_PROGRESS: ["COMPLETED", "FAILED"],
};

async function renderMyInspections(view) {
    setActions("");
    const inspections = await api("/api/inspections/my-inspections");

    view.innerHTML = table(
        ["ID", "Asset", "Scheduled", "Status", "Findings", ""],
        inspections.map(i => `
            <tr>
                <td class="muted">${i.id}</td>
                <td>${esc(i.assetName)}</td>
                <td>${esc(i.scheduledDate)}</td>
                <td>${badge(i.status)}</td>
                <td>${esc(i.findings) || '<span class="muted">—</span>'}</td>
                <td class="actions">
                    ${NEXT_STATUSES[i.status]
                        ? `<button class="btn small primary" data-submit="${i.id}">Update</button>`
                        : ""}
                </td>
            </tr>`),
        "No inspections assigned to you."
    );

    view.onclick = (e) => {
        const id = e.target.dataset.submit;
        if (!id) return;
        const insp = inspections.find(i => String(i.id) === id);
        const options = NEXT_STATUSES[insp.status];

        openModal(`Inspection #${insp.id} – ${insp.assetName}`, `
            <form class="stack">
                <p class="hint">Current status: ${badge(insp.status)}</p>
                <label>New status
                    <select name="status">${options.map(s => `<option>${s}</option>`).join("")}</select>
                </label>
                <label>Findings <textarea name="findings">${esc(insp.findings)}</textarea></label>
                <p class="hint">Marking an inspection FAILED automatically opens a maintenance request.</p>
                <div class="modal-actions">
                    <button type="button" class="btn" data-close-modal>Cancel</button>
                    <button type="submit" class="btn primary">Submit</button>
                </div>
            </form>`,
            async (data) => {
                if (data.status === "FAILED" && !data.findings.trim()) {
                    throw new Error("Please describe the findings for a failed inspection.");
                }
                await api(`/api/inspections/${id}/submit`, { method: "PUT", body: data });
                toast(data.status === "FAILED"
                    ? "Inspection failed – maintenance request opened"
                    : "Inspection updated");
                route();
            });
    };
}

/* ---------- Maintenance ---------- */

const NEXT_MAINTENANCE = { OPEN: "IN_PROGRESS", IN_PROGRESS: "COMPLETED" };

async function renderMaintenance(view) {
    setActions("");
    const requests = await api("/api/maintenance");

    view.innerHTML = table(
        ["ID", "Asset", "Inspection", "Description", "Status", ""],
        requests.map(m => `
            <tr>
                <td class="muted">${m.id}</td>
                <td>${esc(m.assetName)}</td>
                <td class="muted">#${m.inspectionId}</td>
                <td>${esc(m.description)}</td>
                <td>${badge(m.status)}</td>
                <td class="actions">
                    ${NEXT_MAINTENANCE[m.status]
                        ? `<button class="btn small primary" data-advance="${m.id}" data-to="${NEXT_MAINTENANCE[m.status]}">
                               Mark ${NEXT_MAINTENANCE[m.status].replace("_", " ").toLowerCase()}
                           </button>`
                        : ""}
                </td>
            </tr>`),
        "No maintenance requests. They're created automatically when an inspection fails."
    );

    view.onclick = async (e) => {
        const id = e.target.dataset.advance;
        if (!id) return;
        try {
            await api(`/api/maintenance/${id}/status`, {
                method: "PUT",
                body: { status: e.target.dataset.to },
            });
            toast("Maintenance request updated");
            route();
        } catch (err) { toast(err.message, true); }
    };
}

/* ---------- Users (admin) ---------- */

async function renderUsers(view) {
    setActions("");
    const users = await api("/api/users");

    view.innerHTML = table(
        ["ID", "Name", "Email", "Role", ""],
        users.map(u => `
            <tr>
                <td class="muted">${u.id}</td>
                <td>${esc(u.name)}</td>
                <td>${esc(u.email)}</td>
                <td><span class="badge">${esc(u.role)}</span></td>
                <td class="actions">
                    <button class="btn small" data-edit="${u.id}">Edit</button>
                    ${u.id !== state.session.id
                        ? `<button class="btn small danger" data-delete="${u.id}">Delete</button>`
                        : ""}
                </td>
            </tr>`),
        "No users."
    );

    view.onclick = async (e) => {
        const id = e.target.dataset.edit || e.target.dataset.delete;
        if (!id) return;
        const user = users.find(u => String(u.id) === id);

        if (e.target.dataset.edit) {
            openModal("Edit user", `
                <form class="stack">
                    <label>Name <input name="name" required value="${esc(user.name)}"></label>
                    <label>Email <input name="email" type="email" required value="${esc(user.email)}"></label>
                    <div class="modal-actions">
                        <button type="button" class="btn" data-close-modal>Cancel</button>
                        <button type="submit" class="btn primary">Save</button>
                    </div>
                </form>`,
                async (data) => {
                    await api(`/api/users/${id}`, { method: "PUT", body: data });
                    toast("User updated");
                    route();
                });
        } else {
            if (!confirm(`Delete user ${user.email}?`)) return;
            try {
                await api(`/api/users/${id}`, { method: "DELETE" });
                toast("User deleted");
                route();
            } catch (err) { toast(err.message, true); }
        }
    };
}

/* ------------------------------------------------------------------ */
/* Router & shell                                                      */
/* ------------------------------------------------------------------ */

async function route() {
    const key = currentPage();
    const view = $("#view");
    view.onclick = null;

    $("#nav").innerHTML = allowedPages()
        .map(([k, p]) => `<a href="#${k}" class="${k === key ? "active" : ""}">${p.title}</a>`)
        .join("");

    if (!key) {
        $("#page-title").textContent = "";
        view.innerHTML = `<div class="card"><div class="empty">No pages available for your role.</div></div>`;
        return;
    }

    $("#page-title").textContent = PAGES[key].title;
    view.innerHTML = `<div class="muted">Loading…</div>`;
    try {
        await PAGES[key].render(view);
    } catch (err) {
        if (!state.session) return; // logged out by a 401
        view.innerHTML = `<div class="card"><div class="empty">${esc(err.message)}</div></div>`;
    }
}

function render() {
    const loggedIn = !!state.session;
    $("#auth-screen").classList.toggle("hidden", loggedIn);
    $("#app-screen").classList.toggle("hidden", !loggedIn);

    if (loggedIn) {
        $("#me-name").textContent = state.session.name || state.session.email;
        $("#me-role").textContent = state.session.role;
        route();
    }
}

window.addEventListener("hashchange", () => state.session && route());

/* ------------------------------------------------------------------ */
/* Auth                                                                */
/* ------------------------------------------------------------------ */

document.querySelectorAll("[data-auth-tab]").forEach(tab => {
    tab.addEventListener("click", () => {
        const which = tab.dataset.authTab;
        document.querySelectorAll("[data-auth-tab]").forEach(t => t.classList.toggle("active", t === tab));
        $("#login-form").classList.toggle("hidden", which !== "login");
        $("#register-form").classList.toggle("hidden", which !== "register");
    });
});

$("#login-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.target));
    try {
        const res = await api("/api/users/login", { method: "POST", body: data });
        saveSession({ token: res.token, id: res.id, name: res.name, email: res.email, role: res.role });
        location.hash = "";
        e.target.reset();
        render();
    } catch (err) { toast(err.message, true); }
});

$("#register-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.target));
    try {
        await api("/api/users/register", { method: "POST", body: data });
        toast("Account created – you can sign in now");
        e.target.reset();
        document.querySelector('[data-auth-tab="login"]').click();
        $("#login-form [name=email]").value = data.email;
    } catch (err) { toast(err.message, true); }
});

$("#logout").addEventListener("click", () => {
    saveSession(null);
    location.hash = "";
    render();
});

render();
