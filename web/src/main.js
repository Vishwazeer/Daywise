// ── Imports ──────────────────────────────────────────────────────────────────
import { createIcons, icons } from 'lucide';
import './style.css';
import { 
  parseInputWithOpenRouter, 
  callOpenRouterGeneric, 
  DEFAULT_OPENROUTER_MODELS 
} from './openrouter.js';

// ── State Management ─────────────────────────────────────────────────────────
const state = {
  currentScreen: localStorage.getItem('currentScreen') || 'chat',
  theme: localStorage.getItem('theme') || 'light',
  
  // Goals & Profile
  hasCompletedOnboarding: JSON.parse(localStorage.getItem('hasCompletedOnboarding') || 'false'),
  dailyCalorieGoal: JSON.parse(localStorage.getItem('dailyCalorieGoal') || '2000'),
  carbsPercent: JSON.parse(localStorage.getItem('carbsPercent') || '50'),
  proteinPercent: JSON.parse(localStorage.getItem('proteinPercent') || '25'),
  fatPercent: JSON.parse(localStorage.getItem('fatPercent') || '25'),
  
  currentWeightKg: JSON.parse(localStorage.getItem('currentWeightKg') || '80'),
  targetWeightKg: JSON.parse(localStorage.getItem('targetWeightKg') || '70'),
  heightCm: JSON.parse(localStorage.getItem('heightCm') || '170'),
  age: JSON.parse(localStorage.getItem('age') || '25'),
  isMale: JSON.parse(localStorage.getItem('isMale') || 'true'),
  weightGoal: localStorage.getItem('weightGoal') || 'lose',
  aggression: localStorage.getItem('aggression') || 'moderate',
  
  // Onboarding Wizard step
  onboardingStep: 0,
  onboardingData: {
    gender: 'Male',
    age: '',
    height: '',
    weight: '',
    goal: 'Lose',
    aggression: 'Moderate',
    targetWeight: ''
  },

  // Reminders
  morningEnabled: JSON.parse(localStorage.getItem('morningEnabled') || 'true'),
  morningTime: localStorage.getItem('morningTime') || '09:00',
  afternoonEnabled: JSON.parse(localStorage.getItem('afternoonEnabled') || 'true'),
  afternoonTime: localStorage.getItem('afternoonTime') || '13:00',
  eveningEnabled: JSON.parse(localStorage.getItem('eveningEnabled') || 'true'),
  eveningTime: localStorage.getItem('eveningTime') || '19:00',

  // Config
  apiKey: localStorage.getItem('geminiApiKey') || '',
  geminiModel: localStorage.getItem('geminiModel') || 'auto',
  currentLoadingFact: '',
  apiProvider: localStorage.getItem('apiProvider') || 'gemini',
  openRouterApiKey: localStorage.getItem('openRouterApiKey') || '',
  openRouterModel: localStorage.getItem('openRouterModel') || 'meta-llama/llama-3.3-70b-instruct:free',

  // Selected Chat Date
  selectedDateStr: getLocalDateString(new Date()),

  // Database lists (stored in localStorage)
  foodEntries: JSON.parse(localStorage.getItem('foodEntries') || '[]'),
  exerciseEntries: JSON.parse(localStorage.getItem('exerciseEntries') || '[]'),
  weightEntries: JSON.parse(localStorage.getItem('weightEntries') || '[]'),
  chatMessages: JSON.parse(localStorage.getItem('chatMessages') || '[]'),
  
  // New AI Daily Diet Planner & AI Chef state
  cuisineType: localStorage.getItem('cuisineType') || 'Indian',
  cookingStyle: localStorage.getItem('cookingStyle') || 'Simple home cooking',
  dailyDietPlan: JSON.parse(localStorage.getItem('dailyDietPlan') || 'null'),
  
  chefIngredients: localStorage.getItem('chefIngredients') || '',
  chefCalorieMax: localStorage.getItem('chefCalorieMax') || '600',
  chefProteinMin: localStorage.getItem('chefProteinMin') || '25',
  chefFlavorProfile: localStorage.getItem('chefFlavorProfile') || 'Spicy',
  generatedRecipe: JSON.parse(localStorage.getItem('generatedRecipe') || 'null'),
  
  isProcessingChat: false,
  isSidebarOpen: false,
  isGeneratingDiet: false,
  isGeneratingRecipe: false,
  selectedMacroView: 'calories',
  isNutritionDropdownOpen: JSON.parse(localStorage.getItem('isNutritionDropdownOpen') || 'false'),
  dietCalorieGoal: JSON.parse(localStorage.getItem('dietCalorieGoal') || localStorage.getItem('dailyCalorieGoal') || '2000'),
  weeklyInsights: JSON.parse(localStorage.getItem('weeklyInsights') || '{}'),
  isGeneratingWeeklyInsights: false,
  isWeeklyInsightsOpen: JSON.parse(localStorage.getItem('isWeeklyInsightsOpen') || 'true'),
  lastProgress: JSON.parse(localStorage.getItem('lastProgress') || '{"caloriePercent":0,"offset":314.159,"macros":{"protein":0,"carbs":0,"fat":0}}'),
  
  // Expanded days in weekly diet planner
  expandedPlanDays: JSON.parse(localStorage.getItem('expandedPlanDays') || '{}')
};

// Ensure default weight entries exist if empty
if (state.weightEntries.length === 0) {
  state.weightEntries = [
    { id: 1, weightKg: state.currentWeightKg, timestamp: Date.now() - 3 * 24 * 60 * 60 * 1000 },
    { id: 2, weightKg: state.currentWeightKg - 0.5, timestamp: Date.now() }
  ];
  saveStateToStorage();
}

// Purge stale mock values, error states, and legacy schemas from localStorage
if (state.generatedRecipe && (state.generatedRecipe.isMock || state.generatedRecipe.error || (state.generatedRecipe.ingredients && state.generatedRecipe.ingredients.includes("quantity and ingredient 1")))) {
  state.generatedRecipe = null;
  saveStateToStorage();
}
if (state.dailyDietPlan && (state.dailyDietPlan.isMock || state.dailyDietPlan.error || state.dailyDietPlan.days)) {
  state.dailyDietPlan = null;
  saveStateToStorage();
}

// Auto-migrate retired models to auto
if (state.geminiModel === 'gemini-1.5-flash' || state.geminiModel === 'gemini-2.0-flash') {
  state.geminiModel = 'auto';
  saveStateToStorage();
}

// ── Helpers ──────────────────────────────────────────────────────────────────
function getLocalDateString(date) {
  const offset = date.getTimezoneOffset();
  const localDate = new Date(date.getTime() - offset * 60 * 1000);
  return localDate.toISOString().split('T')[0];
}

function saveStateToStorage() {
  localStorage.setItem('currentScreen', state.currentScreen);
  localStorage.setItem('hasCompletedOnboarding', JSON.stringify(state.hasCompletedOnboarding));
  localStorage.setItem('dailyCalorieGoal', JSON.stringify(state.dailyCalorieGoal));
  localStorage.setItem('carbsPercent', JSON.stringify(state.carbsPercent));
  localStorage.setItem('proteinPercent', JSON.stringify(state.proteinPercent));
  localStorage.setItem('fatPercent', JSON.stringify(state.fatPercent));
  localStorage.setItem('currentWeightKg', JSON.stringify(state.currentWeightKg));
  localStorage.setItem('targetWeightKg', JSON.stringify(state.targetWeightKg));
  localStorage.setItem('heightCm', JSON.stringify(state.heightCm));
  localStorage.setItem('age', JSON.stringify(state.age));
  localStorage.setItem('isMale', JSON.stringify(state.isMale));
  localStorage.setItem('weightGoal', state.weightGoal);
  localStorage.setItem('aggression', state.aggression);
  
  localStorage.setItem('morningEnabled', JSON.stringify(state.morningEnabled));
  localStorage.setItem('morningTime', state.morningTime);
  localStorage.setItem('afternoonEnabled', JSON.stringify(state.afternoonEnabled));
  localStorage.setItem('afternoonTime', state.afternoonTime);
  localStorage.setItem('eveningEnabled', JSON.stringify(state.eveningEnabled));
  localStorage.setItem('eveningTime', state.eveningTime);
  
  localStorage.setItem('geminiApiKey', state.apiKey);
  localStorage.setItem('geminiModel', state.geminiModel);
  localStorage.setItem('apiProvider', state.apiProvider);
  localStorage.setItem('openRouterApiKey', state.openRouterApiKey);
  localStorage.setItem('openRouterModel', state.openRouterModel);
  localStorage.setItem('foodEntries', JSON.stringify(state.foodEntries));
  localStorage.setItem('exerciseEntries', JSON.stringify(state.exerciseEntries));
  localStorage.setItem('weightEntries', JSON.stringify(state.weightEntries));
  localStorage.setItem('chatMessages', JSON.stringify(state.chatMessages));
  
  // Daily Diet & Chef
  localStorage.setItem('cuisineType', state.cuisineType);
  localStorage.setItem('cookingStyle', state.cookingStyle);
  localStorage.setItem('dailyDietPlan', JSON.stringify(state.dailyDietPlan));
  localStorage.setItem('chefIngredients', state.chefIngredients);
  localStorage.setItem('chefCalorieMax', state.chefCalorieMax);
  localStorage.setItem('chefProteinMin', state.chefProteinMin);
  localStorage.setItem('chefFlavorProfile', state.chefFlavorProfile);
  localStorage.setItem('generatedRecipe', JSON.stringify(state.generatedRecipe));
  localStorage.setItem('expandedPlanDays', JSON.stringify(state.expandedPlanDays));
  localStorage.setItem('isNutritionDropdownOpen', JSON.stringify(state.isNutritionDropdownOpen));
  localStorage.setItem('dietCalorieGoal', JSON.stringify(state.dietCalorieGoal));
  localStorage.setItem('weeklyInsights', JSON.stringify(state.weeklyInsights));
  localStorage.setItem('isWeeklyInsightsOpen', JSON.stringify(state.isWeeklyInsightsOpen));
  localStorage.setItem('lastProgress', JSON.stringify(state.lastProgress));
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container') || (() => {
    const c = document.createElement('div');
    c.id = 'toast-container';
    c.style.cssText = 'position: absolute; bottom: 80px; left: 50%; transform: translateX(-50%); z-index: 10000; display: flex; flex-direction: column; gap: 8px; width: 90%; max-width: 360px; pointer-events: none;';
    const appWrapper = document.getElementById('screen-body') || document.body;
    appWrapper.appendChild(c);
    return c;
  })();

  const toast = document.createElement('div');
  toast.className = `toast-banner ${type}`;
  toast.style.cssText = `
    padding: 10px 14px;
    border-radius: 12px;
    font-size: 0.82rem;
    font-weight: 600;
    color: white;
    box-shadow: var(--shadow-lg);
    display: flex;
    align-items: center;
    gap: 8px;
    background-color: ${type === 'error' ? 'var(--error)' : type === 'success' ? 'var(--success)' : 'var(--primary)'};
    animation: slideUp 0.3s ease forwards;
    pointer-events: auto;
  `;
  
  const icon = type === 'error' ? '⚠️' : type === 'success' ? '✅' : 'ℹ️';
  toast.innerHTML = `<span>${icon}</span><span style="flex:1;">${message}</span>`;
  
  container.appendChild(toast);
  
  setTimeout(() => {
    toast.style.animation = 'slideDown 0.3s ease forwards';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// Initialize theme
document.documentElement.setAttribute('data-theme', state.theme);

// ── Gemini Integration ───────────────────────────────────────────────────────
const SYSTEM_PROMPT = `
You are a conversational health parser. Extract structured food or exercise details and return ONLY a valid raw JSON object. Do not include markdown code fences, extra text, or formatting.

If the input describes food, return:
{"type": "food", "items": [{"name": "Food Item Name", "calories": 250, "protein_g": 20.0, "carbs_g": 30.0, "fat_g": 5.0, "serving_size": "1 bowl (assumed, ~200gms)"}]}

If the input describes exercise, return:
{"type": "exercise", "items": [{"name": "Workout Description", "duration_minutes": 30, "calories_burned": 240}]}

If the input is a greeting, conversational phrase, single word (like "hello", "wow", "test"), garbage input, or does not describe any specific food or exercise, return:
{"type": "unknown", "message": "I couldn't identify any food or exercise in your message. Try saying something like 'I had 3 boiled eggs' or 'ran for 30 minutes'!"}

Rules:
- Estimate realistic numbers based on general science if exact weights are omitted.
- If the user does not specify a food quantity, you MUST estimate and provide a realistic assumed quantity and its estimated weight in grams in the 'serving_size' field, marked with '(assumed, ~Xgms)' (e.g. '1 bowl (assumed, ~180gms)', '1 apple (assumed, ~150gms)'). If they specify it, provide it and estimate the weight (e.g. '3 eggs (~150gms)', '1 glass milk (~240gms)', '200gms chicken').
- Support multiple items in one go (e.g. "2 eggs and toast" -> items array with two objects).
- Return ONLY the exact JSON, with absolutely no packaging or explainers.
- Do NOT output empty items arrays. If no food or exercise is found, you MUST return the 'unknown' JSON type instead.
`;

async function callGeminiAPI(model, userText) {
  return fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${state.apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      contents: [{ parts: [{ text: `${SYSTEM_PROMPT}\n\nUser input: ${userText}` }] }],
      generationConfig: {
        temperature: 0.1,
        maxOutputTokens: 1024,
        responseMimeType: "application/json"
      }
    })
  });
}

async function handleApiError(response) {
  let detail = `HTTP ${response.status}`;
  try {
    const errJson = await response.json();
    if (errJson.error && errJson.error.message) {
      detail = errJson.error.message;
    }
  } catch (e) {}
  return detail;
}

function getModelExecutionList(preferredModel) {
  // Available models ordered from cheapest (least tokens/cost) to most capable/expensive
  const cheapestToHighest = [
    'gemini-3.1-flash-lite',
    'gemini-2.5-flash',
    'gemini-3.5-flash',
    'gemini-2.5-pro',
    'gemini-3.1-pro'
  ];

  const bestModel = 'gemini-3.1-pro';
  const list = [];

  if (!preferredModel || preferredModel === 'auto') {
    // 1. By default the app must access the best model
    list.push(bestModel);
    // 2. If not, then app must access cheapest, then higher and so on
    cheapestToHighest.forEach(m => {
      if (m !== bestModel) {
        list.push(m);
      }
    });
  } else {
    // 3. User selected a model -> try it with priority
    list.push(preferredModel);
    // If not available, then app must access cheapest, then higher and so on
    cheapestToHighest.forEach(m => {
      if (m !== preferredModel) {
        list.push(m);
      }
    });
  }
  return list;
}

async function parseInputWithGemini(userText) {
  if (state.apiProvider === 'openrouter') {
    if (!state.openRouterApiKey) {
      // Simulated mock parser if no API Key entered
      return simulateMockParsing(userText);
    }
    return parseInputWithOpenRouter(userText, state.openRouterApiKey, state.openRouterModel, SYSTEM_PROMPT, cleanAndParseJSON);
  }

  if (!state.apiKey) {
    // Simulated mock parser if no API Key entered
    return simulateMockParsing(userText);
  }

  const modelList = getModelExecutionList(state.geminiModel);
  let lastError = null;

  for (const model of modelList) {
    try {
      console.info(`Attempting chatbot input parsing with model: ${model}`);
      const response = await callGeminiAPI(model, userText);
      
      if (response.ok) {
        const data = await response.json();
        const rawJsonStr = data.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || "";
        const parsed = cleanAndParseJSON(rawJsonStr);
        parsed.modelUsed = model;
        console.info(`Model ${model} chatbot parsing succeeded!`);
        return parsed;
      } else {
        const errorMsg = await handleApiError(response);
        console.warn(`Model ${model} chatbot parsing failed: ${errorMsg}`);
        lastError = new Error(`Model ${model}: ${errorMsg}`);
      }
    } catch (err) {
      console.warn(`Model ${model} chatbot parsing caught error: ${err.message}`);
      lastError = err;
    }
  }

  console.error("All Gemini models failed chatbot parsing:", lastError);
  return { type: "error", message: lastError ? lastError.message : "AI connection failed" };
}

