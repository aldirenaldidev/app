package com.flexurio.shiftku;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Fasad Firebase: Auth (email/password) + Firestore + FCM.
 *
 * Struktur Firestore:
 *  groups/{groupId}
 *    - name, inviteCode, createdBy, createdAt
 *  groups/{groupId}/members/{uid}
 *    - name, color (int), role ("admin"/"member")
 *  groups/{groupId}/schedule/{yyyy-MM-dd}
 *    - entries: map uid -> {shiftName, shiftColor}
 *  groups/{groupId}/swaps/{swapId}
 *    - date, fromUid, fromName, toUid, toName, status, createdAt
 *
 * Notifikasi: tiap anggota subscribe topik FCM "grp_{groupId}".
 * Pengiriman notifikasi perubahan jadwal dilakukan via Cloud Function
 * (lihat functions/index.js) yang trigger pada write ke schedule/swaps.
 */
public class FirebaseBridge {
    private static final String TAG = "ShiftKu-FB";
    private static boolean inited = false;

    public interface FbCallback {
        void onOk(String info);
        void onErr(String msg);
    }

    public interface GroupListCallback {
        void onOk(java.util.List<Map<String, Object>> groups);
        void onErr(String msg);
    }

    /** Selalu true: Firebase SDK ter-bundle di APK ini. */
    public static boolean isAvailable() { return true; }

