#!/usr/bin/env python3
"""Generate FirebaseConfig.java from google-services.json.

Reads the Firebase project values and writes them as Java constants,
so the app can init Firebase via FirebaseOptions.Builder() without
depending on R.string resources (which broke in v1.1.1 due to a
gen_lib_r.py mapping bug — all library R fields were 0x0).

Usage: gen_firebase_config.py <google-services.json> <output-dir>
"""
import json
import sys

gs_path, out_dir = sys.argv[1], sys.argv[2]

with open(gs_path, encoding="utf-8") as f:
    gs = json.load(f)

client = gs["client"][0]
proj = gs["project_info"]

app_id = client["client_info"]["mobilesdk_app_id"]
sender_id = proj["project_number"]
api_key = client["api_key"][0]["current_key"]
project_id = proj["project_id"]
bucket = proj.get("storage_bucket", "")

java = f"""package com.flexurio.shiftku;

/**
 * Firebase config — auto-generated from google-services.json.
 * DO NOT EDIT MANUALLY. Regenerated at build time by gen_firebase_config.py.
 */
public final class FirebaseConfig {{
    public static final String APP_ID = "{app_id}";
    public static final String SENDER_ID = "{sender_id}";
    public static final String API_KEY = "{api_key}";
    public static final String PROJECT_ID = "{project_id}";
    public static final String STORAGE_BUCKET = "{bucket}";

    private FirebaseConfig() {{}}
}}
"""

import os
os.makedirs(out_dir, exist_ok=True)
with open(os.path.join(out_dir, "FirebaseConfig.java"), "w", encoding="utf-8") as f:
    f.write(java)
print("FirebaseConfig.java generated for project " + project_id)
