# Local LLM · Customer Search POC

[![build](https://github.com/anjanbachhu/local-llm-customer-poc/actions/workflows/ci.yml/badge.svg)](https://github.com/anjanbachhu/local-llm-customer-poc/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![Model](https://img.shields.io/badge/LLM-Qwen2.5--0.5B--Instruct-blue)
![Offline](https://img.shields.io/badge/AI-100%25%20offline-success)
![License](https://img.shields.io/badge/license-Proprietary-red)

A **Java 21 + Spring Boot 3 + Gradle** proof-of-concept that runs a Large Language
Model **entirely on your laptop** (no OpenAI, no external AI API). Upload customer
JSON files and ask questions in plain English; a locally hosted
**Qwen2.5-0.5B-Instruct** model converts your question into a structured filter,
and plain Java business logic does the searching.

> **Hybrid design (the important bit):** the customer records are **never** sent
> to the model. Only the *question* goes to the LLM, which returns a small JSON
> filter. Java then filters the in-memory records. This keeps the approach
> scalable no matter how many customers you load.

```
   Question ──▶ LLM (local) ──▶ {"city":"London","status":"Active"} ──▶ Java filter ──▶ results
```

---

## Requirements

| Tool          | Version            | Notes                                              |
|---------------|--------------------|----------------------------------------------------|
| JDK           | **21**             | Temurin/OpenJDK. `java -version` should print 21.  |
| Gradle        | 8.5+ *(optional)*  | Only if you don't use the wrapper.                 |
| RAM           | 16 GB              | The 0.5B Q4 model needs well under 1 GB.           |
| Disk          | ~0.5 GB            | For the GGUF model file.                            |

The `de.kherud:llama` dependency **bundles the native llama.cpp binaries** for
Windows, Linux and macOS — no C++ toolchain or separate install needed.

---

## 1. Download the model

The model file is **not** included in the repo. Put a GGUF file into `models/`.

### Hugging Face CLI (recommended)

```bash
pip install -U "huggingface_hub[cli]"
huggingface-cli download Qwen/Qwen2.5-0.5B-Instruct-GGUF qwen2.5-0.5b-instruct-q4_k_m.gguf --local-dir models
```

### Or download manually

Grab `qwen2.5-0.5b-instruct-q4_k_m.gguf` from
<https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/tree/main>
and drop it into the `models/` folder.

The path is configured in `src/main/resources/application.yml`:

```yaml
app:
  llm:
    model-path: models/qwen2.5-0.5b-instruct-q4_k_m.gguf
```

> **No model yet?** The app still starts and works using a built-in deterministic
> rule-based extractor, so you can try the UI immediately. Download the GGUF to
> get true natural-language understanding. The UI badge (top-right) shows whether
> the model is **loaded** or running in **fallback** mode.

---

## 2. Run

A **Gradle wrapper is included**, so you do **not** need Gradle installed — only
JDK 21. From the project root (`D:\Local_LLM_POC`):

```powershell
# Windows
.\gradlew.bat bootRun
```
```bash
# macOS / Linux
./gradlew bootRun
```

The first run downloads Gradle 8.10.2 and the dependencies automatically. Then
open <http://localhost:8080>.

> If you have a system Gradle 8.5+ on your PATH you can also just run `gradle bootRun`.

> **Verified toolchain:** built and smoke-tested with **Eclipse Temurin JDK 21.0.11**
> and **Gradle 8.10.2** (via the wrapper) on Windows 11. `gradlew test` passes and
> the app boots in fallback mode without a model.

### Try it

1. Drag the two files from `sample-data/` onto the upload zone (or click to browse).
2. Watch the customer count and file list update.
3. Ask any of the example questions:
   - `Find customers from London`
   - `Show active customers`
   - `Find inactive customers`
   - `Find customers whose email contains gmail`
   - `Show duplicate customers`
   - `Show customers with missing email addresses`
   - `How many customers are from Manchester`
   - `List all active customers in London`

---

## 3. How it works

| Layer            | Class                                                            | Responsibility                                  |
|------------------|-----------------------------------------------------------------|-------------------------------------------------|
| Controller       | `controller/CustomerController`                                  | UI + REST endpoints (`/api/upload`, `/api/query`) |
| Orchestration    | `service/QueryService`                                           | Hybrid flow: LLM → filter → response            |
| LLM (abstraction)| `service/llm/LlmService`                                         | Pluggable inference contract                    |
| LLM (impl)       | `service/llm/LocalLlmService`                                    | llama.cpp GGUF inference + JSON extraction      |
| LLM (fallback)   | `service/llm/RuleBasedCriteriaExtractor`                         | Deterministic backup when model is absent       |
| Search           | `service/CustomerFilterService`                                 | Pure-Java filtering of in-memory records        |
| Parsing          | `service/JsonCustomerParser`                                    | Tolerant JSON → `Customer` parsing              |
| Storage          | `service/CustomerStore`                                         | Thread-safe in-memory store                     |
| Models           | `model/Customer`, `model/SearchCriteria`                        | Domain types                                    |
| Errors           | `exception/GlobalExceptionHandler`                              | Uniform JSON error responses                    |

### Prompt template

```
You are a customer search assistant.
Convert the user's request into a JSON filter object.
Supported fields: customerId, name, email, city, status, emailContains, missingEmail, duplicates.
Rules: return only valid JSON; no explanations; no markdown; no invented fields.
```

The question is wrapped in Qwen's ChatML format (`<|im_start|>…<|im_end|>`) and the
model's reply is parsed by extracting the first `{ … }` block.

### Swapping in a bigger model

Change `app.llm.model-path` (and `model-name`) in `application.yml` to point at a
larger GGUF (e.g. `Qwen2.5-3B-Instruct`). No code changes needed — that's the
whole point of the `LlmService` abstraction.

---

## 4. Memory notes (16 GB laptop)

- The GGUF model is **memory-mapped by the native layer**, outside the JVM heap.
- `bootRun` runs with `-Xmx2g`; the Q4 0.5B model itself uses ~0.4 GB.
- Inference runs on **CPU** (`app.llm.gpu-layers: 0`).

---

## 5. Tests

```bash
gradle test
```

`CustomerFilterServiceTest` covers the pure filtering logic (no model required).

---

## Project structure

```
D:\Local_LLM_POC
├── build.gradle
├── settings.gradle
├── models/                       # GGUF model goes here (git-ignored)
│   └── README.md
├── sample-data/
│   ├── customers1.json
│   └── customers2.json
└── src
    ├── main
    │   ├── java/com/poc/llm
    │   │   ├── LocalLlmApplication.java
    │   │   ├── config/LlmProperties.java
    │   │   ├── controller/CustomerController.java
    │   │   ├── dto/…
    │   │   ├── exception/…
    │   │   ├── model/…
    │   │   └── service/…
    │   └── resources
    │       ├── application.yml
    │       ├── templates/index.html
    │       └── static/{css,js}
    └── test/java/com/poc/llm/service/CustomerFilterServiceTest.java
```

---

## License

**Proprietary — All Rights Reserved.** This source is published for demonstration
and evaluation only; it is **not** free to use, copy, modify, or sell. Commercial
use requires a separate signed license. See [LICENSE](LICENSE) or contact
**anjan.bachhu@gmail.com**.
