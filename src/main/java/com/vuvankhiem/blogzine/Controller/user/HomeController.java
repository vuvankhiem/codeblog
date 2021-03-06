package com.vuvankhiem.blogzine.Controller.user;

import com.vuvankhiem.blogzine.Common.Common;
import com.vuvankhiem.blogzine.DTO.PostDTO;
import com.vuvankhiem.blogzine.Entity.Post;
import com.vuvankhiem.blogzine.Entity.Subscriber;
import com.vuvankhiem.blogzine.Service.user.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;

@Controller
public class HomeController extends Common {

    static List<Post> st_postList;
    static String st_code;
    static Subscriber st_subscriber;
    static String tb = "";

    @Autowired
    @Qualifier("homeServiceImpl")
    HomeService homeService;

    @Autowired
    JavaMailSender javaMailSender;

    @GetMapping(value = {"/", "trang-chu"})
    public String homePage(Model model,
                           @ModelAttribute("subscriber") Subscriber subscriber,
                           HttpSession session) {

        String message = "";
        st_postList = homeService.getAllRandomPost();
        model.addAttribute("tb", tb);
        model.addAttribute("posts", homeService.getPostByPage(st_postList, 5, 5));
        model.addAttribute("postMostView", homeService.getTop15PostMostViewed());
        model.addAttribute("postSlides", homeService.getPostSildes());
        model.addAttribute("postLastest", homeService.getTop8PostLastest());
        session.setAttribute("categories", homeService.getAllCategories());
        session.setAttribute("tags", homeService.getAllTag());
        session.setAttribute("top4PostsLastest", homeService.getTop4PostLastest());
        tb = "";
        return "us/index";
    }

    @ResponseBody
    @GetMapping("/api/load-more/{index}")
    public List<PostDTO> loadMore(@PathVariable(value = "index") int index) {
        return homeService.getPostByPage(st_postList, index, 5);
    }

    @PostMapping("/subscriber")
    public String subscribe(@Valid @ModelAttribute("subscriber") Subscriber subscriber,
                            BindingResult bindingResult,
                            HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            List<ObjectError> errors = bindingResult.getAllErrors();
            for (ObjectError error : errors
                 ) {
                tb = error.getDefaultMessage();
            }
            return "redirect:/";
        }
        if (homeService.existsSubscriberBySubscriberEmail(subscriber.getSubscriberEmail())) {
            tb = "T??i kho???n c???a b???n ???? ???????c ????ng k?? tr?????c ????";
            return "redirect:/";
        }

        st_subscriber = subscriber;
        st_code = super.randomCode();

        String confirm_link = "https://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/subscriber/verify/" + st_code;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(subscriber.getSubscriberEmail());
        message.setSubject("X??c th???c t??i kho???n ????ng k?? nh???n tin t??? BlogZine");
        message.setText("Click link : " + confirm_link + " ????? ho??n t???t qu?? tr??nh ????ng k?? nh???n tin cho t??i kho???n Email c???a b???n.");
        javaMailSender.send(message);
        tb = "????? ho??n t???t vi???c ????ng k??. B???n c???n ph???i x??c th???c t??i kho???n c???a b???n trong Gmail.";
        return "redirect:/";

    }

    @GetMapping("/subscriber/verify/{code}")
    public String verifySubscriber(@PathVariable String code) {
        if (!st_code.equals(code)) {
            tb = "X??c th???c t??i kho???n th???t b???i";
            return "redirect:/";
        }
        homeService.saveSubscriber(st_subscriber);
        return "redirect:/";
    }

}
