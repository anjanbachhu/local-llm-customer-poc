/* Local LLM Customer Search POC — front-end logic (vanilla JS, no build step). */
(function () {
    "use strict";

    const $ = (id) => document.getElementById(id);

    const dropzone = $("dropzone");
    const fileInput = $("fileInput");
    const fileList = $("fileList");
    const customerCount = $("customerCount");
    const resetBtn = $("resetBtn");

    const queryForm = $("queryForm");
    const question = $("question");
    const searchBtn = $("searchBtn");
    const spinner = $("spinner");

    const errorBox = $("errorBox");
    const answerBox = $("answerBox");
    const answerText = $("answerText");
    const engineTag = $("engineTag");
    const criteriaText = $("criteriaText");
    const resultsTable = $("resultsTable");
    const resultsBody = $("resultsBody");
    const noResults = $("noResults");

    // ---------------- upload (click + drag & drop) ----------------
    dropzone.addEventListener("click", () => fileInput.click());
    dropzone.addEventListener("keydown", (e) => {
        if (e.key === "Enter" || e.key === " ") { e.preventDefault(); fileInput.click(); }
    });
    fileInput.addEventListener("change", () => {
        if (fileInput.files.length) uploadFiles(fileInput.files);
        fileInput.value = "";
    });

    ["dragenter", "dragover"].forEach((evt) =>
        dropzone.addEventListener(evt, (e) => {
            e.preventDefault();
            dropzone.classList.add("dropzone--drag");
        })
    );
    ["dragleave", "drop"].forEach((evt) =>
        dropzone.addEventListener(evt, (e) => {
            e.preventDefault();
            dropzone.classList.remove("dropzone--drag");
        })
    );
    dropzone.addEventListener("drop", (e) => {
        const files = e.dataTransfer && e.dataTransfer.files;
        if (files && files.length) uploadFiles(files);
    });

    async function uploadFiles(files) {
        hideError();
        const form = new FormData();
        for (const f of files) form.append("files", f);
        try {
            const res = await fetch("/api/upload", { method: "POST", body: form });
            const data = await res.json();
            if (!res.ok) {
                showError(data.message || "Upload failed.");
                return;
            }
            renderState(data);
            if (data.errors && data.errors.length) {
                showError("Some files were skipped:\n• " + data.errors.join("\n• "));
            }
        } catch (err) {
            showError("Upload failed: " + err.message);
        }
    }

    resetBtn.addEventListener("click", async () => {
        hideError();
        try {
            const res = await fetch("/api/reset", { method: "POST" });
            const data = await res.json();
            renderState(data);
            hideResults();
        } catch (err) {
            showError("Reset failed: " + err.message);
        }
    });

    function renderState(data) {
        customerCount.textContent = data.totalCustomers;
        fileList.innerHTML = "";
        if (!data.fileNames || data.fileNames.length === 0) {
            const li = document.createElement("li");
            li.className = "files__empty";
            li.textContent = "No files uploaded yet.";
            fileList.appendChild(li);
            return;
        }
        for (const name of data.fileNames) {
            const li = document.createElement("li");
            li.className = "files__item";
            li.textContent = name;
            fileList.appendChild(li);
        }
    }

    // ---------------- example chips ----------------
    document.querySelectorAll(".chip").forEach((chip) =>
        chip.addEventListener("click", () => {
            question.value = chip.textContent.trim();
            question.focus();
        })
    );

    // ---------------- query ----------------
    queryForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const q = question.value.trim();
        hideError();
        if (!q) { showError("Please type a question first."); return; }

        setLoading(true);
        try {
            const res = await fetch("/api/query", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ question: q })
            });
            const data = await res.json();
            if (!res.ok) {
                showError(data.message || "Query failed.");
                hideResults();
                return;
            }
            renderResults(data);
        } catch (err) {
            showError("Query failed: " + err.message);
            hideResults();
        } finally {
            setLoading(false);
        }
    });

    function renderResults(data) {
        answerBox.hidden = false;
        answerText.textContent = data.answer;
        engineTag.textContent = data.engine;
        criteriaText.textContent = JSON.stringify(stripEmpty(data.criteria));

        resultsBody.innerHTML = "";
        const rows = data.results || [];
        if (rows.length === 0) {
            resultsTable.hidden = true;
            noResults.hidden = false;
            return;
        }
        noResults.hidden = true;
        resultsTable.hidden = false;
        for (const c of rows) {
            const tr = document.createElement("tr");
            tr.appendChild(cell(c.customerId));
            tr.appendChild(cell(c.name));
            tr.appendChild(cell(c.email || "—"));
            tr.appendChild(cell(c.city));
            tr.appendChild(statusCell(c.status));
            resultsBody.appendChild(tr);
        }
    }

    function cell(value) {
        const td = document.createElement("td");
        td.textContent = (value === null || value === undefined || value === "") ? "—" : value;
        return td;
    }

    function statusCell(status) {
        const td = document.createElement("td");
        if (!status) { td.textContent = "—"; return td; }
        const span = document.createElement("span");
        const isActive = status.toLowerCase() === "active";
        span.className = "status-pill " + (isActive ? "status-pill--active" : "status-pill--inactive");
        span.textContent = status;
        td.appendChild(span);
        return td;
    }

    function stripEmpty(obj) {
        const out = {};
        Object.keys(obj || {}).forEach((k) => {
            const v = obj[k];
            if (v !== null && v !== "" && v !== false && v !== undefined) out[k] = v;
        });
        return out;
    }

    // ---------------- helpers ----------------
    function setLoading(on) {
        searchBtn.disabled = on;
        spinner.hidden = !on;
    }
    function showError(msg) {
        errorBox.textContent = msg;
        errorBox.hidden = false;
    }
    function hideError() { errorBox.hidden = true; }
    function hideResults() {
        answerBox.hidden = true;
        resultsTable.hidden = true;
        noResults.hidden = true;
    }
})();
