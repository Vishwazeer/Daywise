/**
 * Daywise NVIDIA API Secluded Integration
 * 
 * This module isolates all API calls, endpoints, and model definitions
 * for NVIDIA API Catalog, allowing easy integration and deletion if needed.
 */

// A hand-picked list of active models in the NVIDIA API Catalog (build.nvidia.com)
export const DEFAULT_NVIDIA_MODELS = [
  { id: 'meta/llama-3.2-11b-vision-instruct', name: 'Llama 3.2 11B Vision (Vision / Default)', vision: true },
  { id: 'meta/llama-3.2-90b-vision-instruct', name: 'Llama 3.2 90B Vision (Vision / Powerful)', vision: true },
  { id: 'nvidia/cosmos-reason2-8b', name: 'Cosmos Reason2 8B (Vision / NVIDIA)', vision: true },
  { id: 'microsoft/phi-3.5-vision-instruct', name: 'Phi 3.5 Vision (Vision / Microsoft)', vision: true },
  { id: 'meta/llama-3.3-70b-instruct', name: 'Llama 3.3 70B Instruct (Text)' },
  { id: 'nvidia/llama-3.1-nemotron-70b-instruct', name: 'Llama 3.1 Nemotron 70B Instruct (Text)' },
  { id: 'google/gemma-2-2b-it', name: 'Gemma 2 2B Instruct (Text / Google)' },
  { id: 'google/gemma-2-9b-it', name: 'Gemma 2 9B Instruct (Text / Google)' },
  { id: 'google/gemma-3-27b-it', name: 'Gemma 3 27B Instruct (Text / Google)' },
  { id: 'microsoft/phi-3.5-mini-instruct', name: 'Phi 3.5 Mini Instruct (Text / Microsoft)' },
  { id: 'meta/llama-3.1-8b-instruct', name: 'Llama 3.1 8B Instruct (Text / Meta)' },
  { id: 'meta/llama-3.1-405b-instruct', name: 'Llama 3.1 405B Instruct (Text / Meta / Massive)' },
  { id: 'openai/gpt-oss-120b', name: 'GPT-OSS 120B Instruct (Text / OpenAI / MoE)' },
  { id: 'openai/gpt-oss-20b', name: 'GPT-OSS 20B Instruct (Text / OpenAI / MoE)' }
];

function getNvidiaFallbackList(preferredModel, isImage = false) {
  const allModels = DEFAULT_NVIDIA_MODELS;
  const filteredModels = isImage ? allModels.filter(m => m.vision) : allModels;
  const ids = filteredModels.map(m => m.id);
  const list = [];
  
  // Always prioritize the selected model first if it exists, regardless of the isImage filter!
  if (preferredModel) {
    list.push(preferredModel);
  }
  
  ids.forEach(id => {
    if (!list.includes(id)) {
      list.push(id);
    }
  });
  
  if (isImage && list.length === 0) {
    list.push('meta/llama-3.2-11b-vision-instruct');
    list.push('meta/llama-3.2-90b-vision-instruct');
  }
  
  return list;
}

async function handleNvidiaApiError(response) {
  let detail = `HTTP ${response.status}`;
  try {
    const errJson = await response.json();
    if (errJson.error && errJson.error.message) {
      detail = errJson.error.message;
    }
  } catch (e) {}
  return detail;
}

export async function callNvidiaCompletionWithFallback(prompt, apiKey, preferredModel, responseFormatJson = false, imageFileObj = null) {
  const isImage = !!imageFileObj;
  const modelList = getNvidiaFallbackList(preferredModel, isImage);
  console.info(`NVIDIA preferred model: ${preferredModel}. Fallback order: ${modelList.join(', ')}`);
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
      console.info(`Attempting NVIDIA API call with model: ${model}`);
      
      const response = await fetch("https://integrate.api.nvidia.com/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${apiKey}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          model: model,
          messages: [
            { role: 'user', content: userContent }
          ]
        })
      });

      if (!response.ok) {
        const errorDetail = await handleNvidiaApiError(response);
        console.warn(`NVIDIA model ${model} failed with: ${errorDetail}`);
        lastError = new Error(`Model ${model}: ${errorDetail}`);
        continue;
      }

      const data = await response.json();
      const rawText = data.choices?.[0]?.message?.content?.trim() || "";

      if (!rawText) {
        console.warn(`NVIDIA model ${model} returned empty content.`);
        lastError = new Error(`Model ${model}: empty content`);
        continue;
      }

      console.info(`NVIDIA model ${model} succeeded!`);
      return { text: rawText, modelUsed: model };
    } catch (err) {
      console.warn(`NVIDIA model ${model} caught error: ${err.message}`);
      lastError = err;
    }
  }
  
  throw lastError || new Error("All NVIDIA models failed.");
}

export async function parseInputWithNvidia(userText, apiKey, model, systemPrompt, cleanAndParseFn, imageFileObj = null) {
  try {
    const prompt = `${systemPrompt}\n\nUser input: ${userText}`;
    const { text, modelUsed } = await callNvidiaCompletionWithFallback(prompt, apiKey, model, true, imageFileObj);
    const parsed = cleanAndParseFn(text);
    parsed.modelUsed = modelUsed;
    return parsed;
  } catch (err) {
    console.error("NVIDIA chatbot parsing failed:", err);
    return { type: "error", message: err.message || "NVIDIA connection failed" };
  }
}

export async function callNvidiaGeneric(prompt, apiKey, model) {
  try {
    const { text } = await callNvidiaCompletionWithFallback(prompt, apiKey, model, true);
    
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
    console.error("NVIDIA generic prompt failed:", err);
    throw err;
  }
}
