package com.mad.expensetracker.ui.dashboard;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.card.MaterialCardView;
import com.mad.expensetracker.R;
import com.mad.expensetracker.databinding.FragmentDashboardBinding;
import com.mad.expensetracker.databinding.ItemBreakdownRowBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private boolean isChartAnimated = false;

    // BUG FIX: keep a reference so we can cancel it in onDestroyView().
    // Without this, the animator keeps firing its update listener (which
    // touches `binding`) even after the fragment's view is destroyed —
    // e.g. when the user navigates away from Dashboard while the count-up
    // animation is still running. That caused the repeated crash on rapid
    // screen switching.
    private ValueAnimator totalAnimator;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        binding.tvMonthLabel.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date()));

        viewModel.getTotalAmount().observe(getViewLifecycleOwner(), this::animateTotal);
        viewModel.getCategoryTotals().observe(getViewLifecycleOwner(), this::renderBreakdownAndChart);
        viewModel.getIsEmpty().observe(getViewLifecycleOwner(), isEmpty -> {
            if (binding != null) binding.tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        viewModel.loadCurrentMonth();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && viewModel.shouldRefresh()) {
            viewModel.loadCurrentMonth();
        }
    }

    private void animateTotal(double target) {
        if (binding == null) return;

        // Cancel any animation still running from a previous call before
        // starting a new one — prevents two animators racing to update
        // the same TextView, and prevents leaks if this fires again
        // quickly (e.g. Firestore returning twice in a row).
        if (totalAnimator != null) {
            totalAnimator.cancel();
        }

        totalAnimator = ValueAnimator.ofFloat(0f, (float) target);
        totalAnimator.setDuration(700);
        totalAnimator.addUpdateListener(anim -> {
            // BUG FIX: this is the actual crash. The listener can still
            // fire after onDestroyView() has run and set binding = null,
            // if the fragment's view is destroyed mid-animation (e.g. the
            // user quickly navigates to another tab). Must re-check here,
            // not just once at the top of animateTotal().
            if (binding == null) return;
            binding.tvTotalAmount.setText(
                    String.format(Locale.getDefault(), "Rs. %,.0f", (float) anim.getAnimatedValue()));
        });
        totalAnimator.start();

        updateProgress(target);
    }

    private void updateProgress(double used) {
        if (binding == null) return;
        double limit = 30000;
        int percentage = (int) ((used / limit) * 100);
        if (percentage > 100) percentage = 100;
        binding.progressBar.setProgress(percentage);
        binding.tvUsedAmount.setText(String.format(Locale.getDefault(), "Used: Rs. %,.0f", used));
    }

    private void renderBreakdownAndChart(Map<String, Double> categoryTotals) {
        if (binding == null) return;
        renderChart(categoryTotals);
        renderBreakdownRows(categoryTotals);
    }

    private void renderChart(Map<String, Double> categoryTotals) {
        if (binding == null) return;

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
                colors.add(ContextCompat.getColor(requireContext(), categoryColorRes(entry.getKey())));
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);
        dataSet.setValueTextSize(0f);
        dataSet.setValueTextColor(android.graphics.Color.TRANSPARENT);
        dataSet.setSliceSpace(3f);

        binding.pieChart.setData(new PieData(dataSet));
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.getLegend().setEnabled(false);
        binding.pieChart.setDrawEntryLabels(false);
        binding.pieChart.setEntryLabelColor(android.graphics.Color.TRANSPARENT);
        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleRadius(62f);
        binding.pieChart.setHoleColor(ContextCompat.getColor(requireContext(), R.color.surface));
        binding.pieChart.setCenterText("By Category");
        binding.pieChart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        binding.pieChart.setCenterTextSize(13f);

        if (!isChartAnimated) {
            binding.pieChart.animateY(700);
            isChartAnimated = true;
        }
        binding.pieChart.invalidate();
    }

    private void renderBreakdownRows(Map<String, Double> categoryTotals) {
        if (binding == null) return;
        binding.breakdownContainer.removeAllViews();

        LinkedHashMap<String, Double> sorted = new LinkedHashMap<>();
        categoryTotals.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));

        for (Map.Entry<String, Double> entry : sorted.entrySet()) {
            if (binding == null) return; // guard mid-loop in case the view was destroyed

            ItemBreakdownRowBinding row = ItemBreakdownRowBinding.inflate(
                    getLayoutInflater(), binding.breakdownContainer, false);

            row.tvCategoryName.setText(entry.getKey());
            row.tvCategoryAmount.setText(String.format(Locale.getDefault(), "Rs. %,.0f", entry.getValue()));
            row.ivCategoryIcon.setImageResource(categoryIconRes(entry.getKey()));

            MaterialCardView chip = (MaterialCardView) row.ivCategoryIcon.getParent();
            chip.setCardBackgroundColor(ContextCompat.getColor(requireContext(), categoryColorRes(entry.getKey())));

            binding.breakdownContainer.addView(row.getRoot());
        }
    }

    private int categoryColorRes(String category) {
        switch (category) {
            case "Food": return R.color.cat_food;
            case "Transport": return R.color.cat_transport;
            case "Shopping": return R.color.cat_shopping;
            case "Bills": return R.color.cat_bills;
            case "Entertainment": return R.color.cat_entertainment;
            default: return R.color.cat_other;
        }
    }

    private int categoryIconRes(String category) {
        switch (category) {
            case "Food": return R.drawable.ic_food;
            case "Transport": return R.drawable.ic_transport;
            case "Shopping": return R.drawable.ic_shopping;
            case "Bills": return R.drawable.ic_bill;
            case "Entertainment": return R.drawable.ic_entertainment;
            default: return R.drawable.ic_others;
        }
    }

    @Override
    public void onDestroyView() {

        if (totalAnimator != null) {
            totalAnimator.cancel();
            totalAnimator = null;
        }
        super.onDestroyView();
        binding = null;
    }
}