function simulateMockParsing(text) {
  const lower = text.toLowerCase();
  // Extract number if present
  const countMatch = text.match(/\d+/);
  const hasCount = !!countMatch;
  const count = hasCount ? parseInt(countMatch[0]) : 1;

  if (lower.includes('egg') || lower.includes('chicken') || lower.includes('rice') || lower.includes('banana') || lower.includes('shake') || lower.includes('toast') || lower.includes('apple') || lower.includes('salad') || lower.includes('coffee') || lower.includes('pizza') || lower.includes('burger') || lower.includes('milk') || lower.includes('cookie') || lower.includes('sandwich')) {
    const items = [];
    if (lower.includes('egg')) {
      const q = hasCount ? count : 2;
      const label = hasCount ? `${q} egg${q > 1 ? 's' : ''} (~${q * 50}gms)` : `2 eggs (assumed, ~100gms)`;
      items.push({ name: `Boiled/Scrambled Eggs`, calories: q * 70, protein_g: q * 6, carbs_g: q * 0.5, fat_g: q * 5, serving_size: label });
    }
    if (lower.includes('chicken')) {
      const grams = hasCount ? ((count > 20) ? count : count * 150) : 150;
      const label = hasCount ? `${grams}gms` : `1 portion (assumed, ~150gms)`;
      items.push({ name: `Grilled Chicken Breast`, calories: Math.round(grams * 1.45), protein_g: Math.round(grams * 0.27), carbs_g: 0, fat_g: Math.round(grams * 0.03), serving_size: label });
    }
    if (lower.includes('rice')) {
      const cups = hasCount ? ((count <= 10) ? count : Math.round(count / 150)) : 1;
      const label = hasCount ? `${cups} cup${cups > 1 ? 's' : ''} (~${cups * 150}gms)` : `1 plate (assumed, ~150gms)`;
      items.push({ name: `White Cooked Rice`, calories: cups * 200, protein_g: cups * 4, carbs_g: cups * 44, fat_g: cups * 0.5, serving_size: label });
    }
    if (lower.includes('banana')) {
      const q = count;
      const label = hasCount ? `${q} banana${q > 1 ? 's' : ''} (~${q * 120}gms)` : `1 banana (assumed, ~120gms)`;
      items.push({ name: `Banana`, calories: q * 105, protein_g: q * 1.3, carbs_g: q * 27, fat_g: q * 0.3, serving_size: label });
    }
    if (lower.includes('toast')) {
      const q = hasCount ? count : 2;
      const label = hasCount ? `${q} slice${q > 1 ? 's' : ''} (~${q * 30}gms)` : `2 slices (assumed, ~60gms)`;
      items.push({ name: `Whole Wheat Toast`, calories: q * 80, protein_g: q * 3, carbs_g: q * 15, fat_g: q * 1, serving_size: label });
    }
    if (lower.includes('shake')) {
      const q = count;
      const label = hasCount ? `${q} shake${q > 1 ? 's' : ''} (~${q * 300}gms)` : `1 glass (assumed, ~300gms)`;
      items.push({ name: `Whey Protein Shake`, calories: q * 160, protein_g: q * 25, carbs_g: q * 3, fat_g: q * 2, serving_size: label });
    }
    if (lower.includes('apple')) {
      const q = count;
      const label = hasCount ? `${q} apple${q > 1 ? 's' : ''} (~${q * 150}gms)` : `1 apple (assumed, ~150gms)`;
      items.push({ name: `Apple`, calories: q * 95, protein_g: q * 0.5, carbs_g: q * 25, fat_g: q * 0.3, serving_size: label });
    }
    if (lower.includes('coffee')) {
      const q = count;
      const label = hasCount ? `${q} cup${q > 1 ? 's' : ''} (~${q * 240}gms)` : `1 cup (assumed, ~240gms)`;
      items.push({ name: `Black Coffee`, calories: q * 5, protein_g: q * 0.2, carbs_g: 0, fat_g: 0, serving_size: label });
    }
    if (lower.includes('salad')) {
      const q = count;
      const label = hasCount ? `${q} serving${q > 1 ? 's' : ''} (assumed, ~150gms)` : `1 bowl (assumed, ~150gms)`;
      items.push({ name: `Garden Salad`, calories: q * 120, protein_g: q * 2, carbs_g: q * 10, fat_g: q * 8, serving_size: label });
    }
    if (lower.includes('pizza')) {
      const q = hasCount ? count : 2;
      const label = hasCount ? `${q} slice${q > 1 ? 's' : ''} (~${q * 100}gms)` : `2 slices (assumed, ~200gms)`;
      items.push({ name: `Pizza`, calories: q * 285, protein_g: q * 12, carbs_g: q * 36, fat_g: q * 10, serving_size: label });
    }
    if (lower.includes('burger')) {
      const q = count;
      const label = hasCount ? `${q} burger${q > 1 ? 's' : ''} (~${q * 220}gms)` : `1 burger (assumed, ~220gms)`;
      items.push({ name: `Beef Burger`, calories: q * 350, protein_g: q * 18, carbs_g: q * 40, fat_g: q * 14, serving_size: label });
    }
    if (lower.includes('milk')) {
      const q = count;
      const label = hasCount ? `${q} glass${q > 1 ? 'es' : ''} (assumed, ~240gms)` : `1 glass (assumed, ~240gms)`;
      items.push({ name: `Whole Milk`, calories: q * 150, protein_g: q * 8, carbs_g: q * 12, fat_g: q * 8, serving_size: label });
    }
    if (lower.includes('cookie')) {
      const q = hasCount ? count : 2;
      const label = hasCount ? `${q} cookie${q > 1 ? 's' : ''} (~${q * 30}gms)` : `2 cookies (assumed, ~60gms)`;
      items.push({ name: `Chocolate Chip Cookie`, calories: q * 140, protein_g: q * 2, carbs_g: q * 20, fat_g: q * 7, serving_size: label });
    }
    if (lower.includes('sandwich')) {
      const q = count;
      const label = hasCount ? `${q} sandwich${q > 1 ? 's' : ''} (~${q * 200}gms)` : `1 sandwich (assumed, ~200gms)`;
      items.push({ name: `Turkey Sandwich`, calories: q * 320, protein_g: q * 18, carbs_g: q * 38, fat_g: q * 9, serving_size: label });
    }
    
    if (items.length === 0) {
      items.push({ name: text.trim(), calories: 250 * count, protein_g: 10 * count, carbs_g: 30 * count, fat_g: 8 * count, serving_size: `1 serving (assumed, ~150gms)` });
    }
    return { type: "food", items, modelUsed: "mock-parser" };
  }
  
  if (lower.includes('run') || lower.includes('walk') || lower.includes('jog') || lower.includes('swim') || lower.includes('workout') || lower.includes('cycle') || lower.includes('gym') || lower.includes('cardio') || lower.includes('pushup') || lower.includes('plank')) {
    const items = [];
    const minutes = (count > 5) ? count : 30; // Use count if it specifies minutes, default to 30 mins
    
    if (lower.includes('run') || lower.includes('jog')) {
      items.push({ name: "Jogging / Running", duration_minutes: minutes, calories_burned: Math.round(minutes * 10.6) });
    }
    if (lower.includes('walk')) {
      items.push({ name: "Power Walking", duration_minutes: minutes, calories_burned: Math.round(minutes * 4.0) });
    }
    if (lower.includes('swim')) {
      items.push({ name: "Swimming", duration_minutes: minutes, calories_burned: Math.round(minutes * 9.3) });
    }
    if (lower.includes('gym') || lower.includes('workout')) {
      items.push({ name: "Resistance Training", duration_minutes: minutes, calories_burned: Math.round(minutes * 5.0) });
    }
    if (lower.includes('cycle')) {
      items.push({ name: "Cycling", duration_minutes: minutes, calories_burned: Math.round(minutes * 7.5) });
    }
    if (lower.includes('pushup') || lower.includes('plank')) {
      items.push({ name: "Core Exercise", duration_minutes: minutes, calories_burned: Math.round(minutes * 4.5) });
    }
    
    if (items.length === 0) {
      items.push({ name: text.trim(), duration_minutes: minutes, calories_burned: minutes * 7 });
    }
    return { type: "exercise", items, modelUsed: "mock-parser" };
  }

  // Simple check for greetings, conversational filler, or single garbage words
  const garbageWords = ['hello', 'hi', 'hey', 'yo', 'wow', 'greetings', 'test', 'okay', 'ok', 'yes', 'no', 'thanks', 'thank you', 'cool', 'nice', 'awesome', 'great'];
  if (garbageWords.includes(lower.trim())) {
    return { type: "unknown", message: "I couldn't identify any specific food or exercise in your message. Try saying something like 'I had 3 scrambled eggs for breakfast' or 'walked for 45 minutes'!", modelUsed: "mock-parser" };
  }

  // Fallback instead of failing: if they didn't match any keywords but wrote a phrase, let's treat it as a custom food log dynamically scaled!
  if (text.trim().length > 3) {
    return {
      type: "food",
      modelUsed: "mock-parser",
      items: [{
        name: text.trim(),
        calories: 150 * count,
        protein_g: 8 * count,
        carbs_g: 20 * count,
        fat_g: 5 * count,
        serving_size: `1 serving (assumed, ~150g)`
      }]
    };
  }

  return { type: "unknown", message: "I couldn't identify any specific food or exercise in your message. Try saying something like 'I had 3 boiled eggs' or 'ran for 30 minutes'!", modelUsed: "mock-parser" };
}

// ── Magnified Modal Content Renderer ─────────────────────────────────────────
function renderMagnifiedModalContent() {
  const msgId = state.editingMessageId;
  const isFood = state.editingMessageType === 'food';
  
  let rowsHtml = '';
  if (isFood) {
    const foods = state.foodEntries.filter(f => f.messageId === msgId);
    rowsHtml = foods.map((f, idx) => `
      <div class="modal-edit-row" data-id="${f.id}" data-type="food" style="margin-bottom: 16px; border-bottom: 1px dashed var(--border-visible); padding-bottom: 12px; display: flex; flex-direction: column; gap: 8px;">
        <div style="font-weight: 700; font-size: 0.9rem; color: var(--primary); display: flex; justify-content: space-between; align-items: center;">
          <span>Item #${idx + 1}</span>
          <button class="btn-delete-row" data-id="${f.id}" data-type="food" title="Delete this item" style="background: none; border: none; color: var(--error); cursor: pointer; display: flex; align-items: center; justify-content: center; padding: 4px;">
            <i data-lucide="trash-2" style="width: 16px; height: 16px;"></i>
          </button>
        </div>
        <div style="display: grid; grid-template-columns: 1fr; gap: 8px;">
          <div>
            <label style="font-size: 0.75rem; font-weight: 600; opacity: 0.8; margin-bottom: 4px; display: block;">Food Name</label>
            <input type="text" class="edit-food-name" value="${f.name}" style="width: 100%; padding: 8px 12px; border-radius: 8px; border: 1.5px solid var(--border-visible); background: var(--surface); color: var(--on-surface); font-size: 0.9rem;" />
          </div>
        </div>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
          <div>
            <label style="font-size: 0.75rem; font-weight: 600; opacity: 0.8; margin-bottom: 4px; display: block;">Serving Size / Weight</label>
            <input type="text" class="edit-food-serving" value="${f.servingSize || ''}" style="width: 100%; padding: 8px 12px; border-radius: 8px; border: 1.5px solid var(--border-visible); background: var(--surface); color: var(--on-surface); font-size: 0.9rem;" />
          </div>
          <div>
            <label style="font-size: 0.75rem; font-weight: 600; opacity: 0.8; margin-bottom: 4px; display: block;">Calories (kcal)</label>
            <input type="number" class="edit-food-calories" value="${f.calories}" style="width: 100%; padding: 8px 12px; border-radius: 8px; border: 1.5px solid var(--border-visible); background: var(--surface); color: var(--on-surface); font-size: 0.9rem;" />
          </div>
        </div>
        <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;">
          <div>
            <label style="font-size: 0.75rem; font-weight: 600; opacity: 0.8; margin-bottom: 4px; display: block;">Protein (g)</label>
            <input type="number" class="edit-food-protein" value="${f.proteinG}" style="width: 100%; padding: 8px 12px; border-radius: 8px; border: 1.5px solid var(--border-visible); background: var(--surface); color: var(--on-surface); font-size: 0.9rem;" />
          </div>
          <div>
            <label style="font-size: 0.75rem; font-weight: 600; opacity: 0.8; margin-bottom: 4px; display: block;">Carbs (g)</label>
            <input type="number" class="edit-food-carbs" value="${f.carbsG}" style="width: 100%; padding: 8px 12px; border-radius: 8px; border: 1.5px solid var(--border-visible); background: var(--surface); color: var(--on-surface); font-size: 0.9rem;" />
          </div>
          <div>
            <label style="font-size: 0.75rem; font-weight: 600; opacity: 0.8; margin-bottom: 4px; display: block;">Fat (g)</label>
            <input type="number" class="edit-food-fat" value="${f.fatG}" style="width: 100%; padding: 8px 12px; border-radius: 8px; border: 1.5px solid var(--border-visible); background: var(--surface); color: var(--on-surface); font-size: 0.9rem;" />
          </div>
        </div>
      </div>
    `).join('');
  } else {
    const exercises = state.exerciseEntries.filter(e => e.messageId === msgId);
    rowsHtml = exercises.map((e, idx) => `
      <div class="modal-edit-row" data-id="${e.id}" data-type="exercise" style="margin-bottom: 16px; border-bottom: 1px dashed var(--border-visible); padding-bottom: 12px; display: flex; flex-direction: column; gap: 8px;">
        <div style="font-weight: 700; font-size: 0.9rem; color: var(--color-exercise); display: flex; justify-content: space-between; align-items: center;">
          <span>Workout #${idx + 1}</span>
          <button class="btn-delete-row" data-id="${e.id}" data-type="exercise" title="Delete this workout" style="background: none; border: none; color: var(--error); cursor: pointer; display: flex; align-items: center; justify-content: center; padding: 4px;">
            <i data-lucide="trash-2" style="width: 16px; height: 16px;"></i>
          </button>
        </div>
        <div style="display: grid; grid-template-columns: 1fr; gap: 8px;">
          <div>
            <label style="font-size: 0.75rem; font-weight: 600; opacity: 0.8; margin-bottom: 4px; display: block;">Workout Name</label>
            <input type="text" class="edit-exercise-name" value="${e.name}" style="width: 100%; padding: 8px 12px; border-radius: 8px; border: 1.5px solid var(--border-visible); background: var(--surface); color: var(--on-surface); font-size: 0.9rem;" />
          </div>
        </div>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
          <div>
            <label style="font-size: 0.75rem; font-weight: 600; opacity: 0.8; margin-bottom: 4px; display: block;">Duration (mins)</label>
            <input type="number" class="edit-exercise-duration" value="${e.duration}" style="width: 100%; padding: 8px 12px; border-radius: 8px; border: 1.5px solid var(--border-visible); background: var(--surface); color: var(--on-surface); font-size: 0.9rem;" />
          </div>
          <div>
            <label style="font-size: 0.75rem; font-weight: 600; opacity: 0.8; margin-bottom: 4px; display: block;">Calories Burned (kcal)</label>
            <input type="number" class="edit-exercise-calories" value="${e.caloriesBurned}" style="width: 100%; padding: 8px 12px; border-radius: 8px; border: 1.5px solid var(--border-visible); background: var(--surface); color: var(--on-surface); font-size: 0.9rem;" />
          </div>
        </div>
      </div>
    `).join('');
  }

  return `
    <div class="magnified-modal-content">
      <h3 style="font-size: 1.15rem; font-weight: 700; margin-bottom: 4px; display: flex; align-items: center; gap: 8px; font-family: var(--font-display);">
        <i data-lucide="${isFood ? 'utensils' : 'dumbbell'}" style="color: var(--primary);"></i>
        Edit Logged Data
      </h3>
      <p style="font-size: 0.75rem; opacity: 0.6; margin-bottom: 12px;">Modify names, estimated servings, or nutrient values below.</p>
      
      <div style="flex: 1; overflow-y: auto; max-height: 380px; padding-right: 6px;">
        ${rowsHtml}
      </div>
      
      <div class="modal-actions" style="display: flex; gap: 12px; justify-content: flex-end; margin-top: 16px; border-top: 1px solid var(--border); padding-top: 12px;">
        <button class="btn" id="btn-cancel-modal" style="padding: 10px 18px; border-radius: 12px; background: var(--surface-variant); border: 1px solid var(--border-visible); color: var(--on-surface); font-weight: 600; font-size: 0.9rem; cursor: pointer; transition: all 0.2s ease;">
          Cancel
        </button>
        <button class="btn btn-primary" id="btn-save-modal" style="padding: 10px 20px; border-radius: 12px; background: var(--primary); color: var(--on-primary); font-weight: 600; font-size: 0.9rem; cursor: pointer; border: none; transition: all 0.2s ease; display: flex; align-items: center; gap: 6px;">
          <i data-lucide="check" style="width: 16px; height: 16px;"></i> Save Changes
        </button>
      </div>
    </div>
  `;
}

// ── Application Core Render Flow ─────────────────────────────────────────────
function appLayoutTemplate() {
  return `
    <!-- Onboarding overlay -->
    ${!state.hasCompletedOnboarding ? onboardingOverlayTemplate() : ''}

    <!-- Collapsible sidebar backdrop -->
    <div id="sidebar-backdrop" class="sidebar-backdrop ${state.isSidebarOpen ? 'active' : ''}"></div>

    <!-- Collapsible sidebar -->
    <aside id="sidebar" class="${state.isSidebarOpen ? 'drawer-open' : ''}">
      <div class="brand">
        <i data-lucide="leaf"></i>
        <span>Daywise</span>
      </div>
      <ul class="drawer-menu">
        <li class="drawer-item ${state.currentScreen === 'chat' ? 'active' : ''}" data-target="chat">
          <a href="#"><i data-lucide="message-square"></i>Track Chat</a>
        </li>
        <li class="drawer-item ${state.currentScreen === 'diet_planner' ? 'active' : ''}" data-target="diet_planner">
          <a href="#"><i data-lucide="calendar-days"></i>AI Daily Diet Planner</a>
        </li>
        <li class="drawer-item ${state.currentScreen === 'chef_recipe' ? 'active' : ''}" data-target="chef_recipe">
          <a href="#"><i data-lucide="cooking-pot"></i>AI Chef Recipes</a>
        </li>
        <li class="drawer-item ${state.currentScreen === 'weekly_summary' ? 'active' : ''}" data-target="weekly_summary">
          <a href="#"><i data-lucide="bar-chart-2"></i>Weekly Summary</a>
        </li>
        <li class="drawer-item ${state.currentScreen === 'weight_tracker' ? 'active' : ''}" data-target="weight_tracker">
          <a href="#"><i data-lucide="scale"></i>Weight Tracker</a>
        </li>
        <li class="drawer-item ${state.currentScreen === 'daily_goals' ? 'active' : ''}" data-target="daily_goals">
          <a href="#"><i data-lucide="flag"></i>Daily Goals</a>
        </li>
        <li class="drawer-item ${state.currentScreen === 'reminders' ? 'active' : ''}" data-target="reminders">
          <a href="#"><i data-lucide="bell"></i>Reminders</a>
        </li>
        <li class="drawer-item ${state.currentScreen === 'settings' ? 'active' : ''}" data-target="settings">
          <a href="#"><i data-lucide="settings"></i>Settings</a>
        </li>
      </ul>
      <div class="drawer-footer">
        <div style="font-size: 0.8rem; opacity: 0.6; text-align: center;">Daywise Web Preview v1.0</div>
      </div>
    </aside>

    <!-- Main Container -->
    <main>
      <header>
        <div class="header-left">
          <button class="menu-toggle" id="btn-toggle-sidebar">
            <i data-lucide="menu"></i>
          </button>
          <h2 style="font-size: 1.25rem;">${getScreenTitle()}</h2>
        </div>
        <div class="header-right">
          <button class="icon-btn" id="btn-theme-toggle" title="Toggle Light/Dark Theme">
            <i data-lucide="${state.theme === 'light' ? 'moon' : 'sun'}"></i>
          </button>
        </div>
      </header>
      
      <!-- Screen content wrapper -->
      <div class="screen-content" id="screen-body">
        ${renderScreenContent()}
      </div>
    </main>

    <!-- Magnified Nutrient Table Modal Overlay -->
    <div id="magnified-modal" class="magnified-modal-overlay ${state.editingMessageId ? 'active' : ''}">
      ${state.editingMessageId ? renderMagnifiedModalContent() : ''}
    </div>
  `;
}

function getScreenTitle() {
  switch (state.currentScreen) {
    case 'chat': return 'Health Tracker Chat';
    case 'diet_planner': return 'AI Daily Diet Planner';
    case 'chef_recipe': return 'AI Chef Recipes';
    case 'weekly_summary': return 'Weekly Report';
    case 'weight_tracker': return 'Weight Tracker';
    case 'daily_goals': return 'Daily Targets';
    case 'reminders': return 'Log Reminders';
    case 'settings': return 'App Configurations';
    default: return 'Daywise';
  }
}

function renderScreenContent() {
  switch (state.currentScreen) {
    case 'chat': return renderChatScreen();
    case 'diet_planner': return renderDietPlannerScreen();
    case 'chef_recipe': return renderChefRecipeScreen();
    case 'weekly_summary': return renderWeeklyScreen();
    case 'weight_tracker': return renderWeightScreen();
    case 'daily_goals': return renderGoalsScreen();
    case 'reminders': return renderRemindersScreen();
    case 'settings': return renderSettingsScreen();
    default: return '';
  }
}

