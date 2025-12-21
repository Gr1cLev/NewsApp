# NewsApp ML Training - Manual Training Workflow

## 📋 Overview

This directory contains a **Jupyter Notebook** for manually training NewsApp's recommendation model using **Collaborative Filtering (SVD)**.

**Why manual training?**
- ✅ **100% FREE** - No Cloud Functions (Blaze Plan) required
- ✅ Full control over training process
- ✅ Easy to debug and customize
- ✅ No ongoing costs

**Trade-off:**
- ❌ Requires manual execution (weekly/monthly)
- ❌ Manual upload to Firebase Storage

---

## 🚀 Quick Start

### 1. Install Dependencies

**For Anaconda users (RECOMMENDED):**

```bash
# Create conda environment (optional but recommended)
conda create -n newsapp-ml python=3.11 -y
conda activate newsapp-ml

# Install packages via conda
conda install numpy pandas scikit-learn matplotlib seaborn tqdm -y

# Install Firebase Admin SDK via pip
pip install firebase-admin
```

**For pip users:**

```bash
cd ml_training
pip install -r requirements.txt
```

### 2. Get Firebase Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select project: **newsapp-fae0d**
3. Click ⚙️ **Settings** → **Project settings**
4. Go to **Service accounts** tab
5. Click **Generate new private key**
6. Save as: `ml_training/serviceAccountKey.json`

⚠️ **IMPORTANT**: Add `serviceAccountKey.json` to `.gitignore`!

### 3. Run Training Notebook

```bash
jupyter notebook train_recommendation_model.ipynb
```

Or open in VS Code and run all cells.

### 4. Upload Model to Firebase

After training completes:

1. Open [Firebase Console - Storage](https://console.firebase.google.com/project/newsapp-fae0d/storage)
2. Create folder: `ml_models/` (if not exists)
3. Upload `models/recommendation_model_latest.json`
4. Rename to: `recommendation_model_v1.json`

Done! The Android app will auto-download the new model.

---

## 📁 Directory Structure

```
ml_training/
├── train_recommendation_model.ipynb  # Main training notebook
├── requirements.txt                  # Python dependencies
├── serviceAccountKey.json            # Firebase credentials (YOU MUST DOWNLOAD)
├── README.md                         # This file
├── datasets/                         # Fetched data backups (JSON)
│   ├── interactions_YYYYMMDD_HHMMSS.json
│   ├── preferences_YYYYMMDD_HHMMSS.json
│   ├── bookmarks_YYYYMMDD_HHMMSS.json
│   ├── metadata_YYYYMMDD_HHMMSS.json
│   └── data_analysis_YYYYMMDD_HHMMSS.png
└── models/                           # Trained models (JSON)
    ├── recommendation_model_vYYYYMMDD_HHMMSS.json
    └── recommendation_model_latest.json  # ← Upload this!
```

---

## 🧠 How It Works

### Training Pipeline:

1. **Fetch Data** from Firestore:
   - `user_interactions/{userId}/articles/{articleId}` - Clicks, reading time
   - `user_preferences/{userId}/ml_data/preferences` - Category scores
   - `user_bookmarks/{userId}/bookmarks/{articleId}` - Bookmarks

2. **Build Interaction Matrix**:
   - Rows = Users
   - Columns = Articles
   - Values = Interaction scores (clicks + bookmarks*5 + reading_time/30)

3. **Train SVD Model** (Truncated Singular Value Decomposition):
   - Factorizes user-article matrix
   - Learns latent user preferences & article features
   - Generates user factors & article factors

4. **Export to JSON**:
   - Format: `ML_ModelArtifacts` (compatible with Android app)
   - Contains: user factors, article factors, category weights, metadata

5. **Save & Upload**:
   - Save to `models/recommendation_model_latest.json`
   - Manual upload to Firebase Storage

---

## 🔄 Retraining Schedule

**Recommended frequency:**
- **Weekly**: If you have active users (100+ interactions/week)
- **Monthly**: If you have moderate users (50+ interactions/month)
- **On-demand**: When you notice recommendation quality degrading

**How to retrain:**
1. Open `train_recommendation_model.ipynb`
2. Run all cells (takes ~2-5 minutes)
3. Upload new model to Firebase Storage (overwrites old one)
4. App will auto-download new model on next launch

---

## 📊 Data Requirements

**Minimum data needed:**
- ✅ **5+ users** with interaction history
- ✅ **50+ total interactions** (clicks + bookmarks)

**If insufficient data:**
- Notebook will show warning
- Model training skipped
- App will continue using rule-based recommendations only

---

## 🛡️ Security

**Service Account Key**:
- ⚠️ **NEVER commit `serviceAccountKey.json` to git**
- ✅ Already in `.gitignore`
- ✅ Keep this file secret
- ✅ Regenerate if compromised

**Firestore Access**:
- Notebook uses Admin SDK (full read/write access)
- Only use on trusted machines
- Don't share service account key

---

## ❓ Troubleshooting

### Error: "Service account key not found"
- **Solution**: Download `serviceAccountKey.json` from Firebase Console (see step 2 above)

### Error: "Not enough data for training"
- **Solution**: Use app more! Need at least 5 users with 50 interactions total
- **Workaround**: App will continue using rule-based recommendations

### Error: "Firebase initialization failed"
- **Solution**: Check `serviceAccountKey.json` is valid
- **Solution**: Ensure internet connection

### Model file too large
- **Solution**: Normal! Model size grows with users/articles
- **Note**: Typical size: 100KB - 2MB (acceptable for Firebase Storage)

---

## 💰 Cost Breakdown (100% FREE)

| Component | Cost |
|-----------|------|
| Python execution (local) | **FREE** |
| Firestore reads (training) | **FREE** (within free tier) |
| Firebase Storage (model file) | **~$0.01/month** |
| Model download by users | **~$0.05/month** (100 users) |
| **TOTAL** | **~$0.06/month** |

**vs. Cloud Functions auto-training**: $3-8/month

---

## 🎯 Next Steps After Training

1. **Verify model uploaded**:
   - Firebase Console → Storage → `ml_models/recommendation_model_v1.json`
   - Should see file with size ~100KB-2MB

2. **Test in app**:
   - Clear app data
   - Launch app
   - Check logcat: "ML model downloaded successfully"
   - Go to Search screen → See ML-powered recommendations

3. **Monitor performance**:
   - Watch user engagement metrics
   - Compare recommendation quality before/after
   - Retrain if quality degrades

---

**Questions?** Check the notebook cells for detailed explanations!

**Happy Training! 🚀**
