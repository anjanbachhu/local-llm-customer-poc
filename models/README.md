# Models directory

Place the GGUF model file here. The application looks for the file named in
`src/main/resources/application.yml` (`app.llm.model-path`), which by default is:

```
models/qwen2.5-0.5b-instruct-q4_k_m.gguf
```

## Download Qwen2.5-0.5B-Instruct (GGUF, quantised — ~400 MB)

### Option A — Hugging Face CLI (recommended)

```bash
pip install -U "huggingface_hub[cli]"
huggingface-cli download Qwen/Qwen2.5-0.5B-Instruct-GGUF qwen2.5-0.5b-instruct-q4_k_m.gguf --local-dir models
```

### Option B — direct download

Download `qwen2.5-0.5b-instruct-q4_k_m.gguf` from
<https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/tree/main>
and save it into this `models/` folder.

> The app still runs **without** the model — it falls back to a deterministic
> rule-based extractor so you can test the UI immediately. Download the model to
> enable true natural-language understanding.

## Plugging in a larger model later

1. Download a bigger GGUF (e.g. `Qwen2.5-3B-Instruct-GGUF`) into this folder.
2. Change `app.llm.model-path` (and optionally `app.llm.model-name`) in
   `application.yml`.
3. Restart. No code changes required.
