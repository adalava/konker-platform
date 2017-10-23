package com.konkerlabs.platform.registry.web.controllers;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import com.konkerlabs.platform.registry.business.model.Token;
import com.konkerlabs.platform.registry.business.model.User;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.business.services.api.TokenService;
import com.konkerlabs.platform.registry.business.services.api.UserService;

import com.konkerlabs.platform.registry.web.forms.UserForm;


@Controller()
@Scope("request")
@RequestMapping("/subscription")
@Profile("email")
public class UserSubscriptionController implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSubscriptionController.class);

    public enum Messages {
        USER_DOES_NOT_EXIST("controller.recover.user.does.not.exist"), USER_EMAIL_SUBJECT(
                "controller.recover.user.email.subject"), USER_CHANGE_PASSWORD_SUCCESS(
                        "controller.recover.user.success");

        public String getCode() {
            return code;
        }

        private String code;

        Messages(String code) {
            this.code = code;
        }
    }



    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;



    private ApplicationContext applicationContext;



    @RequestMapping(value = "/email", method = RequestMethod.POST)
    public  ModelAndView save(UserForm userForm, RedirectAttributes redirectAttributes, Locale locale) {
        
    	User userfromForm = userForm.toModel();      	

    	
        ServiceResponse<User> serviceResponse = userService.createAccount(userfromForm, userForm.getNewPassword(),
        		userForm.getNewPasswordConfirmation());
       
        
        if (serviceResponse.isOk()) {        	

        	User user = serviceResponse.getResult();
        	return new ModelAndView("redirect:/subscription/success");
        }else {
       	
        	return new ModelAndView("redirect:/subscription/fail");
        }
        

    }

 
    

    @RequestMapping(value = "/{token}", method = RequestMethod.GET)
    public ModelAndView showEmailValidationPage(@PathVariable("token") String token, Locale locale) {
        ServiceResponse<Token> serviceResponse = tokenService.getToken(token);
        ServiceResponse<Boolean> validToken = tokenService.isValidToken(token);


        if (!Optional.ofNullable(serviceResponse).isPresent()
                || !Optional.ofNullable(serviceResponse.getResult()).isPresent()) {

            List<String> messages = serviceResponse.getResponseMessages().entrySet().stream()
                    .map(message -> applicationContext.getMessage(message.getKey(), message.getValue(), locale))
                    .collect(Collectors.toList());

            return new ModelAndView("validate-email")
            		.addObject("user", User.builder().build())
                    .addObject("errors", messages)
                    .addObject("isExpired", true);
        }


        if (serviceResponse.getResult().getIsExpired() || !validToken.getResult()) {
            List<String> messages = new ArrayList<>();
            messages.add(applicationContext.getMessage(TokenService.Validations.EXPIRED_TOKEN.getCode(), null, locale));

            return new ModelAndView("validate-email")
            		.addObject("user", User.builder().build())
                    .addObject("errors", messages)
                    .addObject("isExpired", true);
        }


        ServiceResponse<User> responseUser = userService.findByEmail(serviceResponse.getResult().getUserEmail());
        
        User user = responseUser.getResult();        
        user.setActive(true);
       
        userService.save(user,"","");

        return new ModelAndView("validate-email")
        		.addObject("user",user)
        		.addObject("token", token);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}