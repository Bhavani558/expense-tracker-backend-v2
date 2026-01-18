package com.example.expense_tracker;

import com.example.expense_tracker.model.User;
import com.example.expense_tracker.repository.UserRepository;
import com.example.expense_tracker.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    // ===== GET all expenses for logged-in user =====
    @GetMapping
    public List<Expense> getExpenses(@RequestHeader("Authorization") String authHeader) {
        User user = getUserFromToken(authHeader);
        return expenseRepository.findByUser(user);
    }

    // ===== POST new expense =====
    @PostMapping
    public Expense addExpense(@RequestBody Expense expense, @RequestHeader("Authorization") String authHeader) {
        User user = getUserFromToken(authHeader);
        expense.setUser(user);

        if (expense.getDate() == null) {
            expense.setDate(LocalDate.now());
        }

        return expenseRepository.save(expense);
    }

    // ===== PUT update expense =====
    @PutMapping("/{id}")
    public Expense updateExpense(@PathVariable Long id, @RequestBody Expense updatedExpense, @RequestHeader("Authorization") String authHeader) {
        User user = getUserFromToken(authHeader);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        expense.setTitle(updatedExpense.getTitle());
        expense.setAmount(updatedExpense.getAmount());
        expense.setDescription(updatedExpense.getDescription());
        expense.setCategory(updatedExpense.getCategory());
        expense.setDate(updatedExpense.getDate());

        return expenseRepository.save(expense);
    }

    // ===== DELETE expense =====
    @DeleteMapping("/{id}")
    public void deleteExpense(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        User user = getUserFromToken(authHeader);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expense.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        expenseRepository.delete(expense);
    }

    // ===== Expense Summary (total, today, this month) =====
    @GetMapping("/summary-expense")
    public Map<String, Double> getExpenseSummary(@RequestHeader("Authorization") String authHeader) {
        User user = getUserFromToken(authHeader);
        Map<String, Double> summary = new HashMap<>();

        LocalDate todayDate = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();

        double total = expenseRepository.getTotalExpense(user);
        double today = expenseRepository.getTodayExpense(user, todayDate);
        double thisMonth = expenseRepository.getMonthlyExpense(user, currentMonth.getMonthValue(), currentMonth.getYear());

        summary.put("total", total);
        summary.put("today", today);
        summary.put("thisMonth", thisMonth);

        return summary;
    }

    // ===== Filter expenses =====
    @GetMapping("/filter")
    public List<Expense> filterExpenses(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "All") String category,
            @RequestParam(required = false) String title) {

        User user = getUserFromToken(authHeader);
        return expenseRepository.filterExpenses(user, category, title);
    }

    // ===== Helper to extract user from JWT =====
    private User getUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        String email = jwtUtils.extractUsername(token); // <-- use extractUsername


        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    @GetMapping("/category-summary")
    public Map<String, Double> getCategorySummary(
            @RequestHeader("Authorization") String authHeader) {

        User user = getUserFromToken(authHeader);
        Map<String, Double> summary = new HashMap<>();

        List<Object[]> results = expenseRepository.getExpenseByCategory(user);

        for (Object[] row : results) {
            summary.put((String) row[0], (Double) row[1]);
        }
        return summary;
    }
    @GetMapping("/budget-check")
    public Map<String, Object> checkMonthlyBudget(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Double budget) {

        User user = getUserFromToken(authHeader);
        Map<String, Object> response = new HashMap<>();

        YearMonth now = YearMonth.now();
        Double spent = expenseRepository.getMonthlyExpense(
                user, now.getMonthValue(), now.getYear());

        response.put("budget", budget);
        response.put("spent", spent);

        if (spent > budget) {
            response.put("status", "LIMIT_EXCEEDED");
            response.put("overBy", spent - budget);
        } else {
            response.put("status", "WITHIN_LIMIT");
            response.put("remaining", budget - spent);
        }
        return response;
    }

}