// ── Onboarding Screen ────────────────────────────────────────────────────────
function onboardingOverlayTemplate() {
  const step = state.onboardingStep;
  const data = state.onboardingData;
  const progressPercent = (step / 5) * 100;

  // Mifflin-St Jeor Calculation BMR & target calories
  const weight = parseFloat(data.weight) || 0;
  const height = parseFloat(data.height) || 0;
  const age = parseInt(data.age) || 0;
  
  let bmr = 0;
  if (weight > 0 && height > 0 && age > 0) {
    const base = 10 * weight + 6.25 * height - 5 * age;
    bmr = data.gender === 'Male' ? Math.round(base + 5) : Math.round(base - 161);
  }
  const tdee = Math.round(bmr * 1.30); // Sedentary/Light active baseline
  
  let calorieAdjustment = 0;
  if (data.goal === 'Lose') {
    calorieAdjustment = data.aggression === 'Relaxed' ? -250 : data.aggression === 'Moderate' ? -500 : -750;
  } else if (data.goal === 'Gain') {
    calorieAdjustment = data.aggression === 'Relaxed' ? 250 : data.aggression === 'Moderate' ? 500 : 750;
  }
  const targetCalories = Math.max(1200, tdee + calorieAdjustment);

  const proteinG = Math.round((targetCalories * 0.25) / 4);
  const carbsG = Math.round((targetCalories * 0.50) / 4);
  const fatG = Math.round((targetCalories * 0.25) / 9);

  return `
    <div class="onboarding-overlay">
      <div class="onboarding-box">
        <div class="onboarding-progress">
          <div class="onboarding-progress-fill" style="width: ${progressPercent}%"></div>
        </div>

        ${step === 0 ? `
          <div class="onboarding-welcome">
            <i data-lucide="leaf"></i>
            <h2>Welcome to Daywise</h2>
            <p>Let's set up your personalized health and nutrition targets with a quick onboarding quiz!</p>
          </div>
        ` : ''}

        ${step === 1 ? `
          <div class="onboarding-step">
            <h3>Tell us about yourself</h3>
            <p style="font-size: 0.85rem; color: var(--on-surface-variant)">Used for BMI and Calorie target recommendations</p>
            
            <div class="form-group">
              <label>Biological Gender</label>
              <div class="gender-select">
                <div class="selectable-card ${data.gender === 'Male' ? 'selected' : ''}" data-gender="Male">
                  <span class="card-title">Male</span>
                </div>
                <div class="selectable-card ${data.gender === 'Female' ? 'selected' : ''}" data-gender="Female">
                  <span class="card-title">Female</span>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label>Age (years)</label>
              <input type="number" id="ob-age" class="input-style" placeholder="e.g. 25" value="${data.age}" />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label>Height (cm)</label>
                <input type="number" id="ob-height" class="input-style" placeholder="e.g. 175" value="${data.height}" />
              </div>
              <div class="form-group">
                <label>Current Weight (kg)</label>
                <input type="number" id="ob-weight" class="input-style" placeholder="e.g. 78" value="${data.weight}" />
              </div>
            </div>
          </div>
        ` : ''}

        ${step === 2 ? `
          <div class="onboarding-step">
            <h3>What's your primary goal?</h3>
            <div class="goal-select">
              <div class="selectable-card ${data.goal === 'Lose' ? 'selected' : ''}" data-goal="Lose">
                <span class="card-title">🔻 Lose Weight</span>
                <span class="card-desc">Achieve safe, gradual weight reduction</span>
              </div>
              <div class="selectable-card ${data.goal === 'Maintain' ? 'selected' : ''}" data-goal="Maintain">
                <span class="card-title">⚖️ Maintain Weight</span>
                <span class="card-desc">Keep current weight and body composition</span>
              </div>
              <div class="selectable-card ${data.goal === 'Gain' ? 'selected' : ''}" data-goal="Gain">
                <span class="card-title">🔺 Gain Weight</span>
                <span class="card-desc">Support clean muscle and mass gain</span>
              </div>
            </div>
          </div>
        ` : ''}

        ${step === 3 ? `
          <div class="onboarding-step">
            <h3>How aggressive is your goal?</h3>
            <div class="aggression-select">
              <div class="selectable-card ${data.aggression === 'Relaxed' ? 'selected' : ''}" data-aggression="Relaxed">
                <span class="card-title">Relaxed</span>
                <span class="card-desc">0.25 kg weight adjustment per week</span>
              </div>
              <div class="selectable-card ${data.aggression === 'Moderate' ? 'selected' : ''}" data-aggression="Moderate">
                <span class="card-title">Moderate</span>
                <span class="card-desc">0.50 kg weight adjustment per week</span>
              </div>
              <div class="selectable-card ${data.aggression === 'Aggressive' ? 'selected' : ''}" data-aggression="Aggressive">
                <span class="card-title">Aggressive</span>
                <span class="card-desc">0.75 kg weight adjustment per week</span>
              </div>
            </div>
          </div>
        ` : ''}

        ${step === 4 ? `
          <div class="onboarding-step">
            <h3>Set your target weight</h3>
            
            <div class="bmi-badge-box">
              <div class="bmi-row"><span>Current Weight</span> <span>${weight} kg</span></div>
              <div class="bmi-row bold"><span>Current BMI</span> <span>${weight && height ? (weight / ((height/100)*(height/100))).toFixed(1) : '—'}</span></div>
            </div>

            <div class="form-group">
              <label>Target weight (kg)</label>
              <input type="number" id="ob-target" class="input-style" placeholder="e.g. 70" value="${data.targetWeight}" />
            </div>
          </div>
        ` : ''}

        ${step === 5 ? `
          <div class="onboarding-step">
            <h3>Your Personalized Plan</h3>
            <p style="font-size: 0.85rem; color: var(--on-surface-variant)">Calculated via metabolic equations tailored to you</p>
            
            <div class="results-calorie-card">
              <i data-lucide="flame"></i>
              <div class="results-calorie-val">${targetCalories}</div>
              <div style="font-size: 0.9rem; font-weight: 600;">Daily Calorie Goal (kcal)</div>
            </div>

            <div class="bmi-badge-box" style="background-color: var(--surface-variant)">
              <div class="bmi-row bold" style="color: var(--primary);"><span>🥩 Protein Goal</span> <span>${proteinG}g (25%)</span></div>
              <div class="bmi-row bold" style="color: var(--color-carbs);"><span>🍞 Carbs Goal</span> <span>${carbsG}g (50%)</span></div>
              <div class="bmi-row bold" style="color: var(--color-fat);"><span>🥑 Fat Goal</span> <span>${fatG}g (25%)</span></div>
            </div>

            <div style="font-size: 0.85rem; opacity: 0.7; text-align: center;">
              BMR: ${bmr} kcal &nbsp;·&nbsp; TDEE: ${tdee} kcal
            </div>
          </div>
        ` : ''}

        <div class="onboarding-nav">
          ${step > 0 ? `
            <button class="btn-style secondary" id="btn-ob-prev">Back</button>
          ` : ''}
          <button class="btn-style primary" id="btn-ob-next">
            ${step === 0 ? 'Get Started' : step === 5 ? 'Start Tracking' : 'Continue'}
          </button>
        </div>
      </div>
    </div>
  `;
}


// ── Chat Screen UI ───────────────────────────────────────────────────────────
function renderChatScreen() {
  const summary = getDailyNutritionSummary(state.selectedDateStr);
  const activeDate = parseLocalDate(state.selectedDateStr);
  
  // Calculate goals based on percentages
  const carbsGoalG = Math.round((state.dailyCalorieGoal * state.carbsPercent / 100) / 4);
  const proteinGoalG = Math.round((state.dailyCalorieGoal * state.proteinPercent / 100) / 4);
  const fatGoalG = Math.round((state.dailyCalorieGoal * state.fatPercent / 100) / 9);

  let ringProgress = state.dailyCalorieGoal > 0 ? (summary.totalCalories / state.dailyCalorieGoal) : 0;
  let ringColor = 'var(--color-calories)';
  let trackColor = 'rgba(235, 94, 40, 0.08)'; // based on calories
  let numText = summary.totalCalories;
  let goalText = `/ ${state.dailyCalorieGoal} kcal`;
  let leftText = `${Math.max(0, state.dailyCalorieGoal - summary.totalCalories + summary.exerciseCalories)} left`;

  if (state.selectedMacroView === 'protein') {
    ringProgress = proteinGoalG > 0 ? (summary.totalProtein / proteinGoalG) : 0;
    ringColor = 'var(--color-protein)';
    trackColor = 'rgba(74, 144, 217, 0.08)';
    numText = `${Math.round(summary.totalProtein)}g`;
    goalText = `/ ${proteinGoalG}g`;
    leftText = `${Math.max(0, proteinGoalG - Math.round(summary.totalProtein))}g left`;
  } else if (state.selectedMacroView === 'carbs') {
    ringProgress = carbsGoalG > 0 ? (summary.totalCarbs / carbsGoalG) : 0;
    ringColor = 'var(--color-carbs)';
    trackColor = 'rgba(255, 179, 71, 0.08)';
    numText = `${Math.round(summary.totalCarbs)}g`;
    goalText = `/ ${carbsGoalG}g`;
    leftText = `${Math.max(0, carbsGoalG - Math.round(summary.totalCarbs))}g left`;
  } else if (state.selectedMacroView === 'fat') {
    ringProgress = fatGoalG > 0 ? (summary.totalFat / fatGoalG) : 0;
    ringColor = 'var(--color-fat)';
    trackColor = 'rgba(155, 89, 182, 0.08)';
    numText = `${Math.round(summary.totalFat)}g`;
    goalText = `/ ${fatGoalG}g`;
    leftText = `${Math.max(0, fatGoalG - Math.round(summary.totalFat))}g left`;
  }

  ringProgress = parseFloat(ringProgress);
  const radius = 50;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (Math.min(1, ringProgress) * circumference);

  const activeMessages = state.chatMessages.filter(m => m.dateStr === state.selectedDateStr);
  const activeFoods = state.foodEntries.filter(f => f.dateStr === state.selectedDateStr);
  const activeExercises = state.exerciseEntries.filter(e => e.dateStr === state.selectedDateStr);

  const isCalorieActive = state.selectedMacroView === 'calories';
  const ringActiveStyle = isCalorieActive ? 'background-color: var(--surface-variant);' : '';
  const consumed = summary.totalCalories;
  const goal = state.dailyCalorieGoal;
  const caloriePercent = goal > 0 ? Math.min(100, Math.round((consumed / goal) * 100)) : 0;

  const initialCaloriePercent = state.lastProgress && state.lastProgress.caloriePercent !== undefined ? state.lastProgress.caloriePercent : 0;
  const initialOffset = state.lastProgress && state.lastProgress.offset !== undefined ? state.lastProgress.offset : circumference;

  return `
    <div class="chat-container">
      <div class="chat-header-area">
        <!-- Calendar strip selector -->
        ${renderCalendarStrip(activeDate)}

        <!-- Calorie YouTube Progress Bar Dropdown Trigger -->
        <div class="calorie-progress-container ${state.isNutritionDropdownOpen ? 'open' : ''}" id="btn-toggle-nutrition-dropdown" title="Toggle detailed nutrition metrics">
          <div class="calorie-progress-left">
            <span class="progress-label">Calorie Tracker</span>
            <span class="progress-subtext">${caloriePercent}% of daily budget</span>
          </div>
          <div class="calorie-progress-center">
            <div class="yt-progress-track">
              <div class="yt-progress-fill" data-target-percent="${caloriePercent}" style="width: ${initialCaloriePercent}%; background-color: var(--color-calories);">
                <div class="yt-progress-scrubber"></div>
              </div>
            </div>
          </div>
          <div class="calorie-progress-right">
            <span class="progress-values"><b>${consumed}</b> / ${goal} kcal</span>
            <i data-lucide="chevron-down" class="dropdown-arrow"></i>
          </div>
        </div>

        <!-- Dashboard stats summary (collapsible) -->
        <div class="overview-grid ${state.isNutritionDropdownOpen ? 'active' : ''}">
          <div class="calorie-ring-box macro-clickable" data-macro="calories" style="cursor: pointer; padding: 6px; border-radius: 50%; transition: all 0.2s ease; ${ringActiveStyle}">
            <svg viewBox="0 0 120 120">
              <circle class="track" cx="60" cy="60" r="50" style="stroke: ${trackColor}"></circle>
              <circle class="fill" cx="60" cy="60" r="50" stroke-dasharray="${circumference}" stroke-dashoffset="${initialOffset}" style="stroke: ${ringColor}" data-target-offset="${offset}"></circle>
            </svg>
            <div class="calorie-ring-center">
              <span class="ring-num">${numText}</span>
              <span class="ring-goal">${goalText}</span>
              <span class="ring-left" style="color: ${ringColor};">${leftText}</span>
            </div>
          </div>

          <div class="macro-bars">
            ${renderMacroBar("Protein", summary.totalProtein, proteinGoalG, "g", "var(--color-protein)")}
            ${renderMacroBar("Carbs", summary.totalCarbs, carbsGoalG, "g", "var(--color-carbs)")}
            ${renderMacroBar("Fat", summary.totalFat, fatGoalG, "g", "var(--color-fat)")}
          </div>

          ${summary.exerciseCalories > 0 ? `
            <div class="exercise-summary">
              <i data-lucide="dumbbell"></i>
              <span>${summary.exerciseCalories} kcal burned from workouts</span>
            </div>
          ` : ''}
        </div>
      </div>

      <!-- Messages View -->
      <div class="chat-messages" id="chat-scroller">
        ${activeMessages.length === 0 ? `
          <div style="flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; opacity: 0.5; text-align: center; gap: 8px;">
            <i data-lucide="message-square" style="width: 48px; height: 48px;"></i>
            <p>No tracking logs for today. Just talk to the board to log breakfasts, dinners, or runs!</p>
          </div>
        ` : activeMessages.map(m => {
            if (m.isUser) {
              return `
                <div class="chat-bubble-row user">
                  <div class="chat-bubble">
                    <div>${m.content}</div>
                    <div class="chat-bubble-time">${m.time}</div>
                  </div>
                </div>
              `;
            }
            
            // Assistant bubble with food log table
            if (m.messageType === 'food') {
              const msgFoods = state.foodEntries.filter(f => f.messageId === m.id);
              if (msgFoods.length === 0) return ''; // Deleted -> disappear!
              
              let totalCalories = 0;
              let totalProtein = 0;
              let totalCarbs = 0;
              let totalFat = 0;
              
              const tableRows = msgFoods.map(f => {
                totalCalories += f.calories;
                totalProtein += f.proteinG;
                totalCarbs += f.carbsG;
                totalFat += f.fatG;
                return `
                  <tr>
                    <td style="font-weight: 500;">${f.name}</td>
                    <td style="text-align: right; font-style: italic; opacity: 0.85;">${f.servingSize || '—'}</td>
                    <td style="text-align: right;">${f.calories}</td>
                    <td style="text-align: right;">${f.proteinG}g</td>
                    <td style="text-align: right;">${f.carbsG}g</td>
                    <td style="text-align: right;">${f.fatG}g</td>
                  </tr>
                `;
              }).join('');
              
              const loggedNames = msgFoods.map(f => f.name).join(', ');
              
              return `
                <div class="chat-bubble-row assistant">
                  <div class="chat-bubble has-table" style="position: relative; padding-right: 48px;">
                    <div style="font-weight: 700; font-size: 0.95rem; display: flex; align-items: center; gap: 6px; margin-bottom: 8px; padding-right: 8px;">
                      <i data-lucide="check-circle-2" style="width: 16px; height: 16px; color: var(--success); flex-shrink: 0;"></i>
                      Logged: ${loggedNames}
                    </div>
                    <button class="btn-edit-nutrients" data-id="${m.id}" data-type="food" title="Edit logged nutrients" style="position: absolute; top: 12px; right: 12px; background: var(--surface); border: 1.5px solid var(--border-visible); border-radius: 50%; width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: var(--primary); box-shadow: var(--shadow); z-index: 10;">
                      <i data-lucide="edit-3" style="width: 14px; height: 14px;"></i>
                    </button>
                    <div class="chat-table-wrapper" style="margin-top: 6px;">
                      <table class="chat-table">
                        <thead>
                          <tr>
                            <th style="font-weight: 700;">Item</th>
                            <th style="text-align: right; font-weight: 700;">Qty</th>
                            <th style="text-align: right; font-weight: 700;">kcal</th>
                            <th style="text-align: right; font-weight: 700;">P</th>
                            <th style="text-align: right; font-weight: 700;">C</th>
                            <th style="text-align: right; font-weight: 700;">F</th>
                          </tr>
                        </thead>
                        <tbody>
                          ${tableRows}
                          <tr style="font-weight: 700; border-top: 1.5px solid var(--border);">
                            <td>Total</td>
                            <td style="text-align: right;">—</td>
                            <td style="text-align: right; color: var(--primary);">${totalCalories}</td>
                            <td style="text-align: right;">${totalProtein}g</td>
                            <td style="text-align: right;">${totalCarbs}g</td>
                            <td style="text-align: right;">${totalFat}g</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 6px; font-size: 0.65rem; opacity: 0.6;">
                      <span style="font-style: italic;">Model: ${m.modelUsed || 'Llama 3.3 70B'}</span>
                      <span class="chat-bubble-time" style="margin: 0; font-size: 0.65rem;">${m.time}</span>
                    </div>
                  </div>
                </div>
              `;
            }
            
            // Assistant bubble with exercise log table
            if (m.messageType === 'exercise') {
              const msgExercises = state.exerciseEntries.filter(e => e.messageId === m.id);
              if (msgExercises.length === 0) return ''; // Deleted -> disappear!
              
              let totalBurned = 0;
              const tableRows = msgExercises.map(e => {
                totalBurned += e.caloriesBurned;
                return `
                  <tr>
                    <td style="font-weight: 500;">${e.name}</td>
                    <td style="text-align: right;">${e.duration} mins</td>
                    <td style="text-align: right; color: var(--color-exercise); font-weight: 700;">-${e.caloriesBurned} kcal</td>
                  </tr>
                `;
              }).join('');
              
              const loggedNames = msgExercises.map(e => e.name).join(', ');
              
              return `
                <div class="chat-bubble-row assistant">
                  <div class="chat-bubble has-table" style="position: relative; padding-right: 48px;">
                    <div style="font-weight: 700; font-size: 0.95rem; display: flex; align-items: center; gap: 6px; margin-bottom: 8px; padding-right: 8px;">
                      <i data-lucide="check-circle-2" style="width: 16px; height: 16px; color: var(--color-exercise); flex-shrink: 0;"></i>
                      Logged: ${loggedNames}
                    </div>
                    <button class="btn-edit-nutrients" data-id="${m.id}" data-type="exercise" title="Edit logged exercise" style="position: absolute; top: 12px; right: 12px; background: var(--surface); border: 1.5px solid var(--border-visible); border-radius: 50%; width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: var(--primary); box-shadow: var(--shadow); z-index: 10;">
                      <i data-lucide="edit-3" style="width: 14px; height: 14px;"></i>
                    </button>
                    <div class="chat-table-wrapper" style="margin-top: 6px;">
                      <table class="chat-table">
                        <thead>
                          <tr>
                            <th style="font-weight: 700;">Workout</th>
                            <th style="text-align: right; font-weight: 700;">Min</th>
                            <th style="text-align: right; font-weight: 700;">Burn</th>
                          </tr>
                        </thead>
                        <tbody>
                          ${tableRows}
                          <tr style="font-weight: 700; border-top: 1.5px solid var(--border);">
                            <td>Total Burned</td>
                            <td style="text-align: right;">—</td>
                            <td style="text-align: right; color: var(--color-exercise); font-weight: 700;">-${totalBurned} kcal</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 6px; font-size: 0.65rem; opacity: 0.6;">
                      <span style="font-style: italic;">Model: ${m.modelUsed || 'Llama 3.3 70B'}</span>
                      <span class="chat-bubble-time" style="margin: 0; font-size: 0.65rem;">${m.time}</span>
                    </div>
                  </div>
                </div>
              `;
            }
            
            // Standard assistant text bubble
            return `
              <div class="chat-bubble-row assistant">
                <div class="chat-bubble">
                  <div>${m.content}</div>
                  <div class="chat-bubble-time">${m.time}</div>
                </div>
              </div>
            `;
        }).join('')}

        ${state.isProcessingChat ? `
          <div class="chat-loader">
            <div class="chat-loader-dot"></div>
            <div class="chat-loader-dot"></div>
            <div class="chat-loader-dot"></div>
          </div>
        ` : ''}

        <!-- Today's logged items checklist directly inside scroller so it never gets cut off -->
        ${activeFoods.length > 0 || activeExercises.length > 0 ? `
          <div class="today-logs-box" style="margin-top: 16px; border-top: 1px dashed var(--border); padding-top: 16px;">
            <h4 style="font-size: 0.9rem; font-weight: 700; margin-bottom: 12px; color: var(--on-surface-variant);">Logged Items Today</h4>
            <div class="entries-list">
              ${activeFoods.map(f => `
                <div class="entry-card food">
                  <div class="entry-info">
                    <div class="entry-icon"><i data-lucide="utensils"></i></div>
                    <div class="entry-detail-box">
                      <span class="entry-name">${f.name}</span>
                      <span class="entry-subtext">${f.servingSize ? `<b>${f.servingSize}</b> · ` : ''}P: ${f.proteinG}g · C: ${f.carbsG}g · F: ${f.fatG}g</span>
                    </div>
                  </div>
                  <div class="entry-value-box">
                    <span class="entry-calories">${f.calories} kcal</span>
                    <button class="delete-btn btn-delete-food" data-id="${f.id}"><i data-lucide="trash-2"></i></button>
                  </div>
                </div>
              `).join('')}
              
              ${activeExercises.map(e => `
                <div class="entry-card exercise">
                  <div class="entry-info">
                    <div class="entry-icon"><i data-lucide="dumbbell"></i></div>
                    <div class="entry-detail-box">
                      <span class="entry-name">${e.name}</span>
                      <span class="entry-subtext">${e.duration} mins</span>
                    </div>
                  </div>
                  <div class="entry-value-box">
                    <span class="entry-calories">-${e.caloriesBurned} kcal</span>
                    <button class="delete-btn btn-delete-exercise" data-id="${e.id}"><i data-lucide="trash-2"></i></button>
                  </div>
                </div>
              `).join('')}
            </div>
          </div>
        ` : ''}
        
        <!-- Bottom scroll spacer to guarantee no elements leak under the floating input capsule -->
        <div class="chat-bottom-spacer" style="height: 80px; flex-shrink: 0; width: 100%;"></div>
      </div>

      <!-- Bottom entry chat bar -->
      <div class="chat-input-bar">
        <input type="text" id="chat-input" placeholder="Type logs e.g. 'I had 3 boiled eggs and black coffee'..." />
        <button class="chat-send-btn" id="btn-send-chat">
          <i data-lucide="send"></i>
        </button>
      </div>
    </div>
  `;
}

