#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");

const { parseZipBuffer } = require("./lib/iflowParser");
const Diagrams = require("./lib/diagrams");
const { buildPrompt } = require("./lib/promptBuilder");
const Providers = require("./lib/providers");
const DocBuilder = require("./lib/docBuilder");

function loadConfig() {
  const configPath = path.join(__dirname, "..", "..", "techspec.config.json");
  let fileConfig = {};
  if (fs.existsSync(configPath)) {
    fileConfig = JSON.parse(fs.readFileSync(configPath, "utf8"));
  }
  const provider = process.env.TECHSPEC_PROVIDER || fileConfig.provider || "anthropic";
  const model = process.env.TECHSPEC_MODEL || fileConfig.models?.[provider] || fileConfig.model;
  if (!model) {
    throw new Error(
      `No model configured for provider "${provider}". Set it in techspec.config.json ("models": { "${provider}": "..." }) or via the TECHSPEC_MODEL env var.`
    );
  }
  return { provider, model, documentMetadata: fileConfig.documentMetadata || {} };
}

function apiKeyForProvider(provider) {
  const envVar = { anthropic: "ANTHROPIC_API_KEY", openai: "OPENAI_API_KEY", gemini: "GEMINI_API_KEY" }[provider];
  return process.env[envVar];
}

async function main() {
  const [, , inputZipPath, outputDocxPath] = process.argv;
  if (!inputZipPath || !outputDocxPath) {
    console.error("Usage: node generate.js <input-iflow.zip> <output-techspec.docx>");
    process.exit(1);
  }

  const { provider, model, documentMetadata } = loadConfig();
  console.log(`[techspec] Provider: ${provider}  Model: ${model}`);
  console.log(`[techspec] Input:    ${inputZipPath}`);
  console.log(`[techspec] Output:   ${outputDocxPath}`);

  const apiKey = apiKeyForProvider(provider);
  if (!apiKey) {
    console.error(
      `[techspec] Missing API key for provider "${provider}". Add it as a repo secret and pass it into the workflow env (see README).`
    );
    process.exit(1);
  }

  console.log("[techspec] Parsing iFlow zip…");
  const zipBuffer = fs.readFileSync(inputZipPath);
  const parsed = await parseZipBuffer(zipBuffer);

  console.log("[techspec] Generating diagrams…");
  const highLevel = Diagrams.generateHighLevelDiagram(parsed);
  const detailed = Diagrams.generateDetailedDiagram(parsed);

  console.log(`[techspec] Calling ${provider} (${model})…`);
  const { system, user } = buildPrompt(parsed);
  const ai = await Providers.generateJson(provider, apiKey, model, system, user);

  const stepDescMap = ai.stepDescriptions || {};
  function annotate(proc) {
    proc.steps.forEach((s) => (s.aiDescription = stepDescMap[s.id] || ""));
    (proc.subProcesses || []).forEach(annotate);
  }
  parsed.processes.forEach(annotate);

  console.log("[techspec] Assembling .docx…");
  const buffer = await DocBuilder.build({ parsed, ai, diagrams: { highLevel, detailed }, docTitle: "", documentMetadata });

  fs.mkdirSync(path.dirname(outputDocxPath), { recursive: true });
  fs.writeFileSync(outputDocxPath, buffer);
  console.log(`[techspec] Wrote ${outputDocxPath} (${buffer.length} bytes)`);

  // Write scripts + mapping/XSLT/schema resources as real files next to the
  // doc, so the document can reference "attachments/<file>" instead of
  // dumping source inline.
  const baseName = path.basename(outputDocxPath).replace(/\.docx$/i, "");
  const attachmentsDir = path.join(path.dirname(outputDocxPath), `${baseName}_attachments`);
  const attachmentFiles = { ...parsed.scripts, ...Object.fromEntries(Object.entries(parsed.resources || {})) };
  if (Object.keys(attachmentFiles).length) {
    fs.mkdirSync(attachmentsDir, { recursive: true });
    for (const [filename, content] of Object.entries(attachmentFiles)) {
      fs.writeFileSync(path.join(attachmentsDir, filename), content);
    }
    console.log(`[techspec] Wrote ${Object.keys(attachmentFiles).length} attachment(s) to ${attachmentsDir}`);
  }
}

main().catch((err) => {
  console.error("[techspec] FAILED:", err.stack || err.message);
  process.exit(1);
});
