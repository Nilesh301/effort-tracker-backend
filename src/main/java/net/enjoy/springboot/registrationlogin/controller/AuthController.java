package net.enjoy.springboot.registrationlogin.controller;

import jakarta.validation.Valid;
import net.enjoy.springboot.registrationlogin.dto.UserDto;
import net.enjoy.springboot.registrationlogin.entity.User;
import net.enjoy.springboot.registrationlogin.repository.UserRepository;
import net.enjoy.springboot.registrationlogin.service.EmailService;
import net.enjoy.springboot.registrationlogin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // new code start
    @Autowired
    private UserRepository userRepository;

  //  @Autowired
  //  private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // new code end

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    // handler method to handle home page request
    @GetMapping("/index")
    public String home() {
        return "index";
    }

    // handler method to handle user registration form request
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        // create model object to store form data
        UserDto user = new UserDto();
        model.addAttribute("user", user);
        return "register";
    }

    // handler method to handle user registration form submit request
    @PostMapping("/register/save")
    public String registration(@Valid @ModelAttribute("user") UserDto userDto,
                               BindingResult result,
                               Model model) {
        User existingUser = userService.findUserByEmail(userDto.getEmail());

        if (existingUser != null && existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()) {
            result.rejectValue("email", null,
                    "There is already an account registered with the same email");
        }

        if (result.hasErrors()) {
            model.addAttribute("user", userDto);
            return "/register";
        }

        userService.saveUser(userDto);
        return "redirect:/register?success";
    }

    // handler method to handle list of users
/*    @GetMapping("/users")
    public String users(Model model) {
        List<UserDto> users = userService.findAllUsers();
        model.addAttribute("users", users);
        return "users";
    }*/

    // handler method to handle login request
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                        @RequestParam String dob,
                                        Model model) {

        User user = userRepository.findByEmail(email);

        if (user == null) {
            model.addAttribute("error", "Invalid Email");
            return "forgot-password";
        }

        // ✅ FIX: handle null DOB
        if (user.getDob() == null) {
            model.addAttribute("error", "DOB not set for this user. Contact Admin.");
            return "forgot-password";
        }

        LocalDate inputDob = LocalDate.parse(dob);

        if (!user.getDob().equals(inputDob)) {
            model.addAttribute("error", "DOB does not match");
            return "forgot-password";
        }

        return "redirect:/reset-password?email=" + email;
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String email,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       Model model) {

        // ✅ 1. Password length check (ADD HERE)
        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters");
            model.addAttribute("email", email);
            return "reset-password";
        }

        // ✅ 2. Confirm password match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            model.addAttribute("email", email);
            return "reset-password";
        }

        // ✅ 3. Fetch user
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return "redirect:/forgot-password?error";
        }

        // ✅ 4. Save encoded password
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        return "redirect:/login?resetSuccess";
    }

   //  new code start
   @GetMapping("/dashboard")
   public String dashboard(Authentication authentication) {

       String role = authentication.getAuthorities().toString();

       if (role.contains("ADMIN")) {
           return "redirect:/users";
       } else {
           return "redirect:/efforts";
       }
   }

    //  new code end

}