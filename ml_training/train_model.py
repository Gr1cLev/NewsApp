#!/usr/bin/env python3
"""
NewsApp ML Training Script for GitHub Actions
==============================================

This script trains an SVD-based recommendation model using user interaction data
from Firestore, then uploads the trained model back to Firestore for distribution.

Usage:
    python train_model.py

Environment Variables:
    FIREBASE_SERVICE_ACCOUNT_JSON: Base64-encoded Firebase service account JSON

Author: NewsApp Team
"""

import os
import sys
import json
import base64
import tempfile
from datetime import datetime
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.decomposition import TruncatedSVD
from tqdm import tqdm

import firebase_admin
from firebase_admin import credentials, firestore


# ============================================================================
# Configuration
# ============================================================================

# Minimum requirements for training
MIN_USERS = 2
MIN_INTERACTIONS = 3

# SVD parameters
N_COMPONENTS = 10  # Embedding dimension

# Firestore collections
COLLECTION_INTERACTIONS = "user_interactions"
COLLECTION_PREFERENCES = "user_preferences"
COLLECTION_BOOKMARKS = "user_bookmarks"
COLLECTION_ML_MODELS = "ml_models"
MODEL_DOCUMENT_ID = "recommendation_model_v1"


# ============================================================================
# Firebase Initialization
# ============================================================================

def init_firebase():
    """Initialize Firebase from environment variable (base64-encoded JSON)."""
    print("🔥 Initializing Firebase...")
    
    # Check if already initialized
    if firebase_admin._apps:
        print("✅ Firebase already initialized")
        return firestore.client()
    
    # Get credentials from environment variable
    creds_base64 = os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON")
    
    if not creds_base64:
        # Fallback to local file (for local testing)
        local_key_path = Path(__file__).parent / "serviceAccountKey.json"
        if local_key_path.exists():
            print("📁 Using local serviceAccountKey.json")
            cred = credentials.Certificate(str(local_key_path))
        else:
            raise ValueError(
                "No Firebase credentials found! "
                "Set FIREBASE_SERVICE_ACCOUNT_JSON env var or provide serviceAccountKey.json"
            )
    else:
        # Decode base64 credentials
        print("🔐 Decoding credentials from environment variable...")
        creds_json = base64.b64decode(creds_base64).decode("utf-8")
        creds_dict = json.loads(creds_json)
        
        # Write to temp file (firebase-admin requires file path)
        with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as f:
            json.dump(creds_dict, f)
            temp_creds_path = f.name
        
        cred = credentials.Certificate(temp_creds_path)
        
        # Clean up temp file after init
        os.unlink(temp_creds_path)
    
    firebase_admin.initialize_app(cred, {
        "storageBucket": "newsapp-fae0d.appspot.com"
    })
    
    print("✅ Firebase initialized successfully!")
    return firestore.client()


# ============================================================================
# Data Fetching
# ============================================================================

def fetch_user_interactions(db):
    """Fetch all user interactions from Firestore."""
    print("📡 Fetching user interactions...")
    
    interactions = []
    users_ref = db.collection(COLLECTION_INTERACTIONS)
    
    for user_doc in users_ref.list_documents():
        user_id = user_doc.id
        articles_ref = user_doc.collection("articles")
        
        for article_doc in articles_ref.stream():
            data = article_doc.to_dict()
            interactions.append({
                "user_id": user_id,
                "article_id": article_doc.id,
                "click_count": data.get("clickCount", 0),
                "time_spent": data.get("timeSpentReading", 0),
                "is_bookmarked": data.get("isBookmarked", False),
                "category": data.get("category", "unknown")
            })
    
    print(f"✅ Fetched {len(interactions)} interactions")
    return interactions


