package demo;

import demo.domain.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;

import static demo.SecurityRequestPostProcessors.csrf;
import static demo.SecurityRequestPostProcessors.userDetailsService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class UserTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private Filter springSecurityFilterChain;

    private MockMvc mvc;

    @Before
    public void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        // This is a workaround for an issue related to thymeleaf security dialect throwing error when accessing application context.
        // Refer to: http://stackoverflow.com/questions/24999469/how-to-unit-test-a-secured-controller-which-uses-thymeleaf-without-getting-temp
        context.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
        // End of workaround
    }

    @Test
    public void homeRequiresLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(redirectedUrl("http://localhost/signin"));
    }

    @Test
    public void loginWithInvalidUsernamePassword() throws Exception {
        RequestBuilder request = post("/signin")
                .param("username", "jeff")
                .param("password", "invalid")
                .with(csrf());

        mvc.perform(request)
                .andExpect(redirectedUrl("/signin?error"));
    }

    @Test
    public void loginNotAllowedForDisabledUser() throws Exception {
        RequestBuilder request = post("/signin")
                .param("username", "test")
                .param("password", "test1234")
                .with(csrf());

        mvc.perform(request)
                .andExpect(redirectedUrl("/signin?error"));
    }

    @Test
    public void loginRequiresCsrf() throws Exception {
        RequestBuilder request = post("/signin")
                .param("username", "jeff")
                .param("password", "test1234");

        mvc.perform(request)
                .andExpect(status().isForbidden());
    }

    @Test
    public void loginWithValidUsernamePassword() throws Exception {
        RequestBuilder request = post("/signin")
                .param("username", "jeff")
                .param("password", "test1234")
                .with(csrf());

        mvc.perform(request).andExpect(redirectedUrl("/"));
    }

    @Test
    public void logoutRequiresCsrf() throws Exception {
        mvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void logoutWithCsrf() throws Exception {
        mvc.perform(post("/logout").with(csrf()))
                .andExpect(redirectedUrl("/signin?logout"));
    }

    @Test
    public void signupPage() throws Exception {
        mvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("user", new User()));
    }

    @Test
    public void signupWithInvalidData() throws Exception {
        signupShouldFail("username", "");
        signupShouldFail("username", " a");
        signupShouldFail("username", "test1234_%");

        signupShouldFail("password", "");
        signupShouldFail("password", " test1");

        signupShouldFail("confirmPassword", "");

        signupShouldFail("firstName", " ");
        signupShouldFail("lastName", " ");

        signupShouldFail("email", "");
        signupShouldFail("email", "test");
    }

    private void signupShouldFail(String fieldName, String invalidValue) throws Exception {
        RequestBuilder request = post("/signup")
                .param(fieldName, invalidValue)
                .with(csrf());

        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasFieldErrors("user", fieldName));
    }

    @Test
    public void shouldRejectSignupIfConfirmPasswordNotMatch() throws Exception {
        RequestBuilder request = post("/signup")
                .param("username", "jeff1")
                .param("password", "test1234")
                .param("confirmPassword", "test12345")
                .param("firstName", "Jeff")
                .param("lastName", "Fang")
                .param("email", "sanlyfang@gmail.com")
                .with(csrf());

        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeHasErrors("user"));
    }

    @Test
    public void signupWithValidData() throws Exception {

    }

    @Test
    public void showUserProfileInHomepage() throws Exception {
        mvc.perform(get("/").with(userDetailsService("jeff")))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("user", new User("jeff")));
    }
}
