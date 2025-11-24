package org.openl.rules.rest.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;

import org.openl.studio.common.model.GenericView;

public class UserInfoModel {

    @Email(message = "{openl.constraints.user.email.format.message}")
    @Size(max = 254, message = "{openl.constraints.size.max.message}")
    @Parameter(description = "User e-mail", example = "test@test")
    @JsonView({GenericView.Full.class, View.Short.class})
    private String email;

    @Size(max = 64, message = "{openl.constraints.size.max.message}")
    @Parameter(description = "User display name", example = "John Doe")
    @JsonView({GenericView.Full.class, View.Short.class})
    private String displayName;

    @Size(max = 25, message = "{openl.constraints.size.max.message}")
    @Parameter(description = "User first name", example = "John")
    @JsonView(GenericView.Full.class)
    private String firstName;

    @Size(max = 25, message = "{openl.constraints.size.max.message}")
    @Parameter(description = "User last name", example = "Doe")
    @JsonView(GenericView.Full.class)
    private String lastName;

    public String getEmail() {
        return email;
    }

    public UserInfoModel setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserInfoModel setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public UserInfoModel setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public UserInfoModel setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public static final class View {
        private View() {
        }

        public interface Short {
        }
    }
}
