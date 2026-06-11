/**
 * Daywise OpenRouter API Secluded Integration
 * 
 * This module isolates all API calls, endpoints, and model definitions
 * for OpenRouter, allowing easy deletion if needed.
 */

// A clean, hand-picked list of active free OpenRouter models from diverse providers
export const DEFAULT_OPENROUTER_MODELS = [
  { id: 'meta-llama/llama-3.3-70b-instruct:free', name: 'Llama 3.3 70B Instruct (Free / Best)' },
  { id: 'google/gemini-2.5-flash', name: 'Gemini 2.5 Flash (Vision)', vision: true },
  { id: 'google/gemini-2.0-flash-exp:free', name: 'Gemini 2.0 Flash (Free / Vision)', vision: true },
  { id: 'meta-llama/llama-3.2-11b-vision-instruct:free', name: 'Llama 3.2 11B Vision (Free / Vision)', vision: true },
  { id: 'qwen/qwen-2-vl-7b-instruct:free', name: 'Qwen 2 VL 7B (Free / Vision)', vision: true },
  { id: 'z-ai/glm-4.5-air:free', name: 'GLM 4.5 Air (Free / Active)' },
  { id: 'qwen/qwen3-coder:free', name: 'Qwen 3 Coder 480B (Free)' },
  { id: 'google/gemma-4-31b-it:free', name: 'Gemma 4 31B (Free)' },
  { id: 'meta-llama/llama-3.2-3b-instruct:free', name: 'Llama 3.2 3B Instruct (Free)' },
  { id: 'openai/gpt-oss-120b:free', name: 'GPT OSS 120B (Free)' },
  { id: 'poolside/laguna-m.1:free', name: 'Laguna M.1 (Free)' },
  { id: 'openrouter/free', name: 'Auto Free Router (Best Available Free Model)' }
];

/**
 * Helper to build the fallback sequence for OpenRouter models.
 * Start with the preferred model, then try the remaining hand-picked models.
 */
function getOpenRouterFallbackList(preferredModel, isImage = false) {
  const allModels = DEFAULT_OPENROUTER_MODELS;
  const filteredModels = isImage ? allModels.filter(m => m.vision) : allModels;
  const ids = filteredModels.map(m => m.id);
  const list = [];
  
  if (preferredModel && ids.includes(preferredModel)) {
    list.push(preferredModel);
  }
  
  ids.forEach(id => {
    if (!list.includes(id)) {
      list.push(id);
    }
  });
  
  // For images, if list is empty (e.g. preferredModel is not a vision model),
  // fall back to default vision models.
  if (isImage && list.length === 0) {
    list.push('google/gemini-2.5-flash');
    list.push('google/gemini-2.0-flash-exp:free');
    list.push('meta-llama/llama-3.2-11b-vision-instruct:free');
  }
  
  return list;
}

/**
 * Handle API error parsing
 */
async function handleOpenRouterApiError(response) {
  let detail = `HTTP ${response.status}`;
  try {
    const errJson = await response.json();
    if (errJson.error && errJson.error.message) {
      detail = errJson.error.message;
    }
  } catch (e) {}
  return detail;
}

/**
 * Calls OpenRouter chat completion with automatic self-healing fallbacks using standard fetch
 */
async function callOpenRouterCompletionWithFallback(prompt, apiKey, preferredModel, responseFormatJson = false, imageFileObj = null) {
  const isImage = !!imageFileObj;
  const modelList = getOpenRouterFallbackList(preferredModel, isImage);
  let lastError = null;

  // Construct message content
  let userContent = prompt;
  if (isImage) {
    userContent = [
      {
        type: 'text',
        text: prompt
      },
      {
        type: 'image_url',
        image_url: {
          url: `data:${imageFileObj.mimeType};base64,${imageFileObj.data}`
        }
      }
    ];
  }

  for (const model of modelList) {
    try {
      console.info(`Attempting OpenRouter API call with model: ${model}`);
      
      const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${apiKey}`,
          "Content-Type": "application/json",
          "HTTP-Referer": window.location.origin || "http://localhost",
          "X-Title": "Daywise"
        },
        body: JSON.stringify({
          model: model,
          messages: [
            { role: 'user', content: userContent }
          ]
        })
      });

      if (!response.ok) {
        const errorDetail = await handleOpenRouterApiError(response);
        console.warn(`OpenRouter model ${model} failed with: ${errorDetail}`);
        lastError = new Error(`Model ${model}: ${errorDetail}`);
        continue;
      }

      const data = await response.json();
      const rawText = data.choices?.[0]?.message?.content?.trim() || "";

      if (!rawText) {
        console.warn(`OpenRouter model ${model} returned empty content.`);
        lastError = new Error(`Model ${model}: empty content`);
        continue;
      }

      console.info(`OpenRouter model ${model} succeeded!`);
      return { text: rawText, modelUsed: model };
    } catch (err) {
      console.warn(`OpenRouter model ${model} caught error: ${err.message}`);
      lastError = err;
    }
  }
  
  throw lastError || new Error("All OpenRouter models failed.");
}

/**
 * OpenRouter parser endpoint for Chat Logs (converts user speech/image into structured Food/Exercise JSON)
 */
export async function parseInputWithOpenRouter(userText, apiKey, model, systemPrompt, cleanAndParseFn, imageFileObj = null) {
  try {
    const prompt = `${systemPrompt}\n\nUser input: ${userText}`;
    const { text, modelUsed } = await callOpenRouterCompletionWithFallback(prompt, apiKey, model, true, imageFileObj);
    const parsed = cleanAndParseFn(text);
    parsed.modelUsed = modelUsed;
    return parsed;
  } catch (err) {
    console.error("OpenRouter chatbot parsing failed:", err);
    return { type: "error", message: err.message || "OpenRouter connection failed" };
  }
}

/**
 * OpenRouter caller for Daily Diet Planner and AI Chef Recipes
 */
export async function callOpenRouterGeneric(prompt, apiKey, model) {
  try {
    const { text } = await callOpenRouterCompletionWithFallback(prompt, apiKey, model, true);
    
    // Wrap text in a response-like object matching the fetch-response interface in main.js
    return {
      ok: true,
      json: async () => ({
        candidates: [{
          content: {
            parts: [{ text: text }]
          }
        }]
      })
    };
  } catch (err) {
    console.error("OpenRouter generic prompt failed:", err);
    throw err;
  }
}
