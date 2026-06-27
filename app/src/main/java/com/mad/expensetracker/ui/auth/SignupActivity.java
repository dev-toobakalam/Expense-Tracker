package com.mad.expensetracker.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.mad.expensetracker.R;
import com.mad.expensetracker.data.repository.AuthRepository;
import com.mad.expensetracker.databinding.ActivitySignupBinding;

public class SignupActivity extends AppCompatActivity {
    private ActivitySignupBinding binding;
    private final AuthRepository authRepository = new AuthRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String email = getIntent().getStringExtra("email");
        if (email != null) {
            binding.etEmail.setText(email);
        }

        binding.btnSignup.setOnClickListener(v -> attemptSignup());
        binding.tvGoLogin.setOnClickListener(v -> finish());
    }

    private void attemptSignup() {
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
        authRepository.signup(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                authRepository.sendEmailVerification(new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        setLoading(false);
                        showVerificationPendingState(email);
                    }

                    @Override
                    public void onError(String message) {
                        setLoading(false);
                        // Account was created but the verification email failed to send.
                        // Let the user retry right from here instead of burying it in a dialog.
                        Snackbar.make(binding.getRoot(),
                                        "Account created, but we couldn't send the verification email. Tap Retry.",
                                        Snackbar.LENGTH_INDEFINITE)
                                .setAction("Retry", v -> attemptSignup())
                                .show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Account created and verification email sent. We use a persistent
     * Snackbar instead of a blocking dialog so the screen stays usable.
     * The user taps "I've Verified" after clicking the link in their
     * email; we then check with Firebase to confirm before letting them
     * through to Login, so they can't skip ahead without actually
     * verifying.
     */
    private void showVerificationPendingState(String email) {
        binding.btnSignup.setEnabled(false);
        binding.btnSignup.setText("Waiting for verification…");

        Snackbar.make(binding.getRoot(),
                        "Verification email sent to " + email + ". Verify it, then tap below.",
                        Snackbar.LENGTH_INDEFINITE)
                .setAction("I've Verified", v -> checkVerificationAndProceed())
                .show();
    }

    private void checkVerificationAndProceed() {
        setLoading(true);
        authRepository.checkEmailVerified(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Snackbar.make(binding.getRoot(), "Email verified! Taking you to login…", Snackbar.LENGTH_SHORT).show();
                goToLogin();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                binding.btnSignup.setEnabled(false);
                binding.btnSignup.setText("Waiting for verification…");
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                        .setAction("Resend", v -> resendVerification())
                        .show();
            }
        });
    }

    private void resendVerification() {
        authRepository.sendEmailVerification(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                Snackbar.make(binding.getRoot(), "Verification email resent. Check your inbox.", Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSignup.setEnabled(!loading);
    }
}