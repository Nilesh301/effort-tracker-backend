package net.enjoy.springboot.registrationlogin.controller;

import net.enjoy.springboot.registrationlogin.entity.Effort;
import net.enjoy.springboot.registrationlogin.entity.User;
import net.enjoy.springboot.registrationlogin.repository.EffortRepository;
import net.enjoy.springboot.registrationlogin.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class AdminController {

    private final UserRepository userRepository;
    private final EffortRepository effortRepository;  // <-- add this

    // Constructor now includes EffortRepository
    public AdminController(UserRepository userRepository, EffortRepository effortRepository) {
        this.userRepository = userRepository;
        this.effortRepository = effortRepository;
    }

    // existing /users mapping (unchanged)
    @GetMapping("/users")
    public String listUsers(Model model, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);
        String userName = (user != null && user.getName() != null) ? user.getName() : email;
        model.addAttribute("loggedInUser", userName);
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "users";
    }

    // ========== NEW: Show export page with searchable user list ==========
    @GetMapping("/admin/export")
    @PreAuthorize("hasRole('ADMIN')")
    public String showExportPage(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);

        String email = userDetails.getUsername();
        User loggedInUser = userRepository.findByEmail(email);
        String userName = (loggedInUser != null && loggedInUser.getName() != null) ? loggedInUser.getName() : email;
        model.addAttribute("loggedInUser", userName);

        return "admin-export";
    }

    // ========== NEW: Generate Excel file for selected user & date range ==========
    @GetMapping("/admin/export/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportEffortsToExcel(@RequestParam List<Long> userIds,
                                     @RequestParam String startDate,
                                     @RequestParam String endDate,
                                     HttpServletResponse response) throws IOException {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        // Fetch efforts for all selected users
        List<Effort> efforts = effortRepository.findByUserIdsAndDateBetween(userIds, start, end);

        // Fetch all selected users (to display names in Excel)
        List<User> selectedUsers = userRepository.findAllById(userIds);

        // Create Excel workbook
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Efforts");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Header row
        String[] columns = {"User", "Date", "Category", "Task Name", "Description", "Hours", "Minutes"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(i);
        }

        // Data rows
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Effort e : efforts) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(e.getUser().getName());
            row.createCell(1).setCellValue(e.getDate().format(formatter));
            row.createCell(2).setCellValue(e.getCategory() != null ? e.getCategory() : "");
            row.createCell(3).setCellValue(e.getTaskName() != null ? e.getTaskName() : "");
            row.createCell(4).setCellValue(e.getDescription() != null ? e.getDescription() : "");
            row.createCell(5).setCellValue(e.getHours());
            row.createCell(6).setCellValue(e.getMinutes());
        }

        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Build filename with date range and user count
        String filename = String.format("efforts_%d_users_%s_to_%s.xlsx", selectedUsers.size(), start, end);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}