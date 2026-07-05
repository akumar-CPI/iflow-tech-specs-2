# iFlow Tech Spec — CI/CD Pipeline

Automatically generates a Word technical specification whenever an SAP
Integration Suite iFlow export (`.zip`) is pushed to `iflows/`, using the same
parsing/AI/document-building logic as the browser extension — just running
server-side in GitHub Actions instead of in a popup.

## How it works

1. You export an iFlow from Integration Suite and drop the `.zip` into
   `iflows/` (any subfolder works, e.g. `iflows/finance/OrderSync.zip`).
2. On `git push` or in a pull request that touches `iflows/**/*.zip`, the
   workflow in `.github/workflows/techspec.yml` runs.
3. It detects which `.zip` file(s) changed, runs `tools/techspec/generate.js`
   on each, and writes `docs/tech-specs/<name>_TechSpec.docx`.
4. On a direct push (or a PR from a branch in this same repo), the generated
   doc(s) are committed straight back to `docs/tech-specs/`. They're also
   always uploaded as a downloadable workflow artifact either way.
5. On a pull request, the bot also leaves a comment confirming what happened.

## One-time setup

### 1. Add API key secrets

Go to **Settings → Secrets and variables → Actions** and add whichever of
these you plan to use:

- `ANTHROPIC_API_KEY`
- `OPENAI_API_KEY`
- `GEMINI_API_KEY`

You only need the one matching your configured provider (see below), but
adding more costs nothing and lets you switch later.

### 2. Choose provider/model

Edit `techspec.config.json` at the repo root:

```json
{
  "provider": "anthropic",
  "models": {
    "anthropic": "claude-sonnet-4-6",
    "openai": "gpt-4.1",
    "gemini": "gemini-2.5-pro"
  },
  "watchPath": "iflows/**/*.zip",
  "outputDir": "docs/tech-specs"
}
```

`provider` picks which one runs by default. You can override per-run without
editing the file by triggering the workflow manually (Actions tab → **Generate
iFlow Technical Specs** → **Run workflow**) and filling in the `provider` /
`model` inputs — handy for trying a different model on the same iFlow without
committing a config change.

### 3. Push an iFlow

```
git add iflows/MyProcess.zip
git commit -m "Add MyProcess iFlow export"
git push
```

Check the **Actions** tab — the workflow should pick it up, and shortly after
you'll see a new/updated file under `docs/tech-specs/`.

## What's deterministic vs. AI-written

Same principle as the extension: structural facts (change history, interface
table, step tables, channel table, version/metadata) come straight from the
parsed XML. The AI is only used for the overview paragraph, findings, test
plan, and per-step descriptions — and credential-looking values are redacted
before the prompt is built (`tools/techspec/lib/promptBuilder.js`), so a
hardcoded API key in the iFlow never gets echoed into the document.

## Project structure

```
.github/workflows/techspec.yml   The workflow (push / PR / manual trigger)
techspec.config.json              Provider/model/paths config
iflows/                            Drop iFlow .zip exports here
docs/tech-specs/                    Generated .docx files land here
tools/techspec/
  generate.js                      CLI entry point
  package.json
  lib/iflowParser.js               Parses the .iflw XML + scripts (xmldom)
  lib/diagrams.js                  Canvas diagrams (node-canvas)
  lib/promptBuilder.js             AI prompt + credential redaction
  lib/providers.js                 Claude / GPT / Gemini API clients
  lib/docBuilder.js                Assembles the final .docx (docx package)
```

## Known limitations

- **Pull requests from forks**: GitHub Actions can't push commits to a fork's
  branch with the default token, so for fork PRs the doc is only available as
  a workflow artifact (the bot comment says so explicitly) — it isn't
  committed back automatically. PRs from branches within this same repo work
  normally.
- **`node-canvas` native build**: the `canvas` npm package ships prebuilt
  binaries for common Linux runners, so `ubuntu-latest` should just work. If a
  future runner image causes a build failure, add this step before "Install
  generator dependencies":
  ```yaml
  - run: sudo apt-get update && sudo apt-get install -y libcairo2-dev libpango1.0-dev libjpeg-dev libgif-dev librsvg2-dev
  ```
- **No `package-lock.json` committed yet** — the workflow uses `npm install`
  rather than `npm ci` for that reason. For fully reproducible installs, run
  `npm install` once inside `tools/techspec` locally, commit the resulting
  `package-lock.json`, and switch the workflow step back to `npm ci`.
- Like the extension, treat the generated narrative sections as a strong
  first draft — review before publishing externally.
