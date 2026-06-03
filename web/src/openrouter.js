/**
 * Daywise OpenRouter API Secluded Integration
 * 
 * This module isolates all API calls, endpoints, and model definitions
 * for OpenRouter, allowing easy deletion if needed.
 */
import { OpenRouter } from "@openrouter/sdk";

// A clean, hand-picked list of active free OpenRouter models from diverse providers
export const DEFAULT_OPENROUTER_MODELS = [
  { id: 'meta-llama/llama-3.3-70b-instruct:free', name: 'Llama 3.3 70B Instruct (Free / Best)' },
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
function getOpenRouterFallbackList(preferredModel) {
  const models = DEFAULT_OPENROUTER_MODELS.map(m => m.id);
  const list = [];
  
  if (preferredModel && models.includes(preferredModel)) {
    list.push(preferredModel);
  }
  
  models.forEach(m => {
    if (!list.includes(m)) {
      list.push(m);
    }
  });
  
  if (!list.includes('openrouter/free')) {
    list.push('openrouter/free');
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
 * Calls OpenRouter chat completion with automatic self-healing fallbacks
 */
async function callOpenRouterCompletionWithFallback(prompt, apiKey, preferredModel, responseFormatJson = false) {
  const modelList = getOpenRouterFallbackList(preferredModel);
  let lastError = null;

  const openrouter = new OpenRouter({
    apiKey: apiKey
  });

  for (const model of modelList) {
    try {
      console.info(`Attempting OpenRouter SDK call with model: ${model}`);
      
      const stream = await openrouter.chat.send({
        chatRequest: {
          model: model,
          messages: [
            { role: 'user', content: prompt }
          ],
          stream: true
        }
      });

      let rawText = '';
      for await (const chunk of stream) {
        const deltaContent = chunk.choices?.[0]?.delta?.content || '';
        rawText += deltaContent;
      }

      if (!rawText) {
        console.warn(`OpenRouter SDK model ${model} returned empty content.`);
        lastError = new Error(`Model ${model}: empty stream content`);
        continue;
      }

      console.info(`OpenRouter SDK model ${model} succeeded!`);
      return rawText;
    } catch (err) {
      console.warn(`OpenRouter SDK model ${model} caught error: ${err.message}`);
      lastError = err;
    }
  }
  
  throw lastError || new Error("All OpenRouter models failed.");
}

/**
 * OpenRouter parser endpoint for Chat Logs (converts user speech into structured Food/Exercise JSON)
 */
export async function parseInputWithOpenRouter(userText, apiKey, model, systemPrompt, cleanAndParseFn) {
  try {
    const prompt = `${systemPrompt}\n\nUser input: ${userText}`;
    const rawText = await callOpenRouterCompletionWithFallback(prompt, apiKey, model, true);
    return cleanAndParseFn(rawText);
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
    const rawText = await callOpenRouterCompletionWithFallback(prompt, apiKey, model, true);
    
    // Wrap rawText in a response-like object matching the fetch-response interface in main.js
    return {
      ok: true,
      json: async () => ({
        candidates: [{
          content: {
            parts: [{ text: rawText }]
          }
        }]
      })
    };
  } catch (err) {
    console.error("OpenRouter generic prompt failed:", err);
    throw err;
  }
}
