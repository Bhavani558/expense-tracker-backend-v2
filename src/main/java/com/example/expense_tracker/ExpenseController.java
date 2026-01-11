package com.example.expense_tracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;


@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @PostMapping
    public Expense addExpense(@RequestBody Expense expense) {
        return expenseRepository.save(expense);
    }

    @GetMapping
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public void deleteExpense(@PathVariable Long id) {
        expenseRepository.deleteById(id);
    }
    @PutMapping("/{id}")
    public Expense updateExpense(@PathVariable Long id, @RequestBody Expense updatedExpense) {

        Expense existingExpense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with id " + id));

        existingExpense.setTitle(updatedExpense.getTitle());
        existingExpense.setAmount(updatedExpense.getAmount());
        existingExpense.setDescription(updatedExpense.getDescription());
        existingExpense.setCategory(updatedExpense.getCategory());
        existingExpense.setDate(updatedExpense.getDate());

        return expenseRepository.save(existingExpense);
    }
    @GetMapping("/summary-expense")
    public Map<String, Double> getExpenseSummary() {
        Map<String, Double> summary = new HashMap<>();

        List<Expense> expenses = expenseRepository.findAll();

        double total = 0;
        double today = 0;
        double thisMonth = 0;

        LocalDate todayDate = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();

        for (Expense e : expenses) {
            if (e.getDate() == null) continue; // ✅ only date check

            total += e.getAmount();

            if (e.getDate().isEqual(todayDate)) {
                today += e.getAmount();
            }

            if (YearMonth.from(e.getDate()).equals(currentMonth)) {
                thisMonth += e.getAmount();
            }
        }

        summary.put("total", total);
        summary.put("today", today);
        summary.put("thisMonth", thisMonth);

        return summary;
    }
    @GetMapping("/filter")
    public List<Expense> filterExpenses(
            @RequestParam(defaultValue = "All") String category,
            @RequestParam(required = false) String title) {

        return expenseRepository.filterExpenses(category, title);
    }
    @GetMapping("/test")
    public String testEndpoint() {
        return "Backend is live!";
    }

}