function renderMacroBar(label, current, goal, unit, color) {
  const percent = goal > 0 ? Math.min(100, Math.round((current / goal) * 100)) : 0;
  const macroName = label.toLowerCase();
  const isActive = state.selectedMacroView === macroName;
  const activeStyle = isActive ? `border: 1.5px solid ${color}; background-color: var(--surface-variant);` : '';
  
  const initialPercent = (state.lastProgress && state.lastProgress.macros && state.lastProgress.macros[macroName] !== undefined)
    ? state.lastProgress.macros[macroName]
    : 0;

  return `
    <div class="macro-bar-container macro-clickable" data-macro="${macroName}" style="padding: 6px 8px; border-radius: 12px; cursor: pointer; transition: all 0.2s ease; ${activeStyle}">
      <div class="macro-bar-info">
        <span class="macro-bar-label" style="font-weight: 700; font-size: 0.8rem;">${label}</span>
        <span class="macro-bar-value" style="font-size: 0.75rem;">${Math.round(current)}${unit} / ${goal}${unit} (${percent}%)</span>
      </div>
      <div class="macro-bar-track" style="height: 6px; background-color: var(--border);">
        <div class="macro-bar-fill" data-macro-name="${macroName}" data-target-percent="${percent}" style="width: ${initialPercent}%; background-color: ${color}; height: 100%; border-radius: 3px;"></div>
      </div>
    </div>
  `;
}

function renderCalendarStrip(selectedDate) {
  const today = new Date();
  const dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
  
  const weekDays = [];
  // Generate 30 days backwards from today
  for (let i = 29; i >= 0; i--) {
    const d = new Date();
    d.setDate(today.getDate() - i);
    weekDays.push(d);
  }

  return `
    <div class="calendar-strip" id="calendar-strip-container">
      ${weekDays.map(date => {
        const dateStr = getLocalDateString(date);
        const isSelected = dateStr === state.selectedDateStr;
        const isToday = dateStr === getLocalDateString(today);
        
        return `
          <div class="day-cell ${isSelected ? 'selected' : ''}" data-date="${dateStr}">
            <span class="day-label">${dayNames[date.getDay()]}</span>
            <div class="day-num-box">${date.getDate()}</div>
            ${isToday ? '<div class="today-dot"></div>' : ''}
          </div>
        `;
      }).join('')}
    </div>
  `;
}

// ── Weekly Summary Screen ────────────────────────────────────────────────────
function renderWeeklyScreen() {
  const tabs = ["This week", "Last week", "2 weeks ago", "3 weeks ago"];
  const selectedTab = localStorage.getItem('weeklyTab') || '0';
  const tabIndex = parseInt(selectedTab);

  // Bounds
  const today = new Date();
  const currentDayOfWeek = today.getDay();
  const startOfCurrentWeek = new Date(today.getTime() - currentDayOfWeek * 24 * 60 * 60 * 1000);
  startOfCurrentWeek.setHours(0,0,0,0);
  
  const weekStart = new Date(startOfCurrentWeek.getTime() - tabIndex * 7 * 24 * 60 * 60 * 1000);
  const weekDays = [];
  for (let i = 0; i < 7; i++) {
    weekDays.push(new Date(weekStart.getTime() + i * 24 * 60 * 60 * 1000));
  }
  
  const formattedStart = weekDays[0].toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
  const formattedEnd = weekDays[6].toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });

  const weekKey = `${formattedStart} — ${formattedEnd}`;
  const insights = state.weeklyInsights[weekKey];

  let insightsHtml = '';
  if (state.isGeneratingWeeklyInsights) {
    insightsHtml = `
      <div class="insight-card loading" style="padding: 24px; text-align: center; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; background-color: var(--surface); border: 1px solid var(--border); border-radius: 18px; margin-top: 24px;">
        <div class="fact-loader" style="width: 36px; height: 36px; border: 3px solid var(--border); border-top-color: var(--primary); border-radius: 50%; animation: rotate-ring 1s linear infinite;"></div>
        <span style="font-size: 0.9rem; font-weight: 700; color: var(--primary);">AI Nutritionist Analyzing Your Week...</span>
        <p style="font-size: 0.75rem; opacity: 0.6; max-width: 280px; margin: 0; line-height: 1.4;">Scanning foods, calorie logs, and exercise patterns to craft custom health coaching tips...</p>
      </div>
    `;
  } else if (insights) {
    if (insights.error) {
      insightsHtml = `
        <div class="insight-card" style="padding: 20px; background-color: var(--surface); border: 1px solid var(--border); border-radius: 18px; margin-top: 24px;">
          <h3 style="font-size: 1.1rem; font-weight: 700; margin-bottom: 8px; display: flex; align-items: center; gap: 8px; color: var(--error);">
            <i data-lucide="alert-triangle" style="width: 18px; height: 18px;"></i>
            AI Insights Failed
          </h3>
          <p style="font-size: 0.8rem; opacity: 0.85; margin-bottom: 12px;">${insights.error}</p>
          <button class="btn-style primary" id="btn-regenerate-insights" data-tab="${tabIndex}" style="height: 38px; padding: 0 16px; font-size: 0.8rem; display: flex; align-items: center; gap: 6px; border: none; border-radius: 10px; cursor: pointer;">
            <i data-lucide="rotate-cw" style="width: 14px; height: 14px;"></i> Retry AI Analysis
          </button>
        </div>
      `;
    } else {
      insightsHtml = `
        <div class="insight-card ${state.isWeeklyInsightsOpen ? 'open' : ''}" style="padding: 20px; background-color: var(--surface); border: 1px solid var(--border); border-radius: 18px; margin-top: 24px; box-shadow: 0 4px 12px rgba(0,0,0,0.03);">
          <div id="btn-toggle-insights-collapse" style="display: flex; justify-content: space-between; align-items: center; cursor: pointer; user-select: none;">
            <h3 style="font-size: 1.15rem; font-weight: 700; display: flex; align-items: center; gap: 8px; margin: 0; color: var(--primary); font-family: var(--font-display);">
              <i data-lucide="sparkles" style="width: 18px; height: 18px; color: var(--primary);"></i>
              AI Health Coach Insights
            </h3>
            <div style="display: flex; align-items: center; gap: 12px;">
              <button class="btn-style" id="btn-regenerate-insights" data-tab="${tabIndex}" style="height: 28px; padding: 0 10px; font-size: 0.7rem; border-radius: 8px; background: var(--surface-variant); border: 1px solid var(--border-visible); cursor: pointer; display: flex; align-items: center; gap: 4px; color: var(--on-surface-variant); font-weight: 600;">
                <i data-lucide="rotate-cw" style="width: 10px; height: 10px;"></i> Regenerate
              </button>
              <i class="dropdown-arrow" data-lucide="chevron-down" style="width: 16px; height: 16px; color: var(--primary);"></i>
            </div>
          </div>

          <div class="insight-content-wrapper">
            <div style="border-top: 1px solid var(--border); margin-top: 12px; padding-top: 12px; display: flex; flex-direction: column; gap: 14px;">
              ${insights.good && insights.good.length > 0 ? `
                <div>
                  <h4 style="font-size: 0.85rem; font-weight: 700; color: var(--success); display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
                    <i data-lucide="check-circle" style="width: 14px; height: 14px;"></i> Positive Highlights
                  </h4>
                  <ul style="margin: 0; padding-left: 18px; font-size: 0.8rem; line-height: 1.4; opacity: 0.9;">
                    ${insights.good.map(point => `<li style="margin-bottom: 3px;">${point}</li>`).join('')}
                  </ul>
                </div>
              ` : ''}

              ${insights.bad && insights.bad.length > 0 ? `
                <div>
                  <h4 style="font-size: 0.85rem; font-weight: 700; color: #b7791f; display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
                    <i data-lucide="alert-circle" style="width: 14px; height: 14px;"></i> Areas of Concern
                  </h4>
                  <ul style="margin: 0; padding-left: 18px; font-size: 0.8rem; line-height: 1.4; opacity: 0.9;">
                    ${insights.bad.map(point => `<li style="margin-bottom: 3px;">${point}</li>`).join('')}
                  </ul>
                </div>
              ` : ''}

              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 4px; border-top: 1px dashed var(--border); padding-top: 12px;">
                ${insights.focus && insights.focus.length > 0 ? `
                  <div>
                    <h4 style="font-size: 0.85rem; font-weight: 700; color: #3182ce; display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
                      <i data-lucide="target" style="width: 14px; height: 14px;"></i> Focus Next Week
                    </h4>
                    <ul style="margin: 0; padding-left: 16px; font-size: 0.78rem; line-height: 1.4; opacity: 0.9;">
                      ${insights.focus.map(point => `<li style="margin-bottom: 3px;">${point}</li>`).join('')}
                    </ul>
                  </div>
                ` : ''}

                ${insights.avoid && insights.avoid.length > 0 ? `
                  <div>
                    <h4 style="font-size: 0.85rem; font-weight: 700; color: #e53e3e; display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
                      <i data-lucide="ban" style="width: 14px; height: 14px;"></i> What to Avoid
                    </h4>
                    <ul style="margin: 0; padding-left: 16px; font-size: 0.78rem; line-height: 1.4; opacity: 0.9;">
                      ${insights.avoid.map(point => `<li style="margin-bottom: 3px;">${point}</li>`).join('')}
                    </ul>
                  </div>
                ` : ''}
              </div>

              ${insights.tips && insights.tips.length > 0 ? `
                <div style="margin-top: 4px; border-top: 1px dashed var(--border); padding-top: 12px; background-color: var(--surface-variant); padding: 10px 14px; border-radius: 10px;">
                  <h4 style="font-size: 0.85rem; font-weight: 700; color: var(--primary); display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
                    <i data-lucide="lightbulb" style="width: 14px; height: 14px; color: #e5c158;"></i> Coach Actionable Tips
                  </h4>
                  <ul style="margin: 0; padding-left: 18px; font-size: 0.78rem; line-height: 1.4; opacity: 0.9; color: var(--on-surface-variant);">
                    ${insights.tips.map(point => `<li style="margin-bottom: 3px;">${point}</li>`).join('')}
                  </ul>
                </div>
              ` : ''}
            </div>
          </div>
        </div>
      `;
    }
  } else {
    insightsHtml = `
      <div class="insight-card" style="padding: 24px; text-align: center; background-color: var(--surface); border: 1px solid var(--border); border-radius: 18px; margin-top: 24px; display: flex; flex-direction: column; align-items: center; gap: 10px;">
        <i data-lucide="sparkles" style="width: 32px; height: 32px; color: var(--primary); opacity: 0.7;"></i>
        <h4 style="font-size: 0.95rem; font-weight: 700; margin: 0; color: var(--on-surface);">Weekly AI Coaching Analysis</h4>
        <p style="font-size: 0.78rem; opacity: 0.6; max-width: 290px; margin: 0; line-height: 1.4;">
          Get tailored feedback on your calorie goals, macronutrient splits, logged workouts, and eating habits.
        </p>
        <button class="btn-style primary" id="btn-generate-insights" data-tab="${tabIndex}" style="height: 42px; padding: 0 20px; margin-top: 6px; font-weight: 700; display: flex; align-items: center; gap: 8px; border: none; border-radius: 10px; cursor: pointer;">
          <i data-lucide="sparkles" style="width: 16px; height: 16px;"></i> Generate AI Report
        </button>
      </div>
    `;
  }

  let weeklyFoodTotal = 0;
  let weeklyExerciseTotal = 0;
  let daysTrackedCount = 0;
  
  const dailyRows = weekDays.map(date => {
    const dStr = getLocalDateString(date);
    const summary = getDailyNutritionSummary(dStr);
    
    weeklyFoodTotal += summary.totalCalories;
    weeklyExerciseTotal += summary.exerciseCalories;
    
    const hasData = state.foodEntries.some(f => f.dateStr === dStr) || state.exerciseEntries.some(e => e.dateStr === dStr);
    if (hasData) daysTrackedCount++;

    return {
      day: date.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' }),
      food: hasData ? summary.totalCalories : '-',
      exercise: hasData && summary.exerciseCalories > 0 ? summary.exerciseCalories : '-',
      remaining: hasData ? (state.dailyCalorieGoal - summary.totalCalories + summary.exerciseCalories) : '-',
      carbs: hasData ? summary.totalCarbs : '-',
      protein: hasData ? summary.totalProtein : '-',
      fat: hasData ? summary.totalFat : '-'
    };
  });

  const weeklyCalorieBudget = state.dailyCalorieGoal * daysTrackedCount;
  const weeklyRemaining = weeklyCalorieBudget - weeklyFoodTotal + weeklyExerciseTotal;
  const isUnderBudget = weeklyRemaining >= 0;

  return `
    <div style="display: flex; flex-direction: column; gap: 24px;">
      <!-- Tab Selector -->
      <div class="tab-row">
        ${tabs.map((tab, idx) => `
          <div class="tab-item ${idx === tabIndex ? 'active' : ''}" data-tab="${idx}">${tab}</div>
        `).join('')}
      </div>

      <!-- Report summary overview card -->
      <div class="summary-card">
        <span class="summary-title">${formattedStart} — ${formattedEnd}</span>
        <div class="summary-details">
          <span style="font-weight: 700; color: ${isUnderBudget ? 'var(--success)' : 'var(--error)'}">
            ${Math.abs(weeklyRemaining)} calories ${isUnderBudget ? 'under budget' : 'over budget'} this week
          </span>
          <span>${daysTrackedCount} out of 7 days tracked this week</span>
        </div>
      </div>

      <!-- AI Weekly Insights Panel -->
      ${insightsHtml}

      <!-- Calories Breakdown Table -->
      <div class="table-card">
        <h3>Calories Summary</h3>
        <table>
          <thead>
            <tr>
              <th>Day</th>
              <th class="text-end">Food (kcal)</th>
              <th class="text-end">Exercise (kcal)</th>
              <th class="text-end">Remaining (kcal)</th>
            </tr>
          </thead>
          <tbody>
            ${dailyRows.map(row => `
              <tr>
                <td>${row.day}</td>
                <td class="text-end">${row.food}</td>
                <td class="text-end">${row.exercise}</td>
                <td class="text-end" style="color: ${row.remaining !== '-' ? (row.remaining >= 0 ? 'var(--success)' : 'var(--error)') : 'inherit'}">${row.remaining}</td>
              </tr>
            `).join('')}
            <tr class="total-row">
              <td>Total</td>
              <td class="text-end">${weeklyFoodTotal}</td>
              <td class="text-end">${weeklyExerciseTotal}</td>
              <td class="text-end" style="color: ${weeklyRemaining >= 0 ? 'var(--success)' : 'var(--error)'}">${weeklyRemaining}</td>
            </tr>
          </tbody>
        </table>
        <div style="font-size: 0.8rem; opacity: 0.6; margin-top: 12px;">* Based on a daily goal of ${state.dailyCalorieGoal} calories</div>
      </div>

      <!-- Macronutrients Table -->
      <div class="table-card">
        <h3>Macronutrients Summary</h3>
        <table>
          <thead>
            <tr>
              <th>Day</th>
              <th class="text-end">Carbs (g)</th>
              <th class="text-end">Protein (g)</th>
              <th class="text-end">Fat (g)</th>
            </tr>
          </thead>
          <tbody>
            ${dailyRows.map(row => `
              <tr>
                <td>${row.day}</td>
                <td class="text-end">${row.carbs !== '-' ? Math.round(row.carbs) + 'g' : '-'}</td>
                <td class="text-end">${row.protein !== '-' ? Math.round(row.protein) + 'g' : '-'}</td>
                <td class="text-end">${row.fat !== '-' ? Math.round(row.fat) + 'g' : '-'}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// ── Weight Tracker Screen ────────────────────────────────────────────────────
function renderWeightScreen() {
  const period = localStorage.getItem('weightPeriod') || 'WEEK';
  
  // Filter weights by period
  const cutoff = getCutoffDays(period);
  const filtered = state.weightEntries
    .filter(w => w.timestamp >= cutoff)
    .sort((a,b) => a.timestamp - b.timestamp); // Ascending order for chart

  return `
    <div style="display: flex; flex-direction: column; gap: 24px;">
      <!-- Stats cards row -->
      <div class="stat-card-row">
        <div class="stat-card">
          <span class="stat-card-label">Current Weight</span>
          <span class="stat-card-value">${state.currentWeightKg} kg</span>
        </div>
        <div class="stat-card">
          <span class="stat-card-label">Target Weight</span>
          <span class="stat-card-value">${state.targetWeightKg} kg</span>
        </div>
      </div>

      <!-- Chart area -->
      <div class="chart-card">
        <div class="tab-row" style="border: none;">
          <div class="tab-item weight-tab ${period === 'WEEK' ? 'active' : ''}" data-period="WEEK">Week</div>
          <div class="tab-item weight-tab ${period === 'MONTH' ? 'active' : ''}" data-period="MONTH">Month</div>
          <div class="tab-item weight-tab ${period === 'YEAR' ? 'active' : ''}" data-period="YEAR">Year</div>
          <div class="tab-item weight-tab ${period === 'ALL' ? 'active' : ''}" data-period="ALL">All time</div>
        </div>

        <div class="chart-canvas-container">
          <canvas id="weight-chart-canvas"></canvas>
        </div>
      </div>

      <!-- Weight entries logs list -->
      <div style="display: flex; flex-direction: column; gap: 12px;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <h3 style="font-size: 1.15rem; font-weight: 700;">Logged Weight Entries</h3>
          <button class="btn-style primary" id="btn-add-weight-dialog" style="height: 38px; padding: 0 16px; font-size: 0.9rem;">
            <i data-lucide="plus"></i> Add Entry
          </button>
        </div>

        <div class="entries-list">
          ${state.weightEntries.map(w => {
            const dateStr = new Date(w.timestamp).toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
            return `
              <div class="entry-card">
                <div class="entry-info">
                  <div class="entry-icon" style="background-color: var(--primary-container); color: var(--primary);"><i data-lucide="scale"></i></div>
                  <div class="entry-detail-box">
                    <span class="entry-name">${w.weightKg} kg</span>
                    <span class="entry-subtext">${dateStr}</span>
                  </div>
                </div>
                <button class="delete-btn btn-delete-weight" data-id="${w.id}"><i data-lucide="trash-2"></i></button>
              </div>
            `;
          }).join('')}
        </div>
      </div>
    </div>
  `;
}

