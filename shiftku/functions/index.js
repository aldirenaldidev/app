/**
 * Cloud Function: kirim notifikasi FCM ke topik grup saat ada perubahan.
 *
 * CARA PAKAI (sekali saja, dari komputer):
 *   1. Install Node.js LTS + Firebase CLI:  npm install -g firebase-tools
 *   2. Login:  firebase login
 *   3. Di folder ini:  firebase init functions  (pilih project shiftku-bc2e0)
 *   4. Copy file ini ke functions/index.js hasil init
 *   5. Deploy:  firebase deploy --only functions
 *
 * Setelah deploy, setiap ada perubahan jadwal / tukar shift di grup,
 * semua anggota otomatis dapat notifikasi push.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

async function notify(groupId, title, body) {
  await admin.messaging().send({
    topic: "grp_" + groupId,
    notification: { title, body },
    data: { title, body, groupId },
  });
  console.log("Notified grp_" + groupId + ": " + title);
}

// Trigger: jadwal grup berubah
exports.onScheduleChange = functions.firestore
  .document("groups/{groupId}/schedule/{dateId}")
  .onWrite(async (change, ctx) => {
    const gid = ctx.params.groupId;
    const snap = await admin.firestore().doc("groups/" + gid).get();
    const gname = snap.exists ? snap.data().name : "Grup";
    await notify(gid, "ShiftKu — " + gname,
      "Jadwal grup diperbarui. Buka aplikasi untuk melihat perubahan.");
  });

// Trigger: status tukar shift berubah
exports.onSwapChange = functions.firestore
  .document("groups/{groupId}/swaps/{swapId}")
  .onWrite(async (change, ctx) => {
    const gid = ctx.params.groupId;
    const after = change.after.exists ? change.after.data() : null;
    if (!after) return;
    await notify(gid, "ShiftKu — Tukar Shift",
      "Permintaan " + after.fromName + " → " + after.toName + ": " + after.status);
  });
