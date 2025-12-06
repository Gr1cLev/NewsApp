/**
 * Cloud Functions for NewsApp ML Training
 * 
 * This module provides:
 * 1. Scheduled training every 3 days (auto-retrain)
 * 2. Manual training trigger via HTTP
 * 3. Model version management
 * 4. Fallback to rule-based when model unavailable
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import {trainCollaborativeFilteringModel} from "./ml/trainer";
import {aggregateUserInteractions} from "./ml/dataCollector";
import {uploadModelToStorage} from "./ml/modelManager";

admin.initializeApp();

/**
 * Scheduled training function - Runs every 3 days at 2 AM UTC
 * Cron schedule: "0 2 */3 * *" = At 02:00 AM, every 3 days
 */
export const scheduledModelTraining = functions
  .runWith({
    timeoutSeconds: 540, // 9 minutes max
    memory: "2GB",
  })
  .pubsub
  .schedule("0 2 */3 * *")
  .timeZone("UTC")
  .onRun(async (context) => {
    try {
      console.log("🚀 Starting scheduled ML model training...");
      console.log(`Execution time: ${context.timestamp}`);

      // Step 1: Collect training data from Firestore
      console.log("📊 Step 1: Collecting user interaction data...");
      const trainingData = await aggregateUserInteractions();

      // Check if we have enough data to train
      if (trainingData.totalUsers < 5 || trainingData.totalInteractions < 50) {
        console.warn("⚠️ Not enough data for training:");
        console.warn(`  - Users: ${trainingData.totalUsers} (min: 5)`);
        console.warn(`  - Interactions: ${trainingData.totalInteractions} (min: 50)`);
        console.log("⏭️ Skipping training, will retry in 3 days");
        return {
          success: false,
          reason: "insufficient_data",
          data: trainingData,
        };
      }

      console.log(`✅ Training data collected:`);
      console.log(`  - Users: ${trainingData.totalUsers}`);
      console.log(`  - Articles: ${trainingData.totalArticles}`);
      console.log(`  - Interactions: ${trainingData.totalInteractions}`);

      // Step 2: Train collaborative filtering model
      console.log("🧠 Step 2: Training collaborative filtering model...");
      const modelArtifacts = await trainCollaborativeFilteringModel(
        trainingData
      );

      console.log(`✅ Model trained successfully:`);
      console.log(`  - Version: ${modelArtifacts.version}`);
      console.log(`  - Categories: ${modelArtifacts.categoryWeights.length}`);
      console.log(`  - Users in model: ${modelArtifacts.userFactors.length}`);

      // Step 3: Upload model to Firebase Storage
      console.log("☁️ Step 3: Uploading model to Firebase Storage...");
      const uploadResult = await uploadModelToStorage(modelArtifacts);

      console.log(`✅ Model uploaded:`);
      console.log(`  - Path: ${uploadResult.path}`);
      console.log(`  - Size: ${uploadResult.sizeBytes} bytes`);
      console.log(`  - Download URL: ${uploadResult.downloadUrl}`);

      // Step 4: Update model metadata in Firestore
      console.log("💾 Step 4: Updating model metadata...");
      await admin.firestore().collection("ml_models").doc("latest").set({
        version: modelArtifacts.version,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        trainingDataStats: {
          users: trainingData.totalUsers,
          articles: trainingData.totalArticles,
          interactions: trainingData.totalInteractions,
        },
        storagePath: uploadResult.path,
        downloadUrl: uploadResult.downloadUrl,
        status: "available",
      });

      console.log("🎉 ML model training completed successfully!");

      return {
        success: true,
        modelVersion: modelArtifacts.version,
        stats: trainingData,
      };
    } catch (error) {
      console.error("❌ Error during scheduled training:", error);

      // Log error to Firestore for debugging
      await admin.firestore().collection("ml_training_logs").add({
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        status: "failed",
        error: error instanceof Error ? error.message : String(error),
        context: "scheduled_training",
      });

      throw error; // Re-throw to mark function as failed
    }
  });

/**
 * Manual training trigger via HTTP
 * Usage: POST https://YOUR_REGION-YOUR_PROJECT_ID.cloudfunctions.net/trainModel
 * Requires authentication
 */
export const trainModel = functions
  .runWith({
    timeoutSeconds: 540,
    memory: "2GB",
  })
  .https
  .onCall(async (data, context) => {
    // Require authentication
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated to trigger training"
      );
    }

    try {
      console.log(`🔧 Manual training triggered by user: ${context.auth.uid}`);

      // Step 1: Collect data
      const trainingData = await aggregateUserInteractions();

      if (trainingData.totalUsers < 5 || trainingData.totalInteractions < 50) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Not enough data for training. Need at least 5 users and 50 interactions."
        );
      }

      // Step 2: Train model
      const modelArtifacts = await trainCollaborativeFilteringModel(
        trainingData
      );

      // Step 3: Upload
      const uploadResult = await uploadModelToStorage(modelArtifacts);

      // Step 4: Update metadata
      await admin.firestore().collection("ml_models").doc("latest").set({
        version: modelArtifacts.version,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        trainingDataStats: {
          users: trainingData.totalUsers,
          articles: trainingData.totalArticles,
          interactions: trainingData.totalInteractions,
        },
        storagePath: uploadResult.path,
        downloadUrl: uploadResult.downloadUrl,
        status: "available",
        triggeredBy: context.auth.uid,
        triggerType: "manual",
      });

      console.log("✅ Manual training completed");

      return {
        success: true,
        modelVersion: modelArtifacts.version,
        downloadUrl: uploadResult.downloadUrl,
      };
    } catch (error) {
      console.error("❌ Manual training error:", error);
      throw new functions.https.HttpsError(
        "internal",
        error instanceof Error ? error.message : "Training failed"
      );
    }
  });

/**
 * Get latest model version info
 * Usage: Call from app to check if new model available
 */
export const getLatestModelVersion = functions.https.onCall(
  async (data, context) => {
    try {
      const modelDoc = await admin
        .firestore()
        .collection("ml_models")
        .doc("latest")
        .get();

      if (!modelDoc.exists) {
        return {
          available: false,
          message: "No model trained yet. Using rule-based recommendations.",
        };
      }

      const modelData = modelDoc.data();
      return {
        available: true,
        version: modelData?.version,
        createdAt: modelData?.createdAt,
        downloadUrl: modelData?.downloadUrl,
        stats: modelData?.trainingDataStats,
      };
    } catch (error) {
      console.error("Error fetching model version:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Failed to fetch model version"
      );
    }
  }
);