function getCutoffDays(period) {
  const d = new Date();
  d.setHours(0,0,0,0);
  switch (period) {
    case 'WEEK': return d.getTime() - 7 * 24 * 60 * 60 * 1000;
    case 'MONTH': return d.getTime() - 30 * 24 * 60 * 60 * 1000;
    case 'YEAR': return d.getTime() - 365 * 24 * 60 * 60 * 1000;
    case 'ALL': return 0;
  }
}

function renderWeightChartCanvas() {
  const canvas = document.getElementById('weight-chart-canvas');
  if (!canvas) return;

  const dpr = window.devicePixelRatio || 1;
  const rect = canvas.getBoundingClientRect();
  canvas.width = rect.width * dpr;
  canvas.height = rect.height * dpr;
  
  const ctx = canvas.getContext('2d');
  ctx.scale(dpr, dpr);

  const width = rect.width;
  const height = rect.height;

  const period = localStorage.getItem('weightPeriod') || 'WEEK';
  const cutoff = getCutoffDays(period);
  
  // Ascending order for chart
  const entries = state.weightEntries
    .filter(w => w.timestamp >= cutoff)
    .sort((a,b) => a.timestamp - b.timestamp);

  ctx.clearRect(0,0, width, height);

  if (entries.length === 0) {
    ctx.font = '14px Inter';
    ctx.fillStyle = '#888';
    ctx.textAlign = 'center';
    ctx.fillText('No weight entries in this period', width / 2, height / 2);
    return;
  }

  // Calculate Y bounds
  const weights = entries.map(e => e.weightKg).concat([state.targetWeightKg]);
  const maxW = Math.max(...weights);
  const minW = Math.min(...weights);
  const padding = Math.max(5, (maxW - minW) * 0.2);

  const yMax = maxW + padding;
  const yMin = Math.max(0, minW - padding);
  const yRange = yMax - yMin;

  function toY(w) {
    return height - 20 - ((w - yMin) / yRange) * (height - 40);
  }

  function toX(index) {
    if (entries.length <= 1) return width / 2;
    const paddingX = 40;
    return paddingX + (index / (entries.length - 1)) * (width - paddingX - 20);
  }

  // Draw target weight dashed line
  const targetY = toY(state.targetWeightKg);
  ctx.save();
  ctx.strokeStyle = '#2E7D32';
  ctx.lineWidth = 1.5;
  ctx.setLineDash([6, 6]);
  ctx.beginPath();
  ctx.moveTo(40, targetY);
  ctx.lineTo(width - 20, targetY);
  ctx.stroke();
  ctx.restore();

  // Draw grid lines
  const gridRows = 4;
  ctx.font = '10px Inter';
  ctx.fillStyle = '#888';
  ctx.textAlign = 'right';
  for (let i = 0; i <= gridRows; i++) {
    const val = yMin + (i / gridRows) * yRange;
    const y = toY(val);
    
    // Grid horizontal line
    ctx.strokeStyle = 'rgba(0,0,0,0.05)';
    ctx.beginPath();
    ctx.moveTo(40, y);
    ctx.lineTo(width - 20, y);
    ctx.stroke();

    // Text labels
    ctx.fillText(`${Math.round(val)}kg`, 32, y + 3);
  }

  // Plot actual entries
  const points = entries.map((e, idx) => ({ x: toX(idx), y: toY(e.weightKg) }));

  // Draw connection line
  if (points.length > 1) {
    ctx.strokeStyle = 'rgba(45, 106, 79, 1)';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(points[0].x, points[0].y);
    for (let i = 1; i < points.length; i++) {
      ctx.lineTo(points[i].x, points[i].y);
    }
    ctx.stroke();
  }

  // Draw dots
  points.forEach(p => {
    ctx.fillStyle = 'rgba(45, 106, 79, 1)';
    ctx.beginPath();
    ctx.arc(p.x, p.y, 5, 0, 2 * Math.PI);
    ctx.fill();

    ctx.fillStyle = 'white';
    ctx.beginPath();
    ctx.arc(p.x, p.y, 2.5, 0, 2 * Math.PI);
    ctx.fill();
  });
}

// ── Daily Goals Screen ───────────────────────────────────────────────────────
function renderGoalsScreen() {
  const carbsG = Math.round((state.dailyCalorieGoal * state.carbsPercent / 100) / 4);
  const proteinG = Math.round((state.dailyCalorieGoal * state.proteinPercent / 100) / 4);
  const fatG = Math.round((state.dailyCalorieGoal * state.fatPercent / 100) / 9);
  
  const sumPercent = state.carbsPercent + state.proteinPercent + state.fatPercent;
  const isValid = sumPercent === 100;

  return `
    <div style="display: flex; flex-direction: column; gap: 24px;">
      <!-- Calorie target inputs -->
      <div class="card-content-box">
        <h3 style="font-size: 1.15rem;">Calories Target</h3>
        <div class="form-group">
          <label>Calories (kcal)</label>
          <input type="number" id="goals-calories" class="input-style" value="${state.dailyCalorieGoal}" />
        </div>
        <button class="btn-style secondary" id="btn-goals-calculator" style="gap: 8px;">
          <i data-lucide="calculator"></i> Recalculate with Quiz Wizard
        </button>
      </div>

      <!-- Macro Split configuration card -->
      <div class="card-content-box">
        <h3 style="font-size: 1.15rem;">Macro Percent Splits</h3>
        <p style="font-size: 0.85rem; color: ${isValid ? 'var(--on-surface-variant)' : 'var(--error)'}">
          Percentages must sum to exactly 100%. Currently: ${sumPercent}%
        </p>

        <div style="display: flex; flex-direction: column; gap: 16px;">
          <!-- Carbohydrates -->
          <div class="goals-macro-row">
            <div class="goals-macro-info">
              <span class="goals-macro-name">Carbohydrates (${carbsG}g)</span>
              <span class="goals-macro-cal">1g = 4 kcal</span>
            </div>
            <div class="goals-percent-input">
              <input type="number" id="goals-carbs" value="${state.carbsPercent}" />
              <span>%</span>
            </div>
          </div>

          <!-- Protein -->
          <div class="goals-macro-row">
            <div class="goals-macro-info">
              <span class="goals-macro-name">Protein (${proteinG}g)</span>
              <span class="goals-macro-cal">1g = 4 kcal</span>
            </div>
            <div class="goals-percent-input">
              <input type="number" id="goals-protein" value="${state.proteinPercent}" />
              <span>%</span>
            </div>
          </div>

          <!-- Fat -->
          <div class="goals-macro-row">
            <div class="goals-macro-info">
              <span class="goals-macro-name">Fat (${fatG}g)</span>
              <span class="goals-macro-cal">1g = 9 kcal</span>
            </div>
            <div class="goals-percent-input">
              <input type="number" id="goals-fat" value="${state.fatPercent}" />
              <span>%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `;
}

// ── Reminders Screen ─────────────────────────────────────────────────────────
function renderRemindersScreen() {
  function format12Hour(time24) {
    const [h, m] = time24.split(':').map(Number);
    const pm = h >= 12;
    const h12 = h % 12 === 0 ? 12 : h % 12;
    return `${h12}:${String(m).padStart(2,'0')} ${pm ? 'PM' : 'AM'}`;
  }

  return `
    <div class="reminder-list-box">
      <!-- Morning -->
      <div class="reminder-card-row">
        <div class="reminder-time-trigger" data-type="morning">
          <span class="reminder-label">Morning</span>
          <span class="reminder-time">${format12Hour(state.morningTime)}</span>
        </div>
        <div class="reminder-check ${state.morningEnabled ? 'checked' : ''}" data-type="morning">
          ${state.morningEnabled ? '<i data-lucide="check"></i>' : ''}
        </div>
      </div>

      <!-- Afternoon -->
      <div class="reminder-card-row">
        <div class="reminder-time-trigger" data-type="afternoon">
          <span class="reminder-label">Afternoon</span>
          <span class="reminder-time">${format12Hour(state.afternoonTime)}</span>
        </div>
        <div class="reminder-check ${state.afternoonEnabled ? 'checked' : ''}" data-type="afternoon">
          ${state.afternoonEnabled ? '<i data-lucide="check"></i>' : ''}
        </div>
      </div>

      <!-- Evening -->
      <div class="reminder-card-row">
        <div class="reminder-time-trigger" data-type="evening">
          <span class="reminder-label">Evening</span>
          <span class="reminder-time">${format12Hour(state.eveningTime)}</span>
        </div>
        <div class="reminder-check ${state.eveningEnabled ? 'checked' : ''}" data-type="evening">
          ${state.eveningEnabled ? '<i data-lucide="check"></i>' : ''}
        </div>
      </div>
    </div>
  `;
}

// ── Settings Screen ──────────────────────────────────────────────────────────
function renderSettingsScreen() {
  return `
    <div style="display: flex; flex-direction: column; gap: 24px;">
      <!-- API Key Card -->
      <div class="card-content-box">
        <h3 style="font-size: 1.15rem;">AI Settings</h3>
        <p style="font-size: 0.85rem; color: var(--on-surface-variant)">
          Choose your AI API provider and configure credentials to enable intelligent parsing of your text logs into food and exercise entries.
        </p>

        <div class="form-group" style="margin-top: 12px; margin-bottom: 8px;">
          <label>API Provider</label>
          <div style="display: flex; gap: 16px; margin-top: 4px;">
            <label style="display: flex; align-items: center; gap: 6px; cursor: pointer; text-transform: none; font-size: 0.9rem; font-weight: 500;">
              <input type="radio" name="api-provider" value="gemini" ${state.apiProvider === 'gemini' ? 'checked' : ''} style="cursor: pointer;" />
              Google Gemini
            </label>
            <label style="display: flex; align-items: center; gap: 6px; cursor: pointer; text-transform: none; font-size: 0.9rem; font-weight: 500;">
              <input type="radio" name="api-provider" value="openrouter" ${state.apiProvider === 'openrouter' ? 'checked' : ''} style="cursor: pointer;" />
              OpenRouter (Test)
            </label>
          </div>
        </div>

        ${state.apiProvider === 'gemini' ? `
          <div class="form-group" style="position: relative; margin-top: 12px;">
            <label>Gemini API Key</label>
            <input type="password" id="settings-api-key" class="input-style" value="${state.apiKey}" placeholder="Paste your API Key here..." />
          </div>
          <div class="form-group" style="margin-top: 12px;">
            <label>Gemini Model Preference</label>
            <select id="settings-gemini-model" class="input-style" style="background-color: var(--surface); color: var(--on-surface); border: 1.5px solid var(--border-visible); border-radius: var(--radius-sm); padding: 8px; width: 100%;">
              <option value="auto" ${state.geminiModel === 'auto' ? 'selected' : ''}>Auto (Best Model with Fallbacks)</option>
              <option value="gemini-3.1-pro" ${state.geminiModel === 'gemini-3.1-pro' ? 'selected' : ''}>gemini-3.1-pro (Flagship capable)</option>
              <option value="gemini-3.5-flash" ${state.geminiModel === 'gemini-3.5-flash' ? 'selected' : ''}>gemini-3.5-flash (Next Gen Frontier)</option>
              <option value="gemini-2.5-pro" ${state.geminiModel === 'gemini-2.5-pro' ? 'selected' : ''}>gemini-2.5-pro (Capable reasoning)</option>
              <option value="gemini-2.5-flash" ${state.geminiModel === 'gemini-2.5-flash' ? 'selected' : ''}>gemini-2.5-flash (Balanced standard)</option>
              <option value="gemini-3.1-flash-lite" ${state.geminiModel === 'gemini-3.1-flash-lite' ? 'selected' : ''}>gemini-3.1-flash-lite (Ultra-light / cheapest)</option>
            </select>
          </div>
        ` : `
          <div class="form-group" style="position: relative; margin-top: 12px;">
            <label>OpenRouter API Key</label>
            <input type="password" id="settings-openrouter-key" class="input-style" value="${state.openRouterApiKey}" placeholder="Paste your OpenRouter API Key here..." />
          </div>
          <div class="form-group" style="margin-top: 12px;">
            <label>OpenRouter Model</label>
            <select id="settings-openrouter-model" class="input-style" style="background-color: var(--surface); color: var(--on-surface); border: 1.5px solid var(--border-visible); border-radius: var(--radius-sm); padding: 8px; width: 100%;">
              ${DEFAULT_OPENROUTER_MODELS.map(m => `
                <option value="${m.id}" ${state.openRouterModel === m.id ? 'selected' : ''}>${m.name}</option>
              `).join('')}
            </select>
          </div>
        `}
      </div>

      <!-- About Card -->
      <div class="card-content-box">
        <h3 style="font-size: 1.15rem;">About Daywise</h3>
        <div style="font-size: 0.95rem; font-weight: 600;">Version 1.0.0</div>
        <p style="font-size: 0.85rem; color: var(--on-surface-variant)">
          Daywise is a conversational companion for your health. Talk to the chatbot, and our integrated Gemini parser handles the calorie and macro breakdowns instantly!
        </p>
      </div>

      <!-- Danger Zone Card -->
      <div class="card-content-box" style="border: 1px solid hsla(355, 75%, 45%, 0.15); background-color: var(--error-container); color: var(--error)">
        <h3 style="font-size: 1.15rem; color: var(--error)">Danger Zone</h3>
        <p style="font-size: 0.85rem; color: var(--error); opacity: 0.85">
          Reset onboarding or clear your entire logging history and chat logs to start completely fresh.
        </p>
        <div style="display: flex; gap: 12px; margin-top: 12px;">
          <button id="btn-reset-onboarding" class="btn-style secondary" style="background-color: var(--surface); color: var(--on-surface);">Reset Onboarding</button>
          <button id="btn-clear-chat" class="btn-style" style="background-color: var(--error); color: white;">Clear Chat & Logs</button>
        </div>
      </div>
    </div>
  `;
}

// ── Database Aggregation ─────────────────────────────────────────────────────
function getDailyNutritionSummary(dateStr) {
  const foods = state.foodEntries.filter(f => f.dateStr === dateStr);
  const exercises = state.exerciseEntries.filter(e => e.dateStr === dateStr);

  const totalCalories = foods.reduce((sum, f) => sum + f.calories, 0);
  const totalProtein = foods.reduce((sum, f) => sum + f.proteinG, 0);
  const totalCarbs = foods.reduce((sum, f) => sum + f.carbsG, 0);
  const totalFat = foods.reduce((sum, f) => sum + f.fatG, 0);
  
  const exerciseCalories = exercises.reduce((sum, e) => sum + e.caloriesBurned, 0);

  return {
    totalCalories,
    totalProtein,
    totalCarbs,
    totalFat,
    exerciseCalories
  };
}

function parseLocalDate(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number);
  return new Date(y, m - 1, d);
}

// ── Application Mounting & Interactivity ─────────────────────────────────────
function mountApp() {
  // Capture current calendar strip scroll position before re-rendering
  const oldContainer = document.getElementById('calendar-strip-container');
  const savedScrollLeft = oldContainer ? oldContainer.scrollLeft : null;

  const appRoot = document.getElementById('app');
  appRoot.innerHTML = appLayoutTemplate();
  createIcons({ icons });
  console.log("MOUNTED APP INNERHTML:", appRoot.innerHTML);
  
  const screenBody = document.getElementById('screen-body');
  if (screenBody) {
    if (state.currentScreen === 'chat') {
      screenBody.style.padding = '0';
    } else {
      screenBody.style.padding = '16px';
    }
  }

  // Synchronously restore calendar strip scroll position & scroll chat to bottom to prevent blinking
  if (state.currentScreen === 'chat') {
    const container = document.getElementById('calendar-strip-container');
    if (container) {
      if (savedScrollLeft !== null) {
        container.scrollLeft = savedScrollLeft;
      } else {
        container.scrollLeft = container.scrollWidth; // First load scroll to today
      }
    }

    const scroller = document.getElementById('chat-scroller');
    if (scroller) {
      scroller.scrollTop = scroller.scrollHeight;
    }

    // Force layout reflow before triggering transition animations
    document.body.offsetHeight;

    // 1. Animate horizontal YouTube progress bar
    const fillBar = document.querySelector('.yt-progress-fill');
    if (fillBar) {
      const targetPercent = parseFloat(fillBar.dataset.targetPercent || 0);
      fillBar.style.width = `${targetPercent}%`;
      state.lastProgress.caloriePercent = targetPercent;
    }

    // 2. Animate circular progress ring fill
    const fillCircle = document.querySelector('.calorie-ring-box .fill');
    if (fillCircle) {
      const targetOffset = parseFloat(fillCircle.dataset.targetOffset || 314.159);
      fillCircle.style.strokeDashoffset = targetOffset;
      state.lastProgress.offset = targetOffset;
    }

    // 3. Animate macro bars fills
    document.querySelectorAll('.macro-bar-fill').forEach(fill => {
      const macroName = fill.dataset.macroName;
      const targetPercent = parseFloat(fill.dataset.targetPercent || 0);
      fill.style.width = `${targetPercent}%`;
      if (!state.lastProgress.macros) state.lastProgress.macros = {};
      state.lastProgress.macros[macroName] = targetPercent;
    });

    saveStateToStorage();
  }

  // Attach general event listeners
  attachEventListeners();
  
  // Re-draw canvas if Weight Screen is active
  if (state.currentScreen === 'weight_tracker') {
    renderWeightChartCanvas();
  }
}

function attachEventListeners() {
  // Sidebar navigations
  document.querySelectorAll('.drawer-item').forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      const screen = item.dataset.target;
      state.currentScreen = screen;
      state.isSidebarOpen = false;
      saveStateToStorage();
      mountApp();
    });
  });

  // Toggle drawer on mobile
  const btnToggleSidebar = document.getElementById('btn-toggle-sidebar');
  if (btnToggleSidebar) {
    btnToggleSidebar.addEventListener('click', () => {
      state.isSidebarOpen = !state.isSidebarOpen;
      saveStateToStorage();
      
      const sidebar = document.getElementById('sidebar');
      const backdrop = document.getElementById('sidebar-backdrop');
      if (sidebar && backdrop) {
        sidebar.classList.toggle('drawer-open', state.isSidebarOpen);
        backdrop.classList.toggle('active', state.isSidebarOpen);
      }
    });
  }

  // Theme Toggle switcher
  const btnThemeToggle = document.getElementById('btn-theme-toggle');
  if (btnThemeToggle) {
    btnThemeToggle.addEventListener('click', () => {
      state.theme = state.theme === 'light' ? 'dark' : 'light';
      localStorage.setItem('theme', state.theme);
      document.documentElement.setAttribute('data-theme', state.theme);
      mountApp();
    });
  }

  // ── Onboarding Events ──────────────────────────────────────────────────────
  const genderCards = document.querySelectorAll('.gender-select .selectable-card');
  genderCards.forEach(c => {
    c.addEventListener('click', () => {
      state.onboardingData.gender = c.dataset.gender;
      mountApp();
    });
  });

  const goalCards = document.querySelectorAll('.goal-select .selectable-card');
  goalCards.forEach(c => {
    c.addEventListener('click', () => {
      state.onboardingData.goal = c.dataset.goal;
      mountApp();
    });
  });

  const aggressionCards = document.querySelectorAll('.aggression-select .selectable-card');
  aggressionCards.forEach(c => {
    c.addEventListener('click', () => {
      state.onboardingData.aggression = c.dataset.aggression;
      mountApp();
    });
  });

  const obAge = document.getElementById('ob-age');
  if (obAge) obAge.addEventListener('input', (e) => state.onboardingData.age = e.target.value);
  
  const obHeight = document.getElementById('ob-height');
  if (obHeight) obHeight.addEventListener('input', (e) => state.onboardingData.height = e.target.value);
  
  const obWeight = document.getElementById('ob-weight');
  if (obWeight) obWeight.addEventListener('input', (e) => state.onboardingData.weight = e.target.value);
  
  const obTarget = document.getElementById('ob-target');
  if (obTarget) obTarget.addEventListener('input', (e) => state.onboardingData.targetWeight = e.target.value);

  const btnObNext = document.getElementById('btn-ob-next');
  if (btnObNext) {
    btnObNext.addEventListener('click', () => {
      const step = state.onboardingStep;
      if (step === 0) {
        state.onboardingStep = 1;
      } else if (step === 1) {
        if (!state.onboardingData.age || !state.onboardingData.height || !state.onboardingData.weight) {
          showToast("Please fill in all basic info profile parameters!", "error");
          return;
        }
        state.onboardingStep = 2;
      } else if (step === 2) {
        state.onboardingStep = state.onboardingData.goal === 'Maintain' ? 4 : 3;
      } else if (step === 3) {
        state.onboardingStep = 4;
      } else if (step === 4) {
        if (!state.onboardingData.targetWeight) {
          showToast("Please set a realistic target weight!", "error");
          return;
        }
        state.onboardingStep = 5;
      } else if (step === 5) {
        // Compute and save target calorie split budgets
        const weight = parseFloat(state.onboardingData.weight) || 80;
        const height = parseFloat(state.onboardingData.height) || 170;
        const age = parseInt(state.onboardingData.age) || 25;
        const base = 10 * weight + 6.25 * height - 5 * age;
        const bmr = state.onboardingData.gender === 'Male' ? Math.round(base + 5) : Math.round(base - 161);
        const tdee = Math.round(bmr * 1.30);
        
        let calorieAdjustment = 0;
        if (state.onboardingData.goal === 'Lose') {
          calorieAdjustment = state.onboardingData.aggression === 'Relaxed' ? -250 : state.onboardingData.aggression === 'Moderate' ? -500 : -750;
        } else if (state.onboardingData.goal === 'Gain') {
          calorieAdjustment = state.onboardingData.aggression === 'Relaxed' ? 250 : state.onboardingData.aggression === 'Moderate' ? 500 : 750;
        }
        const targetCalories = Math.max(1200, tdee + calorieAdjustment);

        state.dailyCalorieGoal = targetCalories;
        state.dietCalorieGoal = targetCalories;
        state.currentWeightKg = weight;
        state.targetWeightKg = parseFloat(state.onboardingData.targetWeight) || weight;
        state.heightCm = height;
        state.age = age;
        state.isMale = state.onboardingData.gender === 'Male';
        state.weightGoal = state.onboardingData.goal.toLowerCase();
        state.aggression = state.onboardingData.aggression.toLowerCase();
        state.hasCompletedOnboarding = true;

        saveStateToStorage();
      }
      mountApp();
    });
  }

  const btnObPrev = document.getElementById('btn-ob-prev');
  if (btnObPrev) {
    btnObPrev.addEventListener('click', () => {
      const step = state.onboardingStep;
      if (step === 4 && state.onboardingData.goal === 'Maintain') {
        state.onboardingStep = 2;
      } else {
        state.onboardingStep = Math.max(0, step - 1);
      }
      mountApp();
    });
  }

  // ── Chat Screen Events ─────────────────────────────────────────────────────
  
  // Date strip days picker
  document.querySelectorAll('.day-cell').forEach(cell => {
    cell.addEventListener('click', () => {
      state.selectedDateStr = cell.dataset.date;
      mountApp();
    });
  });

  // Toggle detailed nutrition collapsible dropdown in-place (no full app mount to avoid flashes)
  const btnToggleNutrition = document.getElementById('btn-toggle-nutrition-dropdown');
  if (btnToggleNutrition) {
    btnToggleNutrition.addEventListener('click', () => {
      state.isNutritionDropdownOpen = !state.isNutritionDropdownOpen;
      saveStateToStorage();
      
      const grid = document.querySelector('.overview-grid');
      if (grid) {
        grid.classList.toggle('active', state.isNutritionDropdownOpen);
      }
      btnToggleNutrition.classList.toggle('open', state.isNutritionDropdownOpen);
    });
  }

  // Collapsible sidebar backdrop close trigger
  const sidebarBackdrop = document.getElementById('sidebar-backdrop');
  if (sidebarBackdrop) {
    sidebarBackdrop.addEventListener('click', () => {
      state.isSidebarOpen = false;
      saveStateToStorage();
      
      const sidebar = document.getElementById('sidebar');
      if (sidebar) {
        sidebar.classList.remove('drawer-open');
      }
      sidebarBackdrop.classList.remove('active');
    });
  }

  // Interactive macro dashboard clicks (Power-BI style)
  document.querySelectorAll('.macro-clickable').forEach(item => {
    item.addEventListener('click', () => {
      state.selectedMacroView = item.dataset.macro;
      mountApp();
    });
  });

  // Food and exercise log deletions
  document.querySelectorAll('.btn-delete-food').forEach(btn => {
    btn.addEventListener('click', () => {
      const id = parseFloat(btn.dataset.id);
      const food = state.foodEntries.find(f => f.id === id);
      if (food && food.messageId) {
        state.chatMessages = state.chatMessages.filter(m => m.id !== food.messageId);
        state.foodEntries = state.foodEntries.filter(f => f.messageId !== food.messageId);
      } else {
        state.foodEntries = state.foodEntries.filter(f => f.id !== id);
      }
      saveStateToStorage();
      mountApp();
    });
  });

  document.querySelectorAll('.btn-delete-exercise').forEach(btn => {
    btn.addEventListener('click', () => {
      const id = parseFloat(btn.dataset.id);
      const exercise = state.exerciseEntries.find(e => e.id === id);
      if (exercise && exercise.messageId) {
        state.chatMessages = state.chatMessages.filter(m => m.id !== exercise.messageId);
        state.exerciseEntries = state.exerciseEntries.filter(e => e.messageId !== exercise.messageId);
      } else {
        state.exerciseEntries = state.exerciseEntries.filter(e => e.id !== id);
      }
      saveStateToStorage();
      mountApp();
    });
  });

  // Edit logged nutrients modal trigger buttons
  document.querySelectorAll('.btn-edit-nutrients').forEach(btn => {
    btn.addEventListener('click', () => {
      state.editingMessageId = parseFloat(btn.dataset.id);
      state.editingMessageType = btn.dataset.type;
      mountApp();
    });
  });

  // Cancel nutrient editor modal
  const btnCancelModal = document.getElementById('btn-cancel-modal');
  if (btnCancelModal) {
    btnCancelModal.addEventListener('click', () => {
      state.editingMessageId = null;
      state.editingMessageType = null;
      mountApp();
    });
  }

  // Save nutrient editor changes
  const btnSaveModal = document.getElementById('btn-save-modal');
  if (btnSaveModal) {
    btnSaveModal.addEventListener('click', () => {
      const isFood = state.editingMessageType === 'food';
      const editRows = document.querySelectorAll('.modal-edit-row');
      
      editRows.forEach(row => {
        const id = parseFloat(row.dataset.id);
        if (isFood) {
          const entry = state.foodEntries.find(f => f.id === id);
          if (entry) {
            const editName = row.querySelector('.edit-food-name');
            const editServing = row.querySelector('.edit-food-serving');
            const editCals = row.querySelector('.edit-food-calories');
            const editProt = row.querySelector('.edit-food-protein');
            const editCarbs = row.querySelector('.edit-food-carbs');
            const editFat = row.querySelector('.edit-food-fat');
            
            if (editName) entry.name = editName.value.trim();
            if (editServing) entry.servingSize = editServing.value.trim();
            if (editCals) entry.calories = Math.round(parseFloat(editCals.value) || 0);
            if (editProt) entry.proteinG = Math.round(parseFloat(editProt.value) || 0);
            if (editCarbs) entry.carbsG = Math.round(parseFloat(editCarbs.value) || 0);
            if (editFat) entry.fatG = Math.round(parseFloat(editFat.value) || 0);
          }
        } else {
          const entry = state.exerciseEntries.find(e => e.id === id);
          if (entry) {
            const editName = row.querySelector('.edit-exercise-name');
            const editDuration = row.querySelector('.edit-exercise-duration');
            const editCals = row.querySelector('.edit-exercise-calories');
            
            if (editName) entry.name = editName.value.trim();
            if (editDuration) entry.duration = Math.round(parseFloat(editDuration.value) || 0);
            if (editCals) entry.caloriesBurned = Math.round(parseFloat(editCals.value) || 0);
          }
        }
      });
      
      state.editingMessageId = null;
      state.editingMessageType = null;
      saveStateToStorage();
      mountApp();
      showToast('Logged entries updated successfully!', 'success');
    });
  }

  // Delete row inside nutrient editor modal
  document.querySelectorAll('.btn-delete-row').forEach(btn => {
    btn.addEventListener('click', () => {
      const id = parseFloat(btn.dataset.id);
      const type = btn.dataset.type;
      const msgId = state.editingMessageId;
      
      if (type === 'food') {
        state.foodEntries = state.foodEntries.filter(f => f.id !== id);
        const remaining = state.foodEntries.filter(f => f.messageId === msgId);
        if (remaining.length === 0) {
          state.chatMessages = state.chatMessages.filter(m => m.id !== msgId);
          state.editingMessageId = null;
          state.editingMessageType = null;
        }
      } else {
        state.exerciseEntries = state.exerciseEntries.filter(e => e.id !== id);
        const remaining = state.exerciseEntries.filter(e => e.messageId === msgId);
        if (remaining.length === 0) {
          state.chatMessages = state.chatMessages.filter(m => m.id !== msgId);
          state.editingMessageId = null;
          state.editingMessageType = null;
        }
      }
      
      saveStateToStorage();
      mountApp();
      showToast('Item deleted successfully!', 'info');
    });
  });

  // Magnified modal close when clicking outer overlay backdrop
  const magnifiedModal = document.getElementById('magnified-modal');
  if (magnifiedModal) {
    magnifiedModal.addEventListener('click', (e) => {
      if (e.target === magnifiedModal) {
        state.editingMessageId = null;
        state.editingMessageType = null;
        mountApp();
      }
    });
  }

  // Sending chat text logs
  const btnSendChat = document.getElementById('btn-send-chat');
  const chatInput = document.getElementById('chat-input');
  
  if (btnSendChat && chatInput) {
    const handleSend = async () => {
      const txt = chatInput.value.trim();
      if (!txt) return;

      chatInput.value = '';
      
      // Append user bubble
      state.chatMessages.push({
        id: Date.now(),
        dateStr: state.selectedDateStr,
        content: txt,
        isUser: true,
        time: new Date().toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
      });
      
      state.isProcessingChat = true;
      mountApp();

      // Process with Gemini API
      let parsed = await parseInputWithGemini(txt);
      state.isProcessingChat = false;

      let reply = "";
      if (parsed.fallbackUsed) {
        reply += `⚠️ Gemini 2.5 limit/availability reached. Auto-fallback to gemini-3.5-flash succeeded!\n\n`;
      }

      // Guard empty parsed items for food and exercise (e.g. from garbage inputs like "wow")
      if (parsed.type === 'food' && (!parsed.items || parsed.items.length === 0)) {
        parsed.type = 'unknown';
        parsed.message = "I couldn't identify any food items in your message. Try saying something like 'I had 3 scrambled eggs for breakfast'!";
      }
      if (parsed.type === 'exercise' && (!parsed.items || parsed.items.length === 0)) {
        parsed.type = 'unknown';
        parsed.message = "I couldn't identify any exercise items in your message. Try saying something like 'walked for 45 minutes'!";
      }

      const assistantMsgId = Date.now() + 1; // Unique ID for assistant response
      const modelName = parsed.modelUsed || state.openRouterModel || 'gemini-2.5-flash';

      if (parsed.type === 'error') {
        reply = `⚠️ AI failed: ${parsed.message}`;
      } else if (parsed.type === 'food') {
        parsed.items.forEach(item => {
          const cals = Math.round(item.calories || 0);
          const prot = Math.round(item.protein_g || item.proteinG || 0);
          const carb = Math.round(item.carbs_g || item.carbsG || 0);
          const fat = Math.round(item.fat_g || item.fatG || 0);

          const itemQty = item.serving_size || item.servingSize || "1 serving (assumed)";
          const foodEntry = {
            id: Date.now() + Math.random(),
            dateStr: state.selectedDateStr,
            name: item.name,
            calories: cals,
            proteinG: prot,
            carbsG: carb,
            fatG: fat,
            servingSize: itemQty,
            timestamp: Date.now(),
            messageId: assistantMsgId
          };
          state.foodEntries.push(foodEntry);
        });

      } else if (parsed.type === 'exercise') {
        parsed.items.forEach(item => {
          const mins = Math.round(item.duration_minutes || item.durationMinutes || 0);
          const burned = Math.round(item.calories_burned || item.caloriesBurned || 0);

          const exerciseEntry = {
            id: Date.now() + Math.random(),
            dateStr: state.selectedDateStr,
            name: item.name,
            duration: mins,
            caloriesBurned: burned,
            timestamp: Date.now(),
            messageId: assistantMsgId
          };
          state.exerciseEntries.push(exerciseEntry);
        });

      } else if (parsed.type === 'unknown') {
        const msg = parsed.message || "I couldn't identify any specific food or exercise in your message. Try saying something like 'I had 3 scrambled eggs' or 'jogged for 30 minutes'!";
        reply += `⚠️ ${msg}`;
      }

      // Append assistant reply bubble
      state.chatMessages.push({
        id: assistantMsgId,
        dateStr: state.selectedDateStr,
        content: parsed.type === 'food' || parsed.type === 'exercise' ? '' : reply,
        isUser: false,
        messageType: parsed.type,
        modelUsed: modelName,
        hasTable: parsed.type === 'food' || parsed.type === 'exercise',
        time: new Date().toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
      });

      saveStateToStorage();
      mountApp();
      
      // Scroll smoothly to bottom of chat after DOM render settlements
      setTimeout(() => {
        const scroller = document.getElementById('chat-scroller');
        if (scroller) {
          scroller.scrollTo({
            top: scroller.scrollHeight,
            behavior: 'smooth'
          });
        }
      }, 60);
    };

    btnSendChat.addEventListener('click', handleSend);
    chatInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') handleSend();
    });
  }

  // ── Weekly Screen Events ───────────────────────────────────────────────────
  document.querySelectorAll('.tab-row .tab-item').forEach(tab => {
    tab.addEventListener('click', () => {
      localStorage.setItem('weeklyTab', tab.dataset.tab);
      mountApp();
    });
  });

  const btnGenerateInsights = document.getElementById('btn-generate-insights');
  if (btnGenerateInsights) {
    btnGenerateInsights.addEventListener('click', () => {
      const tabIdx = parseInt(btnGenerateInsights.dataset.tab);
      generateWeeklyAIInsights(tabIdx);
    });
  }

  const btnRegenerateInsights = document.getElementById('btn-regenerate-insights');
  if (btnRegenerateInsights) {
    btnRegenerateInsights.addEventListener('click', () => {
      const tabIdx = parseInt(btnRegenerateInsights.dataset.tab);
      generateWeeklyAIInsights(tabIdx);
    });
  }

  const btnToggleInsights = document.getElementById('btn-toggle-insights-collapse');
  if (btnToggleInsights) {
    btnToggleInsights.addEventListener('click', (e) => {
      if (e.target.closest('#btn-regenerate-insights')) {
        return;
      }
      state.isWeeklyInsightsOpen = !state.isWeeklyInsightsOpen;
      saveStateToStorage();
      
      const card = btnToggleInsights.closest('.insight-card');
      if (card) {
        card.classList.toggle('open', state.isWeeklyInsightsOpen);
      }
    });
  }

  // ── Weight Screen Events ───────────────────────────────────────────────────
  document.querySelectorAll('.weight-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      localStorage.setItem('weightPeriod', tab.dataset.period);
      mountApp();
    });
  });

  // Modal actions
  const btnAddWeightDialog = document.getElementById('btn-add-weight-dialog');
  if (btnAddWeightDialog) {
    btnAddWeightDialog.addEventListener('click', () => {
      const modal = document.createElement('div');
      modal.className = 'modal-overlay';
      modal.innerHTML = `
        <div class="modal-box">
          <span class="modal-title">Log Weight</span>
          <div class="form-group">
            <label>Weight (kg)</label>
            <input type="number" step="0.1" id="input-weight-val" class="input-style" placeholder="e.g. 78.5" />
          </div>
          <div class="modal-actions">
            <button class="btn-style secondary" id="btn-cancel-modal">Cancel</button>
            <button class="btn-style primary" id="btn-confirm-weight">Add</button>
          </div>
        </div>
      `;
      const appContainer = document.getElementById('app') || document.body;
      appContainer.appendChild(modal);
      
      // Focus input
      const input = document.getElementById('input-weight-val');
      if (input) input.focus();

      document.getElementById('btn-cancel-modal').addEventListener('click', () => modal.remove());
      
      document.getElementById('btn-confirm-weight').addEventListener('click', () => {
        const val = parseFloat(input.value);
        if (val > 0) {
          state.currentWeightKg = val;
          state.weightEntries.push({
            id: Date.now(),
            weightKg: val,
            timestamp: Date.now()
          });
          saveStateToStorage();
          modal.remove();
          mountApp();
        } else {
          showToast("Please enter a valid weight!", "error");
        }
      });
    });
  }

  document.querySelectorAll('.btn-delete-weight').forEach(btn => {
    btn.addEventListener('click', () => {
      const id = parseInt(btn.dataset.id);
      state.weightEntries = state.weightEntries.filter(w => w.id !== id);
      saveStateToStorage();
      mountApp();
    });
  });

  // ── Daily Goals Screen Events ──────────────────────────────────────────────
  const goalsCalories = document.getElementById('goals-calories');
  if (goalsCalories) {
    goalsCalories.addEventListener('input', (e) => {
      const val = parseInt(e.target.value);
      if (val > 0) {
        state.dailyCalorieGoal = val;
        saveStateToStorage();
        // Reactive calculation inside other cards on this screen
        const carbsG = Math.round((state.dailyCalorieGoal * state.carbsPercent / 100) / 4);
        const proteinG = Math.round((state.dailyCalorieGoal * state.proteinPercent / 100) / 4);
        const fatG = Math.round((state.dailyCalorieGoal * state.fatPercent / 100) / 9);
        
        const labelCarbs = document.querySelector('.goals-macro-name');
        if (labelCarbs) labelCarbs.textContent = `Carbohydrates (${carbsG}g)`;
      }
    });
  }

  const goalsCarbs = document.getElementById('goals-carbs');
  const goalsProtein = document.getElementById('goals-protein');
  const goalsFat = document.getElementById('goals-fat');
  
  const handlePercentUpdate = () => {
    if (!goalsCarbs || !goalsProtein || !goalsFat) return;
    const c = parseInt(goalsCarbs.value) || 0;
    const p = parseInt(goalsProtein.value) || 0;
    const f = parseInt(goalsFat.value) || 0;
    
    state.carbsPercent = c;
    state.proteinPercent = p;
    state.fatPercent = f;
    saveStateToStorage();
  };

  if (goalsCarbs) goalsCarbs.addEventListener('input', handlePercentUpdate);
  if (goalsProtein) goalsProtein.addEventListener('input', handlePercentUpdate);
  if (goalsFat) goalsFat.addEventListener('input', handlePercentUpdate);

  const btnGoalsCalculator = document.getElementById('btn-goals-calculator');
  if (btnGoalsCalculator) {
    btnGoalsCalculator.addEventListener('click', () => {
      state.hasCompletedOnboarding = false;
      state.onboardingStep = 0;
      saveStateToStorage();
      mountApp();
    });
  }

  // ── Reminders Screen Events ────────────────────────────────────────────────
  document.querySelectorAll('.reminder-check').forEach(chk => {
    chk.addEventListener('click', () => {
      const type = chk.dataset.type;
      if (type === 'morning') state.morningEnabled = !state.morningEnabled;
      else if (type === 'afternoon') state.afternoonEnabled = !state.afternoonEnabled;
      else if (type === 'evening') state.eveningEnabled = !state.eveningEnabled;
      saveStateToStorage();
      mountApp();
    });
  });

  document.querySelectorAll('.reminder-time-trigger').forEach(trigger => {
    trigger.addEventListener('click', () => {
      const type = trigger.dataset.type;
      const currentVal = type === 'morning' ? state.morningTime : type === 'afternoon' ? state.afternoonTime : state.eveningTime;
      
      const modal = document.createElement('div');
      modal.className = 'modal-overlay';
      modal.innerHTML = `
        <div class="modal-box">
          <span class="modal-title">Edit Reminder Time</span>
          <div class="form-group">
            <label>Time</label>
            <input type="time" id="input-time-val" class="input-style" value="${currentVal}" />
          </div>
          <div class="modal-actions">
            <button class="btn-style secondary" id="btn-cancel-modal">Cancel</button>
            <button class="btn-style primary" id="btn-confirm-time">Save</button>
          </div>
        </div>
      `;
      const appContainer = document.getElementById('app') || document.body;
      appContainer.appendChild(modal);

      document.getElementById('btn-cancel-modal').addEventListener('click', () => modal.remove());
      document.getElementById('btn-confirm-time').addEventListener('click', () => {
        const val = document.getElementById('input-time-val').value;
        if (val) {
          if (type === 'morning') state.morningTime = val;
          else if (type === 'afternoon') state.afternoonTime = val;
          else if (type === 'evening') state.eveningTime = val;
          saveStateToStorage();
          modal.remove();
          mountApp();
        }
      });
    });
  });

  // ── Settings Screen Events ─────────────────────────────────────────────────
  const apiProviders = document.getElementsByName('api-provider');
  apiProviders.forEach(r => {
    r.addEventListener('change', (e) => {
      state.apiProvider = e.target.value;
      saveStateToStorage();
      mountApp();
    });
  });

  const settingsApiKey = document.getElementById('settings-api-key');
  if (settingsApiKey) {
    settingsApiKey.addEventListener('input', (e) => {
      state.apiKey = e.target.value.trim();
      saveStateToStorage();
    });
  }

  const settingsGeminiModel = document.getElementById('settings-gemini-model');
  if (settingsGeminiModel) {
    settingsGeminiModel.addEventListener('change', (e) => {
      state.geminiModel = e.target.value;
      saveStateToStorage();
      console.info("Default model updated by user:", state.geminiModel);
    });
  }

  const openRouterKey = document.getElementById('settings-openrouter-key');
  if (openRouterKey) {
    openRouterKey.addEventListener('input', (e) => {
      state.openRouterApiKey = e.target.value.trim();
      saveStateToStorage();
    });
  }

  const openRouterModelSel = document.getElementById('settings-openrouter-model');
  if (openRouterModelSel) {
    openRouterModelSel.addEventListener('change', (e) => {
      state.openRouterModel = e.target.value;
      saveStateToStorage();
    });
  }



  const btnResetOnboarding = document.getElementById('btn-reset-onboarding');
  if (btnResetOnboarding) {
    btnResetOnboarding.addEventListener('click', () => {
      if (confirm("Are you sure you want to reset onboarding quiz parameters?")) {
        state.hasCompletedOnboarding = false;
        state.onboardingStep = 0;
        saveStateToStorage();
        mountApp();
      }
    });
  }

  const btnClearChat = document.getElementById('btn-clear-chat');
  if (btnClearChat) {
    btnClearChat.addEventListener('click', () => {
      if (confirm("Are you sure you want to clear your entire chat log history and tracking logs?")) {
        state.chatMessages = [];
        state.foodEntries = [];
        state.exerciseEntries = [];
        state.dailyDietPlan = null;
        state.generatedRecipe = null;
        saveStateToStorage();
        mountApp();
        showToast("Entire tracking history cleared successfully! You can start a fresh track chat now.", "success");
      }
    });
  }

  // ── Diet Planner Screen Events ─────────────────────────────────────────────
  if (state.currentScreen === 'diet_planner') {
    const cuisineSelect = document.getElementById('diet-cuisine');
    if (cuisineSelect) {
      cuisineSelect.addEventListener('change', (e) => {
        state.cuisineType = e.target.value;
        saveStateToStorage();
      });
    }

    const styleInput = document.getElementById('diet-style');
    if (styleInput) {
      styleInput.addEventListener('input', (e) => {
        state.cookingStyle = e.target.value;
        saveStateToStorage();
      });
    }

    const calorieInput = document.getElementById('diet-calories');
    if (calorieInput) {
      calorieInput.addEventListener('input', (e) => {
        const val = parseInt(e.target.value);
        if (val > 0) {
          state.dietCalorieGoal = val;
          saveStateToStorage();
        }
      });
    }

    const btnGenerateDiet = document.getElementById('btn-generate-diet');
    if (btnGenerateDiet) {
      btnGenerateDiet.addEventListener('click', async (e) => {
        if (e) {
          e.preventDefault();
          e.stopPropagation();
        }
        startFactRotation();
        state.isGeneratingDiet = true;
        mountApp();
        await generateDailyDietPlan();
        state.isGeneratingDiet = false;
        stopFactRotation();
        mountApp();
      });
    }
  }

  // ── Chef Recipe Screen Events ──────────────────────────────────────────────
  if (state.currentScreen === 'chef_recipe') {
    const ingInput = document.getElementById('chef-ingredients');
    if (ingInput) {
      ingInput.addEventListener('input', (e) => {
        state.chefIngredients = e.target.value;
        saveStateToStorage();
      });
    }

    const calInput = document.getElementById('chef-cal-max');
    if (calInput) {
      calInput.addEventListener('input', (e) => {
        state.chefCalorieMax = e.target.value;
        saveStateToStorage();
      });
    }

    const protInput = document.getElementById('chef-prot-min');
    if (protInput) {
      protInput.addEventListener('input', (e) => {
        state.chefProteinMin = e.target.value;
        saveStateToStorage();
      });
    }

    // Flavor chip clicks
    document.querySelectorAll('.flavor-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        state.chefFlavorProfile = chip.dataset.flavor;
        saveStateToStorage();
        mountApp();
      });
    });

    const btnGenerateRecipe = document.getElementById('btn-generate-recipe');
    if (btnGenerateRecipe) {
      btnGenerateRecipe.addEventListener('click', async (e) => {
        if (e) {
          e.preventDefault();
          e.stopPropagation();
        }
        if (!state.chefIngredients.trim()) {
          showToast("Please type some ingredients you have on hand!", "error");
          return;
        }
        startFactRotation();
        state.isGeneratingRecipe = true;
        mountApp();
        await generateChefRecipe();
        state.isGeneratingRecipe = false;
        stopFactRotation();
        mountApp();
      });
    }
  }
}

