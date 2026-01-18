package com.example.expense_tracker;

import com.example.expense_tracker.model.User;
import com.example.expense_tracker.repository.UserRepository;
import com.example.expense_tracker.security.JwtUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public AnalyticsController(
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            JwtUtils jwtUtils
    ) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    // ===== SUMMARY =====
    @GetMapping("/summary")
    public Map<String, Double> getExpenseSummary(
            @RequestHeader("Authorization") String authHeader
    ) {
        User user = getUserFromToken(authHeader);

        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.now();

        Map<String, Double> summary = new HashMap<>();
        summary.put("total", expenseRepository.getTotalExpense(user));
        summary.put("today", expenseRepository.getTodayExpense(user, today));
        summary.put("thisMonth",
                expenseRepository.getMonthlyExpense(
                        user,
                        month.getMonthValue(),
                        month.getYear()
                ));

        return summary;
    }

    // ===== CATEGORY SUMMARY =====
    @GetMapping("/category-summary")
    public Map<String, Double> getCategorySummary(
            @RequestHeader("Authorization") String authHeader
    ) {
        User user = getUserFromToken(authHeader);
        Map<String, Double> result = new HashMap<>();

        List<Object[]> rows = expenseRepository.getExpenseByCategory(user);
        for (Object[] row : rows) {
            result.put((String) row[0], (Double) row[1]);
        }
        return result;
    }

    // ===== BUDGET CHECK =====
    @GetMapping("/budget-check")
    public Map<String, Object> checkBudget(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Double budget
    ) {
        User user = getUserFromToken(authHeader);
        YearMonth month = YearMonth.now();

        double spent = expenseRepository.getMonthlyExpense(
                user,
                month.getMonthValue(),
                month.getYear()
        );

        Map<String, Object> response = new HashMap<>();
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

    // ===== JWT → USER =====
    private User getUserFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing Authorization header");
        }

        String token = authHeader.substring(7);
        String email = jwtUtils.extractUsername(token); // <-- use extractUsername


        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
