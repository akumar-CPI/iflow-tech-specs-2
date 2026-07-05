"use strict";
// Node port of src/providers.js. Running server-side removes the CORS
// limitation that affects OpenAI in the browser extension — all three
// providers work identically here.

function extractJson(text) {
  const cleaned = (text || "").trim().replace(/^```json/i, "").replace(/^```/, "").replace(/```$/, "").trim();
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  const slice = start !== -1 && end !== -1 ? cleaned.slice(start, end + 1) : cleaned;
  return JSON.parse(slice);
}

async function callAnthropic(apiKey, model, systemPrompt, userPrompt) {
  const res = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-api-key": apiKey,
      "anthropic-version": "2023-06-01",
    },
    body: JSON.stringify({
      model,
      max_tokens: 4000,
      system: systemPrompt,
      messages: [{ role: "user", content: userPrompt }],
    }),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`Anthropic API error (${res.status}): ${t.slice(0, 500)}`);
  }
  const data = await res.json();
  const text = (data.content || []).map((b) => b.text || "").join("\n");
  return extractJson(text);
}

async function callOpenAI(apiKey, model, systemPrompt, userPrompt) {
  const res = await fetch("https://api.openai.com/v1/chat/completions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${apiKey}`,
    },
    body: JSON.stringify({
      model,
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: userPrompt },
      ],
      temperature: 0.3,
      response_format: { type: "json_object" },
    }),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`OpenAI API error (${res.status}): ${t.slice(0, 500)}`);
  }
  const data = await res.json();
  const text = data.choices?.[0]?.message?.content || "";
  return extractJson(text);
}

async function callGemini(apiKey, model, systemPrompt, userPrompt) {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${encodeURIComponent(apiKey)}`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      systemInstruction: { parts: [{ text: systemPrompt }] },
      contents: [{ parts: [{ text: userPrompt }] }],
      generationConfig: { responseMimeType: "application/json", temperature: 0.3 },
    }),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(`Gemini API error (${res.status}): ${t.slice(0, 500)}`);
  }
  const data = await res.json();
  const text = data.candidates?.[0]?.content?.parts?.map((p) => p.text).join("\n") || "";
  return extractJson(text);
}

async function generateJson(providerId, apiKey, model, systemPrompt, userPrompt) {
  if (!apiKey) {
    throw new Error(`No API key provided for provider "${providerId}". Set the matching repo secret (see README).`);
  }
  if (providerId === "anthropic") return callAnthropic(apiKey, model, systemPrompt, userPrompt);
  if (providerId === "openai") return callOpenAI(apiKey, model, systemPrompt, userPrompt);
  if (providerId === "gemini") return callGemini(apiKey, model, systemPrompt, userPrompt);
  throw new Error(`Unknown provider: ${providerId}`);
}

module.exports = { generateJson };
