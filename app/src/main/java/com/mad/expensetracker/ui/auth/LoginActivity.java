package com.mad.expensetracker.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mad.expensetracker.R;
import com.mad.expensetracker.data.repository.AuthRepository;
import com.mad.expensetracker.databinding.ActivityLoginBinding;
import com.mad.expensetracker.ui.MainActivity;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private final AuthRepository authRepository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (authRepository.isLoggedIn()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.isEmailVerified()) {
                goToMain();
                return;
            } else {
                authRepository.logout();
                Snackbar.make(binding.getRoot(), "Please verify your email first.", Snackbar.LENGTH_LONG).show();
            }
        }

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.tvGoSignup.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));
        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        boolean valid = true;

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_email_invalid));
            valid = false;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.error_password_short));
            valid = false;
        }
        if (!valid) return;

        setLoading(true);
        authRepository.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    user.reload().addOnSuccessListener(aVoid -> {
                        if (user.isEmailVerified()) {
                            goToMain();
                        } else {
                            authRepository.logout();
                            Snackbar.make(binding.getRoot(),
                                    "Please verify your email first. Check your inbox.",
                                    Snackbar.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener(e -> goToMain());
                } else {
                    goToMain();
                }
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }

            // Called when Firebase says no account / wrong credentials exist for this email
            @Override
            public void onNoAccountFound(String email) {
                setLoading(false);
                showNoAccountDialog(email);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Forgot-password dialog.
     *
     * We no longer check "does this account exist?" before sending the
     * reset email, and no longer show a "No Account Found" dialog here.
     * The old check used fetchSignInMethodsForEmail(), which is deprecated
     * and always returns an empty result now (Email Enumeration
     * Protection) — that's exactly why real accounts were incorrectly
     * told "no account found."
     *
     * Firebase intentionally doesn't tell your app whether an email is
     * registered when requesting a password reset, so we send the
     * request and show one neutral confirmation — the same approach
     * Gmail, GitHub, etc. use.
     */
    private void showForgotPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        TextInputEditText etResetEmail = dialogView.findViewById(R.id.etResetEmail);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_AlertDialog_Dark)
                .setView(dialogView)
                .setPositiveButton("Send reset link", null)
                .setNegativeButton("Cancel", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.primary));
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = etResetEmail.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etResetEmail.setError("Enter a valid email");
                return;
            }

            authRepository.sendPasswordReset(email, new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess() {
                    dialog.dismiss();
                    Snackbar.make(binding.getRoot(),
                            "If an account exists for that email, a reset link is on its way.",
                            Snackbar.LENGTH_LONG).show();
                }

                @Override
                public void onError(String message) {
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

    // Shown for login attempts with unknown/incorrect credentials.
    //
    // IMPORTANT CONTEXT: modern Firebase Auth often returns the same generic
    // ERROR_INVALID_CREDENTIAL code for both "no account exists" and "account
    // exists but password is wrong" — this is intentional on Firebase's part,
    // for the same privacy reasons that affect password reset (see
    // AuthRepository.sendPasswordReset). That means we genuinely cannot
    // reliably tell these two cases apart on the client.
    //
    // Rather than guessing and showing the wrong suggestion (e.g. telling
    // someone with a real account to "Sign Up" when they just mistyped
    // their password), we offer both paths and let the user pick the one
    // that matches their actual situation.
    private void showNoAccountDialog(String email) {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_AlertDialog_Dark)
                .setTitle("Couldn't Log You In")
                .setMessage("We couldn't sign you in with:\n\n" + email
                        + "\n\nThis could mean the password is incorrect, or there's no account for this email yet."
                        + "\n\nWhat would you like to do?")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("Forgot Password", (d, which) -> showForgotPasswordDialogFor(email))
                .setNegativeButton("Sign Up Instead", (d, which) -> {
                    Intent intent = new Intent(this, SignupActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    // Opens the forgot-password dialog pre-filled with the email the user
    // just tried to log in with, so they don't have to retype it.
    private void showForgotPasswordDialogFor(String prefillEmail) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        TextInputEditText etResetEmail = dialogView.findViewById(R.id.etResetEmail);
        etResetEmail.setText(prefillEmail);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_AlertDialog_Dark)
                .setView(dialogView)
                .setPositiveButton("Send reset link", null)
                .setNegativeButton("Cancel", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.primary));
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = etResetEmail.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etResetEmail.setError("Enter a valid email");
                return;
            }

            authRepository.sendPasswordReset(email, new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess() {
                    dialog.dismiss();
                    Snackbar.make(binding.getRoot(),
                            "If an account exists for that email, a reset link is on its way.",
                            Snackbar.LENGTH_LONG).show();
                }

                @Override
                public void onError(String message) {
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }
}