package org.openl.rules.rest.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;

import org.openl.studio.common.model.GenericView;

public class UserInfoModel {

    @Email(message = "{openl.constraints.user.email.format.message}")
    @Getter
    @Size(max = 254, message = "{openl.constraints.size.max.message}")
    @Parameter(description = "User e-mail", example = "test@test")
    @JsonView({GenericView.Full.class, View.Short.class})
    private String email;

    @Getter
    @Size(max = 64, message = "{openl.constraints.size.max.message}")
    @Parameter(description = "User display name", example = "John Doe")
    @JsonView({GenericView.Full.class, View.Short.class})
    private String displayName;

    @Getter
    @Size(max = 25, message = "{openl.constraints.size.max.message}")
    @Parameter(description = "User first name", example = "John")
    @JsonView(GenericView.Full.class)
    private String firstName;

    @Getter
    @Size(max = 25, message = "{openl.constraints.size.max.message}")
    @Parameter(description = "User last name", example = "Doe")
    @JsonView(GenericView.Full.class)
    private String lastName;

    public UserInfoModel setEmail(String email) {
        this.email = email;
        return this;
    }

    public UserInfoModel setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public UserInfoModel setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
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