def fetch_user_preferences(db):
    """Fetch user preferences from Firestore."""
    print("📡 Fetching user preferences...")
    
    preferences = []
    users_ref = db.collection(COLLECTION_PREFERENCES)
    
    for user_doc in users_ref.list_documents():
        user_id = user_doc.id
        ml_data_ref = user_doc.collection("ml_data").document("preferences")
        ml_data = ml_data_ref.get()
        
        if ml_data.exists:
            data = ml_data.to_dict()
            preferences.append({
                "user_id": user_id,
                "category_scores": data.get("categoryScores", {}),
                "total_interactions": data.get("totalInteractions", 0)
            })
    
    print(f"✅ Fetched {len(preferences)} user preferences")
    return preferences


def fetch_bookmarks(db):
    """Fetch user bookmarks from Firestore."""
    print("📡 Fetching user bookmarks...")
    
    bookmarks = []
    users_ref = db.collection(COLLECTION_BOOKMARKS)
    
    for user_doc in users_ref.list_documents():
        user_id = user_doc.id
        bookmarks_ref = user_doc.collection("bookmarks")
        
        for bookmark_doc in bookmarks_ref.stream():
            data = bookmark_doc.to_dict()
            bookmarks.append({
                "user_id": user_id,
                "article_id": bookmark_doc.id,
                "category": data.get("category", "unknown")
            })
    
    print(f"✅ Fetched {len(bookmarks)} bookmarks")
    return bookmarks


# ============================================================================
# Data Processing
# ============================================================================

def build_interaction_matrix(interactions, bookmarks):
    """Build user-article interaction matrix for SVD training."""
    print("🔧 Building interaction matrix...")
    
    # Combine interactions and bookmarks into scores
    scores = {}
    
    for interaction in interactions:
        user_id = interaction["user_id"]
        article_id = interaction["article_id"]
        
        # Score formula: clicks + bookmark*5 + reading_time/30
        score = (
            interaction["click_count"] +
            (5 if interaction["is_bookmarked"] else 0) +
            interaction["time_spent"] / 30.0
        )
        
        key = (user_id, article_id)
        scores[key] = scores.get(key, 0) + score
    
    # Add bookmarks with extra weight
    for bookmark in bookmarks:
        key = (bookmark["user_id"], bookmark["article_id"])
        scores[key] = scores.get(key, 0) + 5
    
    if not scores:
        print("⚠️ No interaction data found!")
        return None, None, None
    
    # Create DataFrame
    df = pd.DataFrame([
        {"user_id": k[0], "article_id": k[1], "score": v}
        for k, v in scores.items()
    ])
    
    # Pivot to matrix
    matrix = df.pivot_table(
        index="user_id",
        columns="article_id",
        values="score",
        fill_value=0
    )
    
    print(f"✅ Matrix shape: {matrix.shape} (users × articles)")
    
    return matrix, list(matrix.index), list(matrix.columns)


# ============================================================================
# Model Training
# ============================================================================

def train_svd_model(matrix, n_components=N_COMPONENTS):
    """Train SVD model on interaction matrix."""
    print(f"🧠 Training SVD model (n_components={n_components})...")
    
    # Ensure we don't have more components than features
    n_components = min(n_components, min(matrix.shape) - 1)
    if n_components < 1:
        n_components = 1
    
    svd = TruncatedSVD(n_components=n_components, random_state=42)
    
    # Fit and transform
    user_factors = svd.fit_transform(matrix.values)
    article_factors = svd.components_.T
    
    explained_variance = svd.explained_variance_ratio_.sum()
    print(f"✅ SVD trained! Explained variance: {explained_variance:.2%}")
    
    return user_factors, article_factors, n_components


# ============================================================================
# Model Export
# ============================================================================

