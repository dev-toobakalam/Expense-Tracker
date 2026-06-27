package com.mad.expensetracker.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mad.expensetracker.R;
import com.mad.expensetracker.data.repository.AuthRepository;
import com.mad.expensetracker.databinding.FragmentSettingsBinding;
import com.mad.expensetracker.ui.auth.LoginActivity;
import com.mad.expensetracker.utils.Validators;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private final AuthRepository authRepository = new AuthRepository();
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnChangePassword.setOnClickListener(v -> changePassword());
        binding.btnLogout.setOnClickListener(v -> logout());
        binding.btnDeleteAccount.setOnClickListener(v -> showDeleteConfirmationDialog());

        displayUserEmail();
        checkVerificationStatus();
    }

    /**
     * Shows the email address of the currently logged-in Firebase user,
     * so the person can confirm which account they're using.
     */
    private void displayUserEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (binding == null) return;

        if (user != null && user.getEmail() != null) {
            binding.tvUserEmail.setText(user.getEmail());
        } else {
            // Shouldn't normally happen — Settings is only reachable while logged in —
            // but avoid leaving the placeholder "user@example.com" visible if it does.
            binding.tvUserEmail.setText("Not signed in");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-check in case the user verified their email while away from the app
        checkVerificationStatus();
    }

    private void checkVerificationStatus() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (binding == null || currentUser == null) return;

        if (!currentUser.isEmailVerified()) {
            binding.tvVerificationStatus.setVisibility(View.VISIBLE);
            binding.tvVerificationStatus.setText("Your email isn't verified yet. Tap to resend the link.");
            binding.tvVerificationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error));
            binding.tvVerificationStatus.setBackgroundResource(R.drawable.bg_verification_status);
            binding.tvVerificationStatus.setOnClickListener(v -> resendVerificationEmail());
        } else {
            binding.tvVerificationStatus.setVisibility(View.GONE);
        }
    }

    private void resendVerificationEmail() {
        if (currentUser == null) {
            Snackbar.make(binding.getRoot(), "No user logged in", Snackbar.LENGTH_SHORT).show();
            return;
        }

        authRepository.sendEmailVerification(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                Snackbar.make(binding.getRoot(), "Verification email sent. Check your inbox.", Snackbar.LENGTH_LONG).show();
                refreshVerificationFromServer();
            }

            @Override
            public void onError(String message) {
                Snackbar.make(binding.getRoot(), "Couldn't send email: " + message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void refreshVerificationFromServer() {
        authRepository.checkEmailVerified(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                if (binding != null) binding.tvVerificationStatus.setVisibility(View.GONE);
            }

            @Override
            public void onError(String message) {
                // Not verified yet — leave the banner showing, no need to alarm the user
            }
        });
    }

    private void changePassword() {
        String newPassword = binding.etNewPassword.getText().toString().trim();

        binding.tilNewPassword.setError(null);
        if (!Validators.isValidPassword(newPassword)) {
            binding.tilNewPassword.setError("Password must be at least 6 characters");
            return;
        }

        authRepository.changePassword(newPassword, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                Snackbar.make(binding.getRoot(), "Password updated", Snackbar.LENGTH_SHORT).show();
                binding.etNewPassword.setText("");
            }

            @Override
            public void onError(String message) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void logout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    authRepository.logout();
                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_AlertDialog_Dark)
                .setTitle("Delete Account")
                .setMessage("This action is permanent and cannot be undone. All your expenses and account data will be lost.\n\nAre you sure you want to continue?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete", (dialog, which) -> performDeleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDeleteAccount() {
        Snackbar.make(binding.getRoot(), "Deleting account…", Snackbar.LENGTH_INDEFINITE).show();

        authRepository.deleteAccount(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) return;
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            }

            @Override
            public void onError(String message) {
                if (binding != null) {
                    Snackbar.make(binding.getRoot(), "Failed to delete account: " + message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}