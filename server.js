import express from 'express';
import axios from 'axios';
import cors from 'cors';
import dotenv from 'dotenv';
import { createLogger, format, transports } from 'winston';

// Logger setup
const logger = createLogger({
  level: 'info',
  format: format.combine(
    format.timestamp(),
    format.printf(({ timestamp, level, message }) => {
      return `${timestamp} ${level}: ${message}`;
    })
  ),
  transports: [new transports.Console()]
});

// Configuration
dotenv.config();
const app = express();
const port = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Gemini Configuration
const API_KEY = "AIzaSyAiphEyvVD6aP7Z5lTngfnwwQYiscknJNo";
if (!API_KEY) {
  logger.warn("⚠ GEMINI_API_KEY is missing. AI responses will not work.");
}
const GEMINI_API_URL = `https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro:generateContent?key=${API_KEY}`;

// Conversation history storage
const conversationHistories = new Map();

// Helper Functions
async function generateResponse(prompt) {
  try {
    const response = await axios.post(GEMINI_API_URL, {
      contents: [{ role: "user", parts: [{ text: prompt }] }]
    });
    logger.info("✅ Gemini response received");
    return response.data;
  } catch (error) {
    logger.error("❌ Gemini Error:", error.response?.data || error.message);
    throw new Error("Failed to generate response from Gemini AI");
  }
}

function buildMedicalPrompt(symptoms, history = []) {
  const basePrompt = `Analyze these symptoms: ${symptoms}\n` +
    `Format response as:\n` +
    `1. [Condition] (Likelihood: High/Medium/Low)\n` +
    `2. [Condition] (Likelihood: High/Medium/Low)\n` +
    `3. [Condition] (Likelihood: High/Medium/Low)\n\n` +
    `Include brief recommendations for each.\n` +
    `DISCLAIMER: This is not medical advice. Consult a doctor.`;

  if (history.length > 0) {
    return `Previous conversation:\n${history.join('\n')}\n\nCurrent query:\n${basePrompt}`;
  }
  return basePrompt;
}

// Routes
app.post('/analyze-symptoms', async (req, res) => {
  try {
    const { symptoms, userId } = req.body;
    
    // Input validation
    if (!symptoms || typeof symptoms !== 'string') {
      logger.warn("⚠ Invalid symptoms input");
      return res.status(400).json({ error: "Valid symptoms string required" });
    }

    // Get conversation history if userId provided
    let history = [];
    if (userId && conversationHistories.has(userId)) {
      history = conversationHistories.get(userId);
    }

    // Generate prompt
    const prompt = buildMedicalPrompt(symptoms, history);
    logger.debug(`Generated prompt: ${prompt}`);

    // Get AI response
    const result = await generateResponse(prompt);
    
    if (!result?.candidates?.[0]?.content?.parts?.[0]?.text) {
      throw new Error("Invalid response structure from Gemini");
    }

    const analysis = result.candidates[0].content.parts[0].text;

    // Update conversation history if userId provided
    if (userId) {
      if (!conversationHistories.has(userId)) {
        conversationHistories.set(userId, []);
      }
      conversationHistories.get(userId).push(
        `User: ${symptoms}`,
        `Doctor: ${analysis}`
      );
    }

    logger.info(`✅ Analysis completed for ${userId || 'anonymous'}`);
    res.json({ 
      analysis,
      timestamp: new Date().toISOString()
    });

  } catch (error) {
    logger.error(`❌ Analysis Error: ${error.message}`);
    res.status(500).json({ 
      error: "Analysis failed",
      details: error.message 
    });
  }
});

// Start server
app.listen(port, () => {
  logger.info(`🚀 Server running on http://localhost:${port}`);
});