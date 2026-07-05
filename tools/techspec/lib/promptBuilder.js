"use strict";
// Ported from the extension's src/main.js (condenseProps / redactTable / buildPrompt),
// including the credential-redaction fix, so the CI pipeline and the browser
// extension produce equivalent, equally-safe prompts.

const SECRET_KEY_PATTERN = /api[-_]?key|apikey|secret|password|passwd|token|authorization|privatekey|client[-_]?secret/i;

function condenseProps(props) {
  const drop = new Set(["cmdVariantUri", "componentVersion", "ComponentSWCVId", "ComponentSWCVName", "namespaceMapping"]);
  const out = {};
  Object.entries(props || {}).forEach(([k, v]) => {
    if (drop.has(k) || v === "" || v === undefined) return;
    out[k] = SECRET_KEY_PATTERN.test(k) ? "[REDACTED]" : v;
  });
  return out;
}

function redactTable(rows) {
  if (!rows) return rows;
  return rows.map((row) => {
    const copy = { ...row };
    const nameField = copy.Name || copy.name || copy.id || "";
    if (SECRET_KEY_PATTERN.test(nameField) && copy.Value !== undefined) {
      copy.Value = "[REDACTED]";
    }
    return copy;
  });
}

function condenseStep(s) {
  return {
    id: s.id,
    name: s.name,
    type: s.type,
    xmlTag: s.xmlTag,
    props: condenseProps(s.props),
    propertyTable: redactTable(s.propertyTable),
    headerTable: redactTable(s.headerTable),
    scriptFile: s.scriptFile,
  };
}

function condenseProcess(p) {
  return {
    name: p.name,
    steps: p.steps.map(condenseStep),
    subProcesses: (p.subProcesses || []).map(condenseProcess),
  };
}

function buildPrompt(parsed) {
  const condensed = {
    iflowName: parsed.iflowName,
    bundleVersion: parsed.bundleVersion,
    participants: parsed.participants,
    messageFlows: parsed.messageFlows.map((mf) => ({ name: mf.name, props: condenseProps(mf.props) })),
    processes: parsed.processes.map(condenseProcess),
    scripts: parsed.scripts,
    parametersDefined: parsed.parametersDefined,
  };

  const system = `You are a senior SAP Integration Suite (Cloud Integration / CPI) architect writing an internal technical specification document for a fellow integration developer. You are given a structured JSON extraction of an iFlow's steps, adapters, and embedded Groovy scripts. Write clearly, technically, and honestly — call out design smells, unreachable steps, hardcoded credentials, or missing error handling if you see them, the same way a careful senior reviewer would. Do not invent adapter settings, systems, or business context that are not implied by the data. If you see a value marked "[REDACTED]", describe that a credential/secret is present there without guessing or fabricating its value. Respond with ONLY a single valid JSON object matching the requested schema — no markdown fences, no commentary before or after.`;

  const user = `Here is the parsed iFlow data:\n\n${JSON.stringify(condensed, null, 2)}\n\nProduce a JSON object with exactly these fields:\n{\n  "overview": "one paragraph describing the iFlow's overall purpose and behavior",\n  "highLevelSummary": "one paragraph walking through control flow at a high level, written to sit under a diagram",\n  "dependenciesNote": "1-3 sentences on external dependencies (value mappings, data stores, keystores, other iFlows, external APIs) or stating none were found",\n  "deploymentChecklist": ["short actionable checklist items to verify before deploying this iFlow"],\n  "mappingsNote": "1-2 sentences describing how transformation/mapping is implemented (formal mapping artifacts vs. scripts vs. none)",\n  "mappingsBullets": ["one bullet per script or mapping artifact describing what it does"],\n  "securityNote": "1-2 sentences summarizing the overall security posture of this iFlow (auth methods used, any concerns)",\n  "securityRows": [["Area", "Configuration", "Observation"], ...],\n  "monitoringNote": "1-2 sentences summarizing logging/monitoring configuration actually present",\n  "errorHandlingBullets": ["bullets describing the error handling behavior actually present, referencing real step names"],\n  "connectivityNote": "1-2 sentences summarizing the network/connectivity picture across all channels (on-prem vs. cloud, proxies, timeouts)",\n  "testPlanRows": [["test consideration / scenario description", "Inside SAP (IS) | Outside SAP (OS) | Both (BO)"], ...],\n  "reviewRows": [["\ud83d\udd34 High | \ud83d\udfe0 Medium | \ud83d\udfe1 Low", "Area", "Finding", "Recommendation"], ...],\n  "appendixBullets": ["assumptions, open points, or things to verify before go-live"],\n  "stepDescriptions": { "<stepId>": "one sentence describing exactly what this step does, referencing its actual configuration" }\n}\n\nInclude a stepDescriptions entry for every step id present in the processes/subProcesses data. testPlanRows scope must be exactly one of "Inside SAP (IS)", "Outside SAP (OS)", or "Both (BO)" — do not invent a target test date, that is filled in manually. Base every claim strictly on the provided data.`;

  return { system, user };
}

module.exports = { condenseProps, redactTable, condenseStep, condenseProcess, buildPrompt, SECRET_KEY_PATTERN };
