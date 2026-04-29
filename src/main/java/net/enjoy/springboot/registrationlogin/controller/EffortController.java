package net.enjoy.springboot.registrationlogin.controller;

import jakarta.servlet.http.HttpServletResponse;
import net.enjoy.springboot.registrationlogin.entity.Effort;
import net.enjoy.springboot.registrationlogin.entity.User;
import net.enjoy.springboot.registrationlogin.repository.EffortRepository;
import net.enjoy.springboot.registrationlogin.repository.UserRepository;
import net.enjoy.springboot.registrationlogin.service.EffortService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/efforts")
public class EffortController {

    private final EffortService effortService;
    private final UserRepository userRepository;
    private final EffortRepository effortRepository;

    public EffortController(EffortService effortService,
                            UserRepository userRepository,
                            EffortRepository effortRepository) {
        this.effortService = effortService;
        this.userRepository = userRepository;
        this.effortRepository = effortRepository;
    }

    // ================= VIEW PAGE =================
    @GetMapping
    public String viewEfforts(Model model,
                              Authentication authentication,
                              @RequestParam(defaultValue = "0") int page) {

        String email = authentication.getName();
        String role = authentication.getAuthorities().toString();

        Pageable pageable = PageRequest.of(page, 5);

        User user = userRepository.findByEmail(email);

        String userName = (user != null && user.getName() != null && !user.getName().isEmpty())
                ? user.getName()
                : email;

        Page<Effort> effortPage;

        if (role.contains("ADMIN")) {
            effortPage = effortRepository.findAll(pageable);
        } else {
            effortPage = effortRepository.findByUser(user, pageable);
        }

        model.addAttribute("loggedInUser", userName);
        model.addAttribute("efforts", effortPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", effortPage.getTotalPages());

        return "efforts";
    }

    // ================= ADD EFFORT =================
    @PostMapping("/add")
    public String addEffort(@ModelAttribute Effort effort,
                            Authentication authentication,
                            Model model) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        LocalDate selectedDate = effort.getDate();

        if (selectedDate == null) {
            model.addAttribute("error", "Please select a date");
            return loadEfforts(model, authentication, 0);
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (!(selectedDate.equals(today) || selectedDate.equals(yesterday))) {
            model.addAttribute("error", "Only today or yesterday allowed");
            return loadEfforts(model, authentication, 0);
        }

       /* Effort existing = effortRepository.findByUserAndDate(user, selectedDate);

        if (existing != null) {
            model.addAttribute("error", "Effort already added for this date");
            return loadEfforts(model, authentication, 0);
        }*/

        effort.setUser(user);
        effortRepository.save(effort);

        return "redirect:/efforts?success";
    }

    private String loadEfforts(Model model,
                               Authentication authentication,
                               int page) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        Pageable pageable = PageRequest.of(page, 5);

        Page<Effort> effortPage;

        String role = authentication.getAuthorities().toString();

        if (role.contains("ADMIN")) {
            effortPage = effortRepository.findAll(pageable);
        } else {
            effortPage = effortRepository.findByUser(user, pageable);
        }

        model.addAttribute("efforts", effortPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", effortPage.getTotalPages());
        model.addAttribute("loggedInUser", user != null ? user.getName() : email);

        return "efforts";
    }

    @GetMapping("/user/{id}")
    public String viewUserEfforts(@PathVariable Long id,
                                  Model model,
                                  Authentication authentication,
                                  @RequestParam(defaultValue = "0") int page) {

        User selectedUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        Pageable pageable = PageRequest.of(page, 5);
        Page<Effort> effortPage = effortRepository.findByUser(selectedUser, pageable);

        // ✅ Ensure the content list does not contain nulls
        List<Effort> efforts = effortPage.getContent().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String email = authentication.getName();
        User loggedIn = userRepository.findByEmail(email);

        model.addAttribute("loggedInUser",
                loggedIn != null ? loggedIn.getName() : email);
        model.addAttribute("efforts", efforts);          // Safe list
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", effortPage.getTotalPages());

        // Optional: log for debugging
        System.out.println("Efforts count: " + efforts.size());
        if (!efforts.isEmpty()) {
            System.out.println("First effort ID: " + efforts.get(0).getId());
        }

        return "efforts";
    }

    // ================= EDIT =================
    @GetMapping("/edit/{id}")
    public String editEffort(@PathVariable Long id, Model model) {

        Effort effort = effortRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Effort not found"));

        model.addAttribute("effort", effort);
        return "edit-effort";
    }

    @PostMapping("/update")
    public String updateEffort(@ModelAttribute Effort effort) {

        Effort existing = effortRepository.findById(effort.getId())
                .orElseThrow(() -> new RuntimeException("Effort not found"));

        existing.setTaskName(effort.getTaskName());
        existing.setDescription(effort.getDescription());
        existing.setMinutes(effort.getMinutes());
        existing.setHours(effort.getHours());

        effortRepository.save(existing);

        return "redirect:/efforts?updated";
    }

    // ================= DELETE =================
    @GetMapping("/delete/{id}")
    public String deleteEffort(@PathVariable Long id) {
        effortRepository.deleteById(id);
        return "redirect:/efforts?deleted";
    }

    // ================= EXPORT CSV =================
    @GetMapping("/export")
    public void exportToCSV(@RequestParam(required = false) String fromDate,
                            @RequestParam(required = false) String toDate,
                            HttpServletResponse response,
                            Authentication authentication) throws Exception {

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=efforts.csv");

        String email = authentication.getName();
        String role = authentication.getAuthorities().toString();

        LocalDate from = (fromDate != null && !fromDate.isEmpty()) ? LocalDate.parse(fromDate) : null;
        LocalDate to = (toDate != null && !toDate.isEmpty()) ? LocalDate.parse(toDate) : null;

        List<Effort> efforts;

        if (role.contains("ADMIN")) {
            efforts = (from != null && to != null)
                    ? effortRepository.findByDateBetween(from, to)
                    : effortRepository.findAll();
        } else {
            efforts = (from != null && to != null)
                    ? effortRepository.findByUserEmailAndDateBetween(email, from, to)
                    : effortService.getUserEfforts(email);
        }

        PrintWriter writer = response.getWriter();
        writer.println("Date,Task,Description,Minutes,Hours,User");

        for (Effort e : efforts) {
            writer.println(
                    e.getDate() + "," +
                            e.getTaskName() + "," +
                            e.getDescription() + "," +
                            e.getMinutes() + "," +
                            e.getHours() + "," +
                            (e.getUser() != null ? e.getUser().getName() : "Unknown")
            );
        }

        writer.flush();
        writer.close();
    }
}