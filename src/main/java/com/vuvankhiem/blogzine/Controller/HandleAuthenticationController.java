package com.vuvankhiem.blogzine.Controller;

import com.restfb.types.User;
import com.vuvankhiem.blogzine.Common.Common;
import com.vuvankhiem.blogzine.Common.Outh2.Facebook.RestFB;
import com.vuvankhiem.blogzine.Common.Outh2.Google.GooglePojo;
import com.vuvankhiem.blogzine.Common.Outh2.Google.RestGG;
import com.vuvankhiem.blogzine.Common.SecurityUtil;
import com.vuvankhiem.blogzine.DTO.AccountDTO;
import com.vuvankhiem.blogzine.DTO.AccountDTO_;
import com.vuvankhiem.blogzine.Entity.Account;
import com.vuvankhiem.blogzine.Service.user.HandleAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Random;

@Controller
public class HandleAuthenticationController extends Common {

    static String st_code;
    static AccountDTO st_account;
    static boolean check = true;
    static AccountDTO_ st_account_;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    HandleAuthenticationService handleAuthenticationService;

    @Autowired
    JavaMailSender javaMailSender;

    @Autowired
    RestGG restGg;

    @Autowired
    RestFB restFb;

    /*----------------------------------------------------------REGISTER-------------------------------------------------------------------*/
    //Register page
    @GetMapping("/dang-ki")
    public String signUpPage(@ModelAttribute("account") AccountDTO account,
                             Model model) {
        model.addAttribute("isLogin", false);
        return "us/signIn_signUp_page";
    }