let factInterval = null;
function startFactRotation() {
  const facts = [
    "Apples are more efficient than caffeine at waking you up in the morning.",
    "Bananas are berries, but strawberries aren't!",
    "Broccoli contains more protein per calorie than steak.",
    "Watermelon is 92% water, making it excellent for hydration.",
    "Honey is the only food that never spoils. You can eat 3000-year-old honey!",
    "Avocados are a fruit, and they have the most calories of any fruit.",
    "Dark chocolate contains antioxidants that can boost brain function.",
    "Cucumbers are 95% water and can help regulate body temperature.",
    "Drinking water can boost your metabolism by 24-30% over 1-1.5 hours.",
    "A tomato is a fruit, but a sweet potato is a root!",
    "Laughter burns about 10-40 calories in 10-15 minutes.",
    "The average person walks about 7,500 steps a day, which is about 5 times around the Earth in a lifetime!"
  ];
  state.currentLoadingFact = facts[Math.floor(Math.random() * facts.length)];
  if (factInterval) clearInterval(factInterval);
  factInterval = setInterval(() => {
    state.currentLoadingFact = facts[Math.floor(Math.random() * facts.length)];
    const factEl = document.getElementById('loading-fact-text');
    if (factEl) {
      factEl.style.opacity = 0;
      setTimeout(() => {
        factEl.textContent = state.currentLoadingFact;
        factEl.style.opacity = 1;
      }, 300);
    }
  }, 4000);
}

function stopFactRotation() {
  if (factInterval) {
    clearInterval(factInterval);
    factInterval = null;
  }
}

function renderLoadingOverlay(titleText) {
  const fact = state.currentLoadingFact || "Preparing something healthy...";
  return `
    <div class="modal-overlay" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border-radius: inherit; background: rgba(0, 0, 0, 0.7); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: center; z-index: 100;">
      <div class="fun-loader-container">
        <div class="fun-loader-ring">
          <i data-lucide="sparkles" class="fun-loader-icon" style="width: 32px; height: 32px;"></i>
        </div>
        <h4 style="font-weight: 800; font-size: 1.1rem; color: var(--primary); margin-bottom: 4px;">${titleText}</h4>
        <p style="font-size: 0.8rem; opacity: 0.8; color: var(--on-surface-variant);">Please wait a moment</p>
        
        <div class="loading-fact-card">
          <div class="loading-fact-title">
            <i data-lucide="lightbulb" style="width: 14px; height: 14px; color: var(--primary);"></i> Fun Nutrition Fact
          </div>
          <div id="loading-fact-text" class="loading-fact-body">${fact}</div>
        </div>
      </div>
    </div>
  `;
}

// ── Screen Rendering Functions ───────────────────────────────────────────────
function renderDietPlannerScreen() {
  const cuisineOptions = ["Indian", "Italian", "Mexican", "Asian", "Mediterranean", "American", "Middle Eastern"];
  const plan = state.dailyDietPlan;
  const targetDateStr = state.selectedDateStr;
  const currentDayName = getDayNameFromDateStr(targetDateStr);

  return `
    <div style="display: flex; flex-direction: column; gap: 24px;">
      <!-- Planner Form Card -->
      <div class="card-content-box" style="position: relative;">
        <h3 style="font-size: 1.25rem; font-weight: 700; margin-bottom: 8px;">AI Daily Diet Planner</h3>
        <p style="font-size: 0.85rem; color: var(--on-surface-variant); margin-bottom: 16px;">
          Cater a personalized single-day meal plan calculated to achieve your metabolic weight goals and target calorie budget for today!
        </p>

        <div class="form-group" style="margin-bottom: 14px;">
          <label for="diet-cuisine">Cuisine Type</label>
          <select id="diet-cuisine" class="input-style" style="background-color: var(--surface); color: var(--on-surface); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 8px; width: 100%;">
            ${cuisineOptions.map(c => `
              <option value="${c}" ${state.cuisineType === c ? 'selected' : ''}>${c}</option>
            `).join('')}
          </select>
        </div>

        <div class="form-group" style="margin-bottom: 14px;">
          <label for="diet-style">Household Dinner Cooking Style</label>
          <input type="text" id="diet-style" class="input-style" value="${state.cookingStyle}" placeholder="e.g. Simple home cooking, One-pot meals, Low carb..." />
        </div>

        <div class="form-group" style="margin-bottom: 18px;">
          <label for="diet-calories">Target Calories (kcal/day)</label>
          <input type="number" id="diet-calories" class="input-style" value="${state.dietCalorieGoal}" placeholder="e.g. 2000" />
        </div>

        <button type="button" class="btn-style primary" id="btn-generate-diet" style="width: 100%; height: 50px; margin-top: 8px; display: flex; justify-content: center; align-items: center; gap: 8px; font-weight: 700;">
          <i data-lucide="sparkles"></i> Generate Plan for ${currentDayName}
        </button>

        ${state.isGeneratingDiet ? renderLoadingOverlay("AI Nutritionist Crafting Plan") : ''}
      </div>

      <!-- Plan Cards Grid -->
      ${plan ? `
        ${plan.error ? `
          <div class="card-content-box" style="border: 1px solid var(--error); background-color: var(--error-container); color: var(--error); padding: 20px; display: flex; flex-direction: column; gap: 8px; border-radius: var(--radius-sm);">
            <h4 style="font-weight: 700; font-size: 1.1rem; color: var(--error); display: flex; align-items: center; gap: 8px;">
              <i data-lucide="alert-triangle" style="width: 20px; height: 20px; color: var(--error);"></i> Live AI Plan Failed
            </h4>
            <p style="font-size: 0.85rem; opacity: 0.95; line-height: 1.4;">${plan.error}</p>
            <p style="font-size: 0.78rem; opacity: 0.7; margin-top: 6px; border-top: 1px dashed hsla(355, 75%, 45%, 0.15); padding-top: 6px;">
              Please switch to Settings to check your Gemini API key or select another model!
            </p>
          </div>
        ` : `
          <div style="display: flex; flex-direction: column; gap: 16px;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <h3 style="font-size: 1.15rem; font-weight: 700;">Your Personal Menu</h3>

            </div>

            <div class="recipe-cookbook-card">
              <div style="background-color: var(--primary-container); color: var(--primary); padding: 18px; border-radius: 12px 12px 0 0; display: flex; justify-content: space-between; align-items: center;">
                <div>
                  <span style="font-size: 1.25rem; font-weight: 800; display: block; line-height: 1.25;">Plan for ${plan.dayName || getDayNameFromDateStr(plan.dateStr || targetDateStr)}</span>
                  <span style="font-size: 0.85rem; font-weight: 500; opacity: 0.85; display: block; margin-top: 4px;">📅 Date: ${plan.dateStr || targetDateStr}</span>
                </div>
                <span style="font-size: 1.35rem; font-weight: 800; color: var(--primary);">${plan.totalCalories} kcal</span>
              </div>

              <!-- Macro split pills -->
              <div class="recipe-macros-box" style="display: flex; gap: 10px; margin: 16px; justify-content: space-around;">
                <div class="recipe-macro-pill" style="flex: 1; text-align: center; background-color: var(--surface-variant); padding: 8px; border-radius: 10px;">
                  <span style="font-weight: 800; display: block; font-size: 1.1rem; color: var(--color-protein);">${plan.protein_g}g</span>
                  <span style="font-size: 0.72rem; opacity: 0.7; font-weight: 600;">Protein</span>
                </div>
                <div class="recipe-macro-pill" style="flex: 1; text-align: center; background-color: var(--surface-variant); padding: 8px; border-radius: 10px;">
                  <span style="font-weight: 800; display: block; font-size: 1.1rem; color: var(--color-carbs);">${plan.carbs_g}g</span>
                  <span style="font-size: 0.72rem; opacity: 0.7; font-weight: 600;">Carbs</span>
                </div>
                <div class="recipe-macro-pill" style="flex: 1; text-align: center; background-color: var(--surface-variant); padding: 8px; border-radius: 10px;">
                  <span style="font-weight: 800; display: block; font-size: 1.1rem; color: var(--color-fat);">${plan.fat_g}g</span>
                  <span style="font-size: 0.72rem; opacity: 0.7; font-weight: 600;">Fat</span>
                </div>
              </div>

              <!-- Meals list -->
              <div style="display: flex; flex-direction: column; gap: 12px; padding: 0 16px 16px 16px;">
                ${plan.meals.map(meal => `
                  <div style="background-color: var(--surface-variant); border-radius: 12px; padding: 14px 16px; border: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; gap: 12px;">
                    <div style="display: flex; flex-direction: column; gap: 4px; flex: 1;">
                      <span style="font-weight: 800; font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--primary);">${meal.type}</span>
                      <span style="font-weight: 700; font-size: 0.95rem; color: var(--on-surface);">${meal.name}</span>
                      <span style="font-size: 0.74rem; color: var(--on-surface-variant); font-weight: 500;">🥩 ${meal.macros}</span>
                    </div>
                    <span style="font-weight: 800; font-size: 1rem; color: var(--primary); flex-shrink: 0; white-space: nowrap;">${meal.calories} kcal</span>
                  </div>
                `).join('')}
              </div>
            </div>
          </div>
        `}
      ` : `
        <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 20px; opacity: 0.6; text-align: center; gap: 12px; background-color: var(--surface); border: 1px solid var(--border); border-radius: var(--radius);">
          <i data-lucide="calendar-days" style="width: 48px; height: 48px; color: var(--primary);"></i>
          <p style="font-weight: 600;">No meal plan generated yet for ${currentDayName}. Select a cuisine and click generate above!</p>
        </div>
      `}
    </div>
  `;
}

