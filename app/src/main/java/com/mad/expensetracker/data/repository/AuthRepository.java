package com.mad.expensetracker.data.repository;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface AuthCallback {
        void onSuccess();
        void onError(String message);

        // Called when no account exists for the given email
        default void onNoAccountFound(String email) {}
    }

    public void signup(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(getFriendlyErrorMessage(e)));
    }

    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthException) {
                        String code = ((FirebaseAuthException) e).getErrorCode();
                        if (code.equals("ERROR_USER_NOT_FOUND")
                                || code.equals("ERROR_WRONG_PASSWORD")
                                || code.equals("ERROR_INVALID_CREDENTIAL")) {
                            callback.onNoAccountFound(email);
                            return;
                        }
                    }
                    callback.onError(getFriendlyErrorMessage(e));
                });
    }

    public void sendPasswordReset(String email, AuthCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(getFriendlyErrorMessage(e)));
    }

    public void sendEmailVerification(AuthCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user logged in");
            return;
        }
        user.sendEmailVerification()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(getFriendlyErrorMessage(e)));
    }

    /**
     * Reloads the current user and reports whether their email is now verified.
     * Used by the signup flow so the user only reaches Login after Firebase
     * actually confirms verification — not just because they tapped a button.
     */
    public void checkEmailVerified(AuthCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user logged in");
            return;
        }
        user.reload()
                .addOnSuccessListener(unused -> {
                    if (user.isEmailVerified()) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Your email isn't verified yet. Please click the link we sent you, then try again.");
                    }
                })
                .addOnFailureListener(e -> callback.onError(getFriendlyErrorMessage(e)));
    }

    public void changePassword(String newPassword, AuthCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user logged in");
            return;
        }
        user.updatePassword(newPassword)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(getFriendlyErrorMessage(e)));
    }

    public void deleteAccount(AuthCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("No user logged in");
            return;
        }
        String uid = user.getUid();
        db.collection("users").document(uid).collection("expenses")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        deleteUserAccount(user, callback);
                        return;
                    }
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> deleteUserAccount(user, callback))
                            .addOnFailureListener(e -> callback.onError("Failed to delete your data: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError("Failed to load your data: " + e.getMessage()));
    }

    private void deleteUserAccount(FirebaseUser user, AuthCallback callback) {
        user.delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(user.getUid()).delete()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onSuccess());
                })
                .addOnFailureListener(e -> callback.onError(getFriendlyErrorMessage(e)));
    }

    public void logout() { auth.signOut(); }

    @NonNull
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : "";
    }

    public boolean isLoggedIn() { return auth.getCurrentUser() != null; }

    public FirebaseUser getCurrentUser() { return auth.getCurrentUser(); }

    private String getFriendlyErrorMessage(Exception e) {
        if (e instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) e).getErrorCode();
            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    return "Invalid email address.";
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "This email is already registered. Please log in.";
                case "ERROR_WEAK_PASSWORD":
                    return "Password must be at least 6 characters.";
                case "ERROR_WRONG_PASSWORD":
                    return "Incorrect password. Please try again.";
                case "ERROR_USER_NOT_FOUND":
                    return "No account found with this email.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Too many attempts. Please try again later.";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Network error. Please check your connection.";
                default:
                    return "Authentication failed. Please try again.";
            }
        }
        return e.getMessage();
    }
}