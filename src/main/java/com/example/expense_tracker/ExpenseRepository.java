package com.example.expense_tracker;

import com.example.expense_tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {


    // ===== Get all expenses of a user =====
    List<Expense> findByUser(User user);

    // ===== Summary queries =====
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user")
    double getTotalExpense(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user = :user AND e.date = :date")
    double getTodayExpense(@Param("user") User user,
                           @Param("date") LocalDate date);

    @Query("""
           SELECT COALESCE(SUM(e.amount), 0)
           FROM Expense e
           WHERE e.user = :user
           AND MONTH(e.date) = :month
           AND YEAR(e.date) = :year
           """)
    double getMonthlyExpense(@Param("user") User user,
                             @Param("month") int month,
                             @Param("year") int year);

    // ===== Filter expenses =====
    @Query("""
           SELECT e FROM Expense e
           WHERE e.user = :user
           AND (:category = 'All' OR e.category = :category)
           AND (:title IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :title, '%')))
           """)
    List<Expense> filterExpenses(@Param("user") User user,
                                 @Param("category") String category,
                                 @Param("title") String title);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user = :user GROUP BY e.category")
    List<Object[]> getExpenseByCategory(@Param("user") User user);


}