    //Register an account
    @PostMapping("/dang-ki")
    public String addUser(@Valid @ModelAttribute("account") AccountDTO account,
                          BindingResult bindingResult,
                          Model model,
                          HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isLogin", false);
            return "us/signIn_signUp_page";
        }
        String confirm_password = request.getParameter("confirm_password");
        boolean checkExistUsername = handleAuthenticationService.existsAccount(account.getUsername());
        boolean chechExistEmail = handleAuthenticationService.existsAccount(account.getEmail());
        if (chechExistEmail) {
            model.addAttribute("err", "T??i kho???n c???a b???n ???? ???????c ????ng k?? tr?????c ????");
            model.addAttribute("isLogin", false);
            return "us/signIn_signUp_page";
        }
        if (checkExistUsername) {
            model.addAttribute("err", "Username ???? t???n t???i");
            model.addAttribute("isLogin", false);
            return "us/signIn_signUp_page";
        }
        if (!confirm_password.equals(account.getPassword())) {
            model.addAttribute("err", "M???t kh???u kh??ng kh???p");
            model.addAttribute("isLogin", false);
            return "us/signIn_signUp_page";
        }
        st_code = super.randomCode();
        if (check == false) {
            model.addAttribute("tb", "Vui l??ng x??c th???c t??i kho???n c???a b???n trong Gmail.");
            model.addAttribute("isLogin", false);
            return "us/signIn_signUp_page";
        }
        st_account = account;
        check = false;
        String confirm_link = "https://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/dang-ki/xac-thuc/" + st_code;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(account.getEmail());
        message.setSubject("X??c minh t??i kho???n BlogZine");
        message.setText("B???n ??ang th???c hi???n qu?? tr??nh ????ng k?? t??i kho???n t???i BlogZine. Click v??o li??n k???t d?????i ????y ????? ho??n t???t qu?? tr??nh ????ng k??." + "\nClick link : " + confirm_link);
        javaMailSender.send(message);
        model.addAttribute("tb", "Th??ng tin t??i kho???n c???a b???n ???? ???????c h??? th??ng l??u l???i. ????? ho??n t???t qu?? tr??nh ????ng k??, b???n c???n ph???i truy c???p v??o gmail ????? ho??n t???t qu?? tr??nh x??c th???c");
        model.addAttribute("isLogin", true);
        return "us/signIn_signUp_page";
    }

    //verify new account
    @GetMapping("/dang-ki/xac-thuc/{code}")
    public String verifyNewAccount(@PathVariable String code) {
        if (code.equals(st_code)) {
            Random random = new Random();
            String arr[] = {"/us/assets/images/avatar/actor.png", "/us/assets/images/avatar/actress.png"};
            Account account = new Account();
            account.setAvatar(arr[random.nextInt(2)]);
            account.setUsername(st_account.getUsername());
            account.setPassword(passwordEncoder.encode(st_account.getPassword()));
            account.setEmail(st_account.getEmail());
            account.setFullName(st_account.getFullName());
            handleAuthenticationService.saveAccount(account);
            st_code = null;
            st_account = null;
            check = true;
            return "redirect:/dang-nhap";
        }
        return "redirect:/";
    }

    /*----------------------------------------------------------LOGIN-------------------------------------------------------------------*/

    //Login page
    @GetMapping("/dang-nhap")
    public String loginPage(@ModelAttribute("account") AccountDTO account,
                            Model model,
                            @RequestParam(name = "err", required = false) String err) {
        if (err != null) {
            model.addAttribute("err", "T??n ho???c m???t kh???u ????ng nh???p sai.");
        }
        model.addAttribute("isLogin", true);
        return "us/signIn_signUp_page";
    }

    // Login with Google account
    @GetMapping("/dang-nhap-google")
    public String loginWithGoogle( @ModelAttribute("account") AccountDTO acc,
                                   @RequestParam(name = "code", required = false) String code,
                                   Model model) throws IOException {
        String token = restGg.getToken(code);
        GooglePojo googlePojo = restGg.getUserInfo(token);
        String auth_provider = "GOOGLE";
        String email = googlePojo.getEmail();
        Account account = handleAuthenticationService.getAccountByUsernameOrEmail(email, auth_provider);
        if (account == null) {
            Account newAccount = new Account();
            newAccount.setEmail(email);
            newAccount.setAuth_provider(auth_provider);
            newAccount.setFullName(googlePojo.getName());
            newAccount.setAvatar(googlePojo.getPicture());
            newAccount.setUsername(googlePojo.getId());
            handleAuthenticationService.saveAccount(newAccount);
            account = handleAuthenticationService.getAccountByUsernameOrEmail(email, auth_provider);
        } else {
            if (!account.isActive()) {
                model.addAttribute("isLogin", true);
                model.addAttribute("err", "T??i kho???n c???a b???n hi???n th???i ??ang b??? kh??a");
                return "us/signIn_signUp_page";
            }
        }
        UserDetails userDetails = restGg.userDetails(account);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        return "redirect:/login-success";
    }

    //Login with Facebook account//
    @GetMapping("/dang-nhap-facebook")
    public String loginWithFacebook(
            @ModelAttribute("account") AccountDTO account,
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(required = false) String error_code,
            @RequestParam(required = false) String error_message,
            Model model) throws IOException {

        if (error_code != null) {
            model.addAttribute("isLogin", true);
            model.addAttribute("err", error_message);
            return "us/signIn_signUp_page";
        }

        String token = restFb.getToken(code);
        User user = restFb.getUserInfo(token);
        String usernameOrEmail = user.getEmail() == null ? user.getId() : user.getEmail();
        String auth_provider = "FACEBOOK";
        String avatar = "https://graph.facebook.com/v3.0/" + user.getId() + "/picture?type=large";
        Account acc = handleAuthenticationService.getAccountByUsernameOrEmail(usernameOrEmail, auth_provider);
        if (acc == null) {
            Account newAccount = new Account();
            if (user.getEmail() != null) {
                newAccount.setEmail(user.getEmail());
            }
            newAccount.setAvatar(avatar);
            newAccount.setAuth_provider(auth_provider);
            newAccount.setFullName(user.getName());
            newAccount.setUsername(user.getId());
            handleAuthenticationService.saveAccount(newAccount);
            acc = handleAuthenticationService.getAccountByUsernameOrEmail(usernameOrEmail, auth_provider);
        } else {
            acc.setAvatar(avatar);
            if (!acc.isActive()) {
                model.addAttribute("isLogin", true);
                model.addAttribute("err", "T??i kho???n c???a b???n hi???n th???i ??ang b??? kh??a");
                return "us/signIn_signUp_page";
            }
        }
        UserDetails userDetails = restFb.userDetails(acc);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        return "redirect:login-success";
    }

    //Login success
    @GetMapping("/login-success")
    public String loginSuccess(HttpSession session, HttpServletRequest request) {
        session.setAttribute("avatarUser", SecurityUtil.getPrincipal().getAvatar());
        session.setAttribute("fullNameUser", SecurityUtil.getPrincipal().getFullName());
        session.setAttribute("accountID", SecurityUtil.getPrincipal().getAccountId());
        if (request.isUserInRole("ROLE_ADMIN"))
            return "redirect:/admin/";
        return "redirect:/";
    }

    /*----------------------------------------------------------FORGOT PASSWORD-------------------------------------------------------------------*/
    @GetMapping("/quen-mat-khau")
    public String forgotPasswordPage(@ModelAttribute("account") AccountDTO_ account, Model model) {
        return "us/forgot-password";
    }

    @PostMapping("/quen-mat-khau")
    public String changePassword(@Valid @ModelAttribute("account") AccountDTO_ account,
                                 BindingResult bindingResult,
                                 Model model,
                                 HttpServletRequest request) {

        String confirm_pass = request.getParameter("confirm_password");
        Account acc = handleAuthenticationService.getAccountByUsernameOrEmail(account.getEmail(), "WEB");
        if (acc == null) {
            model.addAttribute("err", "T??i kho???n c???a b???n ch??a ???????c ????ng k?? tr?????c ????");
            return "us/forgot-password";
        }
        if (bindingResult.hasErrors()) {
            return "us/forgot-password";
        }
        if (!account.getPassword().equals(confirm_pass)) {
            model.addAttribute("err", "M???t kh???u x??c th???c b???n nh???p kh??ng kh???p !");
            return "us/forgot-password";
        }
        if (check == false) {
            model.addAttribute("err", "Vui l??ng x??c th???c t??i kho???n c???a b???n trong Gmail.");
            return "us/signIn_signUp_page";
        }
        st_account_ = account;
        st_code = super.randomCode();
        check = false;
        String confirm_link = "https://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/quen-mat-khau/xac-thuc/" + st_code;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(account.getEmail());
        message.setSubject("X??c minh t??i kho???n BlogZine");
        message.setText("B???n ??ang th???c hi???n qu?? tr??nh thay ?????i m???t kh???u cho t??i kho???n t???i BlogZine. Click v??o li??n k???t d?????i ????y ????? ho??n t???t qu?? tr??nh thay ?????i m???t kh???u." + "\nClick link : " + confirm_link);
        javaMailSender.send(message);
        return "us/forgot-password";
    }

    @GetMapping("/quen-mat-khau/xac-thuc/{code}")
    public String verifyAccount(@ModelAttribute("account") AccountDTO_ account,
                                @PathVariable String code,
                                Model model) {
        if (!code.equals(st_code)) {
            model.addAttribute("err", "X??c th???c Email th???t b???i !");
            return "us/forgot-password";
        }
        Account acc = handleAuthenticationService.getAccountByUsernameOrEmail(st_account_.getEmail(), "WEB");
        acc.setPassword(passwordEncoder.encode(st_account_.getPassword()));
        handleAuthenticationService.saveAccount(acc);
        return "redirect:/dang-nhap";
    }

}