    /** Aman: true jika FirebaseApp DEFAULT sudah ter-init dan bisa dipakai. */
    public static boolean isReady(Context c) {
        try {
            return !FirebaseApp.getApps(c.getApplicationContext()).isEmpty();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Terjemahkan error mentah Firebase Auth menjadi pesan yang jelas
     * untuk pengguna (Bahasa Indonesia).
     */
    public static String friendlyAuthError(String rawMsg) {
        if (rawMsg == null) return "Login gagal: kesalahan tidak diketahui.";
        String m = rawMsg;
        String low = rawMsg.toLowerCase();
        if (low.contains("configuration_not_found")) {
            return "Login gagal: metode Email/Password BELUM diaktifkan di "
                    + "Firebase Console.\n\nAktifkan di: Firebase Console > "
                    + "project shiftku-bc2e0 > Authentication > Sign-in method "
                    + "> Email/Password > Enable.\n\nPanduan lengkap: lihat file "
                    + "AKTIFKAN_EMAIL_PASSWORD.md";
        }
        if (low.contains("invalid-credential") || low.contains("wrong-password")
                || low.contains("user-not-found") || low.contains("invalid_login_credentials")) {
            return "Email atau kata sandi salah. Coba lagi.";
        }
        if (low.contains("user-disabled")) {
            return "Akun ini dinonaktifkan. Hubungi Admin.";
        }
        if (low.contains("too-many-requests")) {
            return "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi.";
        }
        if (low.contains("network-request-failed") || low.contains("network error")) {
            return "Tidak ada koneksi internet. Periksa koneksi lalu coba lagi.";
        }
        if (low.contains("email-already-in-use") || low.contains("already in use")) {
            return "Email ini sudah terdaftar.";
        }
        if (low.contains("weak-password")) {
            return "Kata sandi terlalu lemah (minimal 6 karakter).";
        }
        if (low.contains("invalid-email")) {
            return "Format email tidak valid.";
        }
        return m;
    }

    /** True jika error mentah menandakan CONFIGURATION_NOT_FOUND. */
    public static boolean isConfigNotFound(String rawMsg) {
        return rawMsg != null
                && rawMsg.toLowerCase().contains("configuration_not_found");
    }

    public static synchronized void init(Context c) {
        if (inited && isReady(c)) return;
        try {
            if (!isReady(c)) {
                FirebaseApp.initializeApp(c.getApplicationContext());
            }
            inited = true;
            Log.i(TAG, "FirebaseApp initialized: "
                    + FirebaseApp.getInstance().getOptions().getProjectId());
        } catch (Throwable t) {
            // JANGAN set inited=true agar bisa dicoba lagi nanti.
            // FirebaseApp mungkin sudah di-init oleh ShiftKuApp (manual builder).
            Log.e(TAG, "init gagal: " + t);
        }
    }

    private static FirebaseAuth auth() { return FirebaseAuth.getInstance(); }
    private static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }

    // ---------------- ROLE ----------------
    // Tiga level: user (anggota biasa), leader (ketua tim), admin (pengelola grup).

    public static final String ROLE_USER = "user";
    public static final String ROLE_LEADER = "leader";
    public static final String ROLE_ADMIN = "admin";

    public interface RoleCallback {
        void onRole(String role); // "user"/"leader"/"admin"; null bila gagal/bukan anggota
    }

    /** Nama tampilan role untuk UI. */
    public static String roleLabel(String role) {
        if (ROLE_ADMIN.equals(role)) return "Admin";
        if (ROLE_LEADER.equals(role)) return "Leader";
        return "User";
    }

    /** Ambil role saya di sebuah grup Firestore. */
    public static void getMyRole(Context c, String groupId, final RoleCallback cb) {
        try {
            init(c);
            final String uid = currentUid(c);
            if (uid == null || !isReady(c)) { cb.onRole(null); return; }
            db().collection("groups").document(groupId)
                    .collection("members").document(uid).get()
                    .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>() {
                        public void onSuccess(DocumentSnapshot d) {
                            cb.onRole(d.exists() ? d.getString("role") : null);
                        }
                    })
                    .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                        public void onFailure(Exception e) { cb.onRole(null); }
                    });
        } catch (Throwable t) {
            cb.onRole(null);
        }
    }

    public interface MemberListCallback {
        void onData(java.util.List<Map<String, Object>> members); // tiap map: uid, name, role, color
        void onErr(String msg);
    }

    /** Daftar semua anggota sebuah grup beserta role-nya. */
    public static ListenerRegistration listenMembers(Context c, String groupId,
                                                     final MemberListCallback cb) {
        try {
            init(c);
            if (!isReady(c)) { cb.onErr("Firebase belum siap."); return null; }
            return db().collection("groups").document(groupId).collection("members")
                    .addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                        public void onEvent(QuerySnapshot qs,
                                           com.google.firebase.firestore.FirebaseFirestoreException e) {
                            if (e != null) { cb.onErr(e.getMessage()); return; }
                            java.util.List<Map<String, Object>> out =
                                    new java.util.ArrayList<Map<String, Object>>();
                            for (DocumentSnapshot d : qs.getDocuments()) {
                                Map<String, Object> m = d.getData();
                                if (m != null) {
                                    m.put("_uid", d.getId());
                                    if (m.get("role") == null) m.put("role", ROLE_USER);
                                    out.add(m);
                                }
                            }
                            cb.onData(out);
                        }
                    });
        } catch (Throwable t) {
            cb.onErr(t.getMessage());
            return null;
        }
    }

    /** Ubah role seorang anggota. Hanya boleh dipanggil oleh admin (ditegakkan di UI). */
    public static void setMemberRole(final Context c, String groupId, String memberUid,
                                     String newRole, final FbCallback cb) {
        try {
            init(c);
            if (!isReady(c)) { cb.onErr("Firebase belum siap."); return; }
            db().collection("groups").document(groupId)
                    .collection("members").document(memberUid)
                    .update("role", newRole)
                    .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                        public void onSuccess(Void v) { cb.onOk("Role diubah."); }
                    })
                    .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                        public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                    });
        } catch (Throwable t) {
            cb.onErr(t.getMessage());
        }
    }

    /** Hapus anggota dari grup. Hanya boleh dipanggil oleh admin (ditegakkan di UI). */
    public static void removeMember(final Context c, String groupId, String memberUid,
                                    final FbCallback cb) {
        try {
            init(c);
            if (!isReady(c)) { cb.onErr("Firebase belum siap."); return; }
            db().collection("groups").document(groupId)
                    .collection("members").document(memberUid).delete()
                    .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                        public void onSuccess(Void v) { cb.onOk("Anggota dihapus."); }
                    })
                    .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                        public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                    });
        } catch (Throwable t) {
            cb.onErr(t.getMessage());
        }
    }

    /**
     * Buat akun Firebase Auth baru (dipakai Admin untuk menambah anggota).
     * CATATAN: Firebase client otomatis login sebagai user baru setelah dibuat.
     * onOk menerima UID user baru; pemanggil wajib signOut lalu arahkan Admin
     * kembali ke layar login.
     */
    public static void createAccount(final Context c, String email, String pass,
                                     final FbCallback cb) {
        init(c);
        if (!isReady(c)) { cb.onErr("Firebase belum siap. Periksa koneksi lalu coba lagi."); return; }
        try {
            auth().createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.auth.AuthResult>() {
                    public void onSuccess(com.google.firebase.auth.AuthResult r) {
                        String uid = (r.getUser() != null) ? r.getUser().getUid() : currentUid(c);
                        cb.onOk(uid != null ? uid : "");
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) {
                        cb.onErr("Gagal buat akun: " + e.getMessage());
                    }
                });
        } catch (Throwable t) {
            cb.onErr("Gagal buat akun: " + t.getMessage());
        }
    }

    /** Tambah dokumen anggota (uid asli) ke grup — dipakai Admin setelah createAccount. */
    public static void addMemberDoc(final Context c, String groupId, String memberUid,
                                    String name, String email, String role,
                                    final FbCallback cb) {
        try {
            init(c);
            if (!isReady(c)) { cb.onErr("Firebase belum siap."); return; }
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("name", name);
            m.put("email", email);
            m.put("color", 0xFF3B82F6);
            m.put("role", role != null ? role : ROLE_USER);
            db().collection("groups").document(groupId)
                    .collection("members").document(memberUid).set(m)
                    .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                        public void onSuccess(Void v) { cb.onOk("Anggota ditambahkan."); }
                    })
                    .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                        public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                    });
        } catch (Throwable t) {
            cb.onErr(t.getMessage());
        }
    }

    /** Leader/Admin boleh menyetujui/menolak tukar shift & cuti. */
    public static boolean canApprove(String role) {
        return ROLE_LEADER.equals(role) || ROLE_ADMIN.equals(role);
    }

    public static boolean isAdmin(String role) {
        return ROLE_ADMIN.equals(role);
    }

    // ---------------- AUTH ----------------

    public static void signUp(final Context c, String email, String pass, final FbCallback cb) {
        init(c);
        if (!isReady(c)) { cb.onErr("Firebase belum siap. Periksa koneksi lalu coba lagi."); return; }
        try {
            auth().createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.auth.AuthResult>() {
                    public void onSuccess(com.google.firebase.auth.AuthResult r) {
                        cb.onOk("Daftar berhasil.");
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) {
                        cb.onErr(friendlyAuthError("Gagal daftar: " + e.getMessage()));
                    }
                });
        } catch (Throwable t) {
            cb.onErr(friendlyAuthError("Gagal daftar: " + t.getMessage()));
        }
    }

    public static void signIn(final Context c, String email, String pass, final FbCallback cb) {
        init(c);
        if (!isReady(c)) { cb.onErr("Firebase belum siap. Periksa koneksi lalu coba lagi."); return; }
        try {
            auth().signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.auth.AuthResult>() {
                    public void onSuccess(com.google.firebase.auth.AuthResult r) {
                        cb.onOk("Login berhasil.");
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) {
                        cb.onErr(friendlyAuthError("Gagal login: " + e.getMessage()));
                    }
                });
        } catch (Throwable t) {
            cb.onErr(friendlyAuthError("Gagal login: " + t.getMessage()));
        }
    }

    public static void signOut(Context c) {
        try {
            init(c);
            if (isReady(c)) auth().signOut();
        } catch (Throwable ignored) {}
    }

    /** Kirim tautan reset kata sandi ke email. Bisa dipakai siapa saja. */
    public static void sendPasswordReset(final Context c, String email, final FbCallback cb) {
        init(c);
        if (!isReady(c)) { cb.onErr("Firebase belum siap. Periksa koneksi lalu coba lagi."); return; }
        try {
            auth().sendPasswordResetEmail(email)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    public void onSuccess(Void v) {
                        cb.onOk("Tautan reset terkirim. Cek email Anda.");
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) {
                        cb.onErr(friendlyAuthError("Gagal kirim reset: " + e.getMessage()));
                    }
                });
        } catch (Throwable t) {
            cb.onErr(friendlyAuthError("Gagal kirim reset: " + t.getMessage()));
        }
    }

    public static FirebaseUser currentUser(Context c) {
        try {
            init(c);
            if (!isReady(c)) return null;
            return auth().getCurrentUser();
        } catch (Throwable t) {
            Log.e(TAG, "currentUser: " + t);
            return null;
        }
    }

    public static String currentUid(Context c) {
        FirebaseUser u = currentUser(c);
        return u == null ? null : u.getUid();
    }

    public static String currentUserName(Context c) {
        FirebaseUser u = currentUser(c);
        if (u == null) return null;
        if (u.getDisplayName() != null && !u.getDisplayName().isEmpty())
            return u.getDisplayName();
        String em = u.getEmail();
        if (em != null && em.contains("@")) return em.substring(0, em.indexOf('@'));
        return em != null ? em : "Pengguna";
    }

    public static void setDisplayName(final Context c, String name, final FbCallback cb) {
        init(c);
        if (!isReady(c)) { cb.onErr("Firebase belum siap."); return; }
        try {
            FirebaseUser u = auth().getCurrentUser();
            if (u == null) { cb.onErr("Belum login."); return; }
        com.google.firebase.auth.UserProfileChangeRequest req =
                new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name).build();
        u.updateProfile(req)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    public void onSuccess(Void v) { cb.onOk("Nama disimpan."); }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                });
        } catch (Throwable t) {
            cb.onErr(t.getMessage());
        }
    }

    // ---------------- ADMIN DEFAULT & WAJIB GANTI PASSWORD ----------------

    public interface BoolCallback {
        void onResult(boolean value);
    }

    /**
     * Pastikan akun admin default ada di Firebase Auth.
     * Dipanggil saat aplikasi pertama dibuka (Splash). Bila akun belum ada,
     * dibuat otomatis dengan flag mustChangePassword=true, lalu signOut agar
     * aplikasi TIDAK otomatis login sebagai admin.
     */
    public static void ensureDefaultAdmin(final Context c, final FbCallback cb) {
        init(c);
        if (!isReady(c)) { cb.onErr("Firebase belum siap."); return; }
        try {
            if (Prefs.isAdminSeeded(c)) { cb.onOk("sudah"); return; }
        } catch (Throwable t) { cb.onErr(t.getMessage()); return; }
        final FirebaseAuth a = auth();
        a.createUserWithEmailAndPassword(AdminConfig.ADMIN_EMAIL,
                AdminConfig.ADMIN_DEFAULT_PASSWORD)
            .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.auth.AuthResult>() {
                public void onSuccess(com.google.firebase.auth.AuthResult r) {
                    final String uid = (r.getUser() != null)
                            ? r.getUser().getUid() : currentUid(c);
                    writeAdminDoc(c, uid, "Administrator",
                            new FbCallback() {
                                public void onOk(String i) {
                                    finishSeed(c, true);
                                    cb.onOk("dibuat");
                                }
                                public void onErr(String m) {
                                    finishSeed(c, false);
                                    cb.onErr(m);
                                }
                            });
                }
            })
            .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                public void onFailure(Exception e) {
                    String raw = (e.getMessage() != null) ? e.getMessage() : "";
                    // CONFIGURATION_NOT_FOUND = provider Email/Password belum
                    // diaktifkan di Firebase Console. Bukan crash: log jelas,
                    // splash tetap lanjut, user akan dapat pesan jelas saat login.
                    if (isConfigNotFound(raw)) {
                        android.util.Log.w("ShiftKu",
                                "ensureDefaultAdmin: Email/Password belum diaktifkan "
                                + "di Firebase Console (project shiftku-bc2e0 > "
                                + "Authentication > Sign-in method). Aktifkan lalu "
                                + "buka ulang aplikasi. Lihat AKTIFKAN_EMAIL_PASSWORD.md");
                        cb.onErr("SEED_SKIP_CONFIG");
                        return;
                    }
                    boolean collision =
                            e instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException
                            || raw.toLowerCase().contains("already in use");
                    if (!collision) {
                        cb.onErr("Gagal buat admin default: " + e.getMessage());
                        return;
                    }
                    // Akun sudah ada (dibuat sebelumnya). Coba sign-in dengan
                    // password default untuk memastikan dokumen users/ ada.
                    a.signInWithEmailAndPassword(AdminConfig.ADMIN_EMAIL,
                            AdminConfig.ADMIN_DEFAULT_PASSWORD)
                        .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.auth.AuthResult>() {
                            public void onSuccess(com.google.firebase.auth.AuthResult r2) {
                                final String uid = (r2.getUser() != null)
                                        ? r2.getUser().getUid() : currentUid(c);
                                ensureAdminDoc(c, uid, new FbCallback() {
                                    public void onOk(String i) {
                                        finishSeed(c, true);
                                        cb.onOk("sudah ada");
                                    }
                                    public void onErr(String m) {
                                        finishSeed(c, true);
                                        cb.onOk("sudah ada");
                                    }
                                });
                            }
                        })
                        .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                            public void onFailure(Exception e2) {
                                // Password default sudah diganti -> biarkan.
                                finishSeed(c, true);
                                cb.onOk("sudah ada");
                            }
                        });
                }
            });
    }

    /** SignOut + tandai seeding (true hanya bila benar-benar selesai). */
    private static void finishSeed(Context c, boolean done) {
        try { auth().signOut(); } catch (Throwable ignored) {}
        try { if (done) Prefs.setAdminSeeded(c, true); } catch (Throwable ignored) {}
    }

    /** Tulis dokumen users/{uid} untuk admin default. */
    private static void writeAdminDoc(final Context c, final String uid,
                                      String name, final FbCallback cb) {
        if (uid == null) { cb.onErr("UID kosong."); return; }
        try {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("name", name);
            m.put("email", AdminConfig.ADMIN_EMAIL);
            m.put("role", ROLE_ADMIN);
            m.put(AdminConfig.FIELD_MUST_CHANGE_PASSWORD, true);
            m.put("createdAt", FieldValue.serverTimestamp());
            db().collection(AdminConfig.USERS_COLLECTION).document(uid).set(m)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    public void onSuccess(Void v) {
                        // Set display name (best effort).
                        try {
                            FirebaseUser u = auth().getCurrentUser();
                            if (u != null) {
                                com.google.firebase.auth.UserProfileChangeRequest req =
                                        new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                .setDisplayName(name).build();
                                u.updateProfile(req);
                            }
                        } catch (Throwable ignored) {}
                        cb.onOk("ok");
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                });
        } catch (Throwable t) {
            cb.onErr(t.getMessage());
        }
    }

    /** Pastikan dokumen users/{uid} ada untuk admin lama (upgrade). */
    private static void ensureAdminDoc(final Context c, final String uid,
                                       final FbCallback cb) {
        if (uid == null) { cb.onErr("UID kosong."); return; }
        try {
            db().collection(AdminConfig.USERS_COLLECTION).document(uid).get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>() {
                    public void onSuccess(DocumentSnapshot d) {
                        if (d.exists()) { cb.onOk("ada"); return; }
                        writeAdminDoc(c, uid, "Administrator", cb);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                });
        } catch (Throwable t) {
            cb.onErr(t.getMessage());
        }
    }

    /**
     * Tandai akun (dibuatkan Admin) agar wajib ganti password saat login
     * berikutnya. Best-effort: kegagalan tulis tidak menggagalkan alur.
     */
    public static void markMustChangePassword(final Context c, String uid,
                                              final FbCallback cb) {
        if (uid == null) { cb.onErr("UID kosong."); return; }
        try {
            init(c);
            if (!isReady(c)) { cb.onErr("Firebase belum siap."); return; }
            Map<String, Object> m = new HashMap<String, Object>();
            m.put(AdminConfig.FIELD_MUST_CHANGE_PASSWORD, true);
            db().collection(AdminConfig.USERS_COLLECTION).document(uid)
                .set(m, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    public void onSuccess(Void v) { cb.onOk("ok"); }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                });
        } catch (Throwable t) {
            cb.onErr(t.getMessage());
        }
    }

    public interface UserListCallback {
        void onData(java.util.List<Map<String, Object>> users); // tiap map: _uid, name, email, role
        void onErr(String msg);
    }

    /** Ambil role GLOBAL user saat ini dari dokumen users/{uid}. */
    public static void getGlobalRole(final Context c, final RoleCallback cb) {
        try {
            init(c);
            final String uid = currentUid(c);
            if (uid == null || !isReady(c)) { cb.onRole(null); return; }
            db().collection(AdminConfig.USERS_COLLECTION).document(uid).get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>() {
                    public void onSuccess(DocumentSnapshot d) {
                        cb.onRole(d.exists() ? d.getString("role") : null);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onRole(null); }
                });
        } catch (Throwable t) {
            cb.onRole(null);
        }
    }

    /**
     * Sinkronkan role global ke Prefs lokal (best-effort).
     * Dipanggil setelah login agar badge role & menu Admin akurat.
     */
    public static void syncGlobalRole(final Context c) {
        try {
            getGlobalRole(c, new RoleCallback() {
                public void onRole(String role) {
                    if (role != null && !role.isEmpty() && !"null".equals(role)) {
                        try { Prefs.setUserRole(c, role); } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    /** Daftar semua pengguna (koleksi users). Hanya untuk Admin (ditegakkan di UI). */
    public static ListenerRegistration listenUsers(final Context c, final UserListCallback cb) {
        try {
            init(c);
            if (!isReady(c)) { cb.onErr("Firebase belum siap."); return null; }
            return db().collection(AdminConfig.USERS_COLLECTION)
                .addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    public void onEvent(QuerySnapshot qs,
                                        com.google.firebase.firestore.FirebaseFirestoreException e) {
                        if (e != null) { cb.onErr(e.getMessage()); return; }
                        java.util.List<Map<String, Object>> out =
                                new java.util.ArrayList<Map<String, Object>>();
                        for (DocumentSnapshot d : qs.getDocuments()) {
                            Map<String, Object> m = d.getData();
                            if (m != null) {
                                m.put("_uid", d.getId());
                                if (m.get("role") == null) m.put("role", ROLE_USER);
                                out.add(m);
                            }
                        }
                        cb.onData(out);
                    }
                });
        } catch (Throwable t) {
            cb.onErr(t.getMessage());
            return null;
        }
    }

    /**
     * Tulis dokumen users/{uid} untuk akun yang dibuat Admin.
     * Role disimpan global + flag wajib ganti password.
     */
    public static void writeUserDoc(final Context c, String uid, String name,
                                    String email, String role, final FbCallback cb) {
        if (uid == null) { cb.onErr("UID kosong."); return; }
        try {
            init(c);
            if (!isReady(c)) { cb.onErr("Firebase belum siap."); return; }
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("name", name);
            m.put("email", email);
            m.put("role", role != null ? role : ROLE_USER);
            m.put(AdminConfig.FIELD_MUST_CHANGE_PASSWORD, true);
            m.put("createdAt", FieldValue.serverTimestamp());
            db().collection(AdminConfig.USERS_COLLECTION).document(uid).set(m)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    public void onSuccess(Void v) { cb.onOk("ok"); }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                });
        } catch (Throwable t) {
            cb.onErr(t.getMessage());
        }
    }

    /** Ubah role global seorang pengguna. Hanya boleh dipanggil oleh admin (ditegakkan di UI). */
    public static void setGlobalRole(final Context c, String targetUid, String newRole,
                                     final FbCallback cb) {
        try {
            init(c);
            if (!isReady(c)) { cb.onErr("Firebase belum siap."); return; }
            db().collection(AdminConfig.USERS_COLLECTION).document(targetUid)
                .update("role", newRole)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    public void onSuccess(Void v) { cb.onOk("Role diubah."); }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                });
        } catch (Throwable t) {
            cb.onErr(t.getMessage());
        }
    }

    /**
     * Hapus dokumen users/{uid}. CATATAN: akun Firebase Auth-nya TIDAK terhapus
     * (butuh Admin SDK server) — hapus manual via Firebase Console bila perlu.
     */
    public static void deleteUserDoc(final Context c, String targetUid, final FbCallback cb) {
        try {
            init(c);
            if (!isReady(c)) { cb.onErr("Firebase belum siap."); return; }
            db().collection(AdminConfig.USERS_COLLECTION).document(targetUid).delete()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    public void onSuccess(Void v) { cb.onOk("Pengguna dihapus."); }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                });
        } catch (Throwable t) {
            cb.onErr(t.getMessage());
        }
    }

    /** Cek apakah user saat ini wajib ganti password. */
    public static void mustChangePassword(final Context c, final BoolCallback cb) {        try {
            init(c);
            final String uid = currentUid(c);
            if (uid == null || !isReady(c)) { cb.onResult(false); return; }
            db().collection(AdminConfig.USERS_COLLECTION).document(uid).get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>() {
                    public void onSuccess(DocumentSnapshot d) {
                        boolean v = d.exists()
                                && Boolean.TRUE.equals(d.getBoolean(
                                        AdminConfig.FIELD_MUST_CHANGE_PASSWORD));
                        cb.onResult(v);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onResult(false); }
                });
        } catch (Throwable t) {
            cb.onResult(false);
        }
    }

    /**
     * Ganti kata sandi user yang sedang login, lalu hapus flag
     * mustChangePassword. (updatePassword butuh sesi login yang masih baru;
     * bila gagal karena sesi kedaluwarsa, user diminta login ulang.)
     */
    public static void changePassword(final Context c, String newPass,
                                      final FbCallback cb) {
        init(c);
        if (!isReady(c)) { cb.onErr("Firebase belum siap. Periksa koneksi lalu coba lagi."); return; }
        try {
            final FirebaseUser u = auth().getCurrentUser();
            if (u == null) { cb.onErr("Belum login."); return; }
            u.updatePassword(newPass)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    public void onSuccess(Void v) {
                        db().collection(AdminConfig.USERS_COLLECTION)
                            .document(u.getUid())
                            .update(AdminConfig.FIELD_MUST_CHANGE_PASSWORD, false)
                            .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                                public void onSuccess(Void v2) {
                                    cb.onOk("Kata sandi berhasil diganti.");
                                }
                            })
                            .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                                public void onFailure(Exception e) {
                                    // Password sudah terganti; flag menyusul.
                                    cb.onOk("Kata sandi berhasil diganti.");
                                }
                            });
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) {
                        String msg = e.getMessage() != null ? e.getMessage() : "";
                        if (msg.toLowerCase().contains("recent login")
                                || msg.toLowerCase().contains("requires recent")) {
                            cb.onErr("Sesi kedaluwarsa. Keluar lalu login ulang, kemudian coba lagi.");
                        } else {
                            cb.onErr("Gagal ganti kata sandi: " + e.getMessage());
                        }
                    }
                });
        } catch (Throwable t) {
            cb.onErr("Gagal ganti kata sandi: " + t.getMessage());
        }
    }

    // ---------------- GROUP ----------------

    private static String genCode(String name) {
        String p = name.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (p.length() < 3) p = (p + "XXX").substring(0, 3);
        else p = p.substring(0, Math.min(4, p.length()));
        return p + "-" + (1000 + new Random().nextInt(9000));
    }

    public static void createGroup(final Context c, final String name,
                                   final String memberCsv, final FbCallback cb) {
        init(c);
        final String uid = currentUid(c);
        if (uid == null) { cb.onErr("Login dulu sebelum membuat grup."); return; }
        final String code = genCode(name);
        final Map<String, Object> g = new HashMap<String, Object>();
        g.put("name", name);
        g.put("inviteCode", code);
        g.put("createdBy", uid);
        g.put("createdAt", FieldValue.serverTimestamp());
        db().collection("groups").add(g)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentReference>() {
                    public void onSuccess(com.google.firebase.firestore.DocumentReference ref) {
                        final String gid = ref.getId();
                        Map<String, Object> me = new HashMap<String, Object>();
                        me.put("name", currentUserName(c));
                        me.put("color", 0xFF3B82F6);
                        me.put("role", ROLE_ADMIN);
                        ref.collection("members").document(uid).set(me);
                        // anggota awal (nama saja, tanpa uid)
                        String[] parts = memberCsv.split(",");
                        int ci = 1;
                        int[] pal = {0xFF22C55E, 0xFFF59E0B, 0xFFEF4444, 0xFF8B5CF6, 0xFF06B6D4};
                        for (String p : parts) {
                            String nm = p.trim();
                            if (nm.isEmpty() || nm.equalsIgnoreCase(currentUserName(c))) continue;
                            Map<String, Object> m = new HashMap<String, Object>();
                            m.put("name", nm);
                            m.put("color", pal[(ci - 1) % pal.length]);
                            m.put("role", ROLE_USER);
                            ref.collection("members").document("local_" + ci).set(m);
                            ci++;
                        }
                        subscribeTopic(gid);
                        cb.onOk(code);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onErr("Gagal: " + e.getMessage()); }
                });
    }

    public static void joinGroup(final Context c, final String code, final FbCallback cb) {
        init(c);
        final String uid = currentUid(c);
        if (uid == null) { cb.onErr("Login dulu sebelum gabung grup."); return; }
        db().collection("groups").whereEqualTo("inviteCode", code.toUpperCase()).limit(1).get()
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot>() {
                    public void onSuccess(QuerySnapshot qs) {
                        if (qs.isEmpty()) { cb.onErr("Kode tidak ditemukan."); return; }
                        DocumentSnapshot doc = qs.getDocuments().get(0);
                        final String gid = doc.getId();
                        Map<String, Object> m = new HashMap<String, Object>();
                        m.put("name", currentUserName(c));
                        m.put("color", 0xFF22C55E);
                        m.put("role", Prefs.getUserRole(c));
                        doc.getReference().collection("members").document(uid).set(m)
                                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                                    public void onSuccess(Void v) {
                                        subscribeTopic(gid);
                                        cb.onOk(doc.getString("name"));
                                    }
                                })
                                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                                    public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                                });
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onErr("Gagal: " + e.getMessage()); }
                });
    }

    /** Daftar grup di mana user adalah anggota (via collectionGroup query sederhana). */
    public static void listenMyGroups(final Context c, final GroupListCallback cb) {
        init(c);
        final String uid = currentUid(c);
        if (uid == null) { cb.onErr("Belum login."); return; }
        // Ambil semua grup lalu filter yang memuat uid di members (skala kecil, cukup).
        db().collection("groups").addSnapshotListener(
                new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    public void onEvent(QuerySnapshot qs,
                                       com.google.firebase.firestore.FirebaseFirestoreException e) {
                        if (e != null) { cb.onErr(e.getMessage()); return; }
                        final java.util.List<Map<String, Object>> out =
                                new java.util.ArrayList<Map<String, Object>>();
                        final java.util.List<com.google.firebase.firestore.DocumentSnapshot> docs =
                                qs.getDocuments();
                        if (docs.isEmpty()) { cb.onOk(out); return; }
                        final int[] pending = {docs.size()};
                        for (final DocumentSnapshot d : docs) {
                            d.getReference().collection("members").document(uid).get()
                                    .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>() {
                                        public void onSuccess(DocumentSnapshot m) {
                                            if (m.exists()) {
                                                Map<String, Object> g = new HashMap<String, Object>();
                                                g.put("id", d.getId());
                                                g.put("name", d.getString("name"));
                                                g.put("inviteCode", d.getString("inviteCode"));
                                                out.add(g);
                                            }
                                            if (--pending[0] == 0) cb.onOk(out);
                                        }
                                    })
                                    .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                                        public void onFailure(Exception ex) {
                                            if (--pending[0] == 0) cb.onOk(out);
                                        }
                                    });
                        }
                    }
                });
    }

    // ---------------- SCHEDULE ----------------

    public static void setGroupSchedule(Context c, String groupId, String date,
                                        String shiftName, int shiftColor) {
        init(c);
        final String uid = currentUid(c);
        if (uid == null) return;
        final String myName = currentUserName(c);
        Map<String, Object> entry = new HashMap<String, Object>();
        entry.put("shiftName", shiftName);
        entry.put("shiftColor", shiftColor);
        entry.put("name", myName);
        Map<String, Object> doc = new HashMap<String, Object>();
        doc.put("entries." + uid, entry);
        db().collection("groups").document(groupId)
                .collection("schedule").document(date)
                .set(doc, com.google.firebase.firestore.SetOptions.merge());
    }

    public interface ScheduleCallback {
        void onData(Map<String, Map<String, Object>> byDate); // date -> (uid -> entry)
        void onErr(String msg);
    }

    public static ListenerRegistration listenSchedule(Context c, String groupId,
                                                      final ScheduleCallback cb) {
        init(c);
        return db().collection("groups").document(groupId)
                .collection("schedule")
                .addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    @SuppressWarnings("unchecked")
                    public void onEvent(QuerySnapshot qs,
                                       com.google.firebase.firestore.FirebaseFirestoreException e) {
                        if (e != null) { cb.onErr(e.getMessage()); return; }
                        Map<String, Map<String, Object>> out =
                                new HashMap<String, Map<String, Object>>();
                        for (DocumentSnapshot d : qs.getDocuments()) {
                            Map<String, Object> entries =
                                    (Map<String, Object>) d.get("entries");
                            if (entries != null) {
                                Map<String, Object> m = new HashMap<String, Object>();
                                for (Map.Entry<String, Object> en : entries.entrySet()) {
                                    if (en.getValue() instanceof Map)
                                        m.put(en.getKey(), (Map<String, Object>) en.getValue());
                                }
                                out.put(d.getId(), m);
                            }
                        }
                        cb.onData(out);
                    }
                });
    }

    // ---------------- SWAP ----------------

    public static void requestSwap(final Context c, String groupId, String date,
                                   String toName, String reason, final FbCallback cb) {
        init(c);
        final String uid = currentUid(c);
        if (uid == null) { cb.onErr("Belum login."); return; }
        Map<String, Object> s = new HashMap<String, Object>();
        s.put("date", date);
        s.put("fromUid", uid);
        s.put("fromName", currentUserName(c));
        s.put("toName", toName);
        s.put("reason", reason == null ? "" : reason);
        s.put("status", "Menunggu");
        s.put("createdAt", FieldValue.serverTimestamp());
        db().collection("groups").document(groupId).collection("swaps").add(s)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentReference>() {
                    public void onSuccess(com.google.firebase.firestore.DocumentReference r) {
                        cb.onOk("Permintaan terkirim.");
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                });
    }

    public interface SwapListCallback {
        void onData(java.util.List<Map<String, Object>> swaps);
        void onErr(String msg);
    }

    public static ListenerRegistration listenSwaps(Context c, String groupId,
                                                   final SwapListCallback cb) {
        init(c);
        return db().collection("groups").document(groupId)
                .collection("swaps").orderBy("createdAt",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    public void onEvent(QuerySnapshot qs,
                                       com.google.firebase.firestore.FirebaseFirestoreException e) {
                        if (e != null) { cb.onErr(e.getMessage()); return; }
                        java.util.List<Map<String, Object>> out =
                                new java.util.ArrayList<Map<String, Object>>();
                        for (DocumentSnapshot d : qs.getDocuments()) {
                            Map<String, Object> m = d.getData();
                            if (m != null) { m.put("_id", d.getId()); out.add(m); }
                        }
                        cb.onData(out);
                    }
                });
    }

    public static void setSwapStatus(Context c, String groupId, String swapId, String status) {
        init(c);
        db().collection("groups").document(groupId)
                .collection("swaps").document(swapId)
                .update("status", status);
    }

    // ---------------- ABSEN SELFIE ----------------
    // Metadata absensi disinkron ke groups/{gid}/absen agar Leader/Admin bisa
    // melihat siapa sudah absen. Foto tetap di perangkat masing-masing
    // (tidak di-upload, tanpa Firebase Storage).

    /**
     * Simpan metadata absen ke Firestore grup. Foto TIDAK di-upload.
     */
    public static void saveAbsen(final Context c, final String groupId,
                                 final String date, final String time, final String type,
                                 final double lat, final double lon, final String locName,
                                 final FbCallback cb) {
        init(c);
        final String uid = currentUid(c);
        if (uid == null) { cb.onErr("Login dulu untuk sinkron absen."); return; }
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("uid", uid);
        m.put("name", currentUserName(c));
        m.put("date", date);
        m.put("time", time);
        m.put("type", type);
        m.put("lat", lat);
        m.put("lon", lon);
        m.put("loc_name", locName == null ? "" : locName);
        m.put("photoLocal", true);
        m.put("createdAt", FieldValue.serverTimestamp());
        db().collection("groups").document(groupId)
                .collection("absen").add(m)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentReference>() {
                    public void onSuccess(com.google.firebase.firestore.DocumentReference r) {
                        cb.onOk("Absen tersinkron.");
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    public void onFailure(Exception e) { cb.onErr(e.getMessage()); }
                });
    }

    public interface AbsenListCallback {
        void onData(java.util.List<Map<String, Object>> list);
        void onErr(String msg);
    }

    /** Dengarkan absensi tim (untuk Leader/Admin). Diurut terbaru dulu. */
    public static ListenerRegistration listenAbsen(Context c, String groupId,
                                                   final AbsenListCallback cb) {
        init(c);
        return db().collection("groups").document(groupId)
                .collection("absen").orderBy("createdAt",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    public void onEvent(QuerySnapshot qs,
                                       com.google.firebase.firestore.FirebaseFirestoreException e) {
                        if (e != null) { cb.onErr(e.getMessage()); return; }
                        java.util.List<Map<String, Object>> out =
                                new java.util.ArrayList<Map<String, Object>>();
                        for (DocumentSnapshot d : qs.getDocuments()) {
                            Map<String, Object> m = d.getData();
                            if (m != null) { m.put("_id", d.getId()); out.add(m); }
                        }
                        cb.onData(out);
                    }
                });
    }

    // ---------------- FCM ----------------

    public static void subscribeTopic(String groupId) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic("grp_" + groupId);
        } catch (Exception e) {
            Log.e(TAG, "subscribe: " + e.getMessage());
        }
    }

    public static void unsubscribeTopic(String groupId) {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("grp_" + groupId);
        } catch (Exception e) {
            Log.e(TAG, "unsubscribe: " + e.getMessage());
        }
    }
}