def build_model_artifacts(user_ids, article_ids, user_factors, article_factors, n_components, preferences):
    """Build model artifacts for Android app consumption."""
    print("📦 Building model artifacts...")
    
    # Current timestamp
    now = datetime.now()
    version = f"v{now.strftime('%Y%m%d_%H%M%S')}"
    created_at = now.isoformat()
    
    # Build user factors list
    user_factors_list = []
    for i, user_id in enumerate(user_ids):
        user_factors_list.append({
            "userId": user_id,
            "factors": user_factors[i].tolist()
        })
    
    # Build article factors list
    article_factors_list = []
    for i, article_id in enumerate(article_ids):
        article_factors_list.append({
            "articleId": article_id,
            "factors": article_factors[i].tolist()
        })
    
    # Build category weights from preferences
    category_weights = {}
    for pref in preferences:
        for category, score in pref.get("category_scores", {}).items():
            category_weights[category] = category_weights.get(category, 0) + score
    
    # Normalize category weights
    if category_weights:
        max_weight = max(category_weights.values())
        if max_weight > 0:
            category_weights = {k: v / max_weight for k, v in category_weights.items()}
    
    category_weights_list = [
        {"category": k, "weight": v}
        for k, v in category_weights.items()
    ]
    
    # Assemble model document
    model_doc = {
        "version": version,
        "createdAt": created_at,
        "algorithmType": "collaborative_filtering_svd",
        "nComponents": n_components,
        "trainingStats": {
            "totalUsers": len(user_ids),
            "totalArticles": len(article_ids),
            "totalInteractions": sum(len(uf) for uf in user_factors)
        },
        "categoryWeights": category_weights_list,
        "userFactors": user_factors_list,
        "articleFactors": article_factors_list
    }
    
    print(f"✅ Model artifacts built: {len(user_factors_list)} users, {len(article_factors_list)} articles")
    
    return model_doc, version


def upload_model_to_firestore(db, model_doc, version):
    """Upload trained model to Firestore."""
    print(f"☁️ Uploading model {version} to Firestore...")
    
    doc_ref = db.collection(COLLECTION_ML_MODELS).document(MODEL_DOCUMENT_ID)
    doc_ref.set(model_doc)
    
    print(f"✅ Model uploaded to: {COLLECTION_ML_MODELS}/{MODEL_DOCUMENT_ID}")
    return True


# ============================================================================
# Main Training Pipeline
# ============================================================================

def main():
    """Main training pipeline."""
    print("=" * 60)
    print("🚀 NewsApp ML Training Pipeline")
    print("=" * 60)
    print()
    
    try:
        # Initialize Firebase
        db = init_firebase()
        
        # Fetch data
        interactions = fetch_user_interactions(db)
        preferences = fetch_user_preferences(db)
        bookmarks = fetch_bookmarks(db)
        
        # Check minimum requirements
        user_ids = set(i["user_id"] for i in interactions)
        if len(user_ids) < MIN_USERS:
            print(f"⚠️ Not enough users for training: {len(user_ids)} < {MIN_USERS}")
            print("⏭️ Skipping training. App will use rule-based recommendations.")
            return 0
        
        if len(interactions) < MIN_INTERACTIONS:
            print(f"⚠️ Not enough interactions for training: {len(interactions)} < {MIN_INTERACTIONS}")
            print("⏭️ Skipping training. App will use rule-based recommendations.")
            return 0
        
        # Build interaction matrix
        matrix, user_ids, article_ids = build_interaction_matrix(interactions, bookmarks)
        
        if matrix is None:
            print("❌ Failed to build interaction matrix")
            return 1
        
        # Train SVD model
        user_factors, article_factors, n_components = train_svd_model(matrix)
        
        # Build model artifacts
        model_doc, version = build_model_artifacts(
            user_ids, article_ids,
            user_factors, article_factors,
            n_components, preferences
        )
        
        # Upload to Firestore
        success = upload_model_to_firestore(db, model_doc, version)
        
        if success:
            print()
            print("=" * 60)
            print(f"🎉 Training complete! Model version: {version}")
            print("=" * 60)
            return 0
        else:
            print("❌ Failed to upload model")
            return 1
            
    except Exception as e:
        print(f"❌ Training failed with error: {e}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    sys.exit(main())