function renderChefRecipeScreen() {
  const flavorProfiles = ["Spicy", "Sweet", "Salty", "Savory", "Sour", "Tangy"];
  const recipe = state.generatedRecipe;

  return `
    <div style="display: flex; flex-direction: column; gap: 24px;">
      <!-- Chef Inputs Card -->
      <div class="card-content-box" style="position: relative;">
        <h3 style="font-size: 1.25rem; font-weight: 700; margin-bottom: 8px;">AI Gourmet Chef Recipes</h3>
        <p style="font-size: 0.85rem; color: var(--on-surface-variant); margin-bottom: 16px;">
          Suggest custom, healthy, cookbook-themed recipes using exactly what you have in your pantry!
        </p>

        <div class="form-group" style="margin-bottom: 14px;">
          <label for="chef-ingredients">Ingredients on Hand</label>
          <textarea id="chef-ingredients" class="input-style" style="height: 80px; resize: none; padding: 10px;" placeholder="e.g. Chicken breast, baby spinach, garlic cloves, cherry tomatoes...">${state.chefIngredients}</textarea>
        </div>

        <div class="form-row" style="display: flex; gap: 12px; margin-bottom: 14px;">
          <div class="form-group" style="flex: 1;">
            <label for="chef-cal-max">Max Calories (kcal)</label>
            <input type="number" id="chef-cal-max" class="input-style" value="${state.chefCalorieMax}" placeholder="600" />
          </div>
          <div class="form-group" style="flex: 1;">
            <label for="chef-prot-min">Min Protein (g)</label>
            <input type="number" id="chef-prot-min" class="input-style" value="${state.chefProteinMin}" placeholder="25" />
          </div>
        </div>

        <div class="form-group" style="margin-bottom: 18px;">
          <label>Flavor Craving Profile</label>
          <div class="flavor-chips-grid">
            ${flavorProfiles.map(f => `
              <div class="flavor-chip ${state.chefFlavorProfile === f ? 'selected' : ''}" data-flavor="${f}">${f}</div>
            `).join('')}
          </div>
        </div>

        <button type="button" class="btn-style primary" id="btn-generate-recipe" style="width: 100%; height: 50px; margin-top: 8px; display: flex; justify-content: center; align-items: center; gap: 8px; font-weight: 700;">
          <i data-lucide="cooking-pot"></i> Generate Gourmet Recipe
        </button>

        ${state.isGeneratingRecipe ? renderLoadingOverlay("Chef Crafting Custom Recipe") : ''}
      </div>

      <!-- Generated Recipe Cookbook Card -->
      ${recipe ? `
        ${recipe.error ? `
          <div class="card-content-box" style="border: 1px solid var(--error); background-color: var(--error-container); color: var(--error); padding: 20px; display: flex; flex-direction: column; gap: 8px; border-radius: var(--radius-sm);">
            <h4 style="font-weight: 700; font-size: 1.1rem; color: var(--error); display: flex; align-items: center; gap: 8px;">
              <i data-lucide="alert-triangle" style="width: 20px; height: 20px; color: var(--error);"></i> Gourmet Recipe Failed
            </h4>
            <p style="font-size: 0.85rem; opacity: 0.95; line-height: 1.4;">${recipe.error}</p>
            <p style="font-size: 0.78rem; opacity: 0.7; margin-top: 6px; border-top: 1px dashed hsla(355, 75%, 45%, 0.15); padding-top: 6px;">
              Please switch to Settings to check your Gemini API key or select another model!
            </p>
          </div>
        ` : `
          <div style="display: flex; flex-direction: column; gap: 16px;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <h3 style="font-size: 1.15rem; font-weight: 700;">Gourmet Suggestion</h3>

            </div>

            <div class="recipe-cookbook-card">
              <div class="recipe-header-box">
                <span class="recipe-title">${recipe.recipeTitle}</span>
                <div class="recipe-meta-row" style="margin-top: 6px;">
                  <span style="display: flex; align-items: center; gap: 4px;"><i data-lucide="clock" style="width: 14px; height: 14px;"></i> Prep: ${recipe.prepTime}</span>
                  <span style="display: flex; align-items: center; gap: 4px;"><i data-lucide="flame" style="width: 14px; height: 14px;"></i> Cook: ${recipe.cookTime}</span>
                  <span style="display: flex; align-items: center; gap: 4px;"><i data-lucide="users" style="width: 14px; height: 14px;"></i> Servings: ${recipe.servings}</span>
                </div>
              </div>

              <!-- Recipe Macros splits box -->
              <div class="recipe-macros-box">
                <div class="recipe-macro-pill">
                  <span class="recipe-macro-val" style="color: var(--color-calories);">${recipe.calories}</span>
                  <span class="recipe-macro-label">kcal</span>
                </div>
                <div class="recipe-macro-pill">
                  <span class="recipe-macro-val" style="color: var(--color-protein);">${recipe.protein}g</span>
                  <span class="recipe-macro-label">Protein</span>
                </div>
                <div class="recipe-macro-pill">
                  <span class="recipe-macro-val" style="color: var(--color-carbs);">${recipe.carbs}g</span>
                  <span class="recipe-macro-label">Carbs</span>
                </div>
                <div class="recipe-macro-pill">
                  <span class="recipe-macro-val" style="color: var(--color-fat);">${recipe.fat}g</span>
                  <span class="recipe-macro-label">Fat</span>
                </div>
              </div>

              <!-- Ingredients Checklist -->
              <div>
                <h4 class="recipe-section-title">Ingredients Checklist</h4>
                <ul class="recipe-ingredients-list" style="list-style: none; padding-left: 0;">
                  ${recipe.ingredients.map((ing, i) => `
                    <li style="display: flex; align-items: flex-start; gap: 10px; margin-bottom: 6px;">
                      <input type="checkbox" id="ing-chk-${i}" style="margin-top: 3px; cursor: pointer;" />
                      <label for="ing-chk-${i}" style="cursor: pointer; font-size: 0.9rem;">${ing}</label>
                    </li>
                  `).join('')}
                </ul>
              </div>

              <!-- Cooking Instructions list -->
              <div>
                <h4 class="recipe-section-title">Step-by-step Instructions</h4>
                <ol class="recipe-instructions-list">
                  ${recipe.instructions.map(step => `
                    <li style="margin-bottom: 8px;">${step}</li>
                  `).join('')}
                </ol>
              </div>

              <!-- Chef Pro-Tip Box -->
              ${recipe.chefProTip ? `
                <div class="recipe-chef-tip-box">
                  <i data-lucide="lightbulb" style="color: var(--primary);"></i>
                  <div>
                    <span style="font-weight: 800; display: block; margin-bottom: 2px;">Chef's Pro-Tip</span>
                    <span>${recipe.chefProTip}</span>
                  </div>
                </div>
              ` : ''}
            </div>
          </div>
        `}
      ` : `
        <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 20px; opacity: 0.6; text-align: center; gap: 12px; background-color: var(--surface); border: 1px solid var(--border); border-radius: var(--radius);">
          <i data-lucide="cooking-pot" style="width: 48px; height: 48px; color: var(--primary);"></i>
          <p style="font-weight: 600;">No recipe generated yet. Type ingredients on hand and click generate above!</p>
        </div>
      `}
    </div>
  `;
}

// ── AI Service Integrations ──────────────────────────────────────────────────
function cleanAndParseJSON(str) {
  if (!str) throw new Error("Empty response string");
  
  let cleaned = str.trim();
  
  // 1. Strip markdown fences if present
  if (cleaned.includes('```')) {
    const match = cleaned.match(/```(?:json)?\s*([\s\S]*?)\s*```/);
    if (match && match[1]) {
      cleaned = match[1].trim();
    } else {
      cleaned = cleaned.replace(/^```[a-zA-Z]*\n?/, '').replace(/\n?```$/, '').trim();
    }
  }
  
  // 2. Remove trailing commas in arrays/objects which violate JSON spec
  cleaned = cleaned.replace(/,\s*([\]}])/g, '$1');

  // 3. Try direct parse first (works when responseMimeType: application/json is respected)
  try {
    return JSON.parse(cleaned);
  } catch (firstErr) {
    console.warn("Direct JSON.parse failed, attempting repairs...", firstErr.message);
  }

  // 4. Try to extract JSON object from start brace to last brace
  const startIdx = cleaned.indexOf('{');
  const endIdx = cleaned.lastIndexOf('}');
  if (startIdx !== -1 && endIdx !== -1 && endIdx > startIdx) {
    const extracted = cleaned.substring(startIdx, endIdx + 1);
    try {
      return JSON.parse(extracted);
    } catch (e) {
      console.warn("Brace-aligned parse failed too:", e.message);
    }
  }

  // 5. Handle truncated JSON: if the response was cut off, try to complete it
  //    Count open braces/brackets and close them
  let truncFixed = cleaned;
  if (startIdx !== -1) {
    truncFixed = cleaned.substring(startIdx);
  }
  
  // Check if it looks truncated (no final closing brace, or unbalanced)
  let braceCount = 0;
  let bracketCount = 0;
  let inStr = false;
  let escaped = false;
  for (let i = 0; i < truncFixed.length; i++) {
    const c = truncFixed[i];
    if (inStr) {
      if (escaped) { escaped = false; continue; }
      if (c === '\\') { escaped = true; continue; }
      if (c === '"') { inStr = false; }
    } else {
      if (c === '"') inStr = true;
      else if (c === '{') braceCount++;
      else if (c === '}') braceCount--;
      else if (c === '[') bracketCount++;
      else if (c === ']') bracketCount--;
    }
  }
  
  // If we ended inside a string, close it
  if (inStr) {
    truncFixed += '"';
  }
  
  // Remove any trailing comma before we close
  truncFixed = truncFixed.replace(/,\s*$/, '');
  
  // Close any unclosed brackets/braces
  while (bracketCount > 0) { truncFixed += ']'; bracketCount--; }
  while (braceCount > 0) { truncFixed += '}'; braceCount--; }
  
  try {
    return JSON.parse(truncFixed);
  } catch (finalErr) {
    console.error("All JSON repair attempts failed. Final string:", truncFixed.substring(0, 500));
    throw new Error(`JSON parse failed after all repairs: ${finalErr.message}`);
  }
}

async function callGeminiGeneric(prompt) {
  if (state.apiProvider === 'openrouter') {
    if (!state.openRouterApiKey) {
      throw new Error("OpenRouter API Key not configured in Settings.");
    }
    return callOpenRouterGeneric(prompt, state.openRouterApiKey, state.openRouterModel);
  }

  const modelList = getModelExecutionList(state.geminiModel);
  let lastError = null;

  const payload = {
    contents: [{ parts: [{ text: prompt }] }],
    generationConfig: {
      temperature: 0.3,
      maxOutputTokens: 8192,
      responseMimeType: "application/json"
    }
  };

  for (const model of modelList) {
    try {
      console.info(`Attempting generic prompt with model: ${model}`);
      const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${state.apiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (response.ok) {
        console.info(`Model ${model} generic prompt succeeded!`);
        return response;
      } else {
        const errorMsg = await handleApiError(response);
        console.warn(`Model ${model} generic prompt failed: ${errorMsg}`);
        lastError = new Error(`Model ${model}: ${errorMsg}`);
      }
    } catch (err) {
      console.warn(`Model ${model} generic prompt caught error: ${err.message}`);
      lastError = err;
    }
  }
  throw lastError || new Error("All Gemini models failed generic prompt.");
}

async function generateDailyDietPlan() {
  const targetDateStr = state.selectedDateStr;
  const dayName = getDayNameFromDateStr(targetDateStr);

  if (!state.apiKey) {
    showToast("Please add your Gemini API Key in Settings to generate diet plans!", "error");
    state.dailyDietPlan = { error: 'No Gemini API Key configured. Go to Settings and paste your API key to enable AI-powered diet planning.' };
    saveStateToStorage();
    return;
  }

  const dietPrompt = `
You are a professional nutritionist. Generate a single-day meal plan:
- Date: ${targetDateStr} (${dayName})
- Target: ${state.dietCalorieGoal} kcal (±100)
- Cuisine: ${state.cuisineType}
- Style: ${state.cookingStyle}
- Macros: ~25% Protein, ~50% Carbs, ~25% Fat
- Exactly 4 meals: Breakfast, Lunch, Snack, Dinner.

CRITICAL: Keep meal "name" fields SHORT (max 12 words). No lengthy descriptions.

Return ONLY valid JSON matching this schema:
{"success":true,"dateStr":"${targetDateStr}","dayName":"${dayName}","totalCalories":1980,"protein_g":120,"carbs_g":220,"fat_g":60,"meals":[{"type":"Breakfast","name":"Short meal name here","calories":400,"macros":"P: 25g, C: 45g, F: 12g"},{"type":"Lunch","name":"Short meal name here","calories":600,"macros":"P: 35g, C: 70g, F: 20g"},{"type":"Snack","name":"Short meal name here","calories":250,"macros":"P: 15g, C: 30g, F: 8g"},{"type":"Dinner","name":"Short meal name here","calories":730,"macros":"P: 45g, C: 75g, F: 20g"}]}
`;

  try {
    const res = await callGeminiGeneric(dietPrompt);
    const data = await res.json();
    const rawJsonStr = data.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || "";
    console.log("Gemini Daily Diet Plan Raw Response:", rawJsonStr);
    const parsed = cleanAndParseJSON(rawJsonStr);
    
    if (parsed && parsed.success) {
      state.dailyDietPlan = parsed;
      saveStateToStorage();
    } else {
      throw new Error("Invalid output format from Gemini");
    }
  } catch (err) {
    console.error("Failed to generate diet plan:", err);
    showToast(`AI generation failed: ${err.message || 'connection error'}`, 'error');
    state.dailyDietPlan = { error: err.message || 'Connection or parsing error' };
    saveStateToStorage();
  }
}

async function generateChefRecipe() {
  if (!state.apiKey) {
    showToast("Please add your Gemini API Key in Settings to generate recipes!", "error");
    state.generatedRecipe = { error: 'No Gemini API Key configured. Go to Settings and paste your API key to enable AI-powered recipe generation.' };
    saveStateToStorage();
    return;
  }

  const recipePrompt = `
Create a gourmet healthy recipe:
- Ingredients available: ${state.chefIngredients}
- Max Calories: ${state.chefCalorieMax} kcal
- Min Protein: ${state.chefProteinMin}g
- Flavor: ${state.chefFlavorProfile}

CRITICAL: Keep all string values concise. Max 5 ingredients, max 5 instruction steps, each step max 20 words.

Return ONLY valid JSON:
{"success":true,"recipeTitle":"Recipe Title","prepTime":"15 mins","cookTime":"20 mins","servings":1,"calories":450,"protein":35,"carbs":40,"fat":15,"flavor":"${state.chefFlavorProfile}","ingredients":["qty ingredient 1","qty ingredient 2"],"instructions":["Step 1","Step 2"],"chefProTip":"One sentence tip"}
`;

  try {
    const res = await callGeminiGeneric(recipePrompt);
    const data = await res.json();
    const rawJsonStr = data.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || "";
    console.log("Gemini Chef Recipe Raw Response:", rawJsonStr);
    const parsed = cleanAndParseJSON(rawJsonStr);
    
    if (parsed && parsed.success) {
      state.generatedRecipe = parsed;
      saveStateToStorage();
    } else {
      throw new Error("Invalid output format from Gemini");
    }
  } catch (err) {
    console.error("Failed to generate chef recipe:", err);
    showToast(`AI generation failed: ${err.message || 'connection error'}`, 'error');
    state.generatedRecipe = { error: err.message || 'Connection or parsing error' };
    saveStateToStorage();
  }
}

async function generateWeeklyAIInsights(tabIndex) {
  if (!state.apiKey) {
    showToast("Please add your Gemini API Key in Settings to generate AI insights!", "error");
    return;
  }

  const today = new Date();
  const currentDayOfWeek = today.getDay();
  const startOfCurrentWeek = new Date(today.getTime() - currentDayOfWeek * 24 * 60 * 60 * 1000);
  startOfCurrentWeek.setHours(0,0,0,0);
  
  const weekStart = new Date(startOfCurrentWeek.getTime() - tabIndex * 7 * 24 * 60 * 60 * 1000);
  const weekDays = [];
  for (let i = 0; i < 7; i++) {
    weekDays.push(new Date(weekStart.getTime() + i * 24 * 60 * 60 * 1000));
  }
  
  const formattedStart = weekDays[0].toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
  const formattedEnd = weekDays[6].toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
  const weekKey = `${formattedStart} — ${formattedEnd}`;

  // Gather logs for each day of the week
  let weeklyFoodTotal = 0;
  let weeklyExerciseTotal = 0;
  let daysTrackedCount = 0;

  const weeklySummaryText = weekDays.map(date => {
    const dStr = getLocalDateString(date);
    const summary = getDailyNutritionSummary(dStr);
    
    const hasData = state.foodEntries.some(f => f.dateStr === dStr) || state.exerciseEntries.some(e => e.dateStr === dStr);
    if (hasData) {
      daysTrackedCount++;
      weeklyFoodTotal += summary.totalCalories;
      weeklyExerciseTotal += summary.exerciseCalories;
    } else {
      return `${dStr}: Unlogged (No entries logged)`;
    }

    const dayFoods = state.foodEntries
      .filter(f => f.dateStr === dStr)
      .map(f => `${f.name} (${f.servingSize || '1 serving'}, ${f.calories} kcal, P:${f.proteinG}g C:${f.carbsG}g F:${f.fatG}g)`)
      .join(', ');

    const dayExercises = state.exerciseEntries
      .filter(e => e.dateStr === dStr)
      .map(e => `${e.name} (${e.duration} mins, -${e.caloriesBurned} kcal)`)
      .join(', ');

    return `${dStr}: Food Calories Consumed: ${summary.totalCalories} kcal [${dayFoods || 'None'}]. Exercise Calories Burned: ${summary.exerciseCalories} kcal [${dayExercises || 'None'}]. Remaining Budget: ${state.dailyCalorieGoal - summary.totalCalories + summary.exerciseCalories} kcal.`;
  }).join('\n');

  const prompt = `
You are a premium AI Health & Nutrition coach. Analyze the user's weekly health log and targets:
- Profile: ${state.weightGoal} weight goal (aggression: ${state.aggression}), Target Weight: ${state.targetWeightKg} kg, Current Weight: ${state.currentWeightKg} kg
- Demographics: Age ${state.age}, Biological Gender: ${state.isMale ? 'Male' : 'Female'}
- Target Daily Calories: ${state.dailyCalorieGoal} kcal
- Date Range: ${weekKey}
- Tracked Days: ${daysTrackedCount} out of 7
- Logged items & nutrition totals for each day:
${weeklySummaryText}

Generate a concise, beautiful health analysis report. Bullet points must be short and direct.
Praise their positive patterns (e.g. running on Monday, meeting protein goals). Constructively identify concerns (e.g. eating too much sweets, skipping logs, high fat/sugar items, insufficient food intake). Outline what to focus on and what to avoid, and provide 2 actionable habit tips.

Return ONLY a valid JSON object matching this schema:
{"good":["Short bullet point praising positive behavior (max 15 words)"],"bad":["Short constructive point about concern/deficiency (max 15 words)"],"focus":["What specifically to focus on next week (max 15 words)"],"avoid":["Specific food, habit, or choice to avoid (max 15 words)"],"tips":["Short actionable tip (max 20 words)"]}
`;

  state.isGeneratingWeeklyInsights = true;
  mountApp();

  try {
    const res = await callGeminiGeneric(prompt);
    const data = await res.json();
    const rawJsonStr = data.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || "";
    console.log("Gemini Weekly Insights Raw Response:", rawJsonStr);
    const parsed = cleanAndParseJSON(rawJsonStr);
    
    if (parsed && (parsed.good || parsed.bad || parsed.tips)) {
      state.weeklyInsights[weekKey] = parsed;
      saveStateToStorage();
    } else {
      throw new Error("Invalid output format from Gemini");
    }
  } catch (err) {
    console.error("Failed to generate weekly insights:", err);
    showToast(`AI analysis failed: ${err.message || 'connection error'}`, 'error');
    state.weeklyInsights[weekKey] = { error: err.message || 'Connection or parsing error' };
    saveStateToStorage();
  } finally {
    state.isGeneratingWeeklyInsights = false;
    mountApp();
  }
}

// ── Utility ────────────────────────────────────────────────────────────────────
function getDayNameFromDateStr(dateStr) {
  const parsed = parseLocalDate(dateStr);
  return parsed.toLocaleDateString('en-US', { weekday: 'long' });
}

// ── Global Error Display (shows red screen instead of white) ────────────────
function showFatalError(msg) {
  document.body.style.cssText = 'margin:0;padding:20px;background:#1a0000;color:#ff6b6b;font-family:monospace;font-size:14px;white-space:pre-wrap;word-break:break-all;';
  document.body.innerHTML = '<h2 style="color:#ff4444;margin-bottom:16px">⚠️ DAYWISE ERROR</h2>' + msg;
}

window.onerror = function(msg, src, line, col, err) {
  showFatalError('UNCAUGHT ERROR:\n' + msg + '\n\nFile: ' + src + '\nLine: ' + line + '\n\n' + (err && err.stack ? err.stack : ''));
  return true;
};

window.addEventListener('unhandledrejection', function(e) {
  showFatalError('UNHANDLED PROMISE:\n' + (e.reason && e.reason.stack ? e.reason.stack : String(e.reason)));
});

// Mount app on initial load
console.log("Daywise Web App Initialized Successfully!");
try {
  mountApp();
} catch(e) {
  showFatalError('MOUNT ERROR:\n' + e.message + '\n\n' + (e.stack || ''));
}
