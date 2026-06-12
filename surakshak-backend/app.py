from flask import Flask, request, jsonify
from flask_cors import CORS
from pymongo import MongoClient
from bson import ObjectId
import datetime
import numpy as np

app = Flask(__name__)
CORS(app, supports_credentials=True)

# ---------- MongoDB Setup ----------
client = MongoClient("mongodb+srv://admin:admin%40surakshak@surakshak.kyj7azt.mongodb.net/surakshak?retryWrites=true&w=majority")
db = client['surakshak_db']
training_collection = db['training_sessions']
risk_collection = db['risk_scores']

# ---------- Helper Functions ----------
def safe_avg(arr):
    return float(np.mean(arr)) if arr else 0.0

def average_behavior_vector(batch):
    return {
        "TypingSpeed": safe_avg([s.get("avg_typing_speed", 0) for s in batch]),
        "DwellTime": safe_avg([safe_avg(s.get("hold_times", [])) for s in batch]),
        "FlightTime": safe_avg([safe_avg(s.get("flight_times", [])) for s in batch]),
        "InterKeyDelay": safe_avg([safe_avg(s.get("inter_key_delays", [])) for s in batch]),
        "TapPressure": safe_avg([safe_avg(s.get("pressures", [])) for s in batch]),
        "TouchSize": safe_avg([safe_avg(s.get("touch_sizes", [])) for s in batch]),
        "TiltAngle": safe_avg([safe_avg(s.get("accel_pattern", [])) for s in batch]),
        "GyroPattern": safe_avg([safe_avg(s.get("gyro_pattern", [])) for s in batch]),
        "ScreenHoldTime": safe_avg([s.get("screen_hold_time", 0) for s in batch]),
    }

def calculate_risk_score(vector, baseline):
    weights = {
        "TypingSpeed": 1.2,
        "DwellTime": 1.0,
        "FlightTime": 1.0,
        "InterKeyDelay": 1.0,
        "TapPressure": 1.5,
        "TouchSize": 0.8,
        "TiltAngle": 0.5,
        "GyroPattern": 0.5,
        "ScreenHoldTime": 0.8
    }

    scales = {
        "TypingSpeed": 300.0,
        "DwellTime": 500.0,
        "FlightTime": 500.0,
        "InterKeyDelay": 500.0,
        "TapPressure": 2.0,
        "TouchSize": 1.0,
        "TiltAngle": 90.0,
        "GyroPattern": 90.0,
        "ScreenHoldTime": 3000.0
    }

    # Detect mostly idle (zero) vector
    total_features = len(vector)
    zero_features = sum(1 for v in vector.values() if v == 0)
    if zero_features / total_features >= 0.8:
        return 5.0  # low risk if mostly idle

    distance = 0.0
    for key in vector.keys() & baseline.keys():
        diff = (vector[key] - baseline[key]) / scales.get(key, 1.0)
        distance += weights.get(key, 1.0) * (diff ** 2)

    risk = round(min(100.0, (np.sqrt(distance) / 2) * 100), 2)
    return risk

# ---------- Routes ----------
@app.route('/upload-training-batch', methods=['POST'])
def upload_training_batch():
    try:
        data = request.json
        data['timestamp'] = datetime.datetime.now()
        training_collection.insert_one(data)
        print("✅ Training data uploaded for:", data.get("userID"))
        return jsonify({"status": "success", "message": "Training batch uploaded ✅"}), 200
    except Exception as e:
        print("❌ Upload error:", e)
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/train-hybrid-model', methods=['POST'])
def train_hybrid_model():
    try:
        all_data = list(training_collection.find())
        if not all_data:
            return jsonify({"status": "error", "message": "No training data found"}), 400

        latest_doc = all_data[-1]
        doc_id = latest_doc["_id"]
        user_id = latest_doc.get("userID", "user_01")
        training_batch = latest_doc.get("training_batch", [])

        if not training_batch:
            return jsonify({"status": "error", "message": "No training batch found"}), 400

        avg_vector = average_behavior_vector(training_batch)
        risk_score = calculate_risk_score(avg_vector, avg_vector)

        training_collection.update_one(
            {"_id": ObjectId(doc_id)},
            {"$set": {
                "average_vector": avg_vector,
                "initial_risk_score": risk_score,
                "model_trained_at": datetime.datetime.now()
            }}
        )

        risk_collection.update_one(
            {"userID": user_id},
            {"$set": {
                "userID": user_id,
                "average_vector": avg_vector,
                "all_vectors": training_batch,
                "initial_risk_score": risk_score,
                "timestamp": datetime.datetime.now()
            }},
            upsert=True
        )

        print("🧠 Average vector:", avg_vector)
        print("🔥 Initial Risk Score:", risk_score)

        return jsonify({
            "status": "success",
            "message": "Model trained and risk score stored ✅",
            "avg_vector": avg_vector,
            "risk_score": risk_score
        }), 200

    except Exception as e:
        print("❌ Training error:", e)
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/evaluate-risk', methods=['POST'])
def evaluate_risk():
    try:
        vector = request.get_json(force=True)
        if not vector:
            return jsonify({"status": "error", "message": "Empty or invalid JSON"}), 400

        user_id = vector.get("userID", "user_01")
        print("\n📨 Received behavior vector for evaluation:", vector)

        user_record = risk_collection.find_one({"userID": user_id})
        if not user_record or "average_vector" not in user_record:
            return jsonify({"status": "error", "message": "No baseline vector found"}), 404

        baseline = user_record["average_vector"]
        vector_clean = {k: v for k, v in vector.items() if k != "userID"}

        risk_score = calculate_risk_score(vector_clean, baseline)
        print(f"🔍 Real-time risk score for {user_id}: {risk_score}")

        return jsonify({"status": "success", "risk_score": risk_score}), 200

    except Exception as e:
        print("❌ Error in evaluate-risk:", str(e))
        return jsonify({"status": "error", "message": str(e)}), 500

# ---------- Run Server ----------
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=True)
