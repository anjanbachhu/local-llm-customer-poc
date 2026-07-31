# Architecture Talk-Track

Speakable, technically-accurate narration for the 4-component architecture diagram
(`architecture.html` / deck slide). Point at each box as you go, left to right.

---

## The frame first (one line)
> "Four components, two data stores, all bound to **localhost** — nothing here talks to the
> internet. The arrows are the path of a single call, left to right and back."

## 1. Browser
> "The front end is plain HTML in Chrome. Speech-to-text uses the browser's built-in
> **Web Speech API** — so the mic audio is transcribed **in the browser**, never uploaded.
> It POSTs the transcript to the backend, gets text back, and plays the spoken reply as a
> **WAV blob** from Piper. Login sets the caller identity and that `verified` badge you see
> flip when auth passes."

## 2. Spring Boot — the orchestrator
> "This is the brain-stem — Java 21 on port 8787. Three endpoints: `/login`, `/chat`, `/tts`.
> When `/chat` gets your text, an **agent loop** sends it to the LLM, and if the model asks to
> call a tool, the server executes it and loops back for the spoken reply. The critical piece:
> the **auth gate lives here, in server code** — phone from login plus your spoken date of
> birth. The model can *request* a card action, but this layer refuses to execute it until
> identity checks pass. It also does the policy-doc retrieval before handing excerpts to the
> model."

## 3. Local LLM (Ollama - qwen2.5:3b)
> "The language model runs under **Ollama at 127.0.0.1** — a 3-billion-parameter Qwen model,
> fully in GPU memory. Its only job is **understanding intent and emitting function calls** —
> `freeze_card`, `authenticate_customer`, `search_policy_documents`. It doesn't touch the
> database directly and it doesn't invent answers: for questions it answers **only from the
> excerpts the server retrieves**. Low temperature, small context window — tuned to stay fast
> and stay in VRAM."

## 4. Piper TTS
> "The voice is **Piper**, a local neural TTS — voice 'Amy'. I keep it as a **persistent
> process** so the model's already loaded; a new sentence is about a second, and repeated
> phrases like the greeting are cached and instant. It streams back a WAV. **100% offline** —
> no ElevenLabs, no cloud voice API. We even strip markdown before synthesis so it doesn't
> read asterisks aloud."

## SQLite — securebank.db (bottom-left)
> "State lives in a single **SQLite file** — customer records with phone and DOB, and a cards
> table with last-4, type, and status. When you freeze a card, that `status` column actually
> changes — that's what I show in the admin tab to prove it's real, not scripted."

## Policy_Docs\ — knowledge base (bottom-right)
> "The knowledge base is just a **folder of PDFs and text files**. On any change it
> **auto-reindexes** — no restart. Text PDFs go through **PDFBox**; scanned, image-only PDFs
> fall back to **Tesseract OCR**. Retrieval is keyword-plus-IDF scoring, and only the top
> excerpts go to the model. That's how it answered a phone number out of a document that had
> no selectable text at all."

## The two arrows
> "`read / write` between Spring Boot and SQLite is the card actions. `search` up from the
> docs is the Q&A path. Everything inside that grey box is on one laptop."

---

## Closing hook (when you step back)
> "So the LLM is the only 'fuzzy' part — and it's deliberately boxed in: it can only pick from
> a fixed tool list, it can't act before the server authenticates you, and it can only answer
> from retrieved text. The determinism lives in the Java; the model just drives the
> conversation."

---

## Verified live facts (handy for Q&A)
- `ollama ps` shows `qwen2.5:3b`, **100% GPU**, `UNTIL: Forever` (pinned in VRAM via keep_alive).
- Hot response latency ~**1.3s**; only the first call after a cold start pays the ~20s model load.
- Swap to a bigger model = one line in `application.properties` (`ollama.model`).
- Card-action auth (phone + spoken DOB) is enforced in server code, not the prompt.